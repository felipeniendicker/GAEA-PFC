package Gaea.Api.security;

import Gaea.Api.model.HistoricoStatusProcesso;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.LogAuditoria;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.TipoOperacaoStatus;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import Gaea.Api.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    private static final String CHAVE_TESTE =
            "chave-temporaria-exclusiva-para-testes-gaea-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoEstagioRepository processoRepository;

    @Autowired
    private HistoricoStatusProcessoRepository historicoRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private VinculoSupervisorProcessoRepository vinculoRepository;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void limparBanco() {
        vinculoRepository.deleteAll();
        historicoRepository.deleteAll();
        processoRepository.deleteAll();
        logAuditoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
        instituicaoRepository.deleteAll();
    }

    @Test
    void devePermitirQueAlunoConsulteHistoricoDoProprioProcesso() throws Exception {
        Usuario aluno = salvarUsuario("aluno@example.com", "ALUNO");
        ProcessoEstagio processo = salvarProcesso(aluno, null, "EM_ANALISE");
        historicoRepository.save(new HistoricoStatusProcesso(
                processo,
                "PENDENTE",
                "EM_ANALISE",
                "instituicao@example.com",
                null,
                TipoOperacaoStatus.ALTERACAO_NORMAL));

        mockMvc.perform(get("/api/processos/{id}/historico", processo.getId())
                        .header("Authorization", "Bearer " + jwtService.gerarToken(aluno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].processoId").value(processo.getId()))
                .andExpect(jsonPath("$[0].statusAnterior").value("PENDENTE"))
                .andExpect(jsonPath("$[0].statusNovo").value("EM_ANALISE"))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].token").doesNotExist());
    }

    @Test
    void deveNegarHistoricoDeOutroAluno() throws Exception {
        Usuario outroAluno = salvarUsuario("outro@example.com", "ALUNO");
        Usuario alunoResponsavel = salvarUsuario("aluno@example.com", "ALUNO");
        ProcessoEstagio processo = salvarProcesso(alunoResponsavel, null, "EM_ANALISE");

        mockMvc.perform(get("/api/processos/{id}/historico", processo.getId())
                        .header("Authorization", "Bearer " + jwtService.gerarToken(outroAluno)))
                .andExpect(status().isNotFound());
    }

    @Test
    void devePermitirQueSupervisorConsulteSomenteProcessoComVinculoConfirmado() throws Exception {
        Usuario supervisor = salvarUsuario("empresa@example.com", "EMPRESA");
        Usuario outroSupervisor = salvarUsuario("outra.empresa@example.com", "EMPRESA");
        ProcessoEstagio processo = salvarProcesso(null, null, "PENDENTE");
        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado(supervisor.getEmail());
        vinculo.setSupervisor(supervisor);
        vinculo.setSituacao(SituacaoVinculoSupervisor.CONFIRMADO);
        vinculoRepository.save(vinculo);

        mockMvc.perform(get("/api/processos")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(processo.getId()));

        mockMvc.perform(get("/api/processos")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(outroSupervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/processos/{id}/historico", processo.getId())
                        .header("Authorization", "Bearer " + jwtService.gerarToken(outroSupervisor)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveIsolarProcessosEntreInstituicoes() throws Exception {
        Instituicao primeira = salvarInstituicao("Primeira Faculdade", "11222333000181");
        Instituicao segunda = salvarInstituicao("Segunda Faculdade", "22333444000160");
        Usuario responsavelPrimeira = salvarResponsavelInstituicao(
                "primeira@example.com", primeira);
        Usuario responsavelSegunda = salvarResponsavelInstituicao(
                "segunda@example.com", segunda);
        ProcessoEstagio processoPrimeira = salvarProcesso(null, primeira, "PENDENTE");
        ProcessoEstagio processoSegunda = salvarProcesso(null, segunda, "PENDENTE");

        mockMvc.perform(get("/api/processos")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(responsavelPrimeira)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(processoPrimeira.getId()))
                .andExpect(jsonPath("$[0].instituicao.id").value(primeira.getId()))
                .andExpect(jsonPath("$[0].instituicao.cnpj").doesNotExist())
                .andExpect(jsonPath("$[0].instituicao.ativa").doesNotExist())
                .andExpect(jsonPath("$[0].aluno.senha").doesNotExist());

        mockMvc.perform(get("/api/processos/{id}/historico", processoSegunda.getId())
                        .header("Authorization", "Bearer " + jwtService.gerarToken(responsavelPrimeira)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/processos/{id}/status", processoPrimeira.getId())
                        .header("Authorization", "Bearer " + jwtService.gerarToken(responsavelSegunda))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("EM_ANALISE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveBloquearCadastroInstitucionalSemVinculosConfiaveis() throws Exception {
        Instituicao instituicao = salvarInstituicao("Faculdade de Teste", "11222333000181");
        Usuario responsavel = salvarResponsavelInstituicao("instituicao@example.com", instituicao);

        mockMvc.perform(post("/api/processos")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(responsavel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeAluno": "Aluno",
                                  "emailAluno": "aluno@example.com",
                                  "nomeEmpresa": "Empresa",
                                  "cnpjEmpresa": "11.222.333/0001-81",
                                  "dataInicio": "2026-11-03",
                                  "dataFim": "2027-05-03"
                                }
                                """))
                .andExpect(status().isForbidden());

        assertEquals(0, processoRepository.count());
    }

    @Test
    void deveIsolarAuditoriaEntreInstituicoes() throws Exception {
        Instituicao primeira = salvarInstituicao("Primeira Faculdade", "11222333000181");
        Instituicao segunda = salvarInstituicao("Segunda Faculdade", "22333444000160");
        Usuario responsavelPrimeira = salvarResponsavelInstituicao(
                "primeira@example.com", primeira);
        logAuditoriaRepository.save(new LogAuditoria(
                responsavelPrimeira.getEmail(), "ACAO_PRIMEIRA", "Recurso 1", primeira));
        logAuditoriaRepository.save(new LogAuditoria(
                "segunda@example.com", "ACAO_SEGUNDA", "Recurso 2", segunda));
        logAuditoriaRepository.save(new LogAuditoria(
                "legado@example.com", "ACAO_LEGADA", "Recurso legado"));

        mockMvc.perform(get("/api/auditoria")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(responsavelPrimeira)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].acao").value("ACAO_PRIMEIRA"));
    }

    @Test
    void deveRegistrarLoginInstitucionalComInstituicaoCorreta() throws Exception {
        Instituicao instituicao = salvarInstituicao("Faculdade de Teste", "11222333000181");
        Usuario responsavel = salvarResponsavelInstituicao("instituicao@example.com", instituicao);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"instituicao@example.com\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("INSTITUICAO"));

        LogAuditoria login = logAuditoriaRepository.findAll().stream()
                .filter(log -> "LOGIN".equals(log.getAcao()))
                .findFirst()
                .orElseThrow();
        assertNotNull(login.getInstituicao());
        assertEquals(responsavel.getInstituicao().getId(), login.getInstituicao().getId());
    }

    @Test
    void deveRegistrarFalhaDeLoginInstitucionalSemExporContaOuInstituicao() throws Exception {
        Instituicao instituicao = salvarInstituicao("Faculdade de Teste", "11222333000181");
        Usuario responsavel = salvarResponsavelInstituicao("instituicao@example.com", instituicao);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"instituicao@example.com\",\"senha\":\"senha-incorreta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.instituicao").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        LogAuditoria falha = logAuditoriaRepository.findAll().stream()
                .filter(log -> "LOGIN_FALHA".equals(log.getAcao()))
                .findFirst()
                .orElseThrow();
        assertNotNull(falha.getInstituicao());
        assertEquals(responsavel.getInstituicao().getId(), falha.getInstituicao().getId());
    }

    @Test
    void deveRetornarSomenteEventosDaPropriaInstituicaoIncluindoLogin() throws Exception {
        Instituicao primeira = salvarInstituicao("Primeira Faculdade", "11222333000181");
        Instituicao segunda = salvarInstituicao("Segunda Faculdade", "22333444000160");
        Usuario responsavelPrimeira = salvarResponsavelInstituicao("primeira@example.com", primeira);
        salvarResponsavelInstituicao("segunda@example.com", segunda);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"primeira@example.com\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"segunda@example.com\",\"senha\":\"senha-incorreta\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auditoria")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(responsavelPrimeira)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].acao").value("LOGIN"))
                .andExpect(jsonPath("$[0].usuario").value("primeira@example.com"));
    }

    @Test
    void deveImpedirLoginDeUsuarioVinculadoAInstituicaoInativa() throws Exception {
        Instituicao instituicao = salvarInstituicao("Faculdade Inativa", "11222333000181");
        instituicao.setAtiva(false);
        instituicao = instituicaoRepository.save(instituicao);
        salvarResponsavelInstituicao("instituicao@example.com", instituicao);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"instituicao@example.com\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void deveInvalidarJwtEmitidoQuandoInstituicaoForDesativada() throws Exception {
        Instituicao instituicao = salvarInstituicao("Faculdade de Teste", "11222333000181");
        Usuario responsavel = salvarResponsavelInstituicao("instituicao@example.com", instituicao);
        String token = jwtService.gerarToken(responsavel);

        instituicao.setAtiva(false);
        instituicaoRepository.save(instituicao);

        mockMvc.perform(get("/api/auditoria")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devePermitirDashboardParaInstituicao() throws Exception {
        Usuario instituicao = salvarUsuario("instituicao@example.com", "INSTITUICAO");
        instituicao.setInstituicao(salvarInstituicao("Faculdade de Teste", "11222333000181"));
        instituicao = usuarioRepository.save(instituicao);

        mockMvc.perform(get("/api/dashboard/resumo")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(instituicao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessos").value(0))
                .andExpect(jsonPath("$.percentualAprovacao").value(0));
    }

    @Test
    void devePermitirDashboardParaAluno() throws Exception {
        Usuario aluno = salvarUsuario("aluno@example.com", "ALUNO");
        salvarProcesso(aluno, null, "PENDENTE");

        mockMvc.perform(get("/api/dashboard/resumo")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(aluno)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessos").value(1))
                .andExpect(jsonPath("$.pendentes").value(1));
    }

    @Test
    void devePermitirDashboardParaEmpresaComSupervisaoConfirmada() throws Exception {
        Usuario empresa = salvarUsuario("empresa@example.com", "EMPRESA");
        ProcessoEstagio processo = salvarProcesso(null, null, "EM_ANALISE");
        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado(empresa.getEmail());
        vinculo.setSupervisor(empresa);
        vinculo.setSituacao(SituacaoVinculoSupervisor.CONFIRMADO);
        vinculoRepository.save(vinculo);

        mockMvc.perform(get("/api/dashboard/resumo")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(empresa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessos").value(1))
                .andExpect(jsonPath("$.emAnalise").value(1));
    }

    @Test
    void deveNegarDashboardSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveNegarDashboardParaPerfilNaoAutorizado() throws Exception {
        Usuario usuario = salvarUsuario("administrador@example.com", "ADMIN");

        mockMvc.perform(get("/api/dashboard/resumo")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(usuario)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveNegarDashboardComJwtEmitidoAntesDaDesativacaoDaInstituicao() throws Exception {
        Instituicao instituicao = salvarInstituicao("Faculdade de Teste", "11222333000181");
        Usuario responsavel = salvarResponsavelInstituicao("instituicao@example.com", instituicao);
        String token = jwtService.gerarToken(responsavel);

        instituicao.setAtiva(false);
        instituicaoRepository.save(instituicao);

        mockMvc.perform(get("/api/dashboard/resumo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:5174"})
    void devePermitirPreflightCorsDasOrigensLocais(String origem) throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", origem)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origem));
    }

    @Test
    void deveNegarPreflightCorsDeOrigemNaoAutorizada() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5175")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void deveNegarEndpointProtegidoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/processos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarUnauthorizedParaJwtInvalido() throws Exception {
        mockMvc.perform(get("/api/processos")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarUnauthorizedParaJwtExpirado() throws Exception {
        Usuario instituicao = salvarUsuario("instituicao@example.com", "INSTITUICAO");
        String tokenExpirado = gerarTokenExpirado(instituicao);

        mockMvc.perform(get("/api/processos")
                        .header("Authorization", "Bearer " + tokenExpirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarBadRequestParaLoginSemDados() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void deveUsarMesmaRespostaParaCredenciaisInvalidas() throws Exception {
        salvarUsuario("aluno@example.com", "ALUNO");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inexistente@example.com\",\"senha\":\"senha-errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aluno@example.com\",\"senha\":\"senha-errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"));
    }

    private Usuario salvarUsuario(String email, String perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de Teste");
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode("senha-segura"));
        usuario.setPerfil(perfil);
        usuario.setTermosAceitos(true);
        return usuarioRepository.save(usuario);
    }

    private ProcessoEstagio salvarProcesso(
            Usuario aluno,
            Instituicao instituicao,
            String status) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno("Aluno de Teste");
        processo.setEmailAluno(aluno == null ? "aluno@example.com" : aluno.getEmail());
        processo.setNomeEmpresa("Empresa de Teste");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus(status);
        processo.setAluno(aluno);
        processo.setInstituicao(instituicao);
        return processoRepository.save(processo);
    }

    private Instituicao salvarInstituicao(String nome, String cnpj) {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome(nome);
        instituicao.setCnpj(cnpj);
        return instituicaoRepository.save(instituicao);
    }

    private Usuario salvarResponsavelInstituicao(String email, Instituicao instituicao) {
        Usuario usuario = salvarUsuario(email, "INSTITUICAO");
        usuario.setInstituicao(instituicao);
        return usuarioRepository.save(usuario);
    }

    private String gerarTokenExpirado(Usuario usuario) {
        long agora = System.currentTimeMillis();
        SecretKey chave = Keys.hmacShaKeyFor(CHAVE_TESTE.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(usuario.getEmail())
                .issuedAt(new Date(agora - 7200000))
                .expiration(new Date(agora - 3600000))
                .signWith(chave)
                .compact();
    }
}
