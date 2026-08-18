package org.verse.orgbridge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender; // ✅ interface

    @Override
    public Mono<Void> sendVerificationEmail(String email, String fullName, String verificationLink) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("OrgBridge <no-reply@orgbridge.app>");
            message.setTo(email);
            message.setSubject("Verify your account");
            message.setText("""
                    Hi %s,
                    
                    Please verify your account by clicking the link below:
                    %s
                    
                    This link will expire shortly.
                    
                    — OrgBridge Team
                    """.formatted(fullName, verificationLink));

            mailSender.send(message);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendLoginOtpEmail(String email, String fullName, String otp) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("OrgBridge <no-reply@orgbridge.app>");
            message.setTo(email);
            message.setSubject("Your Login OTP");
            message.setText("""
                    Hi %s,
                    
                    Your login OTP is: %s
                    
                    This OTP is valid for 5 minutes.
                    Do not share it with anyone.
                    
                    — OrgBridge Security
                    """.formatted(fullName, otp));

            mailSender.send(message);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
