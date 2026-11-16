package Gaea.Api.dto;

import org.springframework.core.io.Resource;

public record ConteudoDocumento(
        String nomeOriginal,
        String tipoConteudo,
        long tamanhoBytes,
        Resource recurso) {
}
