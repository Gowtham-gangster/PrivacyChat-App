# PrivyChat Email Delivery & Friend Invitation Guide

## 1. Overview

PrivyChat uses transactional email for two critical security workflows:
1. **Account Verification**: Dispatching 6-digit OTP verification codes to confirm email ownership during onboarding.
2. **Contact Invitations**: Sending secure invitation emails to non-registered friends with high-entropy, time-limited verification links.

---

## 2. Security Safeguards for Invitations

1. **No Credentials in URLs**: Invitation tokens are opaque, random 256-bit cryptographic hex tokens. They contain no passwords, encryption keys, or prekeys.
2. **No Direct APK Attachments**: Email providers often quarantine or flag APK file attachments as malware. PrivyChat emails include verified HTTPS download links rather than raw binaries.
3. **Token Hashing & Expiration**: Only the SHA-256 hash of the invitation token is stored in the PostgreSQL database. Tokens expire after 7 days.
4. **Rate Limiting**: Users are strictly limited to sending at most 10 invitations per hour to prevent spam abuse.
5. **Anti-Enumeration**: Public invite endpoints return uniform responses regardless of whether the target email is already registered.

---

## 3. Responsive HTML Email Template

Invitations are delivered with responsive HTML and plain-text fallback:
- **Header**: PrivyChat Brand Logo & Privacy Shield badge.
- **Body**: Personalized message: `"[Inviter Display Name] invited you to connect privately on PrivyChat."`
- **Call-to-Action**: Prominent button linking to the HTTPS APK download page with the embedded invite token (`https://privychat.example.com/invite?token=...`).
- **Footer**: Explanation that PrivyChat is an end-to-end encrypted private messenger, along with an opt-out mechanism.

---

## 4. SMTP Provider Configuration

Configure environment variables in `.env` or the platform environment:

```env
# Mailtrap (for local development/testing)
SMTP_HOST=smtp.mailtrap.io
SMTP_PORT=587
SMTP_USERNAME=your_mailtrap_user
SMTP_PASSWORD=your_mailtrap_pass
SMTP_STARTTLS_ENABLE=true
MAIL_FROM_ADDRESS=noreply@privychat.example.com
MAIL_FROM_NAME=PrivyChat Security

# AWS SES / SendGrid / Postmark (for production)
# SMTP_HOST=email-smtp.us-east-1.amazonaws.com
# SMTP_PORT=587
# SMTP_USERNAME=AKIA...
# SMTP_PASSWORD=B...
```

