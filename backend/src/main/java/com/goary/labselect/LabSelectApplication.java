package com.goary.labselect;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.goary.labselect.**.mapper")
@SpringBootApplication
public class LabSelectApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabSelectApplication.class, args);
    }
}
