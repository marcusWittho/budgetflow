package com.lwv.budgetflow;

import com.lwv.budgetflow.security.config.CorsProperties;
import com.lwv.budgetflow.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class BudgetflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(BudgetflowApplication.class, args);
	}

}
