package br.com.fatec.tcc.controller;

import br.com.fatec.tcc.dto.UsuarioDTO;
import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/perfil")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

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
}