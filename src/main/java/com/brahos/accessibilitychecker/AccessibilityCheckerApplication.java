package com.brahos.accessibilitychecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootApplication
@ComponentScan(basePackages = "com.brahos.accessibilitychecker")
public class AccessibilityCheckerApplication {

	public static void main(String[] args) {

		WebDriverManager.chromedriver().setup();
		SpringApplication.run(AccessibilityCheckerApplication.class, args);
	}

}
