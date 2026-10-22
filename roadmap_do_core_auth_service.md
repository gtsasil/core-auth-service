# 🛡️ Guia de Implementação: Projeto 2 - core-auth-service

**Tech Lead Notes:** O objetivo deste projeto é criar o `core-auth-service`, a base de segurança do nosso ecossistema. Como você vai codificar sem auto-complete ou IA, os caminhos de pastas, pacotes, métodos, contratos, construtores e regras de negócio estão detalhados passo a passo aqui. Siga a ordem rigorosamente.

---

## 🛠️ 1. Setup Base e Infraestrutura

* [x] **Criação do Projeto (Spring Initializr):**
  * Group: `com.gtsasil`
  * Artifact: `core-auth-service`
  * Java Version: 21
  * Spring Boot: 3.3.x+ (ou 4.x)
* [x] **Dependências do `pom.xml`:**
  * `spring-boot-starter-web` (ou `spring-boot-starter-webmvc`)
  * `spring-boot-starter-data-jpa`
  * `spring-boot-starter-security`
  * `spring-boot-starter-validation`
  * `java-jwt` (`com.auth0:java-jwt:4.4.0` ou superior)
  * `flyway-core` e `flyway-database-postgresql`
  * `postgresql` (runtime)
  * `jacoco-maven-plugin` (no `<build><plugins>` do pom.xml para cobertura de código)
* [x] **Docker Compose (`docker-compose.yml` na raiz do projeto):**
  * Container PostgreSQL: imagem `postgres:16-alpine`, porta `15432:5432`, `POSTGRES_USER=admin`, `POSTGRES_PASSWORD=admin`, `POSTGRES_DB=auth_db`.
  * Container SonarQube: imagem `sonarqube:lts-community`, porta `9000:9000`.
* [x] **`application.properties` (ou `.yml`):**
  * Configurar URL do banco: `spring.datasource.url=jdbc:postgresql://localhost:15432/auth_db`
  * Credenciais do banco (`spring.datasource.username=admin`, `spring.datasource.password=admin`).
  * Habilitar Flyway (`spring.flyway.enabled=true`).
  * Propriedade customizada para o secret do JWT: `api.security.token.secret=SUA_CHAVE_SECRETA_MUITO_FORTE_AQUI`

---

## 🗄️ 2. Migração de Banco de Dados (Flyway)

Usaremos `gen_random_uuid()` nativo do PostgreSQL para geração automática de UUIDs.

* [x] **Criar o arquivo de migração:**
  * Caminho exato: `src/main/resources/db/migration/V1__create_table_users.sql`
  * **Código SQL:**
    ```sql
    CREATE TABLE tb_users (
        id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
        email VARCHAR(255) UNIQUE NOT NULL,
        password VARCHAR(255) NOT NULL,
        role VARCHAR(50) NOT NULL
    );
    ```

---

## 📦 3. Modelagem de Dados e DTOs

Estrutura de pacotes:
* Enums: `com.gtsasil.core_auth_service.domain.user.enums`
* Entidades: `com.gtsasil.core_auth_service.domain.user.entity`
* DTOs: `com.gtsasil.core_auth_service.api.dto`

---

* [x] **`domain.user.enums.UserRole` (Enum):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/domain/user/enums/UserRole.java`
  * **Valores:** `ADMIN("admin")`, `USER("user")`.
  * **Atributo:** `private final String role;`
  * **Construtor:** `UserRole(String role) { this.role = role; }`
  * **Getter:** `public String getRole() { return role; }`

---

* [x] **`domain.user.entity.AppUser` (Entity):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/domain/user/entity/AppUser.java`
  * **Anotações da classe:** `@Entity`, `@Table(name = "tb_users")`.
  * **Interface implementada:** `org.springframework.security.core.userdetails.UserDetails`.
  * **Atributos:**
    * `@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;`
    * `private String email;`
    * `private String password;`
    * `@Enumerated(EnumType.STRING) private UserRole role;`
  * **Construtores:**
    * Construtor padrão sem argumentos: `public AppUser() {}` (obrigatório para o JPA).
    * Construtor com parâmetros para criação/registro: `public AppUser(String email, String password, UserRole role) { ... }`
  * **Getters e Setters:**
    * Gerar getters e setters para `id`, `email`, `password` e `role`.
  * **Métodos da interface `UserDetails`:**
    * `getAuthorities()`:
      * Se `this.role == UserRole.ADMIN`: retornar `List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))`.
      * Se `this.role == UserRole.USER`: retornar `List.of(new SimpleGrantedAuthority("ROLE_USER"))`.
    * `getUsername()`: retornar `this.email` (o identificador de login é o e-mail).
    * `getPassword()`: retornar `this.password`.
    * `isAccountNonExpired()`: retornar `true`.
    * `isAccountNonLocked()`: retornar `true`.
    * `isCredentialsNonExpired()`: retornar `true`.
    * `isEnabled()`: retornar `true`.

---

* [x] **`api.dto.RegisterDTO` (Record):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/api/dto/RegisterDTO.java`
  * **Atributos:**
    * `@NotBlank @Email String email`
    * `@NotBlank String password`
    * `@NotNull UserRole role`

---

* [x] **`api.dto.AuthDTO` (Record):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/api/dto/AuthDTO.java`
  * **Atributos:**
    * `@NotBlank @Email String email`
    * `@NotBlank String password`

---

* [x] **`api.dto.TokenResponseDTO` (Record):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/api/dto/TokenResponseDTO.java`
  * **Atributos:**
    * `String token`

---

## 🔐 4. O Motor de Segurança (Spring Security + JWT)

Pacote: `com.gtsasil.core_auth_service.infra.security`

---

* [x] **`infra.security.TokenService` (@Service):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/infra/security/TokenService.java`
  * **Injeção de propriedades:** `@Value("${api.security.token.secret}") private String secret;`
  * **Método `generateToken(AppUser user)`:**
    * Retorno: `String`
    * Criação do algoritmo: `Algorithm algorithm = Algorithm.HMAC256(secret);`
    * Montagem do JWT com `java-jwt`:
      ```java
      return JWT.create()
          .withIssuer("core-auth-service")
          .withSubject(user.getEmail())
          .withExpiresAt(genExpirationDate())
          .sign(algorithm);
      ```
    * Tratar exceção `JWTCreationException` lançando `RuntimeException("Erro ao gerar token JWT", exception)`.
  * **Método `validateToken(String token)`:**
    * Retorno: `String` (o e-mail/subject do usuário ou string vazia `""` se inválido).
    * Algoritmo: `Algorithm algorithm = Algorithm.HMAC256(secret);`
    * Validação:
      ```java
      return JWT.require(algorithm)
          .withIssuer("core-auth-service")
          .build()
          .verify(token)
          .getSubject();
      ```
    * Tratar exceção `JWTVerificationException` retornando `""` (string vazia).
  * **Método auxiliar `genExpirationDate()`:**
    * Retorno: `Instant`
    * Exemplo de validade de 2 horas: `LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"))`.

---

* [x] **`infra.security.SecurityFilter` (@Component extends `OncePerRequestFilter`):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/infra/security/SecurityFilter.java`
  * **Injeções:** `@Autowired private TokenService tokenService;` e `@Autowired private UserRepository userRepository;`.
  * **Método `doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`:**
    1. Recuperar o token chamando método auxiliar: `var token = this.recoverToken(request);`
    2. Se `token != null`:
       * Obter o login/email: `var login = tokenService.validateToken(token);`
       * Se `!login.isEmpty()`: buscar o usuário com `UserDetails user = userRepository.findByEmail(login);`
       * Criar o objeto de autenticação: `var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());`
       * Setar no contexto do Spring Security: `SecurityContextHolder.getContext().setAuthentication(authentication);`
    3. Continuar o fluxo da requisição: `filterChain.doFilter(request, response);` (sempre chamar ao final!).
  * **Método auxiliar `recoverToken(HttpServletRequest request)`:**
    * Obter header: `var authHeader = request.getHeader("Authorization");`
    * Se `authHeader == null` ou não começar com `"Bearer "`: retornar `null`.
    * Retornar o token limpo: `return authHeader.replace("Bearer ", "");`

---

* [x] **`infra.security.SecurityConfig` (@Configuration, @EnableWebSecurity):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/infra/security/SecurityConfig.java`
  * **Injeções:** `@Autowired private SecurityFilter securityFilter;`
  * **Bean `SecurityFilterChain securityFilterChain(HttpSecurity http)`:**
    * Desabilitar CSRF: `.csrf(csrf -> csrf.disable())`
    * Sessão Stateless: `.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`
    * Regras de autorização:
      ```java
      .authorizeHttpRequests(authorize -> authorize
          .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
          .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
          .anyRequest().authenticated()
      )
      ```
    * Adicionar filtro personalizado antes do filtro padrão do Spring:
      `.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)`
    * Retornar `http.build()`.
  * **Bean `AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)`:**
    * Retornar `authenticationConfiguration.getAuthenticationManager();`
  * **Bean `PasswordEncoder passwordEncoder()`:**
    * Retornar `new BCryptPasswordEncoder();`

---

## ⚙️ 5. Lógica de Negócio e Controladores

---

* [x] **`domain.user.repository.UserRepository` (Interface):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/domain/user/repository/UserRepository.java`
  * **Herança:** `extends JpaRepository<AppUser, UUID>`
  * **Método de consulta:** `UserDetails findByEmail(String email);`

---

* [x] **`domain.user.service.AuthService` (@Service implements `UserDetailsService`):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/domain/user/service/AuthService.java`
  * **Injeções:**
    * `@Autowired private UserRepository userRepository;`
  * **Método `loadUserByUsername(String username)` (da interface UserDetailsService):**
    * Retornar: `this.userRepository.findByEmail(username);`
  * **Método `register(RegisterDTO data, PasswordEncoder passwordEncoder)` (ou injetar PasswordEncoder no service):**
    * Verificar se já existe usuário: `if (this.userRepository.findByEmail(data.email()) != null) return false;` (ou lançar exceção de negócio).
    * Encriptar senha: `String encryptedPassword = passwordEncoder.encode(data.password());`
    * Instanciar novo usuário: `AppUser newUser = new AppUser(data.email(), encryptedPassword, data.role());`
    * Salvar no banco: `this.userRepository.save(newUser);`
    * Retornar sucesso.

---

* [ ] **`api.controller.AuthController` (@RestController, @RequestMapping("/auth")):**
  * **Caminho:** `src/main/java/com/gtsasil/core_auth_service/api/controller/AuthController.java`
  * **Injeções:**
    * `@Autowired private AuthenticationManager authenticationManager;`
    * `@Autowired private UserRepository userRepository;` (ou `AuthService`)
    * `@Autowired private TokenService tokenService;`
    * `@Autowired private PasswordEncoder passwordEncoder;`
  * **Endpoint `POST /auth/login`:**
    * Parâmetro: `@RequestBody @Valid AuthDTO data`
    * Criar token de autenticação: `var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());`
    * Autenticar via manager: `var auth = this.authenticationManager.authenticate(usernamePassword);`
    * Obter usuário autenticado: `var user = (AppUser) auth.getPrincipal();`
    * Gerar token JWT: `var token = tokenService.generateToken(user);`
    * Retornar: `ResponseEntity.ok(new TokenResponseDTO(token));`
  * **Endpoint `POST /auth/register`:**
    * Parâmetro: `@RequestBody @Valid RegisterDTO data`
    * Validar se e-mail já existe: `if (this.userRepository.findByEmail(data.email()) != null) return ResponseEntity.badRequest().build();`
    * Criptografar senha: `String encryptedPassword = this.passwordEncoder.encode(data.password());`
    * Criar entidade: `AppUser newUser = new AppUser(data.email(), encryptedPassword, data.role());`
    * Salvar no repositório: `this.userRepository.save(newUser);`
    * Retornar: `ResponseEntity.ok().build();`

---

## 🧪 6. Garantia de Qualidade e SonarQube

* [ ] **Testes (`src/test/java/...`):**
  * Teste unitário para `AuthService` / regras de autenticação: simular e-mail já existente (esperar falha) e fluxo de sucesso de registro.
* [ ] **Rodar Validações:**
  * Executar no terminal: `./mvnw clean verify`
  * Executar no terminal para enviar análise ao SonarQube local (porta 9000):
    ```bash
    ./mvnw sonar:sonar -Dsonar.projectKey=core-auth-service -Dsonar.host.url=http://localhost:9000 -Dsonar.login=SEU_TOKEN
    ```