package com.gtsasil.core_auth_service.domain.user.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import com.gtsasil.core_auth_service.api.dto.RegisterDTO;
import com.gtsasil.core_auth_service.domain.user.entity.AppUser;
import com.gtsasil.core_auth_service.domain.user.repository.UserRepository;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userRepository.findByEmail(username);
    }

    public boolean register(RegisterDTO data) {
        if (this.userRepository.findByEmail(data.email()) != null) {
            return false; // Usuário já cadastrado
        }
        String encryptedPassword = this.passwordEncoder.encode(data.password());
        AppUser newUser = new AppUser(data.email(), encryptedPassword, data.role());
        this.userRepository.save(newUser);
        return true;
    }

}
