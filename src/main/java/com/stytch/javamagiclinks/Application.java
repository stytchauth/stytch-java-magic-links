package com.stytch.javamagiclinks;

import com.stytch.java.consumer.StytchClient;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	private static final Dotenv dotenv = Dotenv.configure().filename("local.properties").load();

	public static void main(String[] args) {
		StytchClient.configure(dotenv.get("STYTCH_PROJECT_ID"), dotenv.get("STYTCH_PROJECT_SECRET"));
		SpringApplication.run(Application.class, args);
	}
}
