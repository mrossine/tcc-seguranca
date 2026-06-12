package br.com.fatec.tcc.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller responsável por fazer o reverse geocoding no servidor.
 */
@Slf4j
@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
public class GeocodingController {

    private static final String NOMINATIM_BASE = "https://nominatim.openstreetmap.org";
    // User-Agent obrigatório pela Nominatim Usage Policy
    private static final String USER_AGENT = "SegurancaFatecZL/1.0 (tcc-fatec-zl; contato@fatec.sp.gov.br)";

    private final RestTemplate restTemplate;

    /**
     * Recebe latitude e longitude e retorna o endereço textual.
     * Chamado pelo JavaScript do formulário de novo alerta.
     */
    @GetMapping("/reverse")
    public ResponseEntity<Map<String, String>> reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng) {

        Map<String, String> resultado = new HashMap<>();
        try {
            String url = UriComponentsBuilder.fromHttpUrl(NOMINATIM_BASE + "/reverse")
                    .queryParam("format", "json")
                    .queryParam("lat", lat)
                    .queryParam("lon", lng)
                    .queryParam("zoom", 18)
                    .queryParam("addressdetails", 1)
                    .queryParam("accept-language", "pt-BR")
                    .toUriString();

            HttpHeaders headers = nominatimHeaders();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();

            if (response == null) {
                resultado.put("endereco", null);
                return ResponseEntity.ok(resultado);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> address = (Map<String, Object>) response.get("address");
            String endereco = montarEndereco(address, response);
            resultado.put("endereco", endereco);
            log.debug("Reverse geocoding concluído para lat={} lng={}", lat, lng);
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Erro no reverse geocoding: {}", e.getMessage());
            resultado.put("endereco", null);
            return ResponseEntity.ok(resultado);
        }
    }

    /**
     * GET /api/geocoding/geocodificar?endereco={texto}
     * Converte um endereço em coordenadas (forward geocoding via Nominatim).
     * Retorna lat, lng e o endereço formatado. 400 se não encontrado.
     */
    @GetMapping("/geocodificar")
    public ResponseEntity<Map<String, Object>> geocodificar(@RequestParam String endereco) {
        try {
            // Appenda ", São Paulo, Brasil" se o termo não mencionar cidade/país
            String query = endereco;
            String lower = endereco.toLowerCase();
            if (!lower.contains("são paulo") && !lower.contains("sp")
                    && !lower.contains("brasil") && !lower.contains("brazil")) {
                query = endereco + ", São Paulo, Brasil";
            }

            String url = UriComponentsBuilder.fromHttpUrl(NOMINATIM_BASE + "/search")
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("limit", 5)
                    .queryParam("addressdetails", 1)
                    .queryParam("countrycodes", "br")
                    .queryParam("accept-language", "pt-BR")
                    .toUriString();

            HttpHeaders headers = nominatimHeaders();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), List.class).getBody();

            if (results == null || results.isEmpty()) {
                log.warn("Geocodificação sem resultados para '{}'", endereco);
                return ResponseEntity.ok(Map.of("resultados", List.of()));
            }

            List<Map<String, Object>> resultados = new java.util.ArrayList<>();
            for (Map<String, Object> item : results) {
                String lat = (String) item.get("lat");
                String lon = (String) item.get("lon");
                String displayName = (String) item.get("display_name");
                if (lat == null || lon == null) continue;
                Map<String, Object> r = new HashMap<>();
                r.put("endereco", displayName != null ? displayName : endereco);
                r.put("latitude", Double.parseDouble(lat));
                r.put("longitude", Double.parseDouble(lon));
                resultados.add(r);
            }

            log.debug("Geocodificação: {} resultados para '{}'", resultados.size(), endereco);
            return ResponseEntity.ok(Map.of("resultados", resultados));

        } catch (Exception e) {
            log.error("Erro no geocoding direto '{}': {}", endereco, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", "Erro ao geocodificar: " + e.getMessage()));
        }
    }

    private HttpHeaders nominatimHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        headers.set("Accept-Language", "pt-BR,pt;q=0.9");
        return headers;
    }

    private String montarEndereco(Map<String, Object> address, Map<String, Object> response) {
        if (address == null) {
            return (String) response.get("display_name");
        }

        String road     = getFirst(address, "road", "pedestrian", "path", "footway");
        String houseNum = getString(address, "house_number");
        String suburb   = getFirst(address, "suburb", "neighbourhood", "district", "quarter");
        String city     = getFirst(address, "city", "town", "village", "municipality");
        String stateCode = getString(address, "state_code");
        String state     = getString(address, "state");
        String uf = (stateCode != null && !stateCode.isBlank()) ? stateCode : state;

        StringBuilder sb = new StringBuilder();
        if (road != null) {
            sb.append(road);
            if (houseNum != null) sb.append(", ").append(houseNum);
        }
        if (suburb != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(suburb);
        }
        if (city != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(city);
            if (uf != null) sb.append("/").append(uf);
        }

        if (sb.length() == 0) {
            String displayName = (String) response.get("display_name");
            if (displayName != null) {
                String[] parts = displayName.split(",");
                StringBuilder fallback = new StringBuilder();
                for (int i = 0; i < Math.min(3, parts.length); i++) {
                    if (fallback.length() > 0) fallback.append(",");
                    fallback.append(parts[i].trim());
                }
                return fallback.length() > 0 ? fallback.toString() : null;
            }
            return null;
        }
        return sb.toString();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof String s && !s.isBlank()) ? s : null;
    }

    private String getFirst(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            String val = getString(map, key);
            if (val != null) return val;
        }
        return null;
    }
}
