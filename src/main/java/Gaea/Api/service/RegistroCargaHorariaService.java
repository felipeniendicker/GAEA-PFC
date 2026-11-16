package Gaea.Api.service;

import Gaea.Api.dto.AvaliacaoCargaHorariaRequest;
import Gaea.Api.dto.CadastroCargaHorariaRequest;
import Gaea.Api.dto.RegistroCargaHorariaResponse;
import Gaea.Api.dto.ResumoCargaHorariaResponse;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.RegistroCargaHoraria;
import Gaea.Api.model.SituacaoCargaHoraria;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.RegistroCargaHorariaRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class RegistroCargaHorariaService {

    private final RegistroCargaHorariaRepository registroRepository;
    private final ProcessoEstagioRepository processoRepository;
    private final VinculoSupervisorProcessoRepository vinculoRepository;
    private final ProcessoAutorizacaoService autorizacaoService;
    private final LogAuditoriaService auditoriaService;

    public RegistroCargaHorariaService(
            RegistroCargaHorariaRepository registroRepository,
            ProcessoEstagioRepository processoRepository,
            VinculoSupervisorProcessoRepository vinculoRepository,
            ProcessoAutorizacaoService autorizacaoService,
            LogAuditoriaService auditoriaService) {
        this.registroRepository = registroRepository;
        this.processoRepository = processoRepository;
        this.vinculoRepository = vinculoRepository;
        this.autorizacaoService = autorizacaoService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public RegistroCargaHorariaResponse cadastrar(
            Long processoId,
            CadastroCargaHorariaRequest request,
            Usuario aluno) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        validarAlunoProprietario(processo, aluno);

        String atividade = normalizarAtividadeParaExibicao(request.atividade());
        if (atividade.length() > 1000) {
            throw new DadosInvalidosException("A atividade deve ter no máximo 1000 caracteres");
        }
        String atividadeHash = calcularHash(normalizarAtividadeParaChave(atividade));
        if (registroRepository
                .existsByProcessoIdAndDataAtividadeAndDuracaoMinutosAndAtividadeHash(
                        processoId, request.dataAtividade(), request.duracaoMinutos(), atividadeHash)) {
            throw new ConflitoDadosException("Este registro de carga horária já foi cadastrado");
        }

        RegistroCargaHoraria registro = new RegistroCargaHoraria();
        registro.setProcesso(processo);
        registro.setRegistradoPor(aluno);
        registro.setDataAtividade(request.dataAtividade());
        registro.setDuracaoMinutos(request.duracaoMinutos());
        registro.setAtividade(atividade);
        registro.setAtividadeHash(atividadeHash);
        registro.setSituacao(SituacaoCargaHoraria.PENDENTE);
        RegistroCargaHoraria salvo;
        try {
            salvo = registroRepository.saveAndFlush(registro);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflitoDadosException("Este registro de carga horária já foi cadastrado");
        }

        auditoriaService.registrar(
                aluno.getEmail(),
                "REGISTRO_CARGA_HORARIA",
                "Registro " + salvo.getId() + " do processo " + processoId,
                processo.getInstituicao());
        return RegistroCargaHorariaResponse.from(salvo);
    }

    @Transactional(readOnly = true)
    public List<RegistroCargaHorariaResponse> listar(Long processoId, Usuario usuario) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarAcesso(processo, usuario);
        return registroRepository
                .findAllByProcessoIdOrderByDataAtividadeDescCriadoEmDescIdDesc(processoId)
                .stream()
                .map(RegistroCargaHorariaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumoCargaHorariaResponse resumir(Long processoId, Usuario usuario) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarAcesso(processo, usuario);
        Object[] totais = registroRepository.calcularTotais(processoId).get(0);
        long registrado = numero(totais[0]);
        long aprovado = numero(totais[1]);
        long pendente = numero(totais[2]);
        long rejeitado = numero(totais[3]);
        if (registrado != aprovado + pendente + rejeitado) {
            throw new IllegalStateException("Os totais da carga horária estão inconsistentes");
        }
        return new ResumoCargaHorariaResponse(registrado, aprovado, pendente, rejeitado);
    }

    @Transactional
    public RegistroCargaHorariaResponse avaliar(
            Long processoId,
            Long registroId,
            AvaliacaoCargaHorariaRequest request,
            Usuario supervisor) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        validarPerfilSupervisor(supervisor);
        validarVinculoConfirmadoComBloqueio(processoId, supervisor);
        if (request.resultado() == SituacaoCargaHoraria.PENDENTE) {
            throw new DadosInvalidosException("O resultado deve ser APROVADO ou REJEITADO");
        }

        RegistroCargaHoraria registro = registroRepository
                .findByIdAndProcessoIdParaAtualizacao(registroId, processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Registro de carga horária não encontrado"));
        if (registro.getSituacao() != SituacaoCargaHoraria.PENDENTE) {
            throw new ConflitoDadosException("O registro de carga horária já foi avaliado");
        }

        registro.setSituacao(request.resultado());
        registro.setAvaliadoPor(supervisor);
        registro.setAvaliadoEm(LocalDateTime.now());
        registro.setJustificativaRejeicao(
                request.resultado() == SituacaoCargaHoraria.REJEITADO
                        ? request.justificativaNormalizada() : null);
        RegistroCargaHoraria salvo = registroRepository.saveAndFlush(registro);

        auditoriaService.registrar(
                supervisor.getEmail(),
                request.resultado() == SituacaoCargaHoraria.APROVADO
                        ? "APROVACAO_CARGA_HORARIA" : "REJEICAO_CARGA_HORARIA",
                "Registro " + salvo.getId() + " do processo " + processoId,
                processo.getInstituicao());
        return RegistroCargaHorariaResponse.from(salvo);
    }

    private ProcessoEstagio buscarProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
    }

    private void validarAlunoProprietario(ProcessoEstagio processo, Usuario usuario) {
        if (usuario == null || !"ALUNO".equals(usuario.getPerfil())) {
            throw new AccessDeniedException("Somente o aluno pode registrar carga horária");
        }
        autorizacaoService.validarAcesso(processo, usuario);
    }

    private void validarPerfilSupervisor(Usuario usuario) {
        if (usuario == null || !"EMPRESA".equals(usuario.getPerfil())) {
            throw new AccessDeniedException("Somente o supervisor pode avaliar carga horária");
        }
    }

    private void validarVinculoConfirmadoComBloqueio(Long processoId, Usuario supervisor) {
        VinculoSupervisorProcesso vinculo = vinculoRepository
                .findAtivoByProcessoIdParaAtualizacao(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
        if (vinculo.getSupervisor() == null
                || !vinculo.getSupervisor().getId().equals(supervisor.getId())
                || vinculo.getSituacao() != SituacaoVinculoSupervisor.CONFIRMADO) {
            throw new RecursoNaoEncontradoException("Processo não encontrado");
        }
    }

    private String normalizarAtividadeParaExibicao(String atividade) {
        return Normalizer.normalize(atividade, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String normalizarAtividadeParaChave(String atividade) {
        return atividade.toLowerCase(Locale.ROOT);
    }

    private String calcularHash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível", exception);
        }
    }

    private long numero(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }
}
