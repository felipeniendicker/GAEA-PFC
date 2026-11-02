package Gaea.Api.security;

import Gaea.Api.model.HistoricoStatusProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.TipoOperacaoStatus;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limparBanco() {
        historicoRepository.deleteAll();
        processoRepository.deleteAll();
        logAuditoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void devePermitirQueAlunoConsulteHistoricoDoProprioProcesso() throws Exception {
        Usuario aluno = salvarUsuario("aluno@example.com", "ALUNO");
        ProcessoEstagio processo = salvarProcesso("aluno@example.com", "EM_ANALISE");
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
        ProcessoEstagio processo = salvarProcesso("aluno@example.com", "EM_ANALISE");

        mockMvc.perform(get("/api/processos/{id}/historico", processo.getId())
                        .header("Authorization", "Bearer " + jwtService.gerarToken(outroAluno)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveNegarEndpointAdministrativoParaEmpresa() throws Exception {
        Usuario empresa = salvarUsuario("empresa@example.com", "EMPRESA");

        mockMvc.perform(get("/api/processos")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(empresa)))
                .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirDashboardParaInstituicao() throws Exception {
        Usuario instituicao = salvarUsuario("instituicao@example.com", "INSTITUICAO");

        mockMvc.perform(get("/api/dashboard/resumo")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(instituicao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessos").value(0))
                .andExpect(jsonPath("$.percentualAprovacao").value(0));
    }

    @Test
    void deveNegarDashboardParaAluno() throws Exception {
        Usuario aluno = salvarUsuario("aluno@example.com", "ALUNO");

        mockMvc.perform(get("/api/dashboard/resumo")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(aluno)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveNegarDashboardParaEmpresa() throws Exception {
        Usuario empresa = salvarUsuario("empresa@example.com", "EMPRESA");

        mockMvc.perform(get("/api/dashboard/resumo")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(empresa)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveNegarDashboardSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo"))
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

    private ProcessoEstagio salvarProcesso(String emailAluno, String status) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno("Aluno de Teste");
        processo.setEmailAluno(emailAluno);
        processo.setNomeEmpresa("Empresa de Teste");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus(status);
        return processoRepository.save(processo);
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
