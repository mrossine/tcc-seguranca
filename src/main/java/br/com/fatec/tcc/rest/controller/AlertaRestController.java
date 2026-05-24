package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.AlertaResponseDTO;
import br.com.fatec.tcc.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AlertaRestController {

    private final AlertaService alertaService;

    @GetMapping("/alertas")
    public List<AlertaResponseDTO> listarAlertas(Authentication authentication) {
        // Obtém o email do usuário logado e repassa para o service
        String email = authentication.getName();
        return alertaService.listarAlertasAtivos(email);
    }
}