package Gaea.Api.security;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.PendenciaProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoPendencia;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.ConviteAtivacaoAlunoRepository;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.HistoricoVinculoSupervisorRepository;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.PendenciaProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.TokenVerificacaoEmailRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import Gaea.Api.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PendenciaProcessoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PendenciaProcessoRepository pendenciaRepository;

    @Autowired
    private ProcessoEstagioRepository processoRepository;

    @Autowired
    private VinculoSupervisorProcessoRepository vinculoRepository;

    @Autowired
    private HistoricoVinculoSupervisorRepository historicoVinculoRepository;

    @Autowired
    private HistoricoStatusProcessoRepository historicoStatusRepository;

    @Autowired
    private TokenVerificacaoEmailRepository tokenRepository;

    @Autowired
    private ConviteAtivacaoAlunoRepository conviteRepository;

    @Autowired
    private LogAuditoriaRepository auditoriaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    @AfterEach
    void limparBanco() {
        pendenciaRepository.deleteAll();
        historicoVinculoRepository.deleteAll();
        vinculoRepository.deleteAll();
        historicoStatusRepository.deleteAll();
        processoRepository.deleteAll();
        tokenRepository.deleteAll();
        conviteRepository.deleteAll();
        auditoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
        instituicaoRepository.deleteAll();
    }

    @Test
    void deveRegistrarPendenciaValidaComPersistenciaEAuditoria() throws Exception {
        Contexto contexto = contexto();

        mockMvc.perform(post("/api/processos/{id}/pendencias", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"  Corrigir o termo de compromisso  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processoId").value(contexto.processo().getId()))
                .andExpect(jsonPath("$.descricao").value("Corrigir o termo de compromisso"))
                .andExpect(jsonPath("$.situacao").value("ABERTA"))
                .andExpect(jsonPath("$.criadaEm").exists())
                .andExpect(jsonPath("$.resolvidaEm").doesNotExist())
                .andExpect(jsonPath("$.responsavelRegistro").value(contexto.responsavel().getEmail()));

        PendenciaProcesso pendencia = pendenciaRepository.findAll().get(0);
        assertEquals(contexto.processo().getId(), pendencia.getProcesso().getId());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "REGISTRO_PENDENCIA".equals(log.getAcao())
                        && contexto.instituicao().getId().equals(log.getInstituicao().getId())));
    }

    @Test
    void deveRejeitarDescricaoVazia() throws Exception {
        Contexto contexto = contexto();

        mockMvc.perform(post("/api/processos/{id}/pendencias", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descricao").exists());

        assertEquals(0, pendenciaRepository.count());
    }

    @Test
    void deveRetornarNaoEncontradoParaProcessoInexistente() throws Exception {
        Contexto contexto = contexto();

        mockMvc.perform(post("/api/processos/{id}/pendencias", 999999)
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Documento pendente\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveImpedirInstituicaoDeRegistrarOuResolverPendenciaDeOutroProcesso() throws Exception {
        Contexto contexto = contexto();
        Instituicao outra = instituicao("Outra Faculdade", "22333444000160");
        Usuario outroResponsavel = usuario("Outro responsável", "outra@example.com", "INSTITUICAO", outra);
        PendenciaProcesso pendencia = pendencia(contexto.processo(), "Documento pendente");

        mockMvc.perform(post("/api/processos/{id}/pendencias", contexto.processo().getId())
                        .header("Authorization", bearer(outroResponsavel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Tentativa indevida\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/processos/{id}/pendencias/{pendenciaId}/resolver",
                        contexto.processo().getId(), pendencia.getId())
                        .header("Authorization", bearer(outroResponsavel)))
                .andExpect(status().isNotFound());
    }

    @Test
    void devePermitirAlunoConsultarPendenciasDoProprioProcesso() throws Exception {
        Contexto contexto = contexto();
        pendencia(contexto.processo(), "Enviar documento assinado");

        mockMvc.perform(get("/api/processos/{id}/pendencias", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.aluno())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].descricao").value("Enviar documento assinado"));
    }

    @Test
    void deveImpedirAlunoDeConsultarProcessoAlheio() throws Exception {
        Contexto contexto = contexto();
        Usuario outroAluno = usuario("Outro aluno", "outro.aluno@example.com", "ALUNO", contexto.instituicao());
        outroAluno.setMatricula("RGM-2");
        usuarioRepository.save(outroAluno);
        pendencia(contexto.processo(), "Pendência protegida");

        mockMvc.perform(get("/api/processos/{id}/pendencias", contexto.processo().getId())
                        .header("Authorization", bearer(outroAluno)))
                .andExpect(status().isNotFound());
    }

    @Test
    void devePermitirSomenteSupervisorConfirmadoConsultarPendencias() throws Exception {
        Contexto contexto = contexto();
        Usuario supervisorConfirmado = usuario(
                "Supervisor", "supervisor@example.com", "EMPRESA", null);
        Usuario supervisorPendente = usuario(
                "Supervisor pendente", "pendente@example.com", "EMPRESA", null);
        pendencia(contexto.processo(), "Validar carga horária");
        vinculo(contexto.processo(), supervisorConfirmado, SituacaoVinculoSupervisor.CONFIRMADO);

        ProcessoEstagio processoPendente = processo(contexto.aluno(), contexto.instituicao());
        pendencia(processoPendente, "Pendência sem supervisão confirmada");
        vinculo(processoPendente, supervisorPendente, SituacaoVinculoSupervisor.PENDENTE);

        mockMvc.perform(get("/api/processos/{id}/pendencias", contexto.processo().getId())
                        .header("Authorization", bearer(supervisorConfirmado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/processos/{id}/pendencias", processoPendente.getId())
                        .header("Authorization", bearer(supervisorPendente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveResolverPendenciaERegistrarAuditoria() throws Exception {
        Contexto contexto = contexto();
        PendenciaProcesso pendencia = pendencia(contexto.processo(), "Corrigir documentação");

        mockMvc.perform(put("/api/processos/{id}/pendencias/{pendenciaId}/resolver",
                        contexto.processo().getId(), pendencia.getId())
                        .header("Authorization", bearer(contexto.responsavel())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("RESOLVIDA"))
                .andExpect(jsonPath("$.resolvidaEm").exists());

        PendenciaProcesso atualizada = pendenciaRepository.findById(pendencia.getId()).orElseThrow();
        assertEquals(SituacaoPendencia.RESOLVIDA, atualizada.getSituacao());
        assertNotNull(atualizada.getResolvidaEm());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "RESOLUCAO_PENDENCIA".equals(log.getAcao())
                        && contexto.instituicao().getId().equals(log.getInstituicao().getId())));
    }

    @Test
    void deveRejeitarResolucaoDuplicada() throws Exception {
        Contexto contexto = contexto();
        PendenciaProcesso pendencia = pendencia(contexto.processo(), "Corrigir documentação");

        resolver(contexto, pendencia).andExpect(status().isOk());
        resolver(contexto, pendencia)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("A pendência já está resolvida"));
    }

    @Test
    void deveBloquearCriacaoEResolucaoPorAlunoEEmpresa() throws Exception {
        Contexto contexto = contexto();
        Usuario empresa = usuario("Supervisor", "supervisor@example.com", "EMPRESA", null);
        PendenciaProcesso pendencia = pendencia(contexto.processo(), "Documento pendente");

        for (Usuario usuario : new Usuario[]{contexto.aluno(), empresa}) {
            mockMvc.perform(post("/api/processos/{id}/pendencias", contexto.processo().getId())
                            .header("Authorization", bearer(usuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"descricao\":\"Tentativa indevida\"}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/processos/{id}/pendencias/{pendenciaId}/resolver",
                            contexto.processo().getId(), pendencia.getId())
                            .header("Authorization", bearer(usuario)))
                    .andExpect(status().isForbidden());
        }
    }

    private org.springframework.test.web.servlet.ResultActions resolver(
            Contexto contexto,
            PendenciaProcesso pendencia) throws Exception {

        return mockMvc.perform(put("/api/processos/{id}/pendencias/{pendenciaId}/resolver",
                        contexto.processo().getId(), pendencia.getId())
                .header("Authorization", bearer(contexto.responsavel())));
    }

    private Contexto contexto() {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario aluno = usuario("Ana Aluna", "aluno@example.com", "ALUNO", instituicao);
        aluno.setMatricula("RGM-1");
        usuarioRepository.save(aluno);
        Usuario responsavel = usuario(
                "Responsável", "instituicao@example.com", "INSTITUICAO", instituicao);
        ProcessoEstagio processo = processo(aluno, instituicao);
        return new Contexto(instituicao, aluno, responsavel, processo);
    }

    private Instituicao instituicao(String nome, String cnpj) {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome(nome);
        instituicao.setCnpj(cnpj);
        return instituicaoRepository.save(instituicao);
    }

    private Usuario usuario(String nome, String email, String perfil, Instituicao instituicao) {
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

    private ProcessoEstagio processo(Usuario aluno, Instituicao instituicao) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno(aluno.getNome());
        processo.setEmailAluno(aluno.getEmail());
        processo.setNomeEmpresa("Empresa Exemplo");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus("EM_ANALISE");
        processo.setAluno(aluno);
        processo.setInstituicao(instituicao);
        return processoRepository.save(processo);
    }

    private PendenciaProcesso pendencia(ProcessoEstagio processo, String descricao) {
        PendenciaProcesso pendencia = new PendenciaProcesso();
        pendencia.setProcesso(processo);
        pendencia.setDescricao(descricao);
        pendencia.setSituacao(SituacaoPendencia.ABERTA);
        pendencia.setResponsavelRegistro("instituicao@example.com");
        return pendenciaRepository.save(pendencia);
    }

    private void vinculo(
            ProcessoEstagio processo,
            Usuario supervisor,
            SituacaoVinculoSupervisor situacao) {

        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado(supervisor.getEmail());
        vinculo.setSupervisor(supervisor);
        vinculo.setSituacao(situacao);
        vinculoRepository.save(vinculo);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    private record Contexto(
            Instituicao instituicao,
            Usuario aluno,
            Usuario responsavel,
            ProcessoEstagio processo) {
    }
}
