package Gaea.Api.service;

import Gaea.Api.dto.CadastroPendenciaRequest;
import Gaea.Api.dto.PendenciaProcessoResponse;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.PendenciaProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoPendencia;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.PendenciaProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PendenciaProcessoService {

    private final PendenciaProcessoRepository pendenciaRepository;
    private final ProcessoEstagioRepository processoRepository;
    private final ProcessoAutorizacaoService autorizacaoService;
    private final LogAuditoriaService auditoriaService;

    public PendenciaProcessoService(
            PendenciaProcessoRepository pendenciaRepository,
            ProcessoEstagioRepository processoRepository,
            ProcessoAutorizacaoService autorizacaoService,
            LogAuditoriaService auditoriaService) {

        this.pendenciaRepository = pendenciaRepository;
        this.processoRepository = processoRepository;
        this.autorizacaoService = autorizacaoService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public PendenciaProcessoResponse registrar(
            Long processoId,
            CadastroPendenciaRequest request,
            Usuario responsavel) {

        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarGestaoInstitucional(processo, responsavel);

        PendenciaProcesso pendencia = new PendenciaProcesso();
        pendencia.setProcesso(processo);
        pendencia.setDescricao(request.descricao());
        pendencia.setSituacao(SituacaoPendencia.ABERTA);
        pendencia.setResponsavelRegistro(responsavel.getEmail());
        PendenciaProcesso salva = pendenciaRepository.save(pendencia);

        auditoriaService.registrar(
                responsavel.getEmail(),
                "REGISTRO_PENDENCIA",
                "Pendência " + salva.getId() + " do processo " + processo.getId(),
                processo.getInstituicao());

        return PendenciaProcessoResponse.from(salva);
    }

    @Transactional(readOnly = true)
    public List<PendenciaProcessoResponse> listar(Long processoId, Usuario usuario) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarAcesso(processo, usuario);

        return pendenciaRepository.findAllByProcessoIdOrderByCriadaEmDescIdDesc(processoId)
                .stream()
                .map(PendenciaProcessoResponse::from)
                .toList();
    }

    @Transactional
    public PendenciaProcessoResponse resolver(
            Long processoId,
            Long pendenciaId,
            Usuario responsavel) {

        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarGestaoInstitucional(processo, responsavel);

        PendenciaProcesso pendencia = pendenciaRepository
                .findByIdAndProcessoIdParaAtualizacao(pendenciaId, processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pendência não encontrada"));

        if (pendencia.getSituacao() == SituacaoPendencia.RESOLVIDA) {
            throw new ConflitoDadosException("A pendência já está resolvida");
        }

        pendencia.setSituacao(SituacaoPendencia.RESOLVIDA);
        pendencia.setResolvidaEm(LocalDateTime.now());
        PendenciaProcesso atualizada = pendenciaRepository.save(pendencia);

        auditoriaService.registrar(
                responsavel.getEmail(),
                "RESOLUCAO_PENDENCIA",
                "Pendência " + atualizada.getId() + " do processo " + processo.getId(),
                processo.getInstituicao());

        return PendenciaProcessoResponse.from(atualizada);
    }

    private ProcessoEstagio buscarProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
    }
}
