package Gaea.Api.security;

import Gaea.Api.model.HistoricoVinculoSupervisor;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.HistoricoVinculoSupervisorRepository;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.TokenVerificacaoEmailRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import Gaea.Api.service.EnvioEmailVerificacao;
import Gaea.Api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SupervisaoIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired InstituicaoRepository instituicaoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ProcessoEstagioRepository processoRepository;
    @Autowired VinculoSupervisorProcessoRepository vinculoRepository;
    @Autowired HistoricoVinculoSupervisorRepository historicoRepository;
    @Autowired LogAuditoriaRepository auditoriaRepository;
    @Autowired TokenVerificacaoEmailRepository tokenRepository;
    @Autowired JwtService jwtService;

    @MockitoBean
    EnvioEmailVerificacao envioEmail;

    @Test
    void deveVerificarEmailComTokenProtegidoDeUsoUnico() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Supervisor","email":"supervisor@example.com",
                                 "senha":"senha-segura","perfil":"EMPRESA","termosAceitos":true}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(envioEmail).enviar(
                org.mockito.ArgumentMatchers.eq("supervisor@example.com"),
                org.mockito.ArgumentMatchers.eq("Supervisor"), linkCaptor.capture());
        String token = parametroFragmento(linkCaptor.getValue(), "verificarEmail");
        Usuario supervisor = usuarioRepository.findByEmail("supervisor@example.com").orElseThrow();

        assertNull(supervisor.getEmailVerificadoEm());
        assertEquals(64, tokenRepository.findAll().get(0).getTokenHash().length());
        assertTrue(!tokenRepository.findAll().get(0).getTokenHash().equals(token));

        mockMvc.perform(get("/api/supervisao/solicitacoes")
                        .header("Authorization", bearer(supervisor)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/verificacoes-email/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());
        assertTrue(usuarioRepository.findById(supervisor.getId()).orElseThrow()
                .getEmailVerificadoEm() != null);

        mockMvc.perform(post("/api/verificacoes-email/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarTokenDeVerificacaoExpirado() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Supervisor Expirado","email":"expirado@example.com",
                                 "senha":"senha-segura","perfil":"EMPRESA","termosAceitos":true}
                                """))
                .andExpect(status().isOk());

        var tokenPersistido = tokenRepository.findAll().get(0);
        tokenPersistido.setExpiraEm(LocalDateTime.now().minusMinutes(1));
        tokenRepository.saveAndFlush(tokenPersistido);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(envioEmail).enviar(
                org.mockito.ArgumentMatchers.eq("expirado@example.com"),
                org.mockito.ArgumentMatchers.eq("Supervisor Expirado"), linkCaptor.capture());
        String token = parametroFragmento(linkCaptor.getValue(), "verificarEmail");

        mockMvc.perform(post("/api/verificacoes-email/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest());
        assertNull(usuarioRepository.findByEmail("expirado@example.com").orElseThrow()
                .getEmailVerificadoEm());
    }

    @Test
    void supervisorVerificadoDeveVerSomenteSolicitacoesDoProprioEmail() throws Exception {
        Contexto contexto = contexto();
        Usuario outro = supervisor("outro@example.com", true);
        processoComVinculo(contexto.aluno(), contexto.instituicao(), "outro@example.com");

        mockMvc.perform(get("/api/supervisao/solicitacoes")
                        .header("Authorization", bearer(contexto.supervisor())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].aluno").value(contexto.aluno().getNome()))
                .andExpect(jsonPath("$[0].instituicao").value(contexto.instituicao().getNome()))
                .andExpect(jsonPath("$[0].enderecoEmpresa").doesNotExist());
        mockMvc.perform(get("/api/supervisao/solicitacoes")
                        .header("Authorization", bearer(outro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deveConfirmarSemAlterarStatusDoProcessoEPermitirAnaliseDepois() throws Exception {
        Contexto contexto = contexto();

        mockMvc.perform(put("/api/processos/{id}/status", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.TEXT_PLAIN).content("EM_ANALISE"))
                .andExpect(status().isConflict());

        confirmar(contexto.vinculo(), contexto.supervisor()).andExpect(status().isOk());
        VinculoSupervisorProcesso confirmado = vinculoRepository
                .findById(contexto.vinculo().getId()).orElseThrow();
        assertEquals(SituacaoVinculoSupervisor.CONFIRMADO, confirmado.getSituacao());
        assertEquals(contexto.supervisor().getId(), confirmado.getSupervisor().getId());
        assertEquals("PENDENTE", processoRepository.findById(
                contexto.processo().getId()).orElseThrow().getStatus());

        mockMvc.perform(put("/api/processos/{id}/status", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.TEXT_PLAIN).content("EM_ANALISE"))
                .andExpect(status().isOk());
        assertTrue(auditoriaRepository.findAll().stream()
                .anyMatch(log -> "CONFIRMACAO_SUPERVISAO".equals(log.getAcao())));
    }

    @Test
    void deveExigirJustificativaERegistrarRecusa() throws Exception {
        Contexto contexto = contexto();
        mockMvc.perform(post("/api/supervisao/solicitacoes/{id}/recusar", contexto.vinculo().getId())
                        .header("Authorization", bearer(contexto.supervisor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"curta\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/supervisao/solicitacoes/{id}/recusar", contexto.vinculo().getId())
                        .header("Authorization", bearer(contexto.supervisor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"Não reconheço esta solicitação\"}"))
                .andExpect(status().isOk());

        VinculoSupervisorProcesso recusado = vinculoRepository
                .findById(contexto.vinculo().getId()).orElseThrow();
        assertEquals(SituacaoVinculoSupervisor.RECUSADO, recusado.getSituacao());
        assertNull(recusado.getAtivo());
        assertEquals("Não reconheço esta solicitação", recusado.getJustificativaRecusa());
        assertTrue(historicoRepository.findAll().stream().anyMatch(historico ->
                historico.getSituacaoNova() == SituacaoVinculoSupervisor.RECUSADO));
        assertTrue(auditoriaRepository.findAll().stream()
                .anyMatch(log -> "RECUSA_SUPERVISAO".equals(log.getAcao())));
    }

    @Test
    void deveImpedirAcessoPorOutroSupervisorERespostaDuplicada() throws Exception {
        Contexto contexto = contexto();
        Usuario intruso = supervisor("intruso@example.com", true);
        confirmar(contexto.vinculo(), intruso).andExpect(status().isNotFound());
        confirmar(contexto.vinculo(), contexto.supervisor()).andExpect(status().isOk());
        confirmar(contexto.vinculo(), contexto.supervisor()).andExpect(status().isConflict());
    }

    @Test
    void alunoDevePoderTrocarSupervisorSomenteDepoisDaRecusa() throws Exception {
        Contexto contexto = contexto();
        mockMvc.perform(put("/api/processos/{id}/supervisor", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.aluno()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailSupervisor\":\"novo@example.com\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/supervisao/solicitacoes/{id}/recusar", contexto.vinculo().getId())
                        .header("Authorization", bearer(contexto.supervisor()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"Não posso supervisionar o estágio\"}"))
                .andExpect(status().isOk());

        Usuario outroAluno = usuario(
                "Outro Aluno", "outro.aluno@example.com", "ALUNO", contexto.instituicao());
        outroAluno.setMatricula("RGM-2");
        usuarioRepository.save(outroAluno);
        mockMvc.perform(put("/api/processos/{id}/supervisor", contexto.processo().getId())
                        .header("Authorization", bearer(outroAluno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailSupervisor\":\"intruso@example.com\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/processos/{id}/supervisor", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.aluno()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailSupervisor\":\" NOVO@EXAMPLE.COM \"}"))
                .andExpect(status().isOk());

        List<VinculoSupervisorProcesso> vinculos = vinculoRepository.findAll();
        assertEquals(2, vinculos.size());
        assertEquals(1, vinculos.stream().filter(v -> Boolean.TRUE.equals(v.getAtivo())).count());
        assertTrue(vinculos.stream().anyMatch(v ->
                v.getSituacao() == SituacaoVinculoSupervisor.CANCELADO
                        && v.getJustificativaRecusa() != null));
        assertTrue(vinculos.stream().anyMatch(v ->
                v.getSituacao() == SituacaoVinculoSupervisor.PENDENTE
                        && "novo@example.com".equals(v.getEmailConvidado())));
        VinculoSupervisorProcesso cancelado = vinculos.stream()
                .filter(v -> v.getSituacao() == SituacaoVinculoSupervisor.CANCELADO)
                .findFirst().orElseThrow();
        VinculoSupervisorProcesso pendente = vinculos.stream()
                .filter(v -> v.getSituacao() == SituacaoVinculoSupervisor.PENDENTE)
                .findFirst().orElseThrow();
        assertEquals(
                List.of(
                        SituacaoVinculoSupervisor.PENDENTE,
                        SituacaoVinculoSupervisor.RECUSADO,
                        SituacaoVinculoSupervisor.CANCELADO),
                historicoRepository.findAllByVinculoIdOrderByDataHoraAscIdAsc(cancelado.getId())
                        .stream().map(HistoricoVinculoSupervisor::getSituacaoNova).toList());
        assertEquals(
                List.of(SituacaoVinculoSupervisor.PENDENTE),
                historicoRepository.findAllByVinculoIdOrderByDataHoraAscIdAsc(pendente.getId())
                        .stream().map(HistoricoVinculoSupervisor::getSituacaoNova).toList());
        assertTrue(auditoriaRepository.findAll().stream()
                .anyMatch(log -> "ALTERACAO_SUPERVISOR".equals(log.getAcao())));
    }

    @Test
    void bancoDeveImpedirDoisVinculosAtivosParaMesmoProcesso() {
        Contexto contexto = contexto();
        VinculoSupervisorProcesso segundo = new VinculoSupervisorProcesso();
        segundo.setProcesso(contexto.processo());
        segundo.setEmailConvidado("outro@example.com");
        segundo.setSituacao(SituacaoVinculoSupervisor.PENDENTE);

        assertThrows(DataIntegrityViolationException.class,
                () -> vinculoRepository.saveAndFlush(segundo));
    }

    private org.springframework.test.web.servlet.ResultActions confirmar(
            VinculoSupervisorProcesso vinculo, Usuario supervisor) throws Exception {
        return mockMvc.perform(post("/api/supervisao/solicitacoes/{id}/confirmar", vinculo.getId())
                .header("Authorization", bearer(supervisor)));
    }

    private Contexto contexto() {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario aluno = usuario("Ana Aluna", "aluno@example.com", "ALUNO", instituicao);
        aluno.setMatricula("RGM-1");
        usuarioRepository.save(aluno);
        Usuario responsavel = usuario(
                "Responsável", "instituicao@example.com", "INSTITUICAO", instituicao);
        Usuario supervisor = supervisor("supervisor@example.com", true);
        ProcessoEstagio processo = processoComVinculo(
                aluno, instituicao, supervisor.getEmail());
        VinculoSupervisorProcesso vinculo = vinculoRepository
                .findFirstByProcessoIdOrderByCriadoEmDescIdDesc(processo.getId()).orElseThrow();
        return new Contexto(instituicao, aluno, responsavel, supervisor, processo, vinculo);
    }

    private ProcessoEstagio processoComVinculo(
            Usuario aluno, Instituicao instituicao, String emailSupervisor) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno(aluno.getNome());
        processo.setEmailAluno(aluno.getEmail());
        processo.setNomeEmpresa("Empresa Exemplo");
        processo.setCnpjEmpresa("11222333000181");
        processo.setDataInicio(LocalDate.of(2026, 11, 10));
        processo.setDataFim(LocalDate.of(2027, 5, 10));
        processo.setStatus("PENDENTE");
        processo.setAluno(aluno);
        processo.setInstituicao(instituicao);
        processo = processoRepository.save(processo);

        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado(emailSupervisor);
        vinculo.setSituacao(SituacaoVinculoSupervisor.PENDENTE);
        vinculoRepository.save(vinculo);

        HistoricoVinculoSupervisor historico = new HistoricoVinculoSupervisor();
        historico.setVinculo(vinculo);
        historico.setSituacaoNova(SituacaoVinculoSupervisor.PENDENTE);
        historico.setUsuarioResponsavel(aluno.getEmail());
        historicoRepository.save(historico);
        return processo;
    }

    private Usuario supervisor(String email, boolean verificado) {
        Usuario usuario = usuario("Supervisor", email, "EMPRESA", null);
        if (verificado) {
            usuario.setEmailVerificadoEm(LocalDateTime.now());
            usuarioRepository.save(usuario);
        }
        return usuario;
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

    private Instituicao instituicao(String nome, String cnpj) {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome(nome);
        instituicao.setCnpj(cnpj);
        return instituicaoRepository.save(instituicao);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    private String parametroFragmento(String link, String nome) {
        String fragmento = URI.create(link).getFragment();
        for (String parte : fragmento.split("&")) {
            String[] itens = parte.split("=", 2);
            if (itens[0].equals(nome)) return itens[1];
        }
        throw new IllegalArgumentException("Parâmetro ausente");
    }

    private record Contexto(
            Instituicao instituicao,
            Usuario aluno,
            Usuario responsavel,
            Usuario supervisor,
            ProcessoEstagio processo,
            VinculoSupervisorProcesso vinculo) {
    }
}
