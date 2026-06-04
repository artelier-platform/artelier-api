package com.artelier.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ArtelierApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArtelierApiApplication.class, args);
	}

}
