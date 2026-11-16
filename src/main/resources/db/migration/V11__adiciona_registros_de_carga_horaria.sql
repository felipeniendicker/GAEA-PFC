-- Sem horários de início e fim no RF30, a duplicidade considera processo,
-- data, duração e SHA-256 da atividade normalizada.
CREATE TABLE registro_carga_horaria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    registrado_por_usuario_id BIGINT NOT NULL,
    data_atividade DATE NOT NULL,
    duracao_minutos INT NOT NULL,
    atividade VARCHAR(1000) NOT NULL,
    atividade_hash VARCHAR(64) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    avaliado_por_usuario_id BIGINT NULL,
    avaliado_em DATETIME(6) NULL,
    justificativa_rejeicao VARCHAR(1000) NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_carga_horaria_duplicidade
        UNIQUE (processo_id, data_atividade, duracao_minutos, atividade_hash),
    CONSTRAINT fk_carga_horaria_processo
        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id) ON DELETE RESTRICT,
    CONSTRAINT fk_carga_horaria_aluno
        FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT fk_carga_horaria_supervisor
        FOREIGN KEY (avaliado_por_usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_carga_horaria_duracao CHECK (duracao_minutos > 0),
    CONSTRAINT ck_carga_horaria_atividade CHECK (CHAR_LENGTH(TRIM(atividade)) > 0),
    CONSTRAINT ck_carga_horaria_hash CHECK (CHAR_LENGTH(atividade_hash) = 64),
    CONSTRAINT ck_carga_horaria_situacao CHECK (
        CASE situacao
            WHEN 'PENDENTE' THEN 1
            WHEN 'APROVADO' THEN 1
            WHEN 'REJEITADO' THEN 1
            ELSE 0
        END = 1
    ),
    CONSTRAINT ck_carga_horaria_avaliacao CHECK (
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

CREATE INDEX idx_carga_horaria_processo_data
    ON registro_carga_horaria (processo_id, data_atividade);

CREATE INDEX idx_carga_horaria_processo_situacao
    ON registro_carga_horaria (processo_id, situacao);

CREATE INDEX idx_carga_horaria_supervisor
    ON registro_carga_horaria (avaliado_por_usuario_id);
