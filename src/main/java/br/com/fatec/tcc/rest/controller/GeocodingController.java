package br.com.fatec.tcc.rest.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller responsável por fazer o reverse geocoding no servidor.
 */
@Slf4j
@RestController
@RequestMapping("/api/geocoding")
public class GeocodingController {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s&zoom=18&addressdetails=1&accept-language=pt-BR";

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
            String urlStr = String.format(NOMINATIM_URL, lat, lng);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            // User-Agent obrigatório pelo Nominatim Usage Policy
            conn.setRequestProperty("User-Agent",
                    "SegurancaFatecZL/1.0 (tcc-fatec-zl; contato@fatec.sp.gov.br)");
            conn.setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("Nominatim retornou status {}", status);
                resultado.put("endereco", null);
                return ResponseEntity.ok(resultado);
            }

            // Lê a resposta JSON como String
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String json = sb.toString();
            log.debug("Nominatim response: {}", json);

            // Extrai campos do JSON manualmente (sem dependência extra)
            String road      = extrairCampo(json, "road", "pedestrian", "path", "footway");
            String houseNum  = extrairValor(json, "house_number");
            String suburb    = extrairCampo(json, "suburb", "neighbourhood", "district", "quarter");
            String city      = extrairCampo(json, "city", "town", "village", "municipality");
            String stateCode = extrairValor(json, "state_code");
            String state     = extrairValor(json, "state");

            String uf = (stateCode != null && !stateCode.isEmpty()) ? stateCode : state;

            // Monta o endereço: "Rua X, 123, Bairro - Cidade/UF"
            StringBuilder endereco = new StringBuilder();
            if (road != null && !road.isEmpty()) {
                endereco.append(road);
                if (houseNum != null && !houseNum.isEmpty()) {
                    endereco.append(", ").append(houseNum);
                }
            }
            if (suburb != null && !suburb.isEmpty()) {
                if (endereco.length() > 0) endereco.append(", ");
                endereco.append(suburb);
            }
            if (city != null && !city.isEmpty()) {
                if (endereco.length() > 0) endereco.append(" - ");
                endereco.append(city);
                if (uf != null && !uf.isEmpty()) {
                    endereco.append("/").append(uf);
                }
            }

            // Fallback: pega display_name se não conseguiu montar
            if (endereco.length() == 0) {
                String displayName = extrairValor(json, "display_name");
                if (displayName != null && !displayName.isEmpty()) {
                    // Pega apenas as primeiras partes do display_name
                    String[] partes = displayName.split(",");
                    StringBuilder fallback = new StringBuilder();
                    for (int i = 0; i < Math.min(3, partes.length); i++) {
                        if (fallback.length() > 0) fallback.append(",");
                        fallback.append(partes[i].trim());
                    }
                    endereco.append(fallback);
                }
            }

            resultado.put("endereco", endereco.length() > 0 ? endereco.toString() : null);
            log.info("Geocoding lat={} lng={} -> {}", lat, lng, resultado.get("endereco"));
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Erro no reverse geocoding: {}", e.getMessage());
            resultado.put("endereco", null);
            return ResponseEntity.ok(resultado);
        }
    }

    /**
     * Extrai o valor do primeiro campo encontrado no JSON.
     * Tenta cada nome na ordem até achar um com valor não-vazio.
     */
    private String extrairCampo(String json, String... campos) {
        for (String campo : campos) {
            String valor = extrairValor(json, campo);
            if (valor != null && !valor.isEmpty()) {
                return valor;
            }
        }
        return null;
    }

    /**
     * Extrai o valor de um campo JSON via regex simples.
     * Funciona para valores string (entre aspas).
     */
    private String extrairValor(String json, String campo) {
        // Padrão: "campo":"valor"
        Pattern p = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}