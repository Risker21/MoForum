package com.moforum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MoforumApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoforumApplication.class, args);
    }

}
