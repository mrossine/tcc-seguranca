package br.com.fatec.tcc.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private JdbcTemplate jdbcTemplate;

    @GetMapping("/usuarios")
    public String usuarios() {
        return "admin/usuarios";
    }

    @GetMapping("/denuncias")
    public String denuncias() {
        return "admin/denuncias";
    }
}
