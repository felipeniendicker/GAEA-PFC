package Gaea.Api.security;

import Gaea.Api.model.DocumentoProcesso;
import Gaea.Api.model.ConviteAtivacaoAluno;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoDocumento;
import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.SolicitacaoRecuperacaoSenha;
import Gaea.Api.model.TokenFluxoInstituicao;
import Gaea.Api.model.TokenVerificacaoEmail;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.DocumentoProcessoRepository;
import Gaea.Api.repository.ConviteAtivacaoAlunoRepository;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.SolicitacaoPrivacidadeRepository;
import Gaea.Api.repository.SolicitacaoRecuperacaoSenhaRepository;
import Gaea.Api.repository.TokenFluxoInstituicaoRepository;
import Gaea.Api.repository.TokenVerificacaoEmailRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.EnvioEmailAplicacao;
import Gaea.Api.service.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EncerramentoContaPrivacidadeIntegrationTests {

    private static final String SENHA = "Senha-segura-123!";

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired SolicitacaoPrivacidadeRepository solicitacoes;
    @Autowired SolicitacaoRecuperacaoSenhaRepository recuperacoes;
    @Autowired ConviteAtivacaoAlunoRepository convites;
    @Autowired TokenVerificacaoEmailRepository verificacoes;
    @Autowired TokenFluxoInstituicaoRepository tokensInstituicao;
    @Autowired ProcessoEstagioRepository processos;
    @Autowired DocumentoProcessoRepository documentos;
    @Autowired LogAuditoriaRepository auditoria;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired EntityManager entityManager;

    @MockitoBean EnvioEmailAplicacao email;

    @BeforeEach
    void configurarEmail() {
        when(email.disponivel()).thenReturn(true);
    }

    @Test
    void deveEncerrarContaRevogarAcessoPreservarRegistrosEAuditar() throws Exception {
        Instituicao instituicao = instituicao();
        Usuario titular = usuario("Titular", "titular@example.com", "ALUNO", instituicao);
        Usuario admin = usuario("Admin", "admin@example.com", "ADMIN_GAEA", null);
        ProcessoEstagio processo = processo(titular, instituicao);
        DocumentoProcesso documento = documento(titular, processo);
        String jwtAnterior = bearer(titular);
        long versaoAnterior = titular.getCredenciaisVersao();

        Long id = solicitarEncerramento(titular);
        iniciarAnalise(id, admin);
        concluir(id, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"))
                .andExpect(jsonPath("$.evidenciaExecucao").value(
                        org.hamcrest.Matchers.containsString("Conta desativada")));

        entityManager.flush();
        entityManager.clear();
        Usuario encerrado = usuarios.findById(titular.getId()).orElseThrow();
        assertFalse(encerrado.getAtivo());
        assertEquals(versaoAnterior + 1, encerrado.getCredenciaisVersao());
        assertTrue(processos.existsById(processo.getId()));
        assertTrue(documentos.existsById(documento.getId()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"titular@example.com","senha":"Senha-segura-123!"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/processos/meus")
                        .header("Authorization", jwtAnterior))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/senha/recuperacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"titular@example.com\"}"))
                .andExpect(status().isAccepted());
        assertTrue(recuperacoes
                .findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(titular.getId())
                .isEmpty());

        mockMvc.perform(get("/api/admin/privacidade/auditoria")
                        .param("solicitacaoId", id.toString())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.acao == 'SOLICITACAO_PRIVACIDADE')]").exists())
                .andExpect(jsonPath("$[?(@.acao == 'APROVACAO_PRIVACIDADE')]").exists())
                .andExpect(jsonPath("$[?(@.acao == 'EXECUCAO_PRIVACIDADE')]").exists())
                .andExpect(jsonPath("$[?(@.acao == 'DESATIVACAO_CONTA_PRIVACIDADE')]").exists());
    }

    @Test
    void deveSerIdempotenteSemIncrementarCredenciaisDuasVezes() throws Exception {
        Usuario titular = usuario("Titular", "idempotente@example.com", "EMPRESA", null);
        Usuario admin = usuario("Admin", "admin-idempotente@example.com", "ADMIN_GAEA", null);
        Long id = solicitarEncerramento(titular);
        iniciarAnalise(id, admin);

        concluir(id, admin).andExpect(status().isOk());
        entityManager.flush();
        long versaoExecutada = usuarios.findById(titular.getId()).orElseThrow()
                .getCredenciaisVersao();
        concluir(id, admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"));
        entityManager.flush();

        assertEquals(versaoExecutada,
                usuarios.findById(titular.getId()).orElseThrow().getCredenciaisVersao());
        assertEquals(1, auditoria.findAll().stream()
                .filter(log -> "EXECUCAO_PRIVACIDADE".equals(log.getAcao()))
                .count());
    }

    @Test
    void deveImpedirConclusaoDeclaratoriaDeExclusaoEAnonimizacao() throws Exception {
        Usuario titular = usuario("Titular", "dados@example.com", "ALUNO", null);
        Usuario admin = usuario("Admin", "admin-dados@example.com", "ADMIN_GAEA", null);

        for (String tipo : new String[]{"EXCLUSAO", "ANONIMIZACAO"}) {
            Long id = solicitar(titular, tipo, null);
            iniciarAnalise(id, admin);
            concluir(id, admin).andExpect(status().isConflict());
            assertEquals(SituacaoSolicitacaoPrivacidade.EM_ANALISE,
                    solicitacoes.findById(id).orElseThrow().getSituacao());
        }
        assertTrue(usuarios.findById(titular.getId()).orElseThrow().getAtivo());
    }

    @Test
    void deveProtegerUltimaContaResponsavelDaInstituicao() throws Exception {
        Instituicao instituicao = instituicao();
        Usuario responsavel = usuario(
                "Responsável", "responsavel@example.com", "INSTITUICAO", instituicao);
        Usuario admin = usuario("Admin", "admin-instituicao@example.com", "ADMIN_GAEA", null);
        Long id = solicitarEncerramento(responsavel);
        iniciarAnalise(id, admin);

        concluir(id, admin).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("outro responsável institucional")));

        assertTrue(usuarios.findById(responsavel.getId()).orElseThrow().getAtivo());
        assertTrue(instituicoes.findById(instituicao.getId()).orElseThrow().getAtiva());
        assertEquals(SituacaoSolicitacaoPrivacidade.EM_ANALISE,
                solicitacoes.findById(id).orElseThrow().getSituacao());
    }

    @Test
    void deveCancelarTodosOsTokensPendentesDaConta() throws Exception {
        Instituicao instituicao = instituicao();
        Usuario titular = usuario(
                "Responsável", "tokens@example.com", "INSTITUICAO", instituicao);
        usuario("Responsável substituto", "substituto@example.com", "INSTITUICAO", instituicao);
        Usuario admin = usuario("Admin", "admin-tokens@example.com", "ADMIN_GAEA", null);
        Long recuperacaoId = tokenRecuperacao(titular).getId();
        Long conviteId = convite(titular, instituicao).getId();
        Long verificacaoId = tokenVerificacao(titular).getId();
        Long institucionalId = tokenInstituicao(titular).getId();

        Long id = solicitarEncerramento(titular);
        iniciarAnalise(id, admin);
        concluir(id, admin).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        assertTrue(recuperacoes.findById(recuperacaoId).orElseThrow().getCanceladoEm() != null);
        assertTrue(convites.findById(conviteId).orElseThrow().getCanceladoEm() != null);
        assertTrue(verificacoes.findById(verificacaoId).orElseThrow().getCanceladoEm() != null);
        assertTrue(tokensInstituicao.findById(institucionalId).orElseThrow().getCanceladoEm() != null);
        assertTrue(instituicoes.findById(instituicao.getId()).orElseThrow().getAtiva());
    }

    private Long solicitarEncerramento(Usuario titular) throws Exception {
        return solicitar(titular, "ENCERRAMENTO_CONTA", SENHA);
    }

    private Long solicitar(Usuario titular, String tipo, String senha) throws Exception {
        String campoSenha = senha == null ? "" : ",\"senhaAtual\":\"" + senha + "\"";
        mockMvc.perform(post("/api/privacidade/solicitacoes")
                        .header("Authorization", bearer(titular))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"" + tipo
                                + "\",\"descricao\":\"Solicitação fundamentada do titular\""
                                + campoSenha + "}"))
                .andExpect(status().isOk());
        return solicitacoes.findAll().stream()
                .filter(item -> item.getUsuario().getId().equals(titular.getId()))
                .filter(item -> item.getTipo().name().equals(tipo))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private void iniciarAnalise(Long id, Usuario admin) throws Exception {
        mockMvc.perform(put("/api/admin/privacidade/solicitacoes/{id}/analise", id)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions concluir(
            Long id,
            Usuario admin) throws Exception {
        return mockMvc.perform(post("/api/admin/privacidade/solicitacoes/{id}/concluir", id)
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisao\":\"Encerramento aprovado após análise administrativa.\"}"));
    }

    private Usuario usuario(
            String nome,
            String emailUsuario,
            String perfil,
            Instituicao instituicao) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(emailUsuario);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setPerfil(perfil);
        usuario.setInstituicao(instituicao);
        usuario.setAtivo(true);
        usuario.setAtivado(true);
        usuario.setTermosAceitos(true);
        usuario.setCredenciaisVersao(0L);
        return usuarios.saveAndFlush(usuario);
    }

    private Instituicao instituicao() {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome("Instituição de Privacidade");
        instituicao.setCnpj("11222333000181");
        instituicao.setAtiva(true);
        return instituicoes.saveAndFlush(instituicao);
    }

    private ProcessoEstagio processo(Usuario titular, Instituicao instituicao) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setAluno(titular);
        processo.setInstituicao(instituicao);
        processo.setNomeAluno(titular.getNome());
        processo.setEmailAluno(titular.getEmail());
        processo.setNomeEmpresa("Empresa Acadêmica");
        processo.setCnpjEmpresa("11222333000181");
        processo.setDataInicio(LocalDate.now());
        processo.setDataFim(LocalDate.now().plusMonths(6));
        processo.setStatus("PENDENTE");
        return processos.saveAndFlush(processo);
    }

    private DocumentoProcesso documento(Usuario titular, ProcessoEstagio processo) {
        DocumentoProcesso documento = new DocumentoProcesso();
        documento.setProcesso(processo);
        documento.setEnviadoPor(titular);
        documento.setNomeOriginal("termo-estagio.pdf");
        documento.setChaveArmazenamento("preservacao-lgpd.pdf");
        documento.setTipoConteudo("application/pdf");
        documento.setTamanhoBytes(128L);
        documento.setHashSha256("a".repeat(64));
        documento.setSituacao(SituacaoDocumento.PENDENTE);
        return documentos.saveAndFlush(documento);
    }

    private SolicitacaoRecuperacaoSenha tokenRecuperacao(Usuario titular) {
        SolicitacaoRecuperacaoSenha token = new SolicitacaoRecuperacaoSenha();
        token.setUsuario(titular);
        token.setIdentificadorHash("1".repeat(64));
        token.setOrigemHash("2".repeat(64));
        token.setTokenHash("3".repeat(64));
        token.setCriadoEm(LocalDateTime.now());
        token.setExpiraEm(LocalDateTime.now().plusHours(1));
        return recuperacoes.saveAndFlush(token);
    }

    private ConviteAtivacaoAluno convite(Usuario titular, Instituicao instituicao) {
        ConviteAtivacaoAluno token = new ConviteAtivacaoAluno();
        token.setUsuario(titular);
        token.setInstituicao(instituicao);
        token.setEmail(titular.getEmail());
        token.setTokenHash("4".repeat(64));
        token.setCriadoEm(LocalDateTime.now());
        token.setExpiraEm(LocalDateTime.now().plusHours(1));
        return convites.saveAndFlush(token);
    }

    private TokenVerificacaoEmail tokenVerificacao(Usuario titular) {
        TokenVerificacaoEmail token = new TokenVerificacaoEmail();
        token.setUsuario(titular);
        token.setEmail(titular.getEmail());
        token.setTokenHash("5".repeat(64));
        token.setCriadoEm(LocalDateTime.now());
        token.setExpiraEm(LocalDateTime.now().plusHours(1));
        return verificacoes.saveAndFlush(token);
    }

    private TokenFluxoInstituicao tokenInstituicao(Usuario titular) {
        TokenFluxoInstituicao token = new TokenFluxoInstituicao();
        token.setUsuario(titular);
        token.setTipo("ATIVACAO");
        token.setTokenHash("6".repeat(64));
        token.setCriadoEm(LocalDateTime.now());
        token.setExpiraEm(LocalDateTime.now().plusHours(1));
        return tokensInstituicao.saveAndFlush(token);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }
}
