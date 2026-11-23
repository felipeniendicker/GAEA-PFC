ALTER TABLE usuario
    ADD COLUMN credenciais_versao BIGINT NOT NULL DEFAULT 0;

CREATE TABLE solicitacao_recuperacao_senha (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NULL,
    identificador_hash VARCHAR(64) NOT NULL,
    origem_hash VARCHAR(64) NOT NULL,
    token_hash VARCHAR(64) NULL,
    criado_em DATETIME(6) NOT NULL,
    expira_em DATETIME(6) NULL,
    utilizado_em DATETIME(6) NULL,
    cancelado_em DATETIME(6) NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_recuperacao_senha_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_recuperacao_senha_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT
);

CREATE INDEX idx_recuperacao_senha_identificador_data
    ON solicitacao_recuperacao_senha (identificador_hash, criado_em);

CREATE INDEX idx_recuperacao_senha_origem_data
    ON solicitacao_recuperacao_senha (origem_hash, criado_em);

CREATE INDEX idx_recuperacao_senha_usuario_data
    ON solicitacao_recuperacao_senha (usuario_id, criado_em);
