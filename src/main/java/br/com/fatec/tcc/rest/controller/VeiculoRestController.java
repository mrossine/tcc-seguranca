package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.dto.VeiculoDTO;
import br.com.fatec.tcc.dto.VeiculoRequestDTO;
import br.com.fatec.tcc.service.VeiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/veiculos")
@RequiredArgsConstructor
public class VeiculoRestController {

    private final VeiculoService veiculoService;

    @GetMapping
    public ResponseEntity<List<VeiculoDTO>> listar(Authentication auth) {
        return ResponseEntity.ok(veiculoService.listarVeiculosDoUsuario(auth.getName()));
    }

    @GetMapping("/catalogo")
    public ResponseEntity<Map<String, List<String>>> catalogo() {
        return ResponseEntity.ok(veiculoService.getCatalogo());
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@Valid @RequestBody VeiculoRequestDTO req, Authentication auth) {
        try {
            VeiculoDTO dto = veiculoService.cadastrarVeiculo(req, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Essa placa já está cadastrada no sistema."));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("CUSTOM_PENDENTE:")) {
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(Map.of("pendente", true, "message", msg.substring("CUSTOM_PENDENTE:".length())));
            }
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remover(@PathVariable Long id, Authentication auth) {
        try {
            veiculoService.removerVeiculo(id, auth.getName());
            return ResponseEntity.ok(Map.of("message", "Veículo removido com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
