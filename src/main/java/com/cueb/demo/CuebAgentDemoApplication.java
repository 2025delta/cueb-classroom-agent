package com.cueb.demo;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cueb.demo.classroom.mapper")
public class CuebAgentDemoApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .filename(".env.develop")
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(e ->
                System.setProperty(e.getKey(), e.getValue()));

        SpringApplication.run(CuebAgentDemoApplication.class, args);
    }

}
