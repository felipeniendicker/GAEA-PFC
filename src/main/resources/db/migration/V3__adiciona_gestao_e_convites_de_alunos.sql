ALTER TABLE usuario
    ADD COLUMN matricula VARCHAR(50) NULL;

ALTER TABLE usuario
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE usuario
    ADD COLUMN ativado BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE usuario
    MODIFY COLUMN senha VARCHAR(255) NULL;

CREATE TABLE convite_ativacao_aluno (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    instituicao_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    utilizado_em DATETIME(6) NULL,
    cancelado_em DATETIME(6) NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_convite_ativacao_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_convite_ativacao_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT fk_convite_ativacao_instituicao
        FOREIGN KEY (instituicao_id) REFERENCES instituicao (id) ON DELETE RESTRICT
);

CREATE INDEX idx_usuario_instituicao_perfil
    ON usuario (instituicao_id, perfil);

ALTER TABLE usuario
    ADD CONSTRAINT uk_usuario_instituicao_matricula
        UNIQUE (instituicao_id, matricula);

CREATE INDEX idx_convite_ativacao_usuario
    ON convite_ativacao_aluno (usuario_id, criado_em);

CREATE INDEX idx_convite_ativacao_instituicao
    ON convite_ativacao_aluno (instituicao_id);
