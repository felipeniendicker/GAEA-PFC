package Gaea.Api.service;

import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ProcessoAutorizacaoService {

    private final VinculoSupervisorProcessoRepository vinculoRepository;

    public ProcessoAutorizacaoService(VinculoSupervisorProcessoRepository vinculoRepository) {
        this.vinculoRepository = vinculoRepository;
    }

    public void validarAcesso(ProcessoEstagio processo, Usuario usuario) {
        if (!possuiAcesso(processo, usuario)) {
            throw new RecursoNaoEncontradoException("Processo não encontrado");
        }
    }

    public void validarGestaoInstitucional(ProcessoEstagio processo, Usuario usuario) {
        if (!"INSTITUICAO".equals(usuario.getPerfil())) {
            throw new AccessDeniedException("Perfil sem permissão para gerenciar processos");
        }

        Instituicao instituicaoUsuario = usuario.getInstituicao();
        Instituicao instituicaoProcesso = processo.getInstituicao();
        if (instituicaoUsuario == null
                || instituicaoProcesso == null
                || !instituicaoUsuario.getId().equals(instituicaoProcesso.getId())) {
            throw new RecursoNaoEncontradoException("Processo não encontrado");
        }
    }

    private boolean possuiAcesso(ProcessoEstagio processo, Usuario usuario) {
        return switch (usuario.getPerfil()) {
            case "ALUNO" -> processo.getAluno() != null
                    && processo.getAluno().getId().equals(usuario.getId());
            case "INSTITUICAO" -> mesmaInstituicao(processo, usuario);
            case "EMPRESA" -> vinculoRepository
                    .existsByProcessoIdAndSupervisorIdAndSituacaoAndAtivoTrue(
                    processo.getId(),
                    usuario.getId(),
                    SituacaoVinculoSupervisor.CONFIRMADO);
            default -> false;
        };
    }

    private boolean mesmaInstituicao(ProcessoEstagio processo, Usuario usuario) {
        return usuario.getInstituicao() != null
                && processo.getInstituicao() != null
                && usuario.getInstituicao().getId().equals(processo.getInstituicao().getId());
    }
}
