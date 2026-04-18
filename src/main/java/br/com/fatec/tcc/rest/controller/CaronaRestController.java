package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.CaronaResponseDTO;
import br.com.fatec.tcc.service.CaronaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CaronaRestController {

    private final CaronaService caronaService;

    @GetMapping("/caronas")
    public List<CaronaResponseDTO> listarCaronas(
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) String destino,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime horarioInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime horarioFim) {
        return caronaService.listarCaronasDisponiveis(origem, destino, horarioInicio, horarioFim);
    }

    @PostMapping("/caronas/{id}/solicitar")
    public ResponseEntity<?> solicitarVaga(@PathVariable Long id, Authentication auth) {
        try {
            caronaService.solicitarVaga(id, auth.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}