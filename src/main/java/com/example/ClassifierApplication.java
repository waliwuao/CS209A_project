package com.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ClassifierApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClassifierApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner commandLineRunner(MultiThreadQuestionClassifier service){
        return args -> {
            service.classifyAllQuestions();
        };
    }
}