package edu.tlu.klgd;

import edu.tlu.klgd.infracstructure.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class TeachingManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeachingManagementApplication.class, args);
    }
}
