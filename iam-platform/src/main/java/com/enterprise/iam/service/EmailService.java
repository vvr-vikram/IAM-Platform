package com.enterprise.iam.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendVerificationEmail(String email, String token) {
        log.info("--------------------------------------------------");
        log.info("Sending Email verification to: {}", email);
        log.info("Verification token: {}", token);
        log.info("Verification link: http://localhost:8080/api/v1/auth/verify-email?email={}&otp={}", email, token);
        log.info("--------------------------------------------------");
    }

    public void sendPasswordResetEmail(String email, String token) {
        log.info("--------------------------------------------------");
        log.info("Sending Password Reset email to: {}", email);
        log.info("Reset code: {}", token);
        log.info("Reset link: http://localhost:8080/api/v1/auth/reset-password?email={}&otp={}", email, token);
        log.info("--------------------------------------------------");
    }
}
