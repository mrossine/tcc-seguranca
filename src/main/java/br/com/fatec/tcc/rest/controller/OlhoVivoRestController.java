package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.service.OlhoVivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Proxy REST para a API SPTrans Olho Vivo.
 * O frontend nunca acessa a API da SPTrans diretamente — passa sempre por aqui,
 * garantindo que o token permaneça seguro no servidor.
 */
@RestController
@RequestMapping("/api/onibus")
@RequiredArgsConstructor
public class OlhoVivoRestController {

    private final OlhoVivoService olhoVivoService;

    /**
     * GET /api/onibus/linhas?termo={termo}
     * Retorna JSON da SPTrans com as linhas que correspondem ao termo buscado.
     */
    @GetMapping("/linhas")
    public ResponseEntity<String> buscarLinhas(@RequestParam String termo) {
        String json = olhoVivoService.buscarLinhas(termo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    /**
     * GET /api/onibus/posicoes?codigoLinha={codigo}
     * Retorna a posição dos veículos em circulação naquela linha.
     * Resposta vem do cache se a última chamada foi há menos de 60 s.
     */
    @GetMapping("/posicoes")
    public ResponseEntity<String> buscarPosicoes(@RequestParam int codigoLinha) {
        String json = olhoVivoService.buscarPosicoesPorLinha(codigoLinha);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    /**
     * GET /api/onibus/paradas?codigoLinha={codigo}
     * Retorna as paradas da linha — usadas para desenhar a rota no mapa.
     */
    @GetMapping("/paradas")
    public ResponseEntity<String> buscarParadas(@RequestParam int codigoLinha) {
        String json = olhoVivoService.buscarParadasPorLinha(codigoLinha);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    /**
     * GET /api/onibus/debug?termo={termo}
     * Retorna o JSON bruto da SPTrans — útil para inspecionar os campos reais da API.
     * Remover ou restringir antes de colocar em produção.
     */
    @GetMapping("/debug")
    public ResponseEntity<String> debug(@RequestParam String termo) {
        String linhasJson   = olhoVivoService.buscarLinhas(termo);
        String resultado = "{\"linhas\":" + linhasJson + "}";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resultado);
    }
}
