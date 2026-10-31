package com.privychat.backend.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class LiveSmtpDeliveryTest {

    private EmailService emailService;
    private String recipient;
    private boolean configured = false;

    @BeforeEach
    void setUp() {
        String host = System.getenv("SMTP_HOST");
        String portStr = System.getenv("SMTP_PORT");
        String username = System.getenv("SMTP_USERNAME");
        String password = System.getenv("SMTP_PASSWORD");

        // Fallback to reading from .env in project root if env vars not in process
        if (host == null || host.isBlank()) {
            File envFile = new File("../../.env");
            if (!envFile.exists()) {
                envFile = new File("../.env");
            }
            if (!envFile.exists()) {
                envFile = new File(".env");
            }
            if (envFile.exists()) {
                try (Scanner scanner = new Scanner(envFile)) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim();
                        if (line.startsWith("SMTP_HOST=")) host = line.substring("SMTP_HOST=".length()).trim();
                        else if (line.startsWith("SMTP_PORT=")) portStr = line.substring("SMTP_PORT=".length()).trim();
                        else if (line.startsWith("SMTP_USERNAME=")) username = line.substring("SMTP_USERNAME=".length()).trim();
                        else if (line.startsWith("SMTP_PASSWORD=")) password = line.substring("SMTP_PASSWORD=".length()).trim();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (host != null && !host.isBlank() && username != null && !username.isBlank() && password != null && !password.isBlank()) {
            int port = 587;
            try {
                if (portStr != null) port = Integer.parseInt(portStr);
            } catch (NumberFormatException ignored) {}

            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(host);
            mailSender.setPort(port);
            mailSender.setUsername(username);
            mailSender.setPassword(password);

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.debug", "false");

            // Verify live network socket reachability before asserting live delivery
            if (!isReachable(host, port)) {
                this.configured = false;
                return;
            }

            this.emailService = new EmailService(
                    mailSender,
                    username,
                    "PrivyChat Security",
                    "https://privychat.example.com/download/privychat-release.apk"
            );
            this.recipient = username;
            this.configured = true;
        }
    }

    private boolean isReachable(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @DisplayName("Verify real SMTP delivery for registration verification email")
    void testLiveRegistrationVerificationEmail() {
        Assumptions.assumeTrue(configured, "SMTP credentials not configured in environment or .env");
        assertDoesNotThrow(() -> {
            emailService.sendRegistrationOtpEmail(recipient, "PrivyChat Auditor", "123456");
        });
    }

    @Test
    @DisplayName("Verify real SMTP delivery for password reset email")
    void testLivePasswordResetEmail() {
        Assumptions.assumeTrue(configured, "SMTP credentials not configured in environment or .env");
        assertDoesNotThrow(() -> {
            emailService.sendPasswordResetEmail(recipient, "PrivyChat Auditor", "234567");
        });
    }

    @Test
    @DisplayName("Verify real SMTP delivery for email change verification email")
    void testLiveEmailChangeVerificationEmail() {
        Assumptions.assumeTrue(configured, "SMTP credentials not configured in environment or .env");
        assertDoesNotThrow(() -> {
            emailService.sendEmailChangeVerification(recipient, "345678");
        });
    }

    @Test
    @DisplayName("Verify real SMTP delivery for mobile number change verification email")
    void testLivePhoneChangeVerificationEmail() {
        Assumptions.assumeTrue(configured, "SMTP credentials not configured in environment or .env");
        assertDoesNotThrow(() -> {
            emailService.sendPhoneChangeVerificationEmail(recipient, "PrivyChat Auditor", "+91 ****** 8888", "456789");
        });
    }
}
