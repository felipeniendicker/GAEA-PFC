package Gaea.Api.security;

import Gaea.Api.dto.CadastroSolicitacaoPrivacidadeRequest;
import Gaea.Api.dto.DecisaoPrivacidadeRequest;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.TipoSolicitacaoPrivacidade;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.SolicitacaoPrivacidadeRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.EnvioEmailAplicacao;
import Gaea.Api.service.SolicitacaoPrivacidadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
class EncerramentoContaTransacaoIntegrationTests {

    private static final String SENHA = "Senha-segura-123!";

    @Autowired SolicitacaoPrivacidadeService service;
    @Autowired UsuarioRepository usuarios;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired SolicitacaoPrivacidadeRepository solicitacoes;
    @Autowired LogAuditoriaRepository auditoria;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean EnvioEmailAplicacao email;

    @BeforeEach
    void limparMockEmail() {
        reset(email);
    }

    @AfterEach
    void limparDadosCriados() {
        solicitacoes.deleteAll();
        auditoria.deleteAll();
        usuarios.deleteAll();
        instituicoes.deleteAll();
    }

    @Test
    void deveEnviarComunicacaoSomenteDepoisDoCommit() {
        Usuario titular = usuario("pos-commit@example.com", "ALUNO", null);
        Usuario admin = usuario("pos-commit-admin@example.com", "ADMIN_GAEA", null);
        Long id = prepararEncerramento(titular, admin);

        service.concluir(id, decisao(), admin);

        assertEquals(SituacaoSolicitacaoPrivacidade.CONCLUIDA,
                solicitacoes.findById(id).orElseThrow().getSituacao());
        assertTrue(!usuarios.findById(titular.getId()).orElseThrow().getAtivo());
        verify(email).enviarResultadoPrivacidade(
                eq(titular.getEmail()), eq(titular.getNome()), eq(true));
        assertTrue(auditoria.findAll().stream()
                .anyMatch(log -> "COMUNICACAO_PRIVACIDADE".equals(log.getAcao())));
    }

    @Test
    void falhaDeEmailNaoDeveReverterEncerramento() {
        Usuario titular = usuario("email-falha@example.com", "EMPRESA", null);
        Usuario admin = usuario("email-falha-admin@example.com", "ADMIN_GAEA", null);
        Long id = prepararEncerramento(titular, admin);
        doThrow(new IllegalStateException("falha Brevo simulada"))
                .when(email)
                .enviarResultadoPrivacidade(
                        titular.getEmail(), titular.getNome(), true);

        service.concluir(id, decisao(), admin);

        assertEquals(SituacaoSolicitacaoPrivacidade.CONCLUIDA,
                solicitacoes.findById(id).orElseThrow().getSituacao());
        assertTrue(!usuarios.findById(titular.getId()).orElseThrow().getAtivo());
        assertTrue(auditoria.findAll().stream()
                .anyMatch(log -> "FALHA_COMUNICACAO_PRIVACIDADE".equals(log.getAcao())));
    }

    @Test
    void concorrenciaDevePreservarUmResponsavelInstitucionalAtivo() throws Exception {
        Instituicao instituicao = instituicao();
        Usuario primeiro = usuario(
                "concorrencia-um@example.com", "INSTITUICAO", instituicao);
        Usuario segundo = usuario(
                "concorrencia-dois@example.com", "INSTITUICAO", instituicao);
        Usuario admin = usuario(
                "concorrencia-admin@example.com", "ADMIN_GAEA", null);
        Long primeiroId = prepararEncerramento(primeiro, admin);
        Long segundoId = prepararEncerramento(segundo, admin);
        CountDownLatch inicio = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> primeiraExecucao = executor.submit(
                    () -> concluirAposSinal(inicio, primeiroId, admin));
            Future<Object> segundaExecucao = executor.submit(
                    () -> concluirAposSinal(inicio, segundoId, admin));
            inicio.countDown();

            List<Object> resultados = List.of(
                    resultado(primeiraExecucao), resultado(segundaExecucao));
            assertEquals(1, resultados.stream()
                    .filter(resultado -> resultado instanceof ConflitoDadosException)
                    .count());
        } finally {
            executor.shutdownNow();
        }

        List<Usuario> responsaveis = usuarios
                .findAllByPerfilAndInstituicaoIdOrderByNomeAsc(
                        "INSTITUICAO", instituicao.getId());
        assertEquals(1, responsaveis.stream()
                .filter(usuario -> Boolean.TRUE.equals(usuario.getAtivo()))
                .count());
        assertTrue(instituicoes.findById(instituicao.getId()).orElseThrow().getAtiva());
        assertEquals(1, solicitacoes.findAll().stream()
                .filter(item -> item.getId().equals(primeiroId)
                        || item.getId().equals(segundoId))
                .filter(item -> item.getSituacao()
                        == SituacaoSolicitacaoPrivacidade.CONCLUIDA)
                .count());
    }

    private Object concluirAposSinal(
            CountDownLatch inicio,
            Long solicitacaoId,
            Usuario admin) throws InterruptedException {
        inicio.await();
        return service.concluir(solicitacaoId, decisao(), admin);
    }

    private Object resultado(Future<Object> future) throws Exception {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            Throwable causa = exception.getCause();
            assertInstanceOf(ConflitoDadosException.class, causa);
            return causa;
        }
    }

    private Long prepararEncerramento(Usuario titular, Usuario admin) {
        Long id = service.cadastrar(
                        new CadastroSolicitacaoPrivacidadeRequest(
                                TipoSolicitacaoPrivacidade.ENCERRAMENTO_CONTA,
                                "Solicitacao confirmada pelo titular",
                                SENHA),
                        titular)
                .id();
        service.iniciarAnalise(id, admin);
        return id;
    }

    private DecisaoPrivacidadeRequest decisao() {
        return new DecisaoPrivacidadeRequest(
                "Encerramento aprovado depois da analise administrativa.", null);
    }

    private Usuario usuario(
            String emailUsuario,
            String perfil,
            Instituicao instituicao) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de teste transacional");
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
        instituicao.setNome("Instituicao concorrente");
        instituicao.setCnpj("99888777000166");
        instituicao.setAtiva(true);
        return instituicoes.saveAndFlush(instituicao);
    }
}
