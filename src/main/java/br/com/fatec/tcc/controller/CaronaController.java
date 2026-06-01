package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.CaronaRequestDTO;
import br.com.fatec.tcc.dto.CaronaResponseDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.CaronaService;
import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller (páginas + ações de formulário) das caronas solidárias.
 * Entrega as telas Thymeleaf e trata o envio dos formulários (oferecer/solicitar).
 */
@Controller
@RequestMapping("/caronas")
@RequiredArgsConstructor
public class CaronaController {

	private final CaronaService caronaService;
	private final UsuarioService usuarioService;

	/**
	 * GET /caronas — abre a lista de caronas, já aplicando filtros opcionais
	 * (origem, destino e intervalo de horário) e informando se o usuário é admin/moderador.
	 */
	@GetMapping
	public String listarCaronas(@RequestParam(required = false) String origem,
			@RequestParam(required = false) String destino,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime horarioInicio,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime horarioFim,
			Model model, Authentication auth) {
		model.addAttribute("caronas",
				caronaService.listarCaronasDisponiveis(auth.getName(), origem, destino, horarioInicio, horarioFim));
		model.addAttribute("emailUsuario", auth.getName());
		Usuario usuario = usuarioService.findUserByUsername(auth.getName());
		model.addAttribute("isAdminOuModerador",
				usuario.getRole() == Usuario.Role.ADMIN || usuario.getRole() == Usuario.Role.MODERATOR);
		return "caronas/lista";
	}

	/** GET /caronas/nova — abre o formulário para oferecer uma nova carona. */
	@GetMapping("/nova")
	public String novaCarona(Model model) {
		model.addAttribute("carona", new CaronaRequestDTO());
		return "caronas/nova";
	}

	/**
	 * POST /caronas/nova — cria a carona a partir do formulário.
	 * Se o serviço recusar (ex.: regra de avaliação baixa), reexibe o formulário com o erro.
	 */
	@PostMapping("/nova")
	public String oferecerCarona(@ModelAttribute CaronaRequestDTO request, Authentication auth, Model model) {
		try {
			caronaService.oferecerCarona(request, auth.getName());
			return "redirect:/caronas";
		} catch (RuntimeException e) {
			model.addAttribute("erro", e.getMessage());
			model.addAttribute("carona", request);
			return "caronas/nova";
		}
	}

	/** POST /caronas/{id}/solicitar — passageiro solicita uma vaga na carona. */
	@PostMapping("/{id}/solicitar")
	public String solicitarVaga(@PathVariable Long id, Authentication auth) {
		caronaService.solicitarVaga(id, auth.getName());
		return "redirect:/caronas";
	}

	/** GET /caronas/{id} — abre a página de detalhes de uma carona específica. */
	@GetMapping("/{id}")
	public String detalhesCarona(@PathVariable Long id, Model model, Authentication auth) {
		CaronaResponseDTO carona = caronaService.buscarPorId(id);
		model.addAttribute("carona", carona);
		Usuario usuario = usuarioService.findUserByUsername(auth.getName());
		model.addAttribute("isAdminOuModerador",
				usuario.getRole() == Usuario.Role.ADMIN || usuario.getRole() == Usuario.Role.MODERATOR);
		return "caronas/detalhes";
	}
}