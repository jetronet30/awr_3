package com.jaba.awr_3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.jaba.awr_3.inits.MainInit;

@SpringBootApplication
@EnableScheduling
public class Awr3Application {

	public static void main(String[] args) {
		MainInit.initAll();
		SpringApplication.run(Awr3Application.class, args);
	}

}
