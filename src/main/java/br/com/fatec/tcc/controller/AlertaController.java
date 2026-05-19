package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.AlertaRequestDTO;
import br.com.fatec.tcc.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/alertas")
@RequiredArgsConstructor
public class AlertaController {

	private final AlertaService alertaService;

	@GetMapping
	public String listarAlertas(Model model) {
		model.addAttribute("alertas", alertaService.listarAlertasAtivos());
		return "alertas/lista";
	}

	@GetMapping("/novo")
	public String novoAlerta(Model model) {
		model.addAttribute("alerta", AlertaRequestDTO.empty());
		return "alertas/novo";
	}
	

	@PostMapping("/novo")
	public String criarAlerta(@ModelAttribute AlertaRequestDTO request, Authentication auth) {
	    alertaService.criarAlerta(request, auth.getName());
	    return "redirect:/alertas";
	}

	@PostMapping("/{id}/confirmar")
	public String confirmarAlerta(@PathVariable Long id) {
		alertaService.confirmarAlerta(id);
		return "redirect:/alertas";
	}

	@PostMapping("/{id}/denunciar")
	public String denunciarAlerta(@PathVariable Long id) {
		alertaService.denunciarAlerta(id);
		return "redirect:/alertas";
	}
}