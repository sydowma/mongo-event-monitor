package com.sydow.mongoeventmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MongoEventMonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(MongoEventMonitorApplication.class, args);
	}

}
