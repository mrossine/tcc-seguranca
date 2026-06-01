package br.com.fatec.tcc.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller das PÁGINAS administrativas (acesso restrito a ADMIN pelo SecurityConfig).
 * Aqui só abrimos as telas; os dados são carregados via API REST (/api/admin/...).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private JdbcTemplate jdbcTemplate;

    /** GET /admin/usuarios — tela de gerenciamento de usuários. */
    @GetMapping("/usuarios")
    public String usuarios() {
        return "admin/usuarios";
    }

    /** GET /admin/denuncias — tela com as denúncias das caronas. */
    @GetMapping("/denuncias")
    public String denuncias() {
        return "admin/denuncias";
    }
}
