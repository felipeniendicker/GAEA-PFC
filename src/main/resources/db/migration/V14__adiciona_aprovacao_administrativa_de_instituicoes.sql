CREATE TABLE solicitacao_instituicao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome_institucional VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    responsavel_nome VARCHAR(255) NOT NULL,
    email_institucional VARCHAR(255) NOT NULL,
    origem_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    email_verificado_em DATETIME(6) NULL,
    justificativa VARCHAR(1000) NULL,
    analisada_em DATETIME(6) NULL,
    analisada_por_usuario_id BIGINT NULL,
    instituicao_criada_id BIGINT NULL,
    criada_em DATETIME(6) NOT NULL,
    atualizada_em DATETIME(6) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_solicitacao_instituicao_cnpj UNIQUE (cnpj),
    CONSTRAINT uk_solicitacao_instituicao_email UNIQUE (email_institucional),
    CONSTRAINT fk_solicitacao_instituicao_admin FOREIGN KEY (analisada_por_usuario_id)
        REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitacao_instituicao_criada FOREIGN KEY (instituicao_criada_id)
        REFERENCES instituicao (id) ON DELETE RESTRICT
);

CREATE INDEX idx_solicitacao_instituicao_status_data
    ON solicitacao_instituicao (status, criada_em);
CREATE INDEX idx_solicitacao_instituicao_origem_data
    ON solicitacao_instituicao (origem_hash, criada_em);

CREATE TABLE token_fluxo_instituicao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solicitacao_id BIGINT NULL,
    usuario_id BIGINT NULL,
    tipo VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    utilizado_em DATETIME(6) NULL,
    cancelado_em DATETIME(6) NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_token_fluxo_instituicao_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_fluxo_solicitacao FOREIGN KEY (solicitacao_id)
        REFERENCES solicitacao_instituicao (id) ON DELETE RESTRICT,
    CONSTRAINT fk_token_fluxo_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE RESTRICT
);

CREATE INDEX idx_token_fluxo_solicitacao ON token_fluxo_instituicao (solicitacao_id, criado_em);
CREATE INDEX idx_token_fluxo_usuario ON token_fluxo_instituicao (usuario_id, criado_em);
