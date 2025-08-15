package com.example.techstars;

import com.example.techstars.config.GoogleSheetsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableConfigurationProperties({
        GoogleSheetsProperties.class
})
@EnableAsync
@SpringBootApplication
public class TechstarsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechstarsApplication.class, args);
    }

}
