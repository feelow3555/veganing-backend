package com.veganing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VeganingApplication {

	public static void main(String[] args) {
		SpringApplication.run(VeganingApplication.class, args);
	}

}
