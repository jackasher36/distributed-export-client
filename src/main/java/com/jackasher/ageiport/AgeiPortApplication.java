package com.jackasher.ageiport;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@MapperScan("com.jackasher.ageiport.mapper")
@EnableDiscoveryClient
public class AgeiPortApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgeiPortApplication.class, args);
    }

}
