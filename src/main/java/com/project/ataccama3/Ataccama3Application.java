package com.project.ataccama3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Ataccama3Application {

	public static void main(String[] args) {
		SpringApplication.run(Ataccama3Application.class, args);
	}

}
