package org.project.core.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.EmailValidator;
import org.project.core.dto.AuthRequest;
import org.project.core.model.Mail;
import org.project.core.model.Role;
import org.project.core.model.User;
import org.project.core.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AuthService {
    private final QueueProducer queueProducer;
    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public User register(AuthRequest signUpRequest) {
        if (!isValidEmail(signUpRequest.email())) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (userRepository.findByEmail(signUpRequest.email()).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }
        User user = new User();
        user.setEmail(signUpRequest.email());
        user.setRole(Role.USER);
        user.setPassword(passwordEncoder.encode(signUpRequest.password()));
        Mail mail = new Mail();
        mail.setToAddress(user.getEmail());
        mail.setMessage("You have been successfully registered!");
        queueProducer.sendMessage(mail);
        return userRepository.saveAndFlush(user);
    }

    public User authenticate(AuthRequest input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.email(),
                        input.password()
                )
        );

        return userRepository.findByEmail(input.email()).orElseThrow();
    }
    private boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }
}
