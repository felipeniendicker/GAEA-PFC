package Gaea.Api.storage;

import Gaea.Api.exception.FalhaArmazenamentoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@ConditionalOnProperty(
        name = "app.documentos.storage.tipo",
        havingValue = "local",
        matchIfMissing = true)
public class ArmazenamentoLocalDocumento implements ArmazenamentoDocumento {

    private final Path diretorioBase;

    public ArmazenamentoLocalDocumento(
            @Value("${app.documentos.storage.diretorio:${java.io.tmpdir}/gaea-documentos}")
            String diretorio) {
        this.diretorioBase = Path.of(diretorio).toAbsolutePath().normalize();
    }

    @Override
    public void armazenar(String chave, byte[] conteudo) {
        Path destino = resolver(chave);
        Path temporario = null;
        try {
            Files.createDirectories(diretorioBase);
            temporario = Files.createTempFile(diretorioBase, ".upload-", ".tmp");
            Files.write(temporario, conteudo);
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            removerTemporario(temporario);
            throw new FalhaArmazenamentoException(
                    "Não foi possível armazenar o documento", exception);
        }
    }

    @Override
    public Resource carregar(String chave) {
        Path arquivo = resolver(chave);
        if (!Files.isRegularFile(arquivo) || !Files.isReadable(arquivo)) {
            throw new FalhaArmazenamentoException(
                    "O conteúdo do documento não está disponível", null);
        }
        return new FileSystemResource(arquivo);
    }

    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(resolver(chave));
        } catch (IOException exception) {
            throw new FalhaArmazenamentoException(
                    "Não foi possível remover um documento incompleto", exception);
        }
    }

    private Path resolver(String chave) {
        if (chave == null || !chave.matches("[a-f0-9-]+\\.pdf")) {
            throw new FalhaArmazenamentoException("Chave de armazenamento inválida", null);
        }
        Path resolvido = diretorioBase.resolve(chave).normalize();
        if (!resolvido.startsWith(diretorioBase)) {
            throw new FalhaArmazenamentoException("Chave de armazenamento inválida", null);
        }
        return resolvido;
    }

    private void removerTemporario(Path temporario) {
        if (temporario == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException ignored) {
            // A falha original de armazenamento continua sendo a causa relevante.
        }
    }
}
