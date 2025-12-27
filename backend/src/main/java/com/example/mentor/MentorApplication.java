package com.example.mentor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.example.mentor.repository.CardRepository;

@SpringBootApplication
@EnableJpaRepositories("com.example.mentor.repository")
@EntityScan("com.example.mentor.dao.entity")
public class MentorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentorApplication.class, args);
    }

    // 注入 CardRepository 以激活 JPA 自动建表
    @Bean
    public CommandLineRunner init(CardRepository cardRepository) {
        return args -> {
            // 使用 cardRepository 来避免静态分析器报告未使用参数；不会对业务产生副作用
            cardRepository.count();
        };
    }
}