ALTER TABLE solicitacao_privacidade
    DROP CONSTRAINT ck_solicitacao_privacidade_tipo;

ALTER TABLE solicitacao_privacidade
    DROP CONSTRAINT ck_solicitacao_privacidade_situacao;

ALTER TABLE solicitacao_privacidade
    ADD COLUMN atualizado_em DATETIME(6) NULL;

ALTER TABLE solicitacao_privacidade
    ADD COLUMN responsavel_usuario_id BIGINT NULL;

ALTER TABLE solicitacao_privacidade
    ADD COLUMN decisao VARCHAR(2000) NULL;

ALTER TABLE solicitacao_privacidade
    ADD COLUMN evidencia_execucao VARCHAR(2000) NULL;

ALTER TABLE solicitacao_privacidade
    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;

UPDATE solicitacao_privacidade
SET atualizado_em = criado_em
WHERE atualizado_em IS NULL;

ALTER TABLE solicitacao_privacidade
    MODIFY COLUMN atualizado_em DATETIME(6) NOT NULL;

ALTER TABLE solicitacao_privacidade
    ADD CONSTRAINT fk_solicitacao_privacidade_responsavel
        FOREIGN KEY (responsavel_usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT;

ALTER TABLE solicitacao_privacidade
    ADD CONSTRAINT ck_solicitacao_privacidade_tipo CHECK (
        tipo IN (
            'ACESSO', 'CORRECAO', 'EXPORTACAO', 'EXCLUSAO',
            'ANONIMIZACAO', 'REVOGACAO_CONSENTIMENTO', 'ENCERRAMENTO_CONTA'
        )
    );

ALTER TABLE solicitacao_privacidade
    ADD CONSTRAINT ck_solicitacao_privacidade_situacao CHECK (
        situacao IN ('PENDENTE', 'EM_ANALISE', 'CONCLUIDA', 'REJEITADA')
    );

CREATE INDEX idx_solicitacao_privacidade_situacao_data
    ON solicitacao_privacidade (situacao, criado_em);

CREATE INDEX idx_solicitacao_privacidade_responsavel
    ON solicitacao_privacidade (responsavel_usuario_id);
