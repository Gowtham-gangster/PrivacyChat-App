package com.privychat.backend.service;

import com.privychat.backend.exception.BadRequestException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
            @Value("${privychat.mail.from-address:${MAIL_FROM_ADDRESS:${PRIVYCHAT_MAIL_FROM:${SMTP_USERNAME:noreply@privychat.example.com}}}}") String fromAddress,
            @Value("${privychat.mail.from-name:${MAIL_FROM_NAME:${PRIVYCHAT_MAIL_FROM_NAME:PrivyChat Security}}}") String fromName,
            @Value("${privychat.apk-download-url:https://privychat.example.com/download/privychat-release.apk}") String apkDownloadUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.apkDownloadUrl = apkDownloadUrl;
    }

    public void sendRegistrationOtpEmail(String recipientEmail, String displayName, String otpCode) {
        String subject = "PrivyChat Email Verification Code: " + otpCode;
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
                    <p>Hello %s,</p>
                    <p>Thank you for registering with PrivyChat. Enter the following single-use verification code to activate your account:</p>
                    <div class="code-box">%s</div>
                    <p>This code expires in 10 minutes. If you did not create a PrivyChat account, please ignore this email.</p>
                    <div class="footer">&copy; 2026 PrivyChat. Strictly 1-to-1 End-to-End Encrypted Messenger.</div>
                </div>
            </body>
            </html>
            """.formatted(displayName != null ? displayName : "User", otpCode);

        sendEmail(recipientEmail, subject, htmlContent);
    }

    public void sendWelcomeEmail(String recipientEmail, String displayName) {
        String subject = "Welcome to PrivyChat — Your Account is Verified";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .card { max-width: 480px; margin: 0 auto; background-color: #1E293B; border-radius: 12px; padding: 32px; border: 1px solid #334155; }
                    .logo { font-size: 26px; font-weight: 700; color: #10B981; text-align: center; margin-bottom: 20px; }
                    .banner { background-color: #0F172A; border: 1px solid #10B981; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; color: #D1FAE5; }
                    .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 24px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">Welcome to PrivyChat</div>
                    <p>Hello %s,</p>
                    <div class="banner">
                        &#10004; <strong>Account Activated:</strong> Your email address has been successfully verified.
                    </div>
                    <p>PrivyChat gives you zero-knowledge private messaging protected by Double Ratchet end-to-end encryption. Only you and your contacts can decrypt your messages and media.</p>
                    <p>You can now open the app, exchange safety numbers with your contacts, and start secure 1-to-1 conversations.</p>
                    <div class="footer">&copy; 2026 PrivyChat. Zero-knowledge secure private messaging.</div>
                </div>
            </body>
            </html>
            """.formatted(displayName != null ? displayName : "User");

        sendEmail(recipientEmail, subject, htmlContent);
    }

    public void sendPasswordResetEmail(String recipientEmail, String displayName, String resetCode) {
        String subject = "PrivyChat Password Reset Code: " + resetCode;
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .card { max-width: 480px; margin: 0 auto; background-color: #1E293B; border-radius: 12px; padding: 32px; border: 1px solid #334155; }
                    .logo { font-size: 24px; font-weight: 700; color: #F59E0B; text-align: center; margin-bottom: 24px; }
                    .code-box { font-size: 36px; font-weight: 800; letter-spacing: 8px; text-align: center; color: #FCD34D; background-color: #0F172A; padding: 16px; border-radius: 8px; margin: 24px 0; border: 1px solid #F59E0B; }
                    .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 24px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">Password Reset Request</div>
                    <p>Hello %s,</p>
                    <p>A request was received to reset the password for your PrivyChat account. Use the following single-use verification code to complete the reset:</p>
                    <div class="code-box">%s</div>
                    <p>This code expires in 10 minutes. If you did not request a password reset, please secure your email account immediately.</p>
                    <div class="footer">&copy; 2026 PrivyChat Security.</div>
                </div>
            </body>
            </html>
            """.formatted(displayName != null ? displayName : "User", resetCode);

        sendEmail(recipientEmail, subject, htmlContent);
    }

    public void sendPasswordChangedNotification(String recipientEmail, String displayName) {
        String subject = "Security Alert: PrivyChat Password Successfully Changed";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .card { max-width: 480px; margin: 0 auto; background-color: #1E293B; border-radius: 12px; padding: 32px; border: 1px solid #334155; }
                    .logo { font-size: 24px; font-weight: 700; color: #10B981; text-align: center; margin-bottom: 24px; }
                    .alert-box { background-color: #0F172A; border: 1px solid #10B981; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; color: #D1FAE5; }
                    .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 24px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">Password Changed</div>
                    <p>Hello %s,</p>
                    <div class="alert-box">
                        Your PrivyChat account password was recently updated. All existing mobile sessions have been refreshed.
                    </div>
                    <p>If you authorized this change, you can safely ignore this email.</p>
                    <p>If you did <strong>NOT</strong> change your password, contact support or reset your password immediately.</p>
                    <div class="footer">&copy; 2026 PrivyChat Security.</div>
                </div>
            </body>
            </html>
            """.formatted(displayName != null ? displayName : "User");

        sendEmail(recipientEmail, subject, htmlContent);
    }

    public void sendEmailChangeVerification(String newEmail, String verificationCode) {
        String subject = "PrivyChat Email Change Verification Code: " + verificationCode;
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .card { max-width: 480px; margin: 0 auto; background-color: #1E293B; border-radius: 12px; padding: 32px; border: 1px solid #334155; }
                    .logo { font-size: 24px; font-weight: 700; color: #14B8A6; text-align: center; margin-bottom: 24px; }
                    .code-box { font-size: 36px; font-weight: 800; letter-spacing: 8px; text-align: center; color: #10B981; background-color: #0F172A; padding: 16px; border-radius: 8px; margin: 24px 0; border: 1px solid #10B981; }
                    .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 24px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">PrivyChat Security</div>
                    <p>Hello,</p>
                    <p>A request was made to update the email address for your PrivyChat account to this address.</p>
                    <p>Enter the following single-use verification code in the PrivyChat app to confirm this change:</p>
                    <div class="code-box">%s</div>
                    <p>This code expires in 10 minutes. If you did not make this request, no action is required and your account remains secure.</p>
                    <div class="footer">&copy; 2026 PrivyChat. Zero-knowledge secure private messaging.</div>
                </div>
            </body>
            </html>
            """.formatted(verificationCode);

        sendEmail(newEmail, subject, htmlContent);
    }

    public void sendEmailChangedNotification(String previousEmail, String newEmail) {
        String subject = "Security Alert: PrivyChat Account Email Changed";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .card { max-width: 480px; margin: 0 auto; background-color: #1E293B; border-radius: 12px; padding: 32px; border: 1px solid #334155; }
                    .logo { font-size: 24px; font-weight: 700; color: #EF4444; text-align: center; margin-bottom: 24px; }
                    .alert-box { background-color: #450A0A; border: 1px solid #DC2626; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; color: #FECACA; }
                    .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 24px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">PrivyChat Security Alert</div>
                    <p>Hello,</p>
                    <div class="alert-box">
                        Your PrivyChat account email address was recently updated to: <strong>%s</strong>
                    </div>
                    <p>If you authorized this change, you can safely disregard this email.</p>
                    <p>If you did <strong>NOT</strong> authorize this change, please open the PrivyChat app immediately to secure your account.</p>
                    <div class="footer">&copy; 2026 PrivyChat. Zero-knowledge secure private messaging.</div>
                </div>
            </body>
            </html>
            """.formatted(newEmail);

        sendEmail(previousEmail, subject, htmlContent);
    }

    public void sendPhoneChangeVerificationEmail(String recipientEmail, String displayName, String maskedPhone, String verificationCode) {
        String subject = "PrivyChat Mobile Number Change Verification: " + verificationCode;
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .card { max-width: 480px; margin: 0 auto; background-color: #1E293B; border-radius: 12px; padding: 32px; border: 1px solid #334155; }
                    .logo { font-size: 24px; font-weight: 700; color: #38BDF8; text-align: center; margin-bottom: 24px; }
                    .alert-box { background-color: #0F172A; border: 1px solid #0284C7; border-radius: 8px; padding: 16px; margin: 16px 0; font-size: 14px; color: #E0F2FE; }
                    .code-box { font-size: 36px; font-weight: 800; letter-spacing: 8px; text-align: center; color: #38BDF8; background-color: #0F172A; padding: 16px; border-radius: 8px; margin: 20px 0; border: 1px solid #38BDF8; }
                    .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 24px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">Mobile Number Change Request</div>
                    <p>Hello %s,</p>
                    <div class="alert-box">
                        A request was submitted to update your PrivyChat verified mobile number to: <strong>%s</strong>
                    </div>
                    <p>Per PrivyChat security policy, mobile number changes are verified exclusively through your registered email address. Enter the following code in the app to confirm this change:</p>
                    <div class="code-box">%s</div>
                    <p>This code expires in 10 minutes. If you did not request this change, please log in and change your password immediately.</p>
                    <div class="footer">&copy; 2026 PrivyChat Security &bull; End-to-End Encrypted Private Messenger</div>
                </div>
            </body>
            </html>
            """.formatted(displayName != null ? displayName : "User", maskedPhone, verificationCode);

        sendEmail(recipientEmail, subject, htmlContent);
    }

    public void sendPhoneChangedNotification(String recipientEmail, String displayName, String maskedPhone) {
        String subject = "Security Alert: PrivyChat Mobile Number Updated";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0F172A; color: #F8FAFC; margin: 0; padding: 24px; }
                    .card { max-width: 480px; margin: 0 auto; background-color: #1E293B; border-radius: 12px; padding: 32px; border: 1px solid #334155; }
                    .logo { font-size: 24px; font-weight: 700; color: #10B981; text-align: center; margin-bottom: 24px; }
                    .alert-box { background-color: #0F172A; border: 1px solid #10B981; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; color: #D1FAE5; }
                    .footer { font-size: 12px; color: #94A3B8; text-align: center; margin-top: 24px; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="logo">Mobile Number Updated</div>
                    <p>Hello %s,</p>
                    <div class="alert-box">
                        Your PrivyChat account mobile number has been successfully updated to: <strong>%s</strong>
                    </div>
                    <p>This number has been verified via your primary email channel.</p>
                    <p>If you did not authorize this change, please take action immediately in the PrivyChat app.</p>
                    <div class="footer">&copy; 2026 PrivyChat Security.</div>
                </div>
            </body>
            </html>
            """.formatted(displayName != null ? displayName : "User", maskedPhone);

        sendEmail(recipientEmail, subject, htmlContent);
    }

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
            log.info("Email dispatched successfully via SMTP to recipient: {}", to);
        } catch (Exception e) {
            log.warn("SMTP email dispatch failed for recipient {}: {}", to, e.getMessage());
            throw new BadRequestException("Unable to send email via SMTP server. Please check SMTP configuration or try again.");
        }
    }
}
