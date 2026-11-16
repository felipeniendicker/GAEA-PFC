package Gaea.Api.service;

import Gaea.Api.exception.FalhaArmazenamentoException;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.DocumentoProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.storage.ArmazenamentoDocumento;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoProcessoServiceTests {

    @Mock DocumentoProcessoRepository documentoRepository;
    @Mock ProcessoEstagioRepository processoRepository;
    @Mock ProcessoAutorizacaoService autorizacaoService;
    @Mock LogAuditoriaService auditoriaService;
    @Mock ArmazenamentoDocumento armazenamento;
    @Mock NotificacaoService notificacaoService;

    @Test
    void falhaDeArmazenamentoNaoDeveCriarRegistroNemAuditoria() throws Exception {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setId(10L);
        Usuario aluno = new Usuario();
        aluno.setPerfil("ALUNO");
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        doThrow(new FalhaArmazenamentoException("falha", null))
                .when(armazenamento).armazenar(anyString(), any(byte[].class));
        DocumentoProcessoService service = new DocumentoProcessoService(
                documentoRepository,
                processoRepository,
                autorizacaoService,
                auditoriaService,
                armazenamento,
                10 * 1024 * 1024,
                notificacaoService);

        assertThrows(FalhaArmazenamentoException.class, () -> service.enviar(
                10L,
                new MockMultipartFile("arquivo", "documento.pdf", "application/pdf", pdf()),
                null,
                aluno));

        verify(documentoRepository, never()).saveAndFlush(any());
        verify(auditoriaService, never()).registrar(anyString(), anyString(), anyString(), any());
    }

    private byte[] pdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}
