CREATE TABLE IF NOT EXISTS historico_status_processo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    status_anterior VARCHAR(20) NOT NULL,
    status_novo VARCHAR(20) NOT NULL,
    usuario_responsavel VARCHAR(255) NOT NULL,
    data_hora DATETIME(6) NOT NULL,
    justificativa VARCHAR(1000) NULL,
    tipo_operacao VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_historico_processo_data (processo_id, data_hora),
    CONSTRAINT fk_historico_processo
        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id)
);
