package Gaea.Api.service;

import Gaea.Api.dto.AlunoInstituicaoResponse;
import Gaea.Api.dto.AtualizacaoAlunoInstituicaoRequest;
import Gaea.Api.dto.CadastroAlunoInstituicaoRequest;
import Gaea.Api.dto.ConviteAlunoResponse;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.ConviteAtivacaoAluno;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AlunoInstituicaoService {

    private final UsuarioRepository usuarioRepository;
    private final ConviteAlunoService conviteService;
    private final LogAuditoriaService logAuditoriaService;

    public AlunoInstituicaoService(
            UsuarioRepository usuarioRepository,
            ConviteAlunoService conviteService,
            LogAuditoriaService logAuditoriaService) {

        this.usuarioRepository = usuarioRepository;
        this.conviteService = conviteService;
        this.logAuditoriaService = logAuditoriaService;
    }

    @Transactional
    public ConviteAlunoResponse cadastrar(
            CadastroAlunoInstituicaoRequest request,
            Usuario responsavel) {

        Instituicao instituicao = instituicaoDoResponsavel(responsavel);
        if (usuarioRepository.existsByEmailNormalizado(request.email())) {
            throw new ConflitoDadosException("E-mail já cadastrado");
        }
        if (usuarioRepository.existsByInstituicaoIdAndMatricula(
                instituicao.getId(), request.matricula())) {
            throw new ConflitoDadosException("Matrícula já cadastrada nesta instituição");
        }

        Usuario aluno = new Usuario();
        aluno.setNome(request.nome());
        aluno.setEmail(request.email());
        aluno.setMatricula(request.matricula());
        aluno.setPerfil("ALUNO");
        aluno.setInstituicao(instituicao);
        aluno.setAtivo(true);
        aluno.setAtivado(false);
        aluno.setSenha(null);
        aluno.setTermosAceitos(false);
        aluno.setTermosAceitosEm(null);
        usuarioRepository.save(aluno);

        ConviteAlunoService.ConviteGerado convite = conviteService.gerar(aluno);
        logAuditoriaService.registrar(
                responsavel.getEmail(),
                "CADASTRO_ALUNO",
                "Aluno " + aluno.getId(),
                instituicao);

        return new ConviteAlunoResponse(
                toResponse(aluno),
                convite.token(),
                convite.expiraEm());
    }

    @Transactional(readOnly = true)
    public List<AlunoInstituicaoResponse> listar(Usuario responsavel) {
        Instituicao instituicao = instituicaoDoResponsavel(responsavel);
        return usuarioRepository.findAllByPerfilAndInstituicaoIdOrderByNomeAsc(
                        "ALUNO", instituicao.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AlunoInstituicaoResponse atualizar(
            Long alunoId,
            AtualizacaoAlunoInstituicaoRequest request,
            Usuario responsavel) {

        Usuario aluno = buscarAlunoDaInstituicao(alunoId, responsavel);
        if (usuarioRepository.existsByInstituicaoIdAndMatriculaAndIdNot(
                aluno.getInstituicao().getId(), request.matricula(), aluno.getId())) {
            throw new ConflitoDadosException("Matrícula já cadastrada nesta instituição");
        }

        String nomeAnterior = aluno.getNome();
        String matriculaAnterior = aluno.getMatricula();
        aluno.setNome(request.nome());
        aluno.setMatricula(request.matricula());

        if (!Objects.equals(matriculaAnterior, request.matricula())) {
            logAuditoriaService.registrar(
                    responsavel.getEmail(),
                    "CORRECAO_MATRICULA_ALUNO",
                    "Aluno " + aluno.getId() + ": " + matriculaAnterior + " -> " + request.matricula(),
                    aluno.getInstituicao());
        }
        if (!nomeAnterior.equals(request.nome())) {
            logAuditoriaService.registrar(
                    responsavel.getEmail(),
                    "ATUALIZACAO_ALUNO",
                    "Aluno " + aluno.getId(),
                    aluno.getInstituicao());
        }
        return toResponse(aluno);
    }

    @Transactional
    public AlunoInstituicaoResponse alterarSituacao(
            Long alunoId,
            boolean ativo,
            Usuario responsavel) {

        Usuario aluno = buscarAlunoDaInstituicao(alunoId, responsavel);
        aluno.setAtivo(ativo);
        logAuditoriaService.registrar(
                responsavel.getEmail(),
                ativo ? "REATIVACAO_ALUNO" : "DESATIVACAO_ALUNO",
                "Aluno " + aluno.getId(),
                aluno.getInstituicao());
        return toResponse(aluno);
    }

    @Transactional
    public ConviteAlunoResponse reenviarConvite(Long alunoId, Usuario responsavel) {
        Usuario aluno = buscarAlunoDaInstituicao(alunoId, responsavel);
        if (Boolean.TRUE.equals(aluno.getAtivado())) {
            throw new ConflitoDadosException("A conta deste aluno já está ativada");
        }
        if (!Boolean.TRUE.equals(aluno.getAtivo())) {
            throw new ConflitoDadosException("Reative o aluno antes de gerar um novo convite");
        }

        ConviteAlunoService.ConviteGerado convite = conviteService.gerar(aluno);
        logAuditoriaService.registrar(
                responsavel.getEmail(),
                "REENVIO_CONVITE_ALUNO",
                "Aluno " + aluno.getId(),
                aluno.getInstituicao());
        return new ConviteAlunoResponse(
                toResponse(aluno),
                convite.token(),
                convite.expiraEm());
    }

    private Usuario buscarAlunoDaInstituicao(Long alunoId, Usuario responsavel) {
        Instituicao instituicao = instituicaoDoResponsavel(responsavel);
        return usuarioRepository.findByIdAndPerfilAndInstituicaoId(
                        alunoId, "ALUNO", instituicao.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado"));
    }

    private Instituicao instituicaoDoResponsavel(Usuario responsavel) {
        if (!"INSTITUICAO".equals(responsavel.getPerfil()) || responsavel.getInstituicao() == null) {
            throw new AccessDeniedException("Usuário institucional sem instituição vinculada");
        }
        return responsavel.getInstituicao();
    }

    private AlunoInstituicaoResponse toResponse(Usuario aluno) {
        ConviteAtivacaoAluno convite = conviteService.conviteMaisRecente(aluno.getId());
        LocalDateTime expiraEm = convite == null ? null : convite.getExpiraEm();
        return new AlunoInstituicaoResponse(
                aluno.getId(),
                aluno.getNome(),
                aluno.getEmail(),
                aluno.getMatricula(),
                Boolean.TRUE.equals(aluno.getAtivo()),
                Boolean.TRUE.equals(aluno.getAtivado()),
                situacao(aluno, convite),
                expiraEm);
    }

    private String situacao(Usuario aluno, ConviteAtivacaoAluno convite) {
        if (!Boolean.TRUE.equals(aluno.getAtivo())) {
            return "DESATIVADO";
        }
        if (Boolean.TRUE.equals(aluno.getAtivado())) {
            return "ATIVADO";
        }
        if (convite == null) {
            return "SEM_CONVITE";
        }
        if (convite.getCanceladoEm() != null) {
            return "CONVITE_CANCELADO";
        }
        if (convite.getUtilizadoEm() != null) {
            return "ATIVADO";
        }
        if (!convite.getExpiraEm().isAfter(LocalDateTime.now())) {
            return "CONVITE_EXPIRADO";
        }
        return "AGUARDANDO_ATIVACAO";
    }
}
