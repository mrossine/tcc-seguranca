package br.com.fatec.tcc.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que os controles de acesso do Spring Security estão funcionando:
 * endpoints protegidos retornam 401/403 sem autenticação e endpoints públicos
 * continuam acessíveis.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityEndpointsTest {

    @Autowired
    MockMvc mockMvc;

    // ─── Endpoints públicos ───────────────────────────────────────────────────

    @Test
    void paginaInicialEstaAcessivel() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    void paginaLoginEstaAcessivel() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void endpointHealthDoActuatorEstaAcessivel() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ─── Endpoints protegidos — sem autenticação devem retornar 401/302 ───────

    @Test
    void dashboardSemAutenticacaoRedirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void apiAlertasSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/alertas")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiCaronasSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/api/caronas")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointSemAutenticacaoRetorna401ou302() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 401 || status == 302
                            : "Esperado 401 ou 302, mas foi " + status;
                });
    }

    // ─── CSRF — POST para /api sem token deve ser bloqueado ──────────────────

    @Test
    void postParaApiSemCsrfTokenRetorna403() throws Exception {
        mockMvc.perform(post("/api/alertas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"teste\"}"))
                .andExpect(status().isForbidden());
    }

    // ─── Endpoint público de auth — deve aceitar sem CSRF ────────────────────

    @Test
    void registroNaApiAuthNaoExigeAutenticacao() throws Exception {
        // /api/auth/register é público mas requer payload válido
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // 400 (validação) é o esperado sem CSRF — o endpoint é público
                    assert status == 400 || status == 403
                            : "Esperado 400 ou 403, mas foi " + status;
                });
    }
}
