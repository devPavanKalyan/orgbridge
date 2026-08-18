package org.verse.orgbridge.service;

import org.verse.orgbridge.jwt.JwtService;
import org.verse.orgbridge.model.Client;
import org.verse.orgbridge.records.auth.AuthResponse;
import org.verse.orgbridge.records.auth.LoginPayload;
import org.verse.orgbridge.records.auth.SignUpPayload;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class OAuthService {

    protected final JwtService jwtService;
    protected final EmailService emailService;

    protected OAuthService(JwtService jwtService, EmailService emailService) {
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    // ----- CONTRACT METHODS -----

    public abstract Mono<AuthResponse> login(LoginPayload request);

    public abstract Mono<AuthResponse> signup(SignUpPayload request);

    /**
     * Enforced: send verification email after signup
     */
    protected abstract Mono<Void> sendSignupVerificationEmail(Client client);

    /**
     * Enforced: send OTP email for login
     */
    protected abstract Mono<Void> sendLoginOtpEmail(Client client, String otp);

    // ----- SHARED LOGIC -----

    protected AuthResponse buildAuthResponse(Client client) {
        String accessToken = jwtService.generateToken(
                Map.of("roles", client.getRoleList()),
                client.getUsername()
        );

        String refreshToken = jwtService.generateRefreshToken(
                client.getUsername()
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(
                        AuthResponse.User.builder()
                                .fullName(client.getFullName())
                                .username(client.getUsername())
                                .build()
                )
                .build();
    }
}
