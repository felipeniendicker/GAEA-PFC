CREATE TABLE prazo_processo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    data_vencimento DATE NOT NULL,
    criada_em DATETIME(6) NOT NULL,
    atualizada_em DATETIME(6) NOT NULL,
    responsavel_registro VARCHAR(255) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_prazo_processo
        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id) ON DELETE RESTRICT
);

CREATE INDEX idx_prazo_processo_vencimento
    ON prazo_processo (processo_id, data_vencimento);
