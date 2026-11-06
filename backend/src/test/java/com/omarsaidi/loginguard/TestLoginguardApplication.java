package com.omarsaidi.loginguard;

import org.springframework.boot.SpringApplication;

public class TestLoginguardApplication {

	public static void main(String[] args) {
		SpringApplication.from(LoginguardApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
