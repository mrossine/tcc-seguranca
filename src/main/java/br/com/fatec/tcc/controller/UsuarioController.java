package br.com.fatec.tcc.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import br.com.fatec.tcc.dto.AlertaResponseDTO;
import br.com.fatec.tcc.dto.CaronaResponseDTO;
import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.AlertaService;
import br.com.fatec.tcc.service.CaronaService;
import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/perfil")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AlertaService alertaService;
    private final CaronaService caronaService;

    @GetMapping
    public String perfil(Model model, Authentication auth) {
        // Usando o novo método findUserByUsername
        Usuario usuario = usuarioService.findUserByUsername(auth.getName());
        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @PostMapping("/atualizar")
    public String atualizarPerfil(@ModelAttribute UsuarioDTO usuarioDTO, Authentication auth) {
        Usuario usuario = usuarioService.findUserByUsername(auth.getName());
        usuarioService.atualizarPerfil(usuario.getId(), usuarioDTO);
        return "redirect:/perfil?success";
    }

    @PostMapping("/alterar-senha")
    public String alterarSenha(@RequestParam String senhaAtual, 
                               @RequestParam String novaSenha, 
                               Authentication auth) {
        Usuario usuario = usuarioService.findUserByUsername(auth.getName());
        usuarioService.alterarSenha(usuario.getId(), senhaAtual, novaSenha);
        return "redirect:/perfil?senhaAlterada";
    }
    
    @GetMapping("/api/usuario/historico/alertas")
    @ResponseBody
    public List<AlertaResponseDTO> historicoAlertas(Authentication auth) {
        Usuario usuario = usuarioService.findUserByUsername(auth.getName());
        return alertaService.listarAlertasPorUsuario(usuario); // você precisa criar este método no AlertaService
    }

    @GetMapping("/api/usuario/historico/caronas")
    @ResponseBody
    public List<CaronaResponseDTO> historicoCaronas(Authentication auth) {
        Usuario usuario = usuarioService.findUserByUsername(auth.getName());
        return caronaService.listarCaronasPorUsuario(usuario); // crie no CaronaService
    }

    @GetMapping("/api/admin/usuarios")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioService.listarTodosUsuarios();
    }

    @DeleteMapping("/api/admin/usuarios/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletarUsuario(@PathVariable Long id) {
        try {
            usuarioService.deletarUsuario(id);
            return ResponseEntity.ok().body(Map.of("message", "Usuário excluído com sucesso"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}