-- ============================================================================
-- Stored procedures do projeto
--
-- Este script é executado automaticamente pelo Spring Boot no startup
-- (config: spring.sql.init.* no application.properties).
--
-- IMPORTANTE: o separador de comandos configurado é ";;" (dois ponto-e-vírgula).
-- Por isso o ";" interno do corpo da procedure NÃO encerra o comando — só o ";;".
-- Cada DROP/CREATE é idempotente, então pode rodar a cada inicialização.
-- ============================================================================

-- sp_total_usuarios: retorna a quantidade total de usuários cadastrados.
DROP PROCEDURE IF EXISTS sp_total_usuarios;;

CREATE PROCEDURE sp_total_usuarios()
BEGIN
    SELECT COUNT(*) AS total FROM usuarios;
END;;
