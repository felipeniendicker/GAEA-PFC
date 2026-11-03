package Gaea.Api.service;

import Gaea.Api.dto.CadastroProcessoRequest;
import Gaea.Api.dto.HistoricoStatusResponse;
import Gaea.Api.dto.ReaberturaProcessoRequest;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.HistoricoStatusProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.TipoOperacaoStatus;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProcessoEstagioService {

    private final ProcessoEstagioRepository repository;
    private final HistoricoStatusProcessoRepository historicoRepository;
    private final LogAuditoriaService logAuditoriaService;
    private final ProcessoAutorizacaoService autorizacaoService;
    private final VinculoSupervisorProcessoRepository vinculoRepository;

    public ProcessoEstagioService(
            ProcessoEstagioRepository repository,
            HistoricoStatusProcessoRepository historicoRepository,
            LogAuditoriaService logAuditoriaService,
            ProcessoAutorizacaoService autorizacaoService,
            VinculoSupervisorProcessoRepository vinculoRepository) {

        this.repository = repository;
        this.historicoRepository = historicoRepository;
        this.logAuditoriaService = logAuditoriaService;
        this.autorizacaoService = autorizacaoService;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional
    public ProcessoEstagio cadastrar(CadastroProcessoRequest dados, Usuario alunoAutenticado) {
        validarAlunoAptoParaSolicitar(alunoAutenticado);

        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno(alunoAutenticado.getNome());
        processo.setEmailAluno(alunoAutenticado.getEmail());
        processo.setAluno(alunoAutenticado);
        processo.setInstituicao(alunoAutenticado.getInstituicao());
        processo.setNomeEmpresa(dados.nomeEmpresa());
        processo.setCnpjEmpresa(dados.cnpjEmpresa());
        processo.setCepEmpresa(dados.cepEmpresa());
        processo.setEnderecoEmpresa(dados.enderecoEmpresa());
        processo.setDataInicio(dados.dataInicio());
        processo.setDataFim(dados.dataFim());
        processo.setStatus("PENDENTE");
        ProcessoEstagio processoSalvo = repository.save(processo);

        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processoSalvo);
        vinculo.setEmailConvidado(dados.emailSupervisor());
        vinculo.setSituacao(SituacaoVinculoSupervisor.PENDENTE);
        vinculoRepository.save(vinculo);

        logAuditoriaService.registrar(
                alunoAutenticado.getEmail(),
                "SOLICITACAO_ESTAGIO",
                "Processo de estágio " + processoSalvo.getId(),
                alunoAutenticado.getInstituicao());

        return processoSalvo;
    }

    @Transactional(readOnly = true)
    public List<ProcessoEstagio> listarParaUsuario(Usuario usuario) {
        return switch (usuario.getPerfil()) {
            case "ALUNO" -> repository.findAllByAlunoId(usuario.getId());
            case "INSTITUICAO" -> usuario.getInstituicao() == null
                    ? List.of()
                    : repository.findAllByInstituicaoId(usuario.getInstituicao().getId());
            case "EMPRESA" -> repository.findAllBySupervisorConfirmadoId(usuario.getId());
            default -> List.of();
        };
    }

    @Transactional
    public ProcessoEstagio alterarStatus(Long id, String novoStatus, Usuario usuarioResponsavel) {
        ProcessoEstagio processo = buscarProcesso(id);
        autorizacaoService.validarGestaoInstitucional(processo, usuarioResponsavel);
        String statusAnterior = processo.getStatus();
        String statusNormalizado = normalizarStatus(novoStatus);

        validarTransicaoNormal(statusAnterior, statusNormalizado);
        if ("PENDENTE".equals(statusAnterior)
                && "EM_ANALISE".equals(statusNormalizado)
                && !vinculoRepository.existsByProcessoIdAndSituacao(
                        processo.getId(), SituacaoVinculoSupervisor.CONFIRMADO)) {
            throw new ConflitoDadosException(
                    "A análise só pode ser iniciada após a confirmação do supervisor");
        }

        return persistirAlteracao(
                processo,
                statusAnterior,
                statusNormalizado,
                usuarioResponsavel.getEmail(),
                null,
                TipoOperacaoStatus.ALTERACAO_NORMAL,
                "ALTERACAO_STATUS");
    }

    @Transactional
    public ProcessoEstagio reabrir(
            Long id,
            ReaberturaProcessoRequest request,
            Usuario usuarioResponsavel) {

        ProcessoEstagio processo = buscarProcesso(id);
        autorizacaoService.validarGestaoInstitucional(processo, usuarioResponsavel);
        String statusAnterior = processo.getStatus();
        String justificativa = request.justificativa().trim();

        if (!"APROVADO".equals(statusAnterior) && !"REPROVADO".equals(statusAnterior)) {
            throw new ConflitoDadosException(
                    "Somente processos aprovados ou reprovados podem ser reabertos");
        }

        if (justificativa.length() < 10) {
            throw new ConflitoDadosException("Informe uma justificativa significativa para a reabertura");
        }

        return persistirAlteracao(
                processo,
                statusAnterior,
                "EM_ANALISE",
                usuarioResponsavel.getEmail(),
                justificativa,
                TipoOperacaoStatus.REABERTURA,
                "REABERTURA_PROCESSO");
    }

    @Transactional(readOnly = true)
    public List<HistoricoStatusResponse> listarHistorico(Long id, Usuario usuario) {
        ProcessoEstagio processo = buscarProcesso(id);
        autorizacaoService.validarAcesso(processo, usuario);

        return historicoRepository.findByProcessoIdOrderByDataHoraAscIdAsc(id)
                .stream()
                .map(HistoricoStatusResponse::from)
                .toList();
    }

    private ProcessoEstagio persistirAlteracao(
            ProcessoEstagio processo,
            String statusAnterior,
            String statusNovo,
            String usuarioResponsavel,
            String justificativa,
            TipoOperacaoStatus tipoOperacao,
            String acaoAuditoria) {

        processo.setStatus(statusNovo);
        ProcessoEstagio processoAtualizado = repository.save(processo);

        HistoricoStatusProcesso historico = new HistoricoStatusProcesso(
                processoAtualizado,
                statusAnterior,
                statusNovo,
                usuarioResponsavel,
                justificativa,
                tipoOperacao);
        historicoRepository.save(historico);

        logAuditoriaService.registrar(
                usuarioResponsavel,
                acaoAuditoria,
                "Processo " + processo.getId() + ": " + statusAnterior + " -> " + statusNovo,
                processoAtualizado.getInstituicao());

        return processoAtualizado;
    }

    private void validarTransicaoNormal(String statusAnterior, String statusNovo) {
        if (statusAnterior == null || statusAnterior.equals(statusNovo)) {
            throw new ConflitoDadosException("O processo já está no status informado");
        }

        boolean permitida =
                ("PENDENTE".equals(statusAnterior) && "EM_ANALISE".equals(statusNovo))
                || ("EM_ANALISE".equals(statusAnterior)
                && ("APROVADO".equals(statusNovo) || "REPROVADO".equals(statusNovo)));

        if (!permitida) {
            throw new ConflitoDadosException(
                    "Transição de " + statusAnterior + " para " + statusNovo + " não permitida");
        }
    }

    private String normalizarStatus(String status) {
        if (status == null) {
            throw new ConflitoDadosException("Status desconhecido");
        }

        String statusNormalizado = status.trim();
        if (!List.of("PENDENTE", "EM_ANALISE", "APROVADO", "REPROVADO")
                .contains(statusNormalizado)) {
            throw new ConflitoDadosException("Status desconhecido: " + statusNormalizado);
        }

        return statusNormalizado;
    }

    private ProcessoEstagio buscarProcesso(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
    }

    private void validarAlunoAptoParaSolicitar(Usuario aluno) {
        if (aluno == null || !"ALUNO".equals(aluno.getPerfil())) {
            throw new AccessDeniedException("Somente alunos podem solicitar estágio");
        }
        if (!Boolean.TRUE.equals(aluno.getAtivo()) || !Boolean.TRUE.equals(aluno.getAtivado())) {
            throw new AccessDeniedException("A conta do aluno não está ativa");
        }
        if (aluno.getInstituicao() == null
                || aluno.getMatricula() == null
                || aluno.getMatricula().isBlank()) {
            throw new ConflitoDadosException(
                    "O aluno precisa ter instituição e matrícula válidas para solicitar estágio");
        }
    }
}
