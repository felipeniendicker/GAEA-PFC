CREATE TABLE solicitacao_privacidade (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    descricao VARCHAR(2000) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_solicitacao_privacidade_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_solicitacao_privacidade_tipo CHECK (
        tipo IN ('ACESSO', 'CORRECAO')
    ),
    CONSTRAINT ck_solicitacao_privacidade_situacao CHECK (
        situacao = 'PENDENTE'
    )
);

CREATE INDEX idx_solicitacao_privacidade_usuario_data
    ON solicitacao_privacidade (usuario_id, criado_em);
