ALTER TABLE usuario
    ADD COLUMN email_verificado_em DATETIME(6) NULL;

CREATE TABLE token_verificacao_email (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    utilizado_em DATETIME(6) NULL,
    cancelado_em DATETIME(6) NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_token_verificacao_email_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_verificacao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT
);

CREATE INDEX idx_vinculo_supervisor_processo_fk
    ON vinculo_supervisor_processo (processo_id);

ALTER TABLE vinculo_supervisor_processo
    DROP INDEX uk_vinculo_supervisor_processo;

ALTER TABLE vinculo_supervisor_processo
    ADD COLUMN ativo BOOLEAN NULL;

UPDATE vinculo_supervisor_processo
SET ativo = CASE
    WHEN situacao IN ('PENDENTE', 'CONFIRMADO') THEN TRUE
    ELSE NULL
END;

ALTER TABLE vinculo_supervisor_processo
    ADD CONSTRAINT uk_vinculo_supervisor_ativo UNIQUE (processo_id, ativo);

CREATE TABLE historico_vinculo_supervisor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vinculo_id BIGINT NOT NULL,
    situacao_anterior VARCHAR(20) NULL,
    situacao_nova VARCHAR(20) NOT NULL,
    usuario_responsavel VARCHAR(255) NOT NULL,
    justificativa VARCHAR(1000) NULL,
    data_hora DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historico_vinculo
        FOREIGN KEY (vinculo_id) REFERENCES vinculo_supervisor_processo (id) ON DELETE RESTRICT
);

CREATE INDEX idx_token_verificacao_usuario
    ON token_verificacao_email (usuario_id, criado_em);

CREATE INDEX idx_vinculo_email_situacao_criado
    ON vinculo_supervisor_processo (email_convidado, situacao, criado_em);

CREATE INDEX idx_historico_vinculo_data
    ON historico_vinculo_supervisor (vinculo_id, data_hora);
