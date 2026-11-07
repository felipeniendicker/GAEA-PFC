package Gaea.Api.security;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.PrazoProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.ConviteAtivacaoAlunoRepository;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.HistoricoVinculoSupervisorRepository;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.PendenciaProcessoRepository;
import Gaea.Api.repository.PrazoProcessoRepository;
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

import java.time.LocalDate;

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
class PrazoProcessoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrazoProcessoRepository prazoRepository;

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
        prazoRepository.deleteAll();
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
    void deveRegistrarPrazoValidoComPersistenciaEAuditoria() throws Exception {
        Contexto contexto = contexto();

        mockMvc.perform(post("/api/processos/{id}/prazos", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descricao": "  Entregar relatório parcial  ",
                                  "dataVencimento": "2026-11-20"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processoId").value(contexto.processo().getId()))
                .andExpect(jsonPath("$.descricao").value("Entregar relatório parcial"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-11-20"))
                .andExpect(jsonPath("$.criadaEm").exists())
                .andExpect(jsonPath("$.atualizadaEm").exists())
                .andExpect(jsonPath("$.responsavelRegistro").value(contexto.responsavel().getEmail()));

        PrazoProcesso prazo = prazoRepository.findAll().get(0);
        assertEquals(contexto.processo().getId(), prazo.getProcesso().getId());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "REGISTRO_PRAZO".equals(log.getAcao())
                        && contexto.instituicao().getId().equals(log.getInstituicao().getId())));
    }

    @Test
    void deveRejeitarDescricaoVaziaEDataAusenteOuInvalida() throws Exception {
        Contexto contexto = contexto();

        mockMvc.perform(post("/api/processos/{id}/prazos", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.descricao").exists())
                .andExpect(jsonPath("$.campos.dataVencimento").exists());

        mockMvc.perform(post("/api/processos/{id}/prazos", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Prazo\",\"dataVencimento\":\"31/12/2026\"}"))
                .andExpect(status().isBadRequest());

        assertEquals(0, prazoRepository.count());
    }

    @Test
    void deveRetornarNaoEncontradoParaProcessoInexistente() throws Exception {
        Contexto contexto = contexto();

        mockMvc.perform(post("/api/processos/{id}/prazos", 999999)
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Prazo", "2026-11-20")))
                .andExpect(status().isNotFound());
    }

    @Test
    void devePreservarIsolamentoInstitucionalNoCadastroEAtualizacao() throws Exception {
        Contexto contexto = contexto();
        Instituicao outra = instituicao("Outra Faculdade", "22333444000160");
        Usuario outroResponsavel = usuario("Outro responsável", "outra@example.com", "INSTITUICAO", outra);
        PrazoProcesso prazo = prazo(contexto.processo(), "Prazo protegido", LocalDate.of(2026, 11, 20));

        mockMvc.perform(post("/api/processos/{id}/prazos", contexto.processo().getId())
                        .header("Authorization", bearer(outroResponsavel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Tentativa indevida", "2026-11-21")))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/processos/{id}/prazos/{prazoId}",
                        contexto.processo().getId(), prazo.getId())
                        .header("Authorization", bearer(outroResponsavel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Tentativa indevida", "2026-11-22")))
                .andExpect(status().isNotFound());
    }

    @Test
    void devePermitirAlunoConsultarPrazosDoProprioProcessoEBloquearOutroAluno() throws Exception {
        Contexto contexto = contexto();
        prazo(contexto.processo(), "Entrega final", LocalDate.of(2026, 11, 25));
        Usuario outroAluno = usuario("Outro aluno", "outro.aluno@example.com", "ALUNO", contexto.instituicao());
        outroAluno.setMatricula("RGM-2");
        usuarioRepository.save(outroAluno);

        mockMvc.perform(get("/api/processos/{id}/prazos", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.aluno())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].descricao").value("Entrega final"));

        mockMvc.perform(get("/api/processos/{id}/prazos", contexto.processo().getId())
                        .header("Authorization", bearer(outroAluno)))
                .andExpect(status().isNotFound());
    }

    @Test
    void devePermitirSomenteSupervisorAtivoEConfirmadoConsultarPrazos() throws Exception {
        Contexto contexto = contexto();
        Usuario confirmado = usuario("Supervisor", "supervisor@example.com", "EMPRESA", null);
        Usuario pendente = usuario("Supervisor pendente", "pendente@example.com", "EMPRESA", null);
        prazo(contexto.processo(), "Validar relatório", LocalDate.of(2026, 11, 28));
        vinculo(contexto.processo(), confirmado, SituacaoVinculoSupervisor.CONFIRMADO);

        ProcessoEstagio processoPendente = processo(contexto.aluno(), contexto.instituicao());
        prazo(processoPendente, "Prazo sem confirmação", LocalDate.of(2026, 11, 29));
        vinculo(processoPendente, pendente, SituacaoVinculoSupervisor.PENDENTE);

        mockMvc.perform(get("/api/processos/{id}/prazos", contexto.processo().getId())
                        .header("Authorization", bearer(confirmado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/processos/{id}/prazos", processoPendente.getId())
                        .header("Authorization", bearer(pendente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarPrazoCorretoERegistrarAuditoria() throws Exception {
        Contexto contexto = contexto();
        PrazoProcesso prazo = prazo(
                contexto.processo(), "Prazo original", LocalDate.of(2026, 11, 20));

        mockMvc.perform(put("/api/processos/{id}/prazos/{prazoId}",
                        contexto.processo().getId(), prazo.getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Prazo atualizado", "2026-12-05")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Prazo atualizado"))
                .andExpect(jsonPath("$.dataVencimento").value("2026-12-05"))
                .andExpect(jsonPath("$.atualizadaEm").exists());

        PrazoProcesso atualizado = prazoRepository.findById(prazo.getId()).orElseThrow();
        assertEquals("Prazo atualizado", atualizado.getDescricao());
        assertEquals(LocalDate.of(2026, 12, 5), atualizado.getDataVencimento());
        assertNotNull(atualizado.getAtualizadaEm());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "ATUALIZACAO_PRAZO".equals(log.getAcao())
                        && contexto.instituicao().getId().equals(log.getInstituicao().getId())));
    }

    @Test
    void deveImpedirAtualizacaoDePrazoPertencenteAOutroProcesso() throws Exception {
        Contexto contexto = contexto();
        ProcessoEstagio outroProcesso = processo(contexto.aluno(), contexto.instituicao());
        PrazoProcesso prazoDeOutroProcesso = prazo(
                outroProcesso, "Prazo de outro processo", LocalDate.of(2026, 11, 20));

        mockMvc.perform(put("/api/processos/{id}/prazos/{prazoId}",
                        contexto.processo().getId(), prazoDeOutroProcesso.getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Tentativa indevida", "2026-11-30")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveBloquearCadastroEAtualizacaoPorAlunoEEmpresa() throws Exception {
        Contexto contexto = contexto();
        Usuario empresa = usuario("Supervisor", "supervisor@example.com", "EMPRESA", null);
        PrazoProcesso prazo = prazo(contexto.processo(), "Prazo", LocalDate.of(2026, 11, 20));

        for (Usuario usuario : new Usuario[]{contexto.aluno(), empresa}) {
            mockMvc.perform(post("/api/processos/{id}/prazos", contexto.processo().getId())
                            .header("Authorization", bearer(usuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("Tentativa indevida", "2026-11-21")))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put("/api/processos/{id}/prazos/{prazoId}",
                            contexto.processo().getId(), prazo.getId())
                            .header("Authorization", bearer(usuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpo("Tentativa indevida", "2026-11-22")))
                    .andExpect(status().isForbidden());
        }
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

    private PrazoProcesso prazo(
            ProcessoEstagio processo,
            String descricao,
            LocalDate dataVencimento) {

        PrazoProcesso prazo = new PrazoProcesso();
        prazo.setProcesso(processo);
        prazo.setDescricao(descricao);
        prazo.setDataVencimento(dataVencimento);
        prazo.setResponsavelRegistro("instituicao@example.com");
        return prazoRepository.save(prazo);
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

    private String corpo(String descricao, String dataVencimento) {
        return """
                {"descricao":"%s","dataVencimento":"%s"}
                """.formatted(descricao, dataVencimento);
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
