package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;

/*
 * удалить тип устройства + оформить токен експаред тут и на фронте + логаут сделать на фронте
 */

@SpringBootApplication
@EnableAsync
public class ToDoListBackendApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(ToDoListBackendApplication.class, args);
    }

}
