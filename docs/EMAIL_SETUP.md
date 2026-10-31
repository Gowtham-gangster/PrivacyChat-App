# PrivyChat SMTP-Only Communication & Email Delivery Architecture

## 1. Mandatory SMTP Communication Policy

PrivyChat enforces a strict **SMTP-Only Communication Policy**. All account authentication, onboarding verification, credential recovery, identifier modifications, and friend invitations are processed exclusively through **the Spring Boot backend using JavaMailSender and your manually configured SMTP server**.

### Workflows Handled by SMTP
1. **Registration Email Verification**: 6-digit cryptographic single-use OTP dispatched during signup to prove ownership of the email account.
2. **Welcome & Activation Email**: Sent strictly once upon initial successful verification of the account.
3. **Password-Reset Verification**: Secure single-use expiring token/OTP sent to the registered email address.
4. **Password-Changed Confirmation**: Immediate security alert notification confirming password modification and invalidation of prior sessions.
5. **Email Address Changes**: Verification code sent to the requested new email, plus a security notification alert sent to the previous verified address.
6. **Mobile Number Changes**: **Exclusively verified through the user's registered email address.** An email is dispatched identifying the pending change with a masked phone number (e.g. `+141 *** *** 0299`) and a verification code.
7. **Mobile Number Updated Notification**: Immediate security notification dispatched to the registered email upon phone number commit.
8. **Friend Invitations**: Cryptographically signed invitation tokens with deep-links to the application download portal.

> [!IMPORTANT]
> **No SMS Services or Supabase Auth Emails**:
> - All SMS providers (Twilio, AWS SNS) have been completely removed.
> - Supabase Auth email services, Supabase invitation emails, and Supabase password-reset hooks are strictly forbidden. Supabase is utilized exclusively for PostgreSQL database persistence and private object storage.

---

## 2. SMTP Provider Configuration

PrivyChat supports standard Spring Boot mail properties and common environment aliases:

```properties
# Standard Spring Boot Mail Variables
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_email@gmail.com
SPRING_MAIL_PASSWORD=your_app_specific_password
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
PRIVYCHAT_MAIL_FROM=your_email@gmail.com
PRIVYCHAT_MAIL_FROM_NAME=PrivyChat Security

# Backward-Compatible Aliases
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_specific_password
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
MAIL_FROM_ADDRESS=your_email@gmail.com
MAIL_FROM_NAME=PrivyChat Security
```

---

## 3. Security Safeguards

1. **Anti-Enumeration Protection**: The `POST /api/v1/auth/forgot-password` endpoint returns an identical generic response regardless of whether the requested email address exists in the database.
2. **Single-Use Expiring Tokens**: All verification codes and password reset tokens expire after 10 minutes, are hashed with BCrypt prior to storage, and are permanently erased after first use.
3. **Attempt Limiting & Cooldowns**: A maximum of 5 attempts are allowed per code before automatic invalidation.
4. **No Plaintext Credential Logging**: Verification codes, reset tokens, and SMTP credentials are strictly excluded from all application logs and telemetry.
5. **Strict Failure Preservation**: If the SMTP server is unreachable or rejects authentication, the backend throws an actionable `BadRequestException` and preserves the pending change, preventing unverified state transitions.
