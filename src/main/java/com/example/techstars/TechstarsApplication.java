package com.example.techstars;

import com.example.techstars.config.GoogleSheetsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({
        GoogleSheetsProperties.class
})
@SpringBootApplication
public class TechstarsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechstarsApplication.class, args);
    }

}
