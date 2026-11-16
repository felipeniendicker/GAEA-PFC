package Gaea.Api.service;

import Gaea.Api.dto.CadastroSolicitacaoPrivacidadeRequest;
import Gaea.Api.dto.SolicitacaoPrivacidadeResponse;
import Gaea.Api.model.SolicitacaoPrivacidade;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.SolicitacaoPrivacidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SolicitacaoPrivacidadeService {

    private final SolicitacaoPrivacidadeRepository repository;
    private final LogAuditoriaService auditoriaService;

    public SolicitacaoPrivacidadeService(
            SolicitacaoPrivacidadeRepository repository,
            LogAuditoriaService auditoriaService) {
        this.repository = repository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public SolicitacaoPrivacidadeResponse cadastrar(
            CadastroSolicitacaoPrivacidadeRequest request,
            Usuario usuario) {
        SolicitacaoPrivacidade solicitacao = new SolicitacaoPrivacidade();
        solicitacao.setUsuario(usuario);
        solicitacao.setTipo(request.tipo());
        solicitacao.setDescricao(request.descricao().trim());
        SolicitacaoPrivacidade salva = repository.saveAndFlush(solicitacao);

        auditoriaService.registrar(
                usuario.getEmail(),
                "SOLICITACAO_PRIVACIDADE",
                "Solicitação " + salva.getId(),
                usuario.getInstituicao());
        return SolicitacaoPrivacidadeResponse.from(salva);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoPrivacidadeResponse> listar(Usuario usuario) {
        return repository.findAllByUsuarioIdOrderByCriadoEmDescIdDesc(usuario.getId())
                .stream()
                .map(SolicitacaoPrivacidadeResponse::from)
                .toList();
    }
}
