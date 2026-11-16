package Gaea.Api.service;

import Gaea.Api.dto.AnaliseDocumentoRequest;
import Gaea.Api.dto.ConteudoDocumento;
import Gaea.Api.dto.DocumentoProcessoResponse;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.DocumentoProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoDocumento;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.DocumentoProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.storage.ArmazenamentoDocumento;
import org.apache.pdfbox.Loader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentoProcessoService {

    private static final String TIPO_PDF = "application/pdf";

    private final DocumentoProcessoRepository documentoRepository;
    private final ProcessoEstagioRepository processoRepository;
    private final ProcessoAutorizacaoService autorizacaoService;
    private final LogAuditoriaService auditoriaService;
    private final ArmazenamentoDocumento armazenamento;
    private final long tamanhoMaximoBytes;
    private final NotificacaoService notificacaoService;

    public DocumentoProcessoService(
            DocumentoProcessoRepository documentoRepository,
            ProcessoEstagioRepository processoRepository,
            ProcessoAutorizacaoService autorizacaoService,
            LogAuditoriaService auditoriaService,
            ArmazenamentoDocumento armazenamento,
            @Value("${app.documentos.tamanho-maximo-bytes:10485760}") long tamanhoMaximoBytes,
            NotificacaoService notificacaoService) {
        this.documentoRepository = documentoRepository;
        this.processoRepository = processoRepository;
        this.autorizacaoService = autorizacaoService;
        this.auditoriaService = auditoriaService;
        this.armazenamento = armazenamento;
        this.tamanhoMaximoBytes = tamanhoMaximoBytes;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public DocumentoProcessoResponse enviar(
            Long processoId,
            MultipartFile arquivo,
            Long documentoAnteriorId,
            Usuario aluno) {

        ProcessoEstagio processo = buscarProcesso(processoId);
        validarAlunoProprietario(processo, aluno);
        byte[] conteudo = validarArquivo(arquivo);
        DocumentoProcesso anterior = validarReenvio(
                processoId, documentoAnteriorId, aluno);

        String chave = UUID.randomUUID() + ".pdf";
        armazenamento.armazenar(chave, conteudo);
        removerArquivoSeTransacaoFalhar(chave);

        DocumentoProcesso documento = new DocumentoProcesso();
        documento.setProcesso(processo);
        documento.setEnviadoPor(aluno);
        documento.setDocumentoAnterior(anterior);
        documento.setNomeOriginal(nomeSeguro(arquivo.getOriginalFilename()));
        documento.setChaveArmazenamento(chave);
        documento.setTipoConteudo(TIPO_PDF);
        documento.setTamanhoBytes((long) conteudo.length);
        documento.setHashSha256(hashSha256(conteudo));
        documento.setSituacao(SituacaoDocumento.PENDENTE);
        DocumentoProcesso salvo = documentoRepository.saveAndFlush(documento);

        auditoriaService.registrar(
                aluno.getEmail(),
                anterior == null ? "ENVIO_DOCUMENTO" : "REENVIO_DOCUMENTO",
                "Documento " + salvo.getId() + " do processo " + processoId,
                processo.getInstituicao());
        return DocumentoProcessoResponse.from(salvo);
    }

    @Transactional(readOnly = true)
    public List<DocumentoProcessoResponse> listar(Long processoId, Usuario usuario) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarAcesso(processo, usuario);
        return documentoRepository.findAllByProcessoIdOrderByEnviadoEmDescIdDesc(processoId)
                .stream()
                .map(DocumentoProcessoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConteudoDocumento baixar(Long processoId, Long documentoId, Usuario usuario) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarAcesso(processo, usuario);
        DocumentoProcesso documento = buscarDocumento(processoId, documentoId);
        Resource recurso = armazenamento.carregar(documento.getChaveArmazenamento());
        return new ConteudoDocumento(
                documento.getNomeOriginal(),
                documento.getTipoConteudo(),
                documento.getTamanhoBytes(),
                recurso);
    }

    @Transactional
    public DocumentoProcessoResponse analisar(
            Long processoId,
            Long documentoId,
            AnaliseDocumentoRequest request,
            Usuario responsavel) {

        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarGestaoInstitucional(processo, responsavel);
        if (request.resultado() == SituacaoDocumento.PENDENTE) {
            throw new DadosInvalidosException("O resultado deve ser APROVADO ou REPROVADO");
        }

        DocumentoProcesso documento = documentoRepository
                .findByIdAndProcessoIdParaAtualizacao(documentoId, processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));
        if (documento.getSituacao() != SituacaoDocumento.PENDENTE) {
            throw new ConflitoDadosException("O documento já foi analisado");
        }

        documento.setSituacao(request.resultado());
        documento.setAnalisadoPor(responsavel);
        documento.setAnalisadoEm(LocalDateTime.now());
        documento.setJustificativaReprovacao(
                request.resultado() == SituacaoDocumento.REPROVADO
                        ? request.justificativaNormalizada() : null);
        DocumentoProcesso salvo = documentoRepository.save(documento);

        auditoriaService.registrar(
                responsavel.getEmail(),
                request.resultado() == SituacaoDocumento.APROVADO
                        ? "APROVACAO_DOCUMENTO" : "REPROVACAO_DOCUMENTO",
                "Documento " + salvo.getId() + " do processo " + processoId,
                processo.getInstituicao());
        notificacaoService.notificarDocumento(
                processo, salvo.getId(), request.resultado() == SituacaoDocumento.APROVADO);

        return DocumentoProcessoResponse.from(salvo);
    }

    private ProcessoEstagio buscarProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
    }

    private DocumentoProcesso buscarDocumento(Long processoId, Long documentoId) {
        return documentoRepository.findByIdAndProcessoId(documentoId, processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));
    }

    private void validarAlunoProprietario(ProcessoEstagio processo, Usuario usuario) {
        if (!"ALUNO".equals(usuario.getPerfil())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente o aluno pode enviar documentos");
        }
        autorizacaoService.validarAcesso(processo, usuario);
    }

    private DocumentoProcesso validarReenvio(
            Long processoId,
            Long documentoAnteriorId,
            Usuario aluno) {
        if (documentoAnteriorId == null) {
            return null;
        }
        DocumentoProcesso anterior = documentoRepository
                .findByIdAndProcessoIdParaAtualizacao(documentoAnteriorId, processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento anterior não encontrado"));
        if (!anterior.getEnviadoPor().getId().equals(aluno.getId())) {
            throw new RecursoNaoEncontradoException("Documento anterior não encontrado");
        }
        if (anterior.getSituacao() != SituacaoDocumento.REPROVADO) {
            throw new ConflitoDadosException("Somente documentos reprovados podem ser reenviados");
        }
        if (documentoRepository.existsByDocumentoAnteriorId(documentoAnteriorId)) {
            throw new ConflitoDadosException("O documento já possui um reenvio");
        }
        return anterior;
    }

    private byte[] validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new DadosInvalidosException("Selecione um arquivo PDF não vazio");
        }
        if (arquivo.getSize() > tamanhoMaximoBytes) {
            throw new DadosInvalidosException("O arquivo excede o tamanho máximo permitido");
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new DadosInvalidosException("O arquivo deve possuir extensão .pdf");
        }
        String tipo = arquivo.getContentType();
        if (tipo != null && !tipo.isBlank() && !TIPO_PDF.equalsIgnoreCase(tipo)) {
            throw new DadosInvalidosException("O tipo de conteúdo deve ser application/pdf");
        }
        try {
            byte[] conteudo = arquivo.getBytes();
            try (var documentoPdf = Loader.loadPDF(conteudo)) {
                if (documentoPdf.getNumberOfPages() < 1) {
                    throw new DadosInvalidosException("O PDF deve possuir ao menos uma página");
                }
            }
            return conteudo;
        } catch (IOException exception) {
            throw new DadosInvalidosException("O arquivo informado não é um PDF válido");
        }
    }

    private String nomeSeguro(String nome) {
        String apenasNome = nome == null ? "documento.pdf" : nome.replace('\\', '/');
        apenasNome = apenasNome.substring(apenasNome.lastIndexOf('/') + 1).trim();
        if (apenasNome.isBlank()
                || apenasNome.length() > 255
                || apenasNome.chars().anyMatch(Character::isISOControl)) {
            throw new DadosInvalidosException("O nome do arquivo é inválido");
        }
        return apenasNome;
    }

    private String hashSha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível", exception);
        }
    }

    private void removerArquivoSeTransacaoFalhar(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            armazenamento.remover(chave);
            throw new IllegalStateException("Transação indisponível para persistir o documento");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    armazenamento.remover(chave);
                }
            }
        });
    }
}
