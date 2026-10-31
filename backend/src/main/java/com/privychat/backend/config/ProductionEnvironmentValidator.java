package com.privychat.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that required environment variables are present and configured correctly
 * when running with the 'prod' (production) Spring profile.
 *
 * Prevents the application from starting with fallback/development settings (such as localhost)
 * in production environments like AWS Elastic Beanstalk.
 */
public class ProductionEnvironmentValidator implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(ProductionEnvironmentValidator.class);
    private static final String DEFAULT_DEV_JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment env = context.getEnvironment();

        boolean isProd = false;
        for (String profile : env.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                isProd = true;
                break;
            }
        }

        if (!isProd) {
            log.info("Non-production profile active ({}). Skipping production environment validation.",
                    String.join(", ", env.getActiveProfiles().length > 0 ? env.getActiveProfiles() : new String[]{"default/dev"}));
            return;
        }

        log.info("Production profile 'prod' active. Validating required AWS Elastic Beanstalk environment properties...");

        List<String> missing = new ArrayList<>();
        List<String> invalid = new ArrayList<>();

        // 1. Database URL
        String dbUrl = getProperty(env, "DATABASE_URL", "SPRING_DATASOURCE_URL", "spring.datasource.url");
        if (dbUrl == null || dbUrl.isBlank()) {
            missing.add("DATABASE_URL (Supabase PostgreSQL JDBC URL, e.g. jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require)");
        } else {
            String lower = dbUrl.toLowerCase();
            if (lower.contains("localhost") || lower.contains("127.0.0.1") || lower.contains("host.docker.internal")) {
                invalid.add("DATABASE_URL must not point to localhost in production! Found: " + sanitizeUrl(dbUrl));
            }
            if (!lower.startsWith("jdbc:postgresql://")) {
                invalid.add("DATABASE_URL must start with 'jdbc:postgresql://'. Found: " + sanitizeUrl(dbUrl));
            }
        }

        // 2. Database Credentials
        String dbUser = getProperty(env, "DATABASE_USERNAME", "SPRING_DATASOURCE_USERNAME", "spring.datasource.username");
        if (dbUser == null || dbUser.isBlank()) {
            missing.add("DATABASE_USERNAME (Supabase PostgreSQL database user, e.g. postgres.<project-ref>)");
        }

        String dbPass = getProperty(env, "DATABASE_PASSWORD", "SPRING_DATASOURCE_PASSWORD", "spring.datasource.password");
        if (dbPass == null || dbPass.isBlank()) {
            missing.add("DATABASE_PASSWORD (Supabase PostgreSQL database password)");
        }

        // 3. JWT Secret
        String jwtSecret = getProperty(env, "JWT_SECRET", "SUPABASE_JWT_SECRET", "privychat.jwt.secret");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            missing.add("JWT_SECRET (256-bit Hex HMAC-SHA256 secret key for signing auth tokens)");
        } else if (DEFAULT_DEV_JWT_SECRET.equals(jwtSecret.trim())) {
            invalid.add("JWT_SECRET is using the public development default secret. Please configure a unique 256-bit production secret.");
        }

        // 4. Supabase Storage (if storage type is supabase)
        String storageType = env.getProperty("privychat.storage.type", "supabase");
        if ("supabase".equalsIgnoreCase(storageType)) {
            String sbUrl = getProperty(env, "SUPABASE_URL", "privychat.storage.supabase.url");
            if (sbUrl == null || sbUrl.isBlank()) {
                missing.add("SUPABASE_URL (Supabase project URL, e.g. https://<project-ref>.supabase.co)");
            }
            String sbKey = getProperty(env, "SUPABASE_SERVICE_ROLE_KEY", "privychat.storage.supabase.service-role-key");
            if (sbKey == null || sbKey.isBlank()) {
                missing.add("SUPABASE_SERVICE_ROLE_KEY (Supabase service role secret for attachment authorization)");
            }
        }

        // 5. SMTP Configuration (mandatory manual SMTP)
        String smtpHost = getProperty(env, "SMTP_HOST", "SPRING_MAIL_HOST", "spring.mail.host");
        if (smtpHost == null || smtpHost.isBlank()) {
            missing.add("SMTP_HOST (SMTP server host, e.g. smtp.gmail.com)");
        }
        String smtpUser = getProperty(env, "SMTP_USERNAME", "SPRING_MAIL_USERNAME", "spring.mail.username");
        if (smtpUser == null || smtpUser.isBlank()) {
            missing.add("SMTP_USERNAME (SMTP username / sender email address)");
        }
        String smtpPass = getProperty(env, "SMTP_PASSWORD", "SPRING_MAIL_PASSWORD", "spring.mail.password");
        if (smtpPass == null || smtpPass.isBlank()) {
            missing.add("SMTP_PASSWORD (SMTP password or app password)");
        }

        if (!missing.isEmpty() || !invalid.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n================================================================================\n");
            sb.append("PRIVYCHAT PRODUCTION STARTUP CONFIGURATION ERROR\n");
            sb.append("================================================================================\n");
            sb.append("The backend failed to start because required production environment properties\n");
            sb.append("are missing or invalid in AWS Elastic Beanstalk.\n\n");

            if (!missing.isEmpty()) {
                sb.append("MISSING REQUIRED PROPERTIES (must be configured in AWS Console):\n");
                for (String m : missing) {
                    sb.append("  * ").append(m).append("\n");
                }
                sb.append("\n");
            }

            if (!invalid.isEmpty()) {
                sb.append("INVALID CONFIGURATION VALUES:\n");
                for (String inv : invalid) {
                    sb.append("  * ").append(inv).append("\n");
                }
                sb.append("\n");
            }

            sb.append("To resolve this:\n");
            sb.append("1. Open the AWS Elastic Beanstalk Console.\n");
            sb.append("2. Navigate to your Environment -> Configuration -> Software -> Environment properties.\n");
            sb.append("3. Add each missing property above and click 'Apply'.\n");
            sb.append("================================================================================\n");

            String errorMessage = sb.toString();
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        log.info("All required production environment properties are present and valid.");
    }

    private String getProperty(ConfigurableEnvironment env, String... keys) {
        for (String key : keys) {
            String val = env.getProperty(key);
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return null;
    }

    private String sanitizeUrl(String url) {
        if (url == null) return "";
        return url.replaceAll(":[^/@:]+@", ":****@");
    }
}

