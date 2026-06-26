package com.example.backendastramaco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class BackendAstramacoApplication {
    //esto es un comentario para probar
    public static void main(String[] args) {
        SpringApplication.run(BackendAstramacoApplication.class, args);

    }

}


