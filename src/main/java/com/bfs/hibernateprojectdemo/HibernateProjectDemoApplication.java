package com.bfs.hibernateprojectdemo;

import com.bfs.hibernateprojectdemo.service.RegisterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
@EnableTransactionManagement
public class HibernateProjectDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HibernateProjectDemoApplication.class, args);
    }
    @Bean
    public CommandLineRunner initAdmin(RegisterService registerService) {
        return args -> registerService.initAdminUser();
    }
}
