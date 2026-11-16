CREATE TABLE plano_atividades (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    criado_por_usuario_id BIGINT NOT NULL,
    carga_horaria_planejada DECIMAL(7,2) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    avaliado_por_usuario_id BIGINT NULL,
    avaliado_em DATETIME(6) NULL,
    justificativa_rejeicao VARCHAR(1000) NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_plano_atividades_processo UNIQUE (processo_id),
    CONSTRAINT fk_plano_atividades_processo
        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id) ON DELETE RESTRICT,
    CONSTRAINT fk_plano_atividades_criador
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT fk_plano_atividades_avaliador
        FOREIGN KEY (avaliado_por_usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_plano_atividades_carga CHECK (carga_horaria_planejada > 0),
    CONSTRAINT ck_plano_atividades_situacao CHECK (
        CASE situacao
            WHEN 'PENDENTE' THEN 1
            WHEN 'APROVADO' THEN 1
            WHEN 'REJEITADO' THEN 1
            ELSE 0
        END = 1
    ),
    CONSTRAINT ck_plano_atividades_avaliacao CHECK (
        (situacao = 'PENDENTE'
            AND avaliado_por_usuario_id IS NULL
            AND avaliado_em IS NULL
            AND justificativa_rejeicao IS NULL)
        OR
        (situacao = 'APROVADO'
            AND avaliado_por_usuario_id IS NOT NULL
            AND avaliado_em IS NOT NULL
            AND justificativa_rejeicao IS NULL)
        OR
        (situacao = 'REJEITADO'
            AND avaliado_por_usuario_id IS NOT NULL
            AND avaliado_em IS NOT NULL
            AND justificativa_rejeicao IS NOT NULL
            AND CHAR_LENGTH(TRIM(justificativa_rejeicao)) > 0)
    )
);

CREATE INDEX idx_plano_atividades_situacao
    ON plano_atividades (situacao);

CREATE TABLE atividade_plano (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plano_id BIGINT NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    ordem INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_atividade_plano_ordem UNIQUE (plano_id, ordem),
    CONSTRAINT fk_atividade_plano
        FOREIGN KEY (plano_id) REFERENCES plano_atividades (id) ON DELETE CASCADE,
    CONSTRAINT ck_atividade_plano_descricao
        CHECK (CHAR_LENGTH(TRIM(descricao)) > 0),
    CONSTRAINT ck_atividade_plano_ordem CHECK (ordem > 0)
);

CREATE INDEX idx_atividade_plano
    ON atividade_plano (plano_id);
