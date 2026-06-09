package br.com.fatec.tcc.scheduler;

import br.com.fatec.tcc.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarefas agendadas relacionadas a usuários.
 *
 * Desativa automaticamente os alunos que ultrapassaram o prazo máximo de 5 anos
 * (tempo máximo para conclusão dos cursos da Fatec) a partir do ano de ingresso
 * na matrícula. Roda na inicialização e uma vez por dia.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioScheduler {

    private final UsuarioService usuarioService;

    /** Executa diariamente (e ~30s após o startup) verificando alunos com prazo expirado. */
    @Scheduled(initialDelay = 30_000, fixedRate = 24 * 60 * 60 * 1000L)
    public void desativarAlunosExpirados() {
        int total = usuarioService.desativarAlunosExpirados();
        if (total > 0) {
            log.info("UsuarioScheduler: {} aluno(s) desativado(s) por prazo de 5 anos expirado", total);
        }
    }
}
