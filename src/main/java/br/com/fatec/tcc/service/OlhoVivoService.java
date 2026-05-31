package br.com.fatec.tcc.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy para a API SPTrans Olho Vivo v2.1.
 *
 * Responsabilidades:
 *  - Gerenciar autenticação via cookie de sessão (revalida a cada 25 min)
 *  - Expor busca de linhas, posições dos veículos e paradas
 *  - Cache em memória de 60 s para posições (evita rate-limit da SPTrans)
 */
@Slf4j
@Service
public class OlhoVivoService {

    private static final String BASE_URL       = "http://api.olhovivo.sptrans.com.br/v2.1";
    private static final long   CACHE_TTL_MS   = 60_000;          // 60 s para posições
    private static final long   AUTH_TTL_MS    = 25 * 60_000L;    // 25 min para reautenticar

    @Value("${sptrans.token}")
    private String token;

    private HttpClient httpClient;

    // Sessão SPTrans
    private volatile String sessionCookie;
    private volatile long   lastAuthTime = 0;

    // Cache de posições: codigoLinha → PosicaoCache
    private record PosicaoCache(String json, long timestamp) {}
    private final ConcurrentHashMap<Integer, PosicaoCache> posicaoCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Autenticação
    // ─────────────────────────────────────────────────────────────────────────

    private synchronized void autenticar() {
        if (sessionCookie != null && System.currentTimeMillis() - lastAuthTime < AUTH_TTL_MS) return;

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/Login/Autenticar?token=" + token))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            // SPTrans retorna "true" no body e o cookie no header Set-Cookie
            String setCookie = resp.headers().firstValue("Set-Cookie").orElse(null);
            if (setCookie != null) {
                // Extrai apenas "apiCredentials=..." sem flags (Secure, HttpOnly etc.)
                sessionCookie = setCookie.split(";")[0];
                lastAuthTime  = System.currentTimeMillis();
                log.info("SPTrans: autenticação bem-sucedida. Cookie: {}", sessionCookie.substring(0, Math.min(30, sessionCookie.length())));
            } else {
                log.error("SPTrans: cookie ausente na resposta. Body={} Status={}", resp.body(), resp.statusCode());
                sessionCookie = null;
            }
        } catch (Exception e) {
            log.error("SPTrans: erro ao autenticar — {}", e.getMessage());
            sessionCookie = null;
        }
    }

    private void garantirAutenticado() {
        if (sessionCookie == null || System.currentTimeMillis() - lastAuthTime >= AUTH_TTL_MS) {
            autenticar();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Requisição genérica com retry em caso de sessão expirada
    // ─────────────────────────────────────────────────────────────────────────

    private String get(String path) throws Exception {
        garantirAutenticado();

        HttpResponse<String> resp = enviarGet(path);

        // Sessão expirada — reautentica e tenta uma vez mais
        if (resp.statusCode() == 401 || resp.statusCode() == 403
                || "false".equalsIgnoreCase(resp.body().trim())) {
            log.warn("SPTrans: sessão expirada, reautenticando...");
            sessionCookie = null;
            lastAuthTime  = 0;
            autenticar();
            resp = enviarGet(path);
        }

        return resp.body();
    }

    private HttpResponse<String> enviarGet(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Cookie", sessionCookie != null ? sessionCookie : "")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos públicos
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Busca linhas de ônibus pelo nome ou número.
     * Ex: "8000" ou "Lapa"
     */
    public String buscarLinhas(String termo) {
        try {
            String encoded = URLEncoder.encode(termo, StandardCharsets.UTF_8);
            return get("/Linha/Buscar?termosBusca=" + encoded);
        } catch (Exception e) {
            log.error("SPTrans: erro ao buscar linhas '{}' — {}", termo, e.getMessage());
            return "[]";
        }
    }

    /**
     * Retorna a posição atual dos veículos de uma linha.
     * Usa cache de 60 s para respeitar o rate-limit da SPTrans.
     */
    public String buscarPosicoesPorLinha(int codigoLinha) {
        PosicaoCache cached = posicaoCache.get(codigoLinha);
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_TTL_MS) {
            log.debug("SPTrans: retornando posições do cache para linha {}", codigoLinha);
            return cached.json();
        }

        try {
            String json = get("/Posicao/Linha?codigoLinha=" + codigoLinha);
            posicaoCache.put(codigoLinha, new PosicaoCache(json, System.currentTimeMillis()));
            return json;
        } catch (Exception e) {
            log.error("SPTrans: erro ao buscar posições da linha {} — {}", codigoLinha, e.getMessage());
            return "{\"vs\":[]}";
        }
    }

    /**
     * Retorna as paradas (pontos de ônibus) de uma linha — usadas para
     * desenhar a rota no mapa como uma polilinha.
     */
    public String buscarParadasPorLinha(int codigoLinha) {
        try {
            return get("/Parada/BuscarParadasPorLinha?codigoLinha=" + codigoLinha);
        } catch (Exception e) {
            log.error("SPTrans: erro ao buscar paradas da linha {} — {}", codigoLinha, e.getMessage());
            return "[]";
        }
    }
}
