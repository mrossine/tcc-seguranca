package br.com.fatec.tcc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TccApplication {
	public static void main(String[] args) {
		SpringApplication.run(TccApplication.class, args);
	}
}