package com.mystictarot.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class for Mystic Tarot AI Backend
 * 
 * Note: Entities are automatically scanned as they are in sub-packages of this class.
 * @EnableJpaRepositories: Explicitly enables Spring Data JPA repositories
 */
@SpringBootApplication(scanBasePackages = "com.mystictarot.backend")
@EnableJpaRepositories(basePackages = "com.mystictarot.backend.repository")
public class MysticTarotApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        
        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue();
            if (System.getProperty(key) == null) {
                System.setProperty(key, value);
            }
        });

        SpringApplication.run(MysticTarotApplication.class, args);
    }

}
