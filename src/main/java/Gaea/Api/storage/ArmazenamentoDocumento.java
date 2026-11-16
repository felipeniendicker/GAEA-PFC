package Gaea.Api.storage;

import org.springframework.core.io.Resource;

public interface ArmazenamentoDocumento {

    void armazenar(String chave, byte[] conteudo);

    Resource carregar(String chave);

    void remover(String chave);
}
