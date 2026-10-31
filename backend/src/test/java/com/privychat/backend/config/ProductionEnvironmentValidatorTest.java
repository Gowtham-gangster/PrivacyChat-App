package com.privychat.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionEnvironmentValidatorTest {

    private final ProductionEnvironmentValidator validator = new ProductionEnvironmentValidator();

    @Test
    @DisplayName("In dev profile, validator passes even without remote database properties")
    void testDevProfileAllowsLocalhostAndMissingProperties() {
        GenericApplicationContext context = new GenericApplicationContext();
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("dev");
        context.setEnvironment(env);

        assertDoesNotThrow(() -> validator.initialize(context));
    }

    @Test
    @DisplayName("In prod profile, validator throws if DATABASE_URL is missing")
    void testProdProfileThrowsWhenDatabaseUrlMissing() {
        GenericApplicationContext context = new GenericApplicationContext();
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("prod");
        context.setEnvironment(env);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.initialize(context));
        assertTrue(ex.getMessage().contains("DATABASE_URL"), "Error message should mention DATABASE_URL");
        assertTrue(ex.getMessage().contains("DATABASE_USERNAME"), "Error message should mention DATABASE_USERNAME");
        assertTrue(ex.getMessage().contains("DATABASE_PASSWORD"), "Error message should mention DATABASE_PASSWORD");
    }

    @Test
    @DisplayName("In prod profile, validator throws if DATABASE_URL points to localhost")
    void testProdProfileThrowsWhenDatabaseUrlPointsToLocalhost() {
        GenericApplicationContext context = new GenericApplicationContext();
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("prod");

        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "jdbc:postgresql://localhost:5432/privychat");
        props.put("DATABASE_USERNAME", "postgres.test");
        props.put("DATABASE_PASSWORD", "secret123");
        props.put("JWT_SECRET", "1234567890123456789012345678901234567890123456789012345678901234");
        props.put("SUPABASE_URL", "https://test.supabase.co");
        props.put("SUPABASE_SERVICE_ROLE_KEY", "sb_key_123");
        props.put("SMTP_HOST", "smtp.gmail.com");
        props.put("SMTP_USERNAME", "test@example.com");
        props.put("SMTP_PASSWORD", "smtp_pass");

        env.getPropertySources().addFirst(new MapPropertySource("testProps", props));
        context.setEnvironment(env);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.initialize(context));
        assertTrue(ex.getMessage().contains("DATABASE_URL must not point to localhost"),
                "Error message should explicitly forbid localhost in prod");
    }

    @Test
    @DisplayName("In prod profile, validator passes when all required properties are configured for Supabase and SMTP")
    void testProdProfilePassesWithValidProperties() {
        GenericApplicationContext context = new GenericApplicationContext();
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("prod");

        Map<String, Object> props = new HashMap<>();
        props.put("DATABASE_URL", "jdbc:postgresql://aws-0-eu-west-2.pooler.supabase.com:5432/postgres?sslmode=require");
        props.put("DATABASE_USERNAME", "postgres.sampleprojectref");
        props.put("DATABASE_PASSWORD", "superSecretDbPassword123!");
        props.put("JWT_SECRET", "03fd4bea14a0385f9fa17925c3a3cb39a98260cb6c3b0ff26969da34f43a9b20");
        props.put("SUPABASE_URL", "https://sampleprojectref.supabase.co");
        props.put("SUPABASE_SERVICE_ROLE_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummyServiceRoleKey");
        props.put("SMTP_HOST", "smtp.gmail.com");
        props.put("SMTP_USERNAME", "privychat-alerts@example.com");
        props.put("SMTP_PASSWORD", "appPassword1234");

        env.getPropertySources().addFirst(new MapPropertySource("testProps", props));
        context.setEnvironment(env);

        assertDoesNotThrow(() -> validator.initialize(context));
    }
}

