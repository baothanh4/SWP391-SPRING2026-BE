package com.example.SWP391_SPRING2026;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Swp391Spring2026Application {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.directory("./")
				.ignoreIfMissing()
				.load();

		dotenv.entries().forEach(entry ->
				System.out.println("ENV LOAD: " + entry.getKey() + " = " + entry.getValue())
		);

		SpringApplication.run(Swp391Spring2026Application.class, args);
	}

}
