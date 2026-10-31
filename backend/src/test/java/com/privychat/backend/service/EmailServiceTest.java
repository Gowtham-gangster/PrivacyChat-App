package com.privychat.backend.service;

import com.privychat.backend.exception.BadRequestException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(
                mailSender,
                "noreply@privychat.example.com",
                "PrivyChat Security",
                "https://privychat.example.com/download/privychat-release.apk"
        );
    }

    @Test
    void testSendRegistrationOtpEmail_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendRegistrationOtpEmail("user@example.com", "Alice", "123456"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendWelcomeEmail_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendWelcomeEmail("user@example.com", "Alice"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendPasswordResetEmail_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail("user@example.com", "Alice", "654321"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendPhoneChangeVerificationEmail_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendPhoneChangeVerificationEmail("user@example.com", "Alice", "+141 *** *** 0299", "112233"));

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendEmail_FailureThrowsBadRequestException() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(BadRequestException.class, () ->
                emailService.sendRegistrationOtpEmail("user@example.com", "Alice", "123456"));
    }
}

