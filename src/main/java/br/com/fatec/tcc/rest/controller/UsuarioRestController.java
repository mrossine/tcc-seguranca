package br.com.fatec.tcc.rest.controller;

import br.com.fatec.tcc.model.Usuario;
import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuario")
@RequiredArgsConstructor
public class UsuarioRestController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication auth) {
        Usuario u = usuarioService.findUserByUsername(auth.getName());
        Map<String, Object> response = new HashMap<>();
        response.put("id",           u.getId());
        response.put("nomeCompleto", u.getNomeCompleto());
        response.put("email",        u.getEmail());
        response.put("matricula",    u.getMatricula());
        response.put("curso",        u.getCurso());
        response.put("periodo",      u.getPeriodo());
        response.put("fotoPerfil",   u.getFotoPerfil());
        response.put("dataCadastro", u.getDataCadastro());
        response.put("ativo",        u.getAtivo());
        response.put("role",         u.getRole());
        return ResponseEntity.ok(response);
    }
}
