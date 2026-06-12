-- ─────────────────────────────────────────────────────────────────────────────
-- V2 — Cadastro de veículos do usuário e aprovação de modelos customizados
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS veiculos (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id      BIGINT       NOT NULL,
    marca           VARCHAR(100) NOT NULL,
    modelo          VARCHAR(150) NOT NULL,
    placa           VARCHAR(10)  NOT NULL,
    ano             SMALLINT,
    cor             VARCHAR(50),
    ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
    data_cadastro   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_veiculo   PRIMARY KEY (id),
    CONSTRAINT uq_veiculo_placa UNIQUE (placa),
    CONSTRAINT fk_veiculo_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS modelos_custom (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    solicitante_id      BIGINT       NOT NULL,
    marca               VARCHAR(100) NOT NULL,
    modelo              VARCHAR(150) NOT NULL,
    status              ENUM('PENDENTE','APROVADO','REJEITADO') NOT NULL DEFAULT 'PENDENTE',
    observacao_admin    VARCHAR(500),
    data_solicitacao    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_resolucao      DATETIME,
    CONSTRAINT pk_modelo_custom    PRIMARY KEY (id),
    CONSTRAINT fk_modelo_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Adiciona FK de caronas para veiculos (nullable — caronas antigas continuam válidas)
ALTER TABLE caronas ADD COLUMN veiculo_id BIGINT NULL;
ALTER TABLE caronas ADD CONSTRAINT fk_carona_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos (id);
