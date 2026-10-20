package com.his.ae;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.his.ae.mapper")
public class AeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AeApplication.class, args);
    }
}
