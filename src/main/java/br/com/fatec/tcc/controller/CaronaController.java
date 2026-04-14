package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.CaronaRequestDTO;
import br.com.fatec.tcc.dto.CaronaResponseDTO;
import br.com.fatec.tcc.service.CaronaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/caronas")
@RequiredArgsConstructor
public class CaronaController {

	private final CaronaService caronaService;

	@GetMapping
	public String listarCaronas(@RequestParam(required = false) String origem,
			@RequestParam(required = false) String destino,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime horarioInicio,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime horarioFim,
			Model model) {
		model.addAttribute("caronas",
				caronaService.listarCaronasDisponiveis(origem, destino, horarioInicio, horarioFim));
		return "caronas/lista";
	}

	@GetMapping("/nova")
	public String novaCarona(Model model) {
		model.addAttribute("carona", new CaronaRequestDTO());
		return "caronas/nova";
	}

	@PostMapping("/nova")
	public String oferecerCarona(@ModelAttribute CaronaRequestDTO request, Authentication auth) {
		caronaService.oferecerCarona(request, auth.getName());
		return "redirect:/caronas";
	}

	@PostMapping("/{id}/solicitar")
	public String solicitarVaga(@PathVariable Long id, Authentication auth) {
		caronaService.solicitarVaga(id, auth.getName());
		return "redirect:/caronas";
	}

	@GetMapping("/{id}")
	public String detalhesCarona(@PathVariable Long id, Model model) {
		CaronaResponseDTO carona = caronaService.buscarPorId(id);
		model.addAttribute("carona", carona);
		return "caronas/detalhes";
	}
}