package com.JavaTraining.BaiTap_RS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BaiTapRsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaiTapRsApplication.class, args);
    }
}
