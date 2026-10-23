package com.gtsasil.core_auth_service.domain.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gtsasil.core_auth_service.api.dto.RegisterDTO;
import com.gtsasil.core_auth_service.domain.user.entity.AppUser;
import com.gtsasil.core_auth_service.domain.user.enums.UserRole;
import com.gtsasil.core_auth_service.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve registrar um usuário com sucesso quando o e-mail não existir")
    void shouldRegisterUserSuccessfully() {
        // Arrange
        RegisterDTO dto = new RegisterDTO("novo@teste.com", "senha123", UserRole.USER);
        when(userRepository.findByEmail(dto.email())).thenReturn(null);
        when(passwordEncoder.encode(dto.password())).thenReturn("senhaCriptografada");

        // Act
        boolean result = authService.register(dto);

        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Deve falhar ao registrar usuário quando o e-mail já estiver cadastrado")
    void shouldNotRegisterUserWhenEmailAlreadyExists() {
        // Arrange
        RegisterDTO dto = new RegisterDTO("existente@teste.com", "senha123", UserRole.USER);
        AppUser existingUser = new AppUser("existente@teste.com", "senhaCripto", UserRole.USER);
        when(userRepository.findByEmail(dto.email())).thenReturn(existingUser);

        // Act
        boolean result = authService.register(dto);

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Deve carregar usuário por username com sucesso")
    void shouldLoadUserByUsername() {
        // Arrange
        String email = "usuario@teste.com";
        AppUser user = new AppUser(email, "senha", UserRole.USER);
        when(userRepository.findByEmail(email)).thenReturn(user);

        // Act
        UserDetails result = authService.loadUserByUsername(email);

        // Assert
        assertNotNull(result);
        assertEquals(email, result.getUsername());
    }
}
