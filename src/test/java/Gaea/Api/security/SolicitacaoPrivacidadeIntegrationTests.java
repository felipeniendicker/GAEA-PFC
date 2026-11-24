package Gaea.Api.security;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.SolicitacaoPrivacidade;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.SolicitacaoPrivacidadeRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitacaoPrivacidadeIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired InstituicaoRepository instituicaoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired SolicitacaoPrivacidadeRepository solicitacaoRepository;
    @Autowired LogAuditoriaRepository auditoriaRepository;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void deveCadastrarParaAlunoInstituicaoEEmpresa() throws Exception {
        Usuario aluno = salvarUsuario("aluno.privacidade@teste.example", "ALUNO", null);
        Usuario instituicao = salvarUsuario(
                "instituicao.privacidade@teste.example", "INSTITUICAO",
                salvarInstituicao());
        Usuario empresa = salvarUsuario("empresa.privacidade@teste.example", "EMPRESA", null);

        cadastrar(aluno, "{\"tipo\":\"ACESSO\",\"descricao\":\"Solicito acesso aos meus dados\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ACESSO"))
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.criadoEm").exists());
        cadastrar(instituicao, "{\"tipo\":\"CORRECAO\",\"descricao\":\"Corrigir meu nome\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("CORRECAO"));
        cadastrar(empresa, "{\"tipo\":\"ACESSO\",\"descricao\":\"Consultar meus dados\"}")
                .andExpect(status().isOk());

        assertEquals(3, solicitacaoRepository.count());
        assertEquals(3, auditoriaRepository.findAll().stream()
                .filter(log -> "SOLICITACAO_PRIVACIDADE".equals(log.getAcao()))
                .count());
    }

    @Test
    void deveVincularSolicitacaoAoAutenticadoSemAceitarIdentidadeDoCorpo() throws Exception {
        Usuario autor = salvarUsuario("autor.privacidade@teste.example", "ALUNO", null);
        Usuario terceiro = salvarUsuario("terceiro.privacidade@teste.example", "ALUNO", null);

        cadastrar(autor, "{\"tipo\":\"CORRECAO\",\"descricao\":\"  Atualizar meu e-mail  \","
                + "\"usuarioId\":" + terceiro.getId() + "}")
                .andExpect(status().isBadRequest());
        assertEquals(0, solicitacaoRepository.count());

        cadastrar(autor, "{\"tipo\":\"CORRECAO\",\"descricao\":\"  Atualizar meu e-mail  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Atualizar meu e-mail"));
        SolicitacaoPrivacidade salva = solicitacaoRepository.findAll().get(0);
        assertEquals(autor.getId(), salva.getUsuario().getId());
        assertNotNull(salva.getCriadoEm());
    }

    @Test
    void deveListarSomenteSolicitacoesDoUsuarioAutenticado() throws Exception {
        Usuario autor = salvarUsuario("autor.lista@teste.example", "ALUNO", null);
        Usuario terceiro = salvarUsuario("terceiro.lista@teste.example", "EMPRESA", null);
        cadastrar(autor, "{\"tipo\":\"ACESSO\",\"descricao\":\"Solicitação própria\"}")
                .andExpect(status().isOk());
        cadastrar(terceiro, "{\"tipo\":\"CORRECAO\",\"descricao\":\"Solicitação de terceiro\"}")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/privacidade/solicitacoes")
                        .header("Authorization", bearer(autor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(content().string(containsString("Solicitação própria")))
                .andExpect(content().string(not(containsString("Solicitação de terceiro"))));
    }

    @Test
    void deveRejeitarDadosObrigatoriosInvalidosENaoAutenticados() throws Exception {
        Usuario usuario = salvarUsuario("validacao.privacidade@teste.example", "ALUNO", null);
        cadastrar(usuario, "{\"tipo\":null,\"descricao\":\"\"}")
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/privacidade/solicitacoes"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/privacidade/solicitacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"ACESSO\",\"descricao\":\"Sem autenticação\"}"))
                .andExpect(status().isUnauthorized());
        assertEquals(0, solicitacaoRepository.count());
    }

    @Test
    void deveExigirSenhaAtualValidaParaEncerramento() throws Exception {
        Usuario usuario = salvarUsuario("identidade.privacidade@teste.example", "ALUNO", null);
        usuario.setSenha(passwordEncoder.encode("Senha-correta-123!"));
        usuarioRepository.saveAndFlush(usuario);

        cadastrar(usuario, """
                {"tipo":"ENCERRAMENTO_CONTA","descricao":"Encerrar minha conta",
                 "senhaAtual":"Senha-incorreta-123!"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Confirmação de identidade inválida"));
        cadastrar(usuario, """
                {"tipo":"ENCERRAMENTO_CONTA","descricao":"Encerrar minha conta"}
                """)
                .andExpect(status().isBadRequest());
        cadastrar(usuario, """
                {"tipo":"ENCERRAMENTO_CONTA","descricao":"Encerrar minha conta",
                 "senhaAtual":"Senha-correta-123!"}
                """)
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions cadastrar(
            Usuario usuario, String json) throws Exception {
        return mockMvc.perform(post("/api/privacidade/solicitacoes")
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Usuario salvarUsuario(String email, String perfil, Instituicao instituicao) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de Privacidade");
        usuario.setEmail(email);
        usuario.setSenha("hash");
        usuario.setPerfil(perfil);
        usuario.setInstituicao(instituicao);
        usuario.setTermosAceitos(true);
        return usuarioRepository.save(usuario);
    }

    private Instituicao salvarInstituicao() {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome("Instituição de Privacidade");
        instituicao.setCnpj("11222333000181");
        return instituicaoRepository.save(instituicao);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }
}
