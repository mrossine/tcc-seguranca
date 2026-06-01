package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.EstatisticasDTO;
import br.com.fatec.tcc.service.EstatisticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller da PÁGINA de estatísticas.
 * Carrega o relatório no servidor e entrega para o template Thymeleaf renderizar.
 */
@Controller
@RequestMapping("/estatisticas")
@RequiredArgsConstructor
public class EstatisticaController {

	private final EstatisticaService estatisticaService;

	/** GET /estatisticas — busca o relatório e abre a tela de estatísticas. */
	@GetMapping
	public String estatisticas(Model model) {
		EstatisticasDTO estatisticas = estatisticaService.getEstatisticas();
		model.addAttribute("estatisticas", estatisticas);
		return "estatisticas";
	}
}
