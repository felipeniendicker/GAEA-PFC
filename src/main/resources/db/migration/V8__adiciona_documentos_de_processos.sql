CREATE TABLE documento_processo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    enviado_por_usuario_id BIGINT NOT NULL,
    documento_anterior_id BIGINT NULL,
    nome_original VARCHAR(255) NOT NULL,
    chave_armazenamento VARCHAR(255) NOT NULL,
    tipo_conteudo VARCHAR(100) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    enviado_em DATETIME(6) NOT NULL,
    analisado_por_usuario_id BIGINT NULL,
    analisado_em DATETIME(6) NULL,
    justificativa_reprovacao VARCHAR(1000) NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_documento_chave_armazenamento UNIQUE (chave_armazenamento),
    CONSTRAINT uk_documento_reenvio UNIQUE (documento_anterior_id),
    CONSTRAINT fk_documento_processo
        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id) ON DELETE RESTRICT,
    CONSTRAINT fk_documento_enviado_por
        FOREIGN KEY (enviado_por_usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT fk_documento_anterior
        FOREIGN KEY (documento_anterior_id) REFERENCES documento_processo (id) ON DELETE RESTRICT,
    CONSTRAINT fk_documento_analisado_por
        FOREIGN KEY (analisado_por_usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_documento_situacao
        CHECK (
            CASE situacao
                WHEN 'PENDENTE' THEN 1
                WHEN 'APROVADO' THEN 1
                WHEN 'REPROVADO' THEN 1
                ELSE 0
            END = 1
        ),
    CONSTRAINT ck_documento_tamanho CHECK (tamanho_bytes > 0)
);

CREATE INDEX idx_documento_processo_data
    ON documento_processo (processo_id, enviado_em);

CREATE INDEX idx_documento_enviado_por
    ON documento_processo (enviado_por_usuario_id);

CREATE INDEX idx_documento_analisado_por
    ON documento_processo (analisado_por_usuario_id);
