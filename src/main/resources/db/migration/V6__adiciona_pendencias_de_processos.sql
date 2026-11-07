CREATE TABLE pendencia_processo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    criada_em DATETIME(6) NOT NULL,
    resolvida_em DATETIME(6) NULL,
    responsavel_registro VARCHAR(255) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_pendencia_processo
        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id) ON DELETE RESTRICT,
    CONSTRAINT ck_pendencia_situacao
        CHECK (
            CASE situacao
                WHEN 'ABERTA' THEN 1
                WHEN 'RESOLVIDA' THEN 1
                ELSE 0
            END = 1
        )
);

CREATE INDEX idx_pendencia_processo_data
    ON pendencia_processo (processo_id, criada_em);
