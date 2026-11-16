CREATE TABLE notificacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    destinatario_usuario_id BIGINT NOT NULL,
    tipo_evento VARCHAR(60) NOT NULL,
    titulo VARCHAR(160) NOT NULL,
    mensagem VARCHAR(1000) NOT NULL,
    criada_em DATETIME(6) NOT NULL,
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    lida_em DATETIME(6) NULL,
    processo_id BIGINT NULL,
    chave_idempotencia VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notificacao_destinatario_chave
        UNIQUE (destinatario_usuario_id, chave_idempotencia),
    CONSTRAINT fk_notificacao_destinatario
        FOREIGN KEY (destinatario_usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacao_processo
        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id) ON DELETE SET NULL
);

CREATE INDEX idx_notificacao_destinatario_data
    ON notificacao (destinatario_usuario_id, criada_em);

CREATE INDEX idx_notificacao_nao_lida
    ON notificacao (destinatario_usuario_id, lida);
