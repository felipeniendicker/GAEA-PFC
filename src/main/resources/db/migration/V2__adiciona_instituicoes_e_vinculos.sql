CREATE TABLE instituicao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criada_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_instituicao_cnpj UNIQUE (cnpj)
);

ALTER TABLE usuario
    ADD COLUMN instituicao_id BIGINT NULL;

ALTER TABLE processo_estagio
    ADD COLUMN aluno_usuario_id BIGINT NULL;

ALTER TABLE processo_estagio
    ADD COLUMN instituicao_id BIGINT NULL;

ALTER TABLE processo_estagio
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;

ALTER TABLE log_auditoria
    ADD COLUMN instituicao_id BIGINT NULL;

CREATE TABLE vinculo_supervisor_processo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    email_convidado VARCHAR(255) NOT NULL,
    supervisor_usuario_id BIGINT NULL,
    situacao VARCHAR(20) NOT NULL,
    justificativa_recusa VARCHAR(1000) NULL,
    criado_em DATETIME(6) NOT NULL,
    respondido_em DATETIME(6) NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_vinculo_supervisor_processo UNIQUE (processo_id),
    CONSTRAINT fk_vinculo_supervisor_processo
        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id) ON DELETE RESTRICT,
    CONSTRAINT fk_vinculo_supervisor_usuario
        FOREIGN KEY (supervisor_usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT
);

CREATE INDEX idx_usuario_instituicao
    ON usuario (instituicao_id);

CREATE INDEX idx_processo_aluno
    ON processo_estagio (aluno_usuario_id);

CREATE INDEX idx_processo_instituicao
    ON processo_estagio (instituicao_id);

CREATE INDEX idx_log_auditoria_instituicao
    ON log_auditoria (instituicao_id);

CREATE INDEX idx_vinculo_supervisor_email_situacao
    ON vinculo_supervisor_processo (email_convidado, situacao);

CREATE INDEX idx_vinculo_supervisor_usuario_situacao
    ON vinculo_supervisor_processo (supervisor_usuario_id, situacao);

ALTER TABLE usuario
    ADD CONSTRAINT fk_usuario_instituicao
        FOREIGN KEY (instituicao_id) REFERENCES instituicao (id) ON DELETE RESTRICT;

ALTER TABLE processo_estagio
    ADD CONSTRAINT fk_processo_aluno
        FOREIGN KEY (aluno_usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT;

ALTER TABLE processo_estagio
    ADD CONSTRAINT fk_processo_instituicao
        FOREIGN KEY (instituicao_id) REFERENCES instituicao (id) ON DELETE RESTRICT;

ALTER TABLE log_auditoria
    ADD CONSTRAINT fk_log_auditoria_instituicao
        FOREIGN KEY (instituicao_id) REFERENCES instituicao (id) ON DELETE RESTRICT;
