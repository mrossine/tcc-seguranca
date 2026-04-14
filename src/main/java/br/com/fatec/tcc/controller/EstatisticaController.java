package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.EstatisticasDTO;
import br.com.fatec.tcc.service.EstatisticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/estatisticas")
@RequiredArgsConstructor
public class EstatisticaController {

	private final EstatisticaService estatisticaService;

	@GetMapping
	public String estatisticas(Model model) {
		EstatisticasDTO estatisticas = estatisticaService.getEstatisticas();
		model.addAttribute("estatisticas", estatisticas);
		return "estatisticas";
	}
}
