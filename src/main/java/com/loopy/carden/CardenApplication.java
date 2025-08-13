package com.loopy.carden;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class CardenApplication {

	public static void main(String[] args) {
		SpringApplication.run(CardenApplication.class, args);
	}

}
