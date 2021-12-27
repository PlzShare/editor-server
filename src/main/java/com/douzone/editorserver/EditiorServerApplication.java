package com.douzone.editorserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EditiorServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EditiorServerApplication.class, args);
	}
}
