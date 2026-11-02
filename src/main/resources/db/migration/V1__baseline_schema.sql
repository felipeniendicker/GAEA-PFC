CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    perfil VARCHAR(255) NOT NULL,
    termos_aceitos BOOLEAN,
    termos_aceitos_em DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE processo_estagio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome_aluno VARCHAR(255),
    email_aluno VARCHAR(255),
    nome_empresa VARCHAR(255),
    cnpj_empresa VARCHAR(255),
    data_inicio DATE,
    data_fim DATE,
    status VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE log_auditoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario VARCHAR(255),
    acao VARCHAR(255),
    recurso VARCHAR(255),
    data_hora DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE historico_status_processo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    status_anterior VARCHAR(20) NOT NULL,
    status_novo VARCHAR(20) NOT NULL,
    usuario_responsavel VARCHAR(255) NOT NULL,
    data_hora DATETIME(6) NOT NULL,
    justificativa VARCHAR(1000),
    tipo_operacao VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historico_processo
        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id)
);

CREATE INDEX idx_historico_processo_data
    ON historico_status_processo (processo_id, data_hora);
