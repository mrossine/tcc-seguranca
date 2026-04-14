package br.com.fatec.tcc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String index() {
		return "index";
	}

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	@GetMapping("/cadastro")
	public String cadastro() {
		return "cadastro";
	}

	@GetMapping("/dashboard")
	public String dashboard() {
		return "dashboard";
	}
}
