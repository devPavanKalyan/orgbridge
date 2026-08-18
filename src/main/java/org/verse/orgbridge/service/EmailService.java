package org.verse.orgbridge.service;

import reactor.core.publisher.Mono;

public interface EmailService {

    Mono<Void> sendVerificationEmail(
            String email,
            String fullName,
            String verificationLink
    );

    Mono<Void> sendLoginOtpEmail(
            String email,
            String fullName,
            String otp
    );
}
