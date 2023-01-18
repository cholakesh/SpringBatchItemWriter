package com.itemwriter.demo2;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableBatchProcessing
@ComponentScan({ "com.itemwriter.config", "com.itemwriter.reader", "com.itemwriter.processor",
		"com.itemwriter.writer", "com.itemwriter.service", "com.itemwriter.listener" })
public class Demo2Application {

	public static void main(String[] args) {
		SpringApplication.run(Demo2Application.class, args);
	}

}
