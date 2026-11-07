package Gaea.Api.service;

import Gaea.Api.dto.AtualizacaoPrazoRequest;
import Gaea.Api.dto.CadastroPrazoRequest;
import Gaea.Api.dto.PrazoProcessoResponse;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.PrazoProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.PrazoProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PrazoProcessoService {

    private final PrazoProcessoRepository prazoRepository;
    private final ProcessoEstagioRepository processoRepository;
    private final ProcessoAutorizacaoService autorizacaoService;
    private final LogAuditoriaService auditoriaService;

    public PrazoProcessoService(
            PrazoProcessoRepository prazoRepository,
            ProcessoEstagioRepository processoRepository,
            ProcessoAutorizacaoService autorizacaoService,
            LogAuditoriaService auditoriaService) {

        this.prazoRepository = prazoRepository;
        this.processoRepository = processoRepository;
        this.autorizacaoService = autorizacaoService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public PrazoProcessoResponse registrar(
            Long processoId,
            CadastroPrazoRequest request,
            Usuario responsavel) {

        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarGestaoInstitucional(processo, responsavel);

        PrazoProcesso prazo = new PrazoProcesso();
        prazo.setProcesso(processo);
        prazo.setDescricao(request.descricao());
        prazo.setDataVencimento(request.dataVencimento());
        prazo.setResponsavelRegistro(responsavel.getEmail());
        PrazoProcesso salvo = prazoRepository.saveAndFlush(prazo);

        auditoriaService.registrar(
                responsavel.getEmail(),
                "REGISTRO_PRAZO",
                "Prazo " + salvo.getId() + " do processo " + processo.getId(),
                processo.getInstituicao());

        return PrazoProcessoResponse.from(salvo);
    }

    @Transactional(readOnly = true)
    public List<PrazoProcessoResponse> listar(Long processoId, Usuario usuario) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarAcesso(processo, usuario);

        return prazoRepository.findAllByProcessoIdOrderByDataVencimentoAscIdAsc(processoId)
                .stream()
                .map(PrazoProcessoResponse::from)
                .toList();
    }

    @Transactional
    public PrazoProcessoResponse atualizar(
            Long processoId,
            Long prazoId,
            AtualizacaoPrazoRequest request,
            Usuario responsavel) {

        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarGestaoInstitucional(processo, responsavel);

        PrazoProcesso prazo = prazoRepository
                .findByIdAndProcessoIdParaAtualizacao(prazoId, processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prazo não encontrado"));

        prazo.setDescricao(request.descricao());
        prazo.setDataVencimento(request.dataVencimento());
        prazo.setAtualizadaEm(LocalDateTime.now());
        PrazoProcesso atualizado = prazoRepository.saveAndFlush(prazo);

        auditoriaService.registrar(
                responsavel.getEmail(),
                "ATUALIZACAO_PRAZO",
                "Prazo " + atualizado.getId() + " do processo " + processo.getId(),
                processo.getInstituicao());

        return PrazoProcessoResponse.from(atualizado);
    }

    private ProcessoEstagio buscarProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
    }
}
