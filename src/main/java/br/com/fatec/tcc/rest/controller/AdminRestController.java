package br.com.fatec.tcc.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRestController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/sp-total-usuarios")
    public Map<String, Long> chamarProcedureTotalUsuarios() {
        Long total = jdbcTemplate.queryForObject("CALL sp_total_usuarios()", (rs, rowNum) -> rs.getLong("total"));
        return Map.of("total", total);
    }
}