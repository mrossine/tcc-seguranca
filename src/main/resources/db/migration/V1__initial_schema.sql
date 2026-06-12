-- V1: Schema inicial gerado a partir das entidades JPA em 2026-06-10.
-- Em instalações existentes, execute com flyway baseline antes do primeiro deploy
-- para que o Flyway registre este estado sem re-executar as DDLs.

CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    nome_completo   VARCHAR(255)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    senha           VARCHAR(255)    NOT NULL,
    matricula       VARCHAR(255)    NOT NULL,
    curso           VARCHAR(255),
    periodo         VARCHAR(10),
    tipo_usuario    VARCHAR(15),
    foto_perfil     VARCHAR(255),
    data_cadastro   DATETIME(6),
    ativo           BIT             DEFAULT 1,
    role            VARCHAR(20)     DEFAULT 'USER',
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email     UNIQUE (email),
    CONSTRAINT uk_usuario_matricula UNIQUE (matricula)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alertas (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    titulo       VARCHAR(255)    NOT NULL,
    descricao    VARCHAR(1000),
    tipo         VARCHAR(30)     NOT NULL,
    localizacao  VARCHAR(255)    NOT NULL,
    latitude     DOUBLE,
    longitude    DOUBLE,
    data_hora    DATETIME(6)     NOT NULL,
    status       VARCHAR(20)     DEFAULT 'ATIVO',
    usuario_id   BIGINT          NOT NULL,
    data_criacao DATETIME(6),
    confirmacoes INT             DEFAULT 0,
    denuncias    INT             DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_alerta_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alerta_reacoes (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    alerta_id    BIGINT      NOT NULL,
    usuario_id   BIGINT      NOT NULL,
    tipo         VARCHAR(10) NOT NULL,
    data_criacao DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_alerta_usuario UNIQUE (alerta_id, usuario_id),
    CONSTRAINT fk_reacao_alerta  FOREIGN KEY (alerta_id)  REFERENCES alertas  (id),
    CONSTRAINT fk_reacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alerta_confirmacoes (
    id                BIGINT   NOT NULL AUTO_INCREMENT,
    alerta_id         BIGINT   NOT NULL,
    usuario_id        BIGINT   NOT NULL,
    data_confirmacao  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_confirmacao_alerta_usuario UNIQUE (alerta_id, usuario_id),
    CONSTRAINT fk_confirmacao_alerta  FOREIGN KEY (alerta_id)  REFERENCES alertas  (id),
    CONSTRAINT fk_confirmacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS denuncias_alerta (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    alerta_id      BIGINT      NOT NULL,
    denunciante_id BIGINT      NOT NULL,
    categoria      VARCHAR(30) NOT NULL,
    justificativa  VARCHAR(1000) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_denuncia  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_denuncia_alerta_usuario UNIQUE (alerta_id, denunciante_id),
    CONSTRAINT fk_denuncia_alerta_alerta      FOREIGN KEY (alerta_id)      REFERENCES alertas  (id),
    CONSTRAINT fk_denuncia_alerta_denunciante FOREIGN KEY (denunciante_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS caronas (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    motorista_id        BIGINT      NOT NULL,
    origem              VARCHAR(255) NOT NULL,
    destino             VARCHAR(255) NOT NULL,
    horario_saida       DATETIME(6) NOT NULL,
    vagas_disponiveis   INT         NOT NULL,
    veiculo_modelo      VARCHAR(255),
    veiculo_placa       VARCHAR(255),
    observacoes         VARCHAR(500),
    destino_latitude    DOUBLE,
    destino_longitude   DOUBLE,
    status              VARCHAR(20) DEFAULT 'ABERTA',
    data_criacao        DATETIME(6),
    data_finalizacao    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_carona_motorista FOREIGN KEY (motorista_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS participacoes_carona (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    carona_id         BIGINT      NOT NULL,
    passageiro_id     BIGINT      NOT NULL,
    status            VARCHAR(20) DEFAULT 'SOLICITADA',
    data_solicitacao  DATETIME(6),
    data_confirmacao  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_participacao_carona    FOREIGN KEY (carona_id)     REFERENCES caronas  (id),
    CONSTRAINT fk_participacao_passageiro FOREIGN KEY (passageiro_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mensagens_carona (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    carona_id    BIGINT   NOT NULL,
    remetente_id BIGINT   NOT NULL,
    conteudo     TEXT     NOT NULL,
    data_criacao DATETIME(6),
    lido         BIT      DEFAULT 0,
    sistema      BIT      DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_mensagem_carona_data (carona_id, data_criacao),
    CONSTRAINT fk_mensagem_carona    FOREIGN KEY (carona_id)    REFERENCES caronas  (id),
    CONSTRAINT fk_mensagem_remetente FOREIGN KEY (remetente_id) REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS avaliacoes_carona (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    carona_id      BIGINT   NOT NULL,
    passageiro_id  BIGINT   NOT NULL,
    motorista_id   BIGINT   NOT NULL,
    estrelas       INT      NOT NULL,
    comentario     VARCHAR(500),
    data_avaliacao DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_avaliacao_carona_passageiro UNIQUE (carona_id, passageiro_id),
    CONSTRAINT fk_avaliacao_carona     FOREIGN KEY (carona_id)     REFERENCES caronas  (id),
    CONSTRAINT fk_avaliacao_passageiro FOREIGN KEY (passageiro_id) REFERENCES usuarios (id),
    CONSTRAINT fk_avaliacao_motorista  FOREIGN KEY (motorista_id)  REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS denuncias_carona (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    carona_id        BIGINT      NOT NULL,
    denunciante_id   BIGINT      NOT NULL,
    denunciado_id    BIGINT      NOT NULL,
    tipo_denunciante VARCHAR(20) NOT NULL,
    categoria        VARCHAR(30) NOT NULL,
    descricao        VARCHAR(1000) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_denuncia    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_denuncia_carona_denunciante_denunciado UNIQUE (carona_id, denunciante_id, denunciado_id),
    CONSTRAINT fk_denuncia_carona_carona      FOREIGN KEY (carona_id)      REFERENCES caronas  (id),
    CONSTRAINT fk_denuncia_carona_denunciante FOREIGN KEY (denunciante_id) REFERENCES usuarios (id),
    CONSTRAINT fk_denuncia_carona_denunciado  FOREIGN KEY (denunciado_id)  REFERENCES usuarios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
