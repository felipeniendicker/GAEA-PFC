package Gaea.Api.security;

import Gaea.Api.dto.ConfirmacaoEmailRequest;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.TokenVerificacaoEmail;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.TokenVerificacaoEmailRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import Gaea.Api.service.SupervisaoService;
import Gaea.Api.service.VerificacaoEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SupervisaoConcorrenciaIntegrationTests {
    @Autowired InstituicaoRepository instituicaoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ProcessoEstagioRepository processoRepository;
    @Autowired VinculoSupervisorProcessoRepository vinculoRepository;
    @Autowired TokenVerificacaoEmailRepository tokenRepository;
    @Autowired SupervisaoService supervisaoService;
    @Autowired VerificacaoEmailService verificacaoEmailService;

    @Test
    void duasConfirmacoesSimultaneasDevemProduzirUmSucessoEUmConflito() throws Exception {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome("Faculdade Concorrência");
        instituicao.setCnpj("44777099000120");
        instituicao = instituicaoRepository.save(instituicao);

        Usuario aluno = usuario("Aluno", "aluno.concorrencia@example.com", "ALUNO", instituicao);
        aluno.setMatricula("RGM-CONCORRENCIA");
        aluno = usuarioRepository.save(aluno);
        Usuario supervisor = usuario(
                "Supervisor", "supervisor.concorrencia@example.com", "EMPRESA", null);
        supervisor.setEmailVerificadoEm(LocalDateTime.now());
        supervisor = usuarioRepository.save(supervisor);

        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno(aluno.getNome());
        processo.setEmailAluno(aluno.getEmail());
        processo.setNomeEmpresa("Empresa Concorrência");
        processo.setCnpjEmpresa("11222333000181");
        processo.setDataInicio(LocalDate.of(2026, 11, 10));
        processo.setDataFim(LocalDate.of(2027, 5, 10));
        processo.setStatus("PENDENTE");
        processo.setAluno(aluno);
        processo.setInstituicao(instituicao);
        processo = processoRepository.save(processo);

        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado(supervisor.getEmail());
        vinculo.setSituacao(SituacaoVinculoSupervisor.PENDENTE);
        Long vinculoId = vinculoRepository.save(vinculo).getId();

        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Usuario supervisorAutenticado = supervisor;
        try {
            var tarefa = (java.util.concurrent.Callable<String>) () -> {
                prontas.countDown();
                iniciar.await();
                try {
                    supervisaoService.confirmar(vinculoId, supervisorAutenticado);
                    return "SUCESSO";
                } catch (ConflitoDadosException exception) {
                    return "CONFLITO";
                }
            };
            Future<String> primeira = executor.submit(tarefa);
            Future<String> segunda = executor.submit(tarefa);
            prontas.await();
            iniciar.countDown();

            List<String> resultados = List.of(primeira.get(), segunda.get());
            assertEquals(1, resultados.stream().filter("SUCESSO"::equals).count());
            assertEquals(1, resultados.stream().filter("CONFLITO"::equals).count());
        } finally {
            executor.shutdownNow();
        }

        VinculoSupervisorProcesso confirmado = vinculoRepository.findById(vinculoId).orElseThrow();
        assertEquals(SituacaoVinculoSupervisor.CONFIRMADO, confirmado.getSituacao());
        assertTrue(Boolean.TRUE.equals(confirmado.getAtivo()));
    }

    @Test
    void tokenConfirmadoSimultaneamenteDeveTerSomenteUmUsoBemSucedido() throws Exception {
        Usuario supervisor = usuario(
                "Supervisor Token", "supervisor.token@example.com", "EMPRESA", null);
        String tokenAberto = "token-concorrente-com-entropia-simulada-123456";
        TokenVerificacaoEmail token = new TokenVerificacaoEmail();
        token.setUsuario(supervisor);
        token.setEmail(supervisor.getEmail());
        token.setTokenHash(hash(tokenAberto));
        token.setCriadoEm(LocalDateTime.now());
        token.setExpiraEm(LocalDateTime.now().plusHours(1));
        tokenRepository.save(token);

        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var tarefa = (java.util.concurrent.Callable<String>) () -> {
                prontas.countDown();
                iniciar.await();
                try {
                    verificacaoEmailService.confirmar(new ConfirmacaoEmailRequest(tokenAberto));
                    return "SUCESSO";
                } catch (DadosInvalidosException exception) {
                    return "INVALIDO";
                }
            };
            Future<String> primeira = executor.submit(tarefa);
            Future<String> segunda = executor.submit(tarefa);
            prontas.await();
            iniciar.countDown();

            List<String> resultados = List.of(primeira.get(), segunda.get());
            assertEquals(1, resultados.stream().filter("SUCESSO"::equals).count());
            assertEquals(1, resultados.stream().filter("INVALIDO"::equals).count());
        } finally {
            executor.shutdownNow();
        }

        TokenVerificacaoEmail utilizado = tokenRepository.findById(token.getId()).orElseThrow();
        assertTrue(utilizado.getUtilizadoEm() != null);
        assertTrue(usuarioRepository.findById(supervisor.getId()).orElseThrow()
                .getEmailVerificadoEm() != null);
    }

    @Test
    void reenviosSimultaneosDevemManterSomenteUmTokenValido() throws Exception {
        Usuario supervisor = usuario(
                "Supervisor Reenvio", "supervisor.reenvio@example.com", "EMPRESA", null);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var tarefa = (java.util.concurrent.Callable<Void>) () -> {
                prontas.countDown();
                iniciar.await();
                verificacaoEmailService.gerar(supervisor);
                return null;
            };
            Future<Void> primeira = executor.submit(tarefa);
            Future<Void> segunda = executor.submit(tarefa);
            prontas.await();
            iniciar.countDown();
            primeira.get();
            segunda.get();
        } finally {
            executor.shutdownNow();
        }

        List<TokenVerificacaoEmail> tokens = tokenRepository.findAllByUsuarioId(supervisor.getId());
        assertEquals(2, tokens.size());
        assertEquals(1, tokens.stream()
                .filter(item -> item.getUtilizadoEm() == null && item.getCanceladoEm() == null)
                .count());
        assertEquals(1, tokens.stream().filter(item -> item.getCanceladoEm() != null).count());
    }

    private String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    private Usuario usuario(
            String nome, String email, String perfil, Instituicao instituicao) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setInstituicao(instituicao);
        usuario.setAtivo(true);
        usuario.setAtivado(true);
        usuario.setTermosAceitos(true);
        return usuarioRepository.save(usuario);
    }
}
