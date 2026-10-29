package com.privychat.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final String apkDownloadUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${privychat.mail.from-address:noreply@privychat.example.com}") String fromAddress,
            @Value("${privychat.mail.from-name:PrivyChat Security}") String fromName,
            @Value("${privychat.apk-download-url:https://privychat.example.com/download/privychat-release.apk}") String apkDownloadUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.apkDownloadUrl = apkDownloadUrl;
    }

    @Async
    public void sendVerificationOtpEmail(String recipientEmail, String otpCode) {
        String subject = "PrivyChat Verification Code: " + otpCode;
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .card { max-width: 480px; margin: 0 auto; background-color: #1E293B; border-radius: 12px; padding: 32px; border: 1px solid #334155; }
                    .logo { font-size: 24px; font-weight: 700; color: #14B8A6; text-align: center; margin-bottom: 24px; }
                    .code-box { font-size: 36px; font-weight: 800; letter-spacing: 8px; text-align: center; color: #38BDF8; background-color: #0F172A; padding: 16px; border-radius: 8px; margin: 24px 0; border: 1px solid #0EA5E9; }
                    .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 24px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">PrivyChat Security</div>
                    <p>Hello,</p>
                    <p>Use the following single-use verification code to verify your PrivyChat account:</p>
                    <div class="code-box">%s</div>
                    <p>This code expires in 10 minutes. If you did not request this verification, please disregard this email.</p>
                    <div class="footer">&copy; 2026 PrivyChat. End-to-end encrypted private messaging.</div>
                </div>
            </body>
            </html>
            """.formatted(otpCode);

        sendEmail(recipientEmail, subject, htmlContent);
    }

    @Async
    public void sendFriendInvitationEmail(String recipientEmail, String inviterDisplayName, String inviteToken) {
        String subject = inviterDisplayName + " invited you to connect securely on PrivyChat";
        String inviteUrl = apkDownloadUrl + "?invite=" + inviteToken;

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .card { max-width: 520px; margin: 0 auto; background-color: #1E293B; border-radius: 12px; padding: 32px; border: 1px solid #334155; }
                    .logo { font-size: 26px; font-weight: 700; color: #14B8A6; text-align: center; margin-bottom: 16px; }
                    .headline { font-size: 20px; font-weight: 600; text-align: center; margin-bottom: 20px; color: #F1F5F9; }
                    .btn-box { text-align: center; margin: 32px 0; }
                    .btn { display: inline-block; background-color: #0D9488; color: #FFFFFF; font-weight: 600; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-size: 16px; }
                    .badge { background-color: #0F172A; border: 1px solid #14B8A6; border-radius: 6px; padding: 12px; margin: 20px 0; font-size: 13px; color: #CBD5E1; }
                    .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 28px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">PrivyChat</div>
                    <div class="headline">%s invited you to private messaging</div>
                    <p>PrivyChat is a zero-knowledge, strictly one-to-one messaging app where your conversations and calls are protected with military-grade Double Ratchet end-to-end encryption.</p>
                    
                    <div class="badge">
                        &#128274; <strong>Privacy Guarantee:</strong> No group chats, no data mining, no trackers. Only you and %s can read your messages or decrypt your media.
                    </div>

                    <div class="btn-box">
                        <a href="%s" class="btn">Download PrivyChat for Android</a>
                    </div>
                    
                    <p style="font-size: 13px; color: #94A3B8; text-align: center;">Or open this link: <a href="%s" style="color: #38BDF8;">%s</a></p>
                    
                    <div class="footer">&copy; 2026 PrivyChat &bull; Strictly 1-to-1 Secure Communication</div>
                </div>
            </body>
            </html>
            """.formatted(inviterDisplayName, inviterDisplayName, inviteUrl, inviteUrl, inviteUrl);

        sendEmail(recipientEmail, subject, htmlContent);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email dispatched successfully to: {}", to);
        } catch (Exception e) {
            log.warn("Failed to dispatch email to {}. Fallback log mode. Error: {}", to, e.getMessage());
        }
    }
}

