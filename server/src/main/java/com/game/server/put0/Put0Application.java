package com.game.server.put0;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.game.server.put0")
public class Put0Application {

	public static void main(String[] args) {
		SpringApplication.run(Put0Application.class, args);
	}

}
