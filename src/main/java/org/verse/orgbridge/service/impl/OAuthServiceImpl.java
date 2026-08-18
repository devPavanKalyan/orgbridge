package org.verse.orgbridge.service.impl;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.verse.orgbridge.exceptions.AuthenticationException;
import org.verse.orgbridge.exceptions.UserAlreadyExistsException;
import org.verse.orgbridge.jwt.JwtService;
import org.verse.orgbridge.model.Client;
import org.verse.orgbridge.records.auth.AuthResponse;
import org.verse.orgbridge.records.auth.LoginPayload;
import org.verse.orgbridge.records.auth.SignUpPayload;
import org.verse.orgbridge.repository.ClientRepository;
import org.verse.orgbridge.service.EmailService;
import org.verse.orgbridge.service.OAuthService;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class OAuthServiceImpl extends OAuthService {

    private final PasswordEncoder passwordEncoder;
    private final ClientRepository clientRepository;

    public OAuthServiceImpl(
            JwtService jwtService,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            ClientRepository clientRepository
    ) {
        super(jwtService, emailService);
        this.passwordEncoder = passwordEncoder;
        this.clientRepository = clientRepository;
    }

    // ---------------- LOGIN ----------------

    @Override
    public Mono<AuthResponse> login(LoginPayload request) {
        return clientRepository.findByUsername(request.getUsername())
                .switchIfEmpty(
                        Mono.error(new AuthenticationException("Invalid username or password"))
                )
                .flatMap(client -> {
                    if (!passwordEncoder.matches(request.getPassword(), client.getPassword())) {
                        return Mono.error(
                                new AuthenticationException("Invalid username or password")
                        );
                    }

                    // Generate OTP (example)
                    String otp = generateOtp();

                    return sendLoginOtpEmail(client, otp)
                            .thenReturn(AuthResponse.builder().build());
                });
    }

    // ---------------- SIGNUP ----------------

    @Override
    public Mono<AuthResponse> signup(SignUpPayload request) {

        Client client = Client.builder()
                .id(UUID.randomUUID().toString())
                .fullName(request.getFullName())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles("ROLE_USER")
                .emailVerified(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return clientRepository.save(client)
                .onErrorMap(
                        DuplicateKeyException.class,
                        ex -> new UserAlreadyExistsException("User already exists")
                )
                .flatMap(savedClient ->
                        sendSignupVerificationEmail(savedClient)
                                .thenReturn(AuthResponse.builder().build())
                );
    }

    // ---------------- EMAIL ENFORCEMENT ----------------

    @Override
    protected Mono<Void> sendSignupVerificationEmail(Client client) {
        String token = UUID.randomUUID().toString();

        String verificationLink =
                "https://orgbridge.app/verify?token=" + token;

        // save token in DB (recommended, not shown here)

        return emailService.sendVerificationEmail(
                client.getUsername(), // assuming username = email
                client.getFullName(),
                verificationLink
        );
    }

    @Override
    protected Mono<Void> sendLoginOtpEmail(Client client, String otp) {
        return emailService.sendLoginOtpEmail(
                client.getUsername(),
                client.getFullName(),
                otp
        );
    }

    // ---------------- UTIL ----------------

    private String generateOtp() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}
