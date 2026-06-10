package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.AlertaRequestDTO;
import br.com.fatec.tcc.service.AlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller (páginas + ações de formulário) dos alertas de segurança.
 * As ações POST recebem o envio dos formulários e redirecionam de volta à lista.
 */
@Controller
@RequestMapping("/alertas")
@RequiredArgsConstructor
public class AlertaController {

    private final AlertaService alertaService;

    /** GET /alertas — abre a página com a lista de alertas. */
    @GetMapping
    public String listarAlertas(Model model) {
        return "alertas/lista";
    }

    /** GET /alertas/novo — abre o formulário de criação de alerta. */
    @GetMapping("/novo")
    public String novoAlerta(Model model) {
        model.addAttribute("alerta", AlertaRequestDTO.empty());
        return "alertas/novo";
    }

    /** POST /alertas/novo — cria o alerta a partir do formulário enviado. */
    @PostMapping("/novo")
    public String criarAlerta(@ModelAttribute AlertaRequestDTO request, Authentication auth) {
        alertaService.criarAlerta(request, auth.getName());
        return "redirect:/alertas";
    }

    /** POST /alertas/{id}/confirmar — marca o alerta como verdadeiro (+1 confirmação, uma vez por usuário). */
    @PostMapping("/{id}/confirmar")
    public String confirmarAlerta(@PathVariable Long id, Authentication auth) {
        try {
            alertaService.confirmarAlerta(id, auth.getName());
        } catch (RuntimeException e) {
            // Já confirmado ou alerta não encontrado — redireciona normalmente
        }
        return "redirect:/alertas";
    }

    /** POST /alertas/{id}/remover — remove o alerta (se o usuário tiver permissão). */
    @PostMapping("/{id}/remover")
    public String removerAlerta(@PathVariable Long id, Authentication auth) {
        alertaService.removerAlerta(id, auth.getName());
        return "redirect:/alertas";
    }
}