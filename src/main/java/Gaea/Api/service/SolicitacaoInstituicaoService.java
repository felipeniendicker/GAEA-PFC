package Gaea.Api.service;

import Gaea.Api.dto.*;
import Gaea.Api.exception.*;
import Gaea.Api.model.*;
import Gaea.Api.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SolicitacaoInstituicaoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolicitacaoInstituicaoService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String VERIFICACAO = "VERIFICACAO";
    private static final String ATIVACAO = "ATIVACAO";
    private final SolicitacaoInstituicaoRepository solicitacoes;
    private final TokenFluxoInstituicaoRepository tokens;
    private final InstituicaoRepository instituicoes;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final PoliticaSenhaService politicaSenha;
    private final EnvioEmailAplicacao email;
    private final LogAuditoriaService auditoria;
    private final String frontendBaseUrl;
    private final byte[] chaveHash;
    private final long expiracaoHoras;
    private final int limiteOrigem;

    public SolicitacaoInstituicaoService(SolicitacaoInstituicaoRepository solicitacoes,
            TokenFluxoInstituicaoRepository tokens, InstituicaoRepository instituicoes,
            UsuarioRepository usuarios, PasswordEncoder encoder, PoliticaSenhaService politicaSenha,
            EnvioEmailAplicacao email, LogAuditoriaService auditoria,
            @Value("${app.frontend.base-url:http://localhost:5174}") String frontendBaseUrl,
            @Value("${app.password-reset.hash-secret}") String chaveHash,
            @Value("${app.solicitacao-instituicao.expiracao-horas:24}") long expiracaoHoras,
            @Value("${app.solicitacao-instituicao.limite-origem-hora:5}") int limiteOrigem) {
        this.solicitacoes=solicitacoes; this.tokens=tokens; this.instituicoes=instituicoes;
        this.usuarios=usuarios; this.encoder=encoder; this.politicaSenha=politicaSenha;
        this.email=email; this.auditoria=auditoria; this.frontendBaseUrl=frontendBaseUrl;
        this.chaveHash=chaveHash.getBytes(StandardCharsets.UTF_8);
        this.expiracaoHoras=expiracaoHoras; this.limiteOrigem=limiteOrigem;
    }

    @Transactional
    public void solicitar(SolicitacaoInstituicaoRequest request, String origem) {
        if (!email.disponivel()) throw new ServicoIndisponivelException("O envio de e-mail não está disponível");
        String origemHash=hmac(origem==null ? "desconhecida" : origem);
        if (solicitacoes.countByOrigemHashAndCriadaEmAfter(origemHash, LocalDateTime.now().minusHours(1)) >= limiteOrigem)
            throw new LimiteTentativasException("Muitas solicitações. Aguarde antes de tentar novamente");
        if (instituicoes.findByCnpj(request.cnpj()).isPresent() || solicitacoes.existsByCnpj(request.cnpj()))
            throw new ConflitoDadosException("CNPJ já cadastrado ou em análise");
        if (usuarios.existsByEmailNormalizado(request.emailInstitucional())
                || solicitacoes.existsByEmailInstitucionalIgnoreCase(request.emailInstitucional()))
            throw new ConflitoDadosException("E-mail já cadastrado ou em análise");
        LocalDateTime agora=LocalDateTime.now();
        SolicitacaoInstituicao s=new SolicitacaoInstituicao();
        s.setNomeInstitucional(request.nomeInstitucional()); s.setCnpj(request.cnpj());
        s.setResponsavelNome(request.responsavelNome()); s.setEmailInstitucional(request.emailInstitucional());
        s.setOrigemHash(origemHash); s.setStatus(StatusSolicitacaoInstituicao.AGUARDANDO_EMAIL);
        s.setCriadaEm(agora); s.setAtualizadaEm(agora);
        try { solicitacoes.saveAndFlush(s); } catch (DataIntegrityViolationException e) {
            throw new ConflitoDadosException("Instituição ou e-mail já possui solicitação");
        }
        String aberto=criarToken(s, null, VERIFICACAO, agora);
        try {
            email.enviarVerificacaoInstituicao(s.getEmailInstitucional(), s.getResponsavelNome(),
                    frontendBaseUrl + "/#verificarInstituicao=" + URLEncoder.encode(aberto, StandardCharsets.UTF_8));
        } catch (RuntimeException exception) {
            LOGGER.warn("Falha no envio do e-mail de verificação institucional; solicitação permanece aguardando e-mail");
        }
    }

    @Transactional
    public void confirmarEmail(String tokenAberto) {
        TokenFluxoInstituicao token=buscarToken(tokenAberto, VERIFICACAO);
        validarToken(token);
        SolicitacaoInstituicao s=solicitacoes.findByIdParaAtualizacao(token.getSolicitacao().getId())
                .orElseThrow(() -> new DadosInvalidosException("Token inválido"));
        if (s.getEmailVerificadoEm()!=null) throw new ConflitoDadosException("E-mail já confirmado");
        LocalDateTime agora=LocalDateTime.now(); s.setEmailVerificadoEm(agora);
        s.setStatus(StatusSolicitacaoInstituicao.PENDENTE); s.setAtualizadaEm(agora); token.setUtilizadoEm(agora);
    }

    @Transactional(readOnly=true)
    public List<SolicitacaoInstituicaoResponse> listar(StatusSolicitacaoInstituicao status) {
        List<SolicitacaoInstituicao> itens=status==null ? solicitacoes.findAllByOrderByCriadaEmDesc()
                : solicitacoes.findAllByStatusOrderByCriadaEmAsc(status);
        return itens.stream().map(SolicitacaoInstituicaoResponse::from).toList();
    }

    @Transactional
    public SolicitacaoInstituicaoResponse iniciarAnalise(Long id, Usuario admin) {
        exigirAdmin(admin); SolicitacaoInstituicao s=bloquear(id);
        if (s.getStatus()==StatusSolicitacaoInstituicao.EM_ANALISE) return SolicitacaoInstituicaoResponse.from(s);
        if (s.getStatus()!=StatusSolicitacaoInstituicao.PENDENTE || s.getEmailVerificadoEm()==null)
            throw new ConflitoDadosException("A solicitação não está apta para análise");
        s.setStatus(StatusSolicitacaoInstituicao.EM_ANALISE); s.setAnalisadaPor(admin); s.setAtualizadaEm(LocalDateTime.now());
        auditoria.registrar(admin.getEmail(), "INICIO_ANALISE_INSTITUICAO", "Solicitação institucional " + id);
        return SolicitacaoInstituicaoResponse.from(s);
    }

    @Transactional
    public SolicitacaoInstituicaoResponse rejeitar(Long id, DecisaoSolicitacaoInstituicaoRequest request, Usuario admin) {
        exigirAdmin(admin); SolicitacaoInstituicao s=bloquear(id);
        if (s.getStatus()==StatusSolicitacaoInstituicao.REJEITADA) return SolicitacaoInstituicaoResponse.from(s);
        if (s.getStatus()!=StatusSolicitacaoInstituicao.EM_ANALISE) throw new ConflitoDadosException("Solicitação não está em análise");
        LocalDateTime agora=LocalDateTime.now(); s.setStatus(StatusSolicitacaoInstituicao.REJEITADA);
        s.setJustificativa(request.justificativa().trim()); s.setAnalisadaPor(admin); s.setAnalisadaEm(agora); s.setAtualizadaEm(agora);
        auditoria.registrar(admin.getEmail(), "REJEICAO_INSTITUICAO", "Solicitação institucional " + id);
        return SolicitacaoInstituicaoResponse.from(s);
    }

    @Transactional
    public SolicitacaoInstituicaoResponse aprovar(Long id, AprovacaoInstituicaoRequest request, Usuario admin) {
        exigirAdmin(admin); SolicitacaoInstituicao s=bloquear(id);
        if (s.getStatus()==StatusSolicitacaoInstituicao.APROVADA) return SolicitacaoInstituicaoResponse.from(s);
        if (s.getStatus()!=StatusSolicitacaoInstituicao.EM_ANALISE) throw new ConflitoDadosException("Solicitação não está em análise");
        if (instituicoes.findByCnpj(s.getCnpj()).isPresent() || usuarios.existsByEmailNormalizado(s.getEmailInstitucional()))
            throw new ConflitoDadosException("Instituição ou responsável já cadastrado");
        Instituicao instituicao=new Instituicao(); instituicao.setNome(s.getNomeInstitucional()); instituicao.setCnpj(s.getCnpj()); instituicao.setAtiva(true);
        instituicao.setSigla(request.sigla()); instituicao.setFicticia(request.ficticia());
        instituicao=instituicoes.saveAndFlush(instituicao);
        Usuario responsavel=new Usuario(); responsavel.setNome(s.getResponsavelNome()); responsavel.setEmail(s.getEmailInstitucional());
        responsavel.setPerfil("INSTITUICAO"); responsavel.setInstituicao(instituicao); responsavel.setSenha(null);
        responsavel.setAtivo(true); responsavel.setAtivado(false); responsavel.setTermosAceitos(false);
        responsavel.setEmailVerificadoEm(s.getEmailVerificadoEm()); responsavel=usuarios.saveAndFlush(responsavel);
        LocalDateTime agora=LocalDateTime.now(); String aberto=criarToken(null, responsavel, ATIVACAO, agora);
        s.setStatus(StatusSolicitacaoInstituicao.APROVADA); s.setInstituicaoCriada(instituicao);
        s.setAnalisadaPor(admin); s.setAnalisadaEm(agora); s.setAtualizadaEm(agora);
        auditoria.registrar(admin.getEmail(), "APROVACAO_INSTITUICAO", "Instituição " + instituicao.getId(), instituicao);
        email.enviarAtivacaoInstituicao(responsavel.getEmail(), responsavel.getNome(),
                frontendBaseUrl + "/#ativarInstituicao=" + URLEncoder.encode(aberto, StandardCharsets.UTF_8));
        return SolicitacaoInstituicaoResponse.from(s);
    }

    @Transactional
    public void ativar(AtivacaoInstituicaoRequest request) {
        politicaSenha.validar(request.senha()); TokenFluxoInstituicao token=buscarToken(request.token(), ATIVACAO); validarToken(token);
        Usuario u=usuarios.findByIdParaAtualizacao(token.getUsuario().getId()).orElseThrow(() -> new DadosInvalidosException("Convite inválido"));
        if (!"INSTITUICAO".equals(u.getPerfil()) || Boolean.TRUE.equals(u.getAtivado())) throw new ConflitoDadosException("Conta já ativada ou inválida");
        LocalDateTime agora=LocalDateTime.now(); u.setSenha(encoder.encode(request.senha())); u.setAtivado(true);
        DocumentosLegais.registrarAceiteAtual(u, agora); token.setUtilizadoEm(agora);
        auditoria.registrar(u.getEmail(), "ATIVACAO_INSTITUICAO", "Ativação do responsável institucional", u.getInstituicao());
    }

    @Transactional(readOnly=true)
    public List<InstituicaoAdminResponse> listarInstituicoes() {
        return instituicoes.findAllByOrderByNomeAsc().stream().map(InstituicaoAdminResponse::from).toList();
    }

    public Map<String,Long> resumo() {
        Map<String,Long> r=new LinkedHashMap<>(); for (StatusSolicitacaoInstituicao s: StatusSolicitacaoInstituicao.values()) r.put(s.name(), solicitacoes.countByStatus(s));
        r.put("INSTITUICOES", instituicoes.count()); return r;
    }

    private SolicitacaoInstituicao bloquear(Long id) { return solicitacoes.findByIdParaAtualizacao(id).orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada")); }
    private void exigirAdmin(Usuario u) { if (u==null || !"ADMIN_GAEA".equals(u.getPerfil())) throw new org.springframework.security.access.AccessDeniedException("Acesso administrativo necessário"); }
    private String criarToken(SolicitacaoInstituicao s, Usuario u, String tipo, LocalDateTime agora) {
        String aberto=novoToken(); TokenFluxoInstituicao t=new TokenFluxoInstituicao(); t.setSolicitacao(s); t.setUsuario(u); t.setTipo(tipo);
        t.setTokenHash(hash(aberto)); t.setCriadoEm(agora); t.setExpiraEm(agora.plusHours(expiracaoHoras)); tokens.save(t); return aberto;
    }
    private TokenFluxoInstituicao buscarToken(String aberto, String tipo) { TokenFluxoInstituicao t=tokens.findByTokenHashParaAtualizacao(hash(aberto)).orElseThrow(() -> new DadosInvalidosException("Token inválido")); if (!tipo.equals(t.getTipo())) throw new DadosInvalidosException("Token inválido"); return t; }
    private void validarToken(TokenFluxoInstituicao t) { if(t.getUtilizadoEm()!=null||t.getCanceladoEm()!=null||!t.getExpiraEm().isAfter(LocalDateTime.now())) throw new DadosInvalidosException("Token expirado ou já utilizado"); }
    private String novoToken() { byte[] b=new byte[32]; RANDOM.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private String hash(String v) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8))); } catch(Exception e){ throw new IllegalStateException(e); } }
    private String hmac(String v) { try { Mac m=Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(chaveHash,"HmacSHA256")); return HexFormat.of().formatHex(m.doFinal(v.getBytes(StandardCharsets.UTF_8))); } catch(Exception e){ throw new IllegalStateException(e); } }
}
