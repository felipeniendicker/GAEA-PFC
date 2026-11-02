package Gaea.Api.security;

import Gaea.Api.model.ConviteAtivacaoAluno;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.ConviteAtivacaoAlunoRepository;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GestaoAlunosIntegrationTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private ConviteAtivacaoAlunoRepository conviteRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    @AfterEach
    void limparBanco() {
        conviteRepository.deleteAll();
        logAuditoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
        instituicaoRepository.deleteAll();
    }

    @Test
    void deveCadastrarListarEProtegerDadosDoAlunoDaPropriaInstituicao() throws Exception {
        Usuario responsavel = responsavel("instituicao@example.com", instituicao("Faculdade A", "11222333000181"));

        MvcResult cadastro = cadastrarAluno(responsavel, "Aluno Teste", "aluno@example.com", "RGM-123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aluno.nome").value("Aluno Teste"))
                .andExpect(jsonPath("$.aluno.email").value("aluno@example.com"))
                .andExpect(jsonPath("$.aluno.matricula").value("RGM-123"))
                .andExpect(jsonPath("$.aluno.ativado").value(false))
                .andExpect(jsonPath("$.aluno.situacaoAtivacao").value("AGUARDANDO_ATIVACAO"))
                .andExpect(jsonPath("$.tokenAtivacao").isNotEmpty())
                .andExpect(jsonPath("$.aluno.senha").doesNotExist())
                .andExpect(jsonPath("$.aluno.termosAceitos").doesNotExist())
                .andReturn();

        String token = json(cadastro).get("tokenAtivacao").asText();
        ConviteAtivacaoAluno convite = conviteRepository.findAll().get(0);
        assertNotEquals(token, convite.getTokenHash());
        assertFalse(convite.getTokenHash().contains(token));

        mockMvc.perform(get("/api/instituicoes/alunos")
                        .header("Authorization", bearer(responsavel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("aluno@example.com"))
                .andExpect(jsonPath("$[0].senha").doesNotExist())
                .andExpect(jsonPath("$[0].tokenAtivacao").doesNotExist())
                .andExpect(jsonPath("$[0].tokenHash").doesNotExist());

        Usuario aluno = usuarioRepository.findByEmailNormalizado("aluno@example.com").orElseThrow();
        assertNull(aluno.getSenha());
        assertFalse(aluno.getAtivado());
        assertTrue(logAuditoriaRepository.findAllByInstituicaoIdOrderByDataHoraDescIdDesc(
                responsavel.getInstituicao().getId()).stream()
                .anyMatch(log -> "CADASTRO_ALUNO".equals(log.getAcao())));
    }

    @Test
    void deveIsolarAlunosEntreInstituicoes() throws Exception {
        Usuario primeira = responsavel("primeira@example.com", instituicao("Faculdade A", "11222333000181"));
        Usuario segunda = responsavel("segunda@example.com", instituicao("Faculdade B", "22333444000160"));
        MvcResult cadastro = cadastrarAluno(primeira, "Aluno A", "aluno.a@example.com", "RGM-A")
                .andExpect(status().isOk())
                .andReturn();
        long alunoId = json(cadastro).at("/aluno/id").asLong();

        mockMvc.perform(get("/api/instituicoes/alunos")
                        .header("Authorization", bearer(segunda)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/api/instituicoes/alunos/{id}", alunoId)
                        .header("Authorization", bearer(segunda))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Tentativa indevida","matricula":"OUTRA"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/instituicoes/alunos/{id}/situacao", alunoId)
                        .header("Authorization", bearer(segunda))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"ativo\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveImpedirEmailDuplicadoMesmoEmOutraInstituicao() throws Exception {
        Usuario primeira = responsavel("primeira@example.com", instituicao("Faculdade A", "11222333000181"));
        Usuario segunda = responsavel("segunda@example.com", instituicao("Faculdade B", "22333444000160"));
        cadastrarAluno(primeira, "Aluno A", "aluno@example.com", "RGM-001")
                .andExpect(status().isOk());

        cadastrarAluno(segunda, "Outro Aluno", " ALUNO@EXAMPLE.COM ", "RGM-002")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }

    @Test
    void deveExigirMatriculaNaoVazia() throws Exception {
        Usuario responsavel = responsavel("instituicao@example.com", instituicao("Faculdade A", "11222333000181"));

        cadastrarAluno(responsavel, "Aluno Sem Matrícula", "sem.matricula@example.com", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.matricula").value("A matrícula é obrigatória"));

        cadastrarAluno(responsavel, "Aluno Com Espaços", "espacos@example.com", "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.matricula").value("A matrícula é obrigatória"));
    }

    @Test
    void deveImpedirMatriculaDuplicadaNaMesmaInstituicao() throws Exception {
        Usuario responsavel = responsavel("instituicao@example.com", instituicao("Faculdade A", "11222333000181"));
        cadastrarAluno(responsavel, "Primeiro Aluno", "primeiro@example.com", "000123")
                .andExpect(status().isOk());

        cadastrarAluno(responsavel, "Segundo Aluno", "segundo@example.com", "000123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Matrícula já cadastrada nesta instituição"));
    }

    @Test
    void devePermitirMesmaMatriculaEmInstituicoesDiferentes() throws Exception {
        Usuario primeira = responsavel("primeira@example.com", instituicao("Faculdade A", "11222333000181"));
        Usuario segunda = responsavel("segunda@example.com", instituicao("Faculdade B", "22333444000160"));

        cadastrarAluno(primeira, "Aluno A", "aluno.a@example.com", "000123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aluno.matricula").value("000123"));
        cadastrarAluno(segunda, "Aluno B", "aluno.b@example.com", "000123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aluno.matricula").value("000123"));
    }

    @Test
    void deveAtivarContaComTokenUmaUnicaVezESenhaDefinidaPeloAluno() throws Exception {
        Usuario responsavel = responsavel("instituicao@example.com", instituicao("Faculdade A", "11222333000181"));
        MvcResult cadastro = cadastrarAluno(responsavel, "Aluno", "aluno@example.com", "RGM-001")
                .andExpect(status().isOk())
                .andReturn();
        String token = json(cadastro).get("tokenAtivacao").asText();

        ativar(token, "senha-segura-123")
                .andExpect(status().isNoContent());

        Usuario aluno = usuarioRepository.findByEmailNormalizado("aluno@example.com").orElseThrow();
        assertTrue(aluno.getAtivado());
        assertTrue(aluno.getTermosAceitos());
        assertTrue(passwordEncoder.matches("senha-segura-123", aluno.getSenha()));
        assertTrue(conviteRepository.findAll().get(0).getUtilizadoEm() != null);

        ativar(token, "outra-senha-123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Este convite de ativação já foi utilizado"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"aluno@example.com","senha":"senha-segura-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ALUNO"));
    }

    @Test
    void deveRejeitarConviteInvalidoEExpirado() throws Exception {
        ativar("token-invalido-com-tamanho-minimo-000000000000", "senha-segura-123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Convite de ativação inválido"));

        Usuario responsavel = responsavel("instituicao@example.com", instituicao("Faculdade A", "11222333000181"));
        MvcResult cadastro = cadastrarAluno(responsavel, "Aluno", "aluno@example.com", "RGM-001")
                .andExpect(status().isOk())
                .andReturn();
        String token = json(cadastro).get("tokenAtivacao").asText();
        ConviteAtivacaoAluno convite = conviteRepository.findAll().get(0);
        convite.setExpiraEm(LocalDateTime.now().minusMinutes(1));
        conviteRepository.saveAndFlush(convite);

        ativar(token, "senha-segura-123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Este convite de ativação expirou"));
    }

    @Test
    void deveInvalidarConviteAnteriorAoGerarOutro() throws Exception {
        Usuario responsavel = responsavel("instituicao@example.com", instituicao("Faculdade A", "11222333000181"));
        MvcResult cadastro = cadastrarAluno(responsavel, "Aluno", "aluno@example.com", "RGM-001")
                .andExpect(status().isOk())
                .andReturn();
        long alunoId = json(cadastro).at("/aluno/id").asLong();
        String tokenAnterior = json(cadastro).get("tokenAtivacao").asText();

        MvcResult novoConvite = mockMvc.perform(post("/api/instituicoes/alunos/{id}/convite", alunoId)
                        .header("Authorization", bearer(responsavel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAtivacao").isNotEmpty())
                .andReturn();
        String tokenNovo = json(novoConvite).get("tokenAtivacao").asText();
        assertNotEquals(tokenAnterior, tokenNovo);

        ativar(tokenAnterior, "senha-segura-123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Este convite de ativação não é mais válido"));
        ativar(tokenNovo, "senha-segura-123")
                .andExpect(status().isNoContent());
    }

    @Test
    void deveDesativarReativarEBloquearAutenticacaoEJwtExistente() throws Exception {
        Usuario responsavel = responsavel("instituicao@example.com", instituicao("Faculdade A", "11222333000181"));
        MvcResult cadastro = cadastrarAluno(responsavel, "Aluno", "aluno@example.com", "RGM-001")
                .andExpect(status().isOk())
                .andReturn();
        long alunoId = json(cadastro).at("/aluno/id").asLong();
        String tokenConvite = json(cadastro).get("tokenAtivacao").asText();
        ativar(tokenConvite, "senha-segura-123").andExpect(status().isNoContent());
        Usuario aluno = usuarioRepository.findById(alunoId).orElseThrow();
        String jwtAnterior = jwtService.gerarToken(aluno);

        alterarSituacao(responsavel, alunoId, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.situacaoAtivacao").value("DESATIVADO"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"aluno@example.com","senha":"senha-segura-123"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/processos/meus")
                        .header("Authorization", "Bearer " + jwtAnterior))
                .andExpect(status().isUnauthorized());

        alterarSituacao(responsavel, alunoId, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"aluno@example.com","senha":"senha-segura-123"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deveRestringirGestaoAInstituicaoAutenticada() throws Exception {
        Usuario aluno = usuario("aluno@example.com", "ALUNO", null);
        Usuario empresa = usuario("empresa@example.com", "EMPRESA", null);

        mockMvc.perform(get("/api/instituicoes/alunos")
                        .header("Authorization", bearer(aluno)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/instituicoes/alunos")
                        .header("Authorization", bearer(empresa)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/instituicoes/alunos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveImpedirCadastroPublicoDeAlunoSemInstituicaoEMatricula() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":"Aluno fora da instituição",
                                  "email":"aluno.publico@example.com",
                                  "senha":"senha-segura-123",
                                  "perfil":"ALUNO",
                                  "termosAceitos":true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.perfil").exists());

        assertFalse(usuarioRepository.existsByEmailNormalizado("aluno.publico@example.com"));
    }

    @Test
    void devePermitirEditarSomenteNomeEMatricula() throws Exception {
        Usuario responsavel = responsavel("instituicao@example.com", instituicao("Faculdade A", "11222333000181"));
        MvcResult cadastro = cadastrarAluno(responsavel, "Aluno", "aluno@example.com", "RGM-001")
                .andExpect(status().isOk())
                .andReturn();
        long alunoId = json(cadastro).at("/aluno/id").asLong();

        mockMvc.perform(put("/api/instituicoes/alunos/{id}", alunoId)
                        .header("Authorization", bearer(responsavel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":"Aluno Atualizado",
                                  "matricula":"RGM-456",
                                  "email":"fraude@example.com",
                                  "instituicaoId":999,
                                  "ativo":false
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/instituicoes/alunos/{id}", alunoId)
                        .header("Authorization", bearer(responsavel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":"Aluno Atualizado",
                                  "matricula":"RGM-456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Aluno Atualizado"))
                .andExpect(jsonPath("$.matricula").value("RGM-456"))
                .andExpect(jsonPath("$.email").value("aluno@example.com"))
                .andExpect(jsonPath("$.ativo").value(true));

        assertTrue(logAuditoriaRepository.findAllByInstituicaoIdOrderByDataHoraDescIdDesc(
                responsavel.getInstituicao().getId()).stream()
                .anyMatch(log -> "CORRECAO_MATRICULA_ALUNO".equals(log.getAcao())
                        && log.getRecurso().contains("RGM-001 -> RGM-456")));
    }

    private org.springframework.test.web.servlet.ResultActions cadastrarAluno(
            Usuario responsavel,
            String nome,
            String email,
            String matricula) throws Exception {

        String matriculaJson = matricula == null ? "null" : "\"" + matricula + "\"";
        return mockMvc.perform(post("/api/instituicoes/alunos")
                .header("Authorization", bearer(responsavel))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"%s","email":"%s","matricula":%s}
                        """.formatted(nome, email, matriculaJson)));
    }

    private org.springframework.test.web.servlet.ResultActions ativar(
            String token,
            String senha) throws Exception {

        return mockMvc.perform(post("/api/ativacoes/aluno")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"%s","senha":"%s","termosAceitos":true}
                        """.formatted(token, senha)));
    }

    private org.springframework.test.web.servlet.ResultActions alterarSituacao(
            Usuario responsavel,
            long alunoId,
            boolean ativo) throws Exception {

        return mockMvc.perform(patch("/api/instituicoes/alunos/{id}/situacao", alunoId)
                .header("Authorization", bearer(responsavel))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"ativo\":" + ativo + "}"));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Instituicao instituicao(String nome, String cnpj) {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome(nome);
        instituicao.setCnpj(cnpj);
        return instituicaoRepository.save(instituicao);
    }

    private Usuario responsavel(String email, Instituicao instituicao) {
        return usuario(email, "INSTITUICAO", instituicao);
    }

    private Usuario usuario(String email, String perfil, Instituicao instituicao) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de Teste");
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode("senha-segura-123"));
        usuario.setPerfil(perfil);
        usuario.setInstituicao(instituicao);
        usuario.setAtivo(true);
        usuario.setAtivado(true);
        usuario.setTermosAceitos(true);
        usuario.setTermosAceitosEm(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }
}
