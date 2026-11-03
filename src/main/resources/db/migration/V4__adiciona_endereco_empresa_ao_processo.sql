ALTER TABLE processo_estagio
    ADD COLUMN cep_empresa VARCHAR(8) NULL;

ALTER TABLE processo_estagio
    ADD COLUMN endereco_empresa VARCHAR(500) NULL;
