package com.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableConfigurationProperties
@EnableCaching
public class EcommerceApplication {

	public static void main(String[] args) {
		var context = SpringApplication.run(EcommerceApplication.class, args);
		var env = context.getEnvironment();
		System.out.println("Active Spring Profiles: " + java.util.Arrays.toString(env.getActiveProfiles()));
		System.out.println("Ecommerce redis application started..");
	}

}
