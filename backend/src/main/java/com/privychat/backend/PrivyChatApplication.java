package com.privychat.backend;

import com.privychat.backend.config.ProductionEnvironmentValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PrivyChatApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PrivyChatApplication.class);
        app.addInitializers(new ProductionEnvironmentValidator());
        app.run(args);
    }
}

