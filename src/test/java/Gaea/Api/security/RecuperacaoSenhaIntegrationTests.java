package Gaea.Api.security;

import Gaea.Api.model.SolicitacaoRecuperacaoSenha;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.SolicitacaoRecuperacaoSenhaRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.EnvioEmailAplicacao;
import Gaea.Api.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.password-reset.intervalo-minimo-segundos=0",
        "app.password-reset.limite-identificador=2",
        "app.password-reset.limite-origem=20"
})
@AutoConfigureMockMvc
@Transactional
class RecuperacaoSenhaIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired SolicitacaoRecuperacaoSenhaRepository solicitacaoRepository;
    @Autowired LogAuditoriaRepository auditoriaRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @MockitoBean
    EnvioEmailAplicacao envioEmail;

    @BeforeEach
    void habilitarEmail() {
        when(envioEmail.disponivel()).thenReturn(true);
    }

    @Test
    void deveRedefinirSenhaComTokenProtegidoDeUsoUnicoERevogarJwtAnterior() throws Exception {
        Usuario usuario = salvarUsuario("aluno@example.com", "senha-antiga");
        String jwtAnterior = jwtService.gerarToken(usuario);

        solicitar("aluno@example.com").andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensagem").exists());

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(envioEmail).enviarRecuperacaoSenha(
                eq("aluno@example.com"), eq("Aluno"), link.capture());
        String token = parametroFragmento(link.getValue(), "redefinirSenha");
        SolicitacaoRecuperacaoSenha solicitacao = solicitacaoRepository.findAll().get(0);
        assertEquals(64, solicitacao.getTokenHash().length());
        assertFalse(token.equals(solicitacao.getTokenHash()));

        mockMvc.perform(post("/api/auth/senha/redefinicao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(redefinicao(token, "Nova frase segura 2026!")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aluno@example.com\",\"senha\":\"senha-antiga\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aluno@example.com\",\"senha\":\"Nova frase segura 2026!\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/resumo")
                        .header("Authorization", "Bearer " + jwtAnterior))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/senha/redefinicao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(redefinicao(token, "Outra frase segura 2026!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Este token de recuperação já foi utilizado"));
    }

    @Test
    void deveRejeitarTokenExpiradoEInvalido() throws Exception {
        salvarUsuario("aluno@example.com", "senha-antiga");
        solicitar("aluno@example.com").andExpect(status().isAccepted());
        SolicitacaoRecuperacaoSenha solicitacao = solicitacaoRepository.findAll().get(0);
        solicitacao.setExpiraEm(LocalDateTime.now().minusMinutes(1));
        solicitacaoRepository.save(solicitacao);

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(envioEmail).enviarRecuperacaoSenha(anyString(), anyString(), link.capture());
        String token = parametroFragmento(link.getValue(), "redefinirSenha");

        mockMvc.perform(post("/api/auth/senha/redefinicao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(redefinicao(token, "Nova frase segura 2026!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Este token de recuperação expirou"));

        mockMvc.perform(post("/api/auth/senha/redefinicao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(redefinicao("token-invalido-com-tamanho-suficiente-123456789", "Nova frase segura 2026!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Token de recuperação inválido"));
    }

    @Test
    void deveResponderIgualParaEmailInexistenteSemEnviarEmail() throws Exception {
        solicitar("inexistente@example.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensagem").value(
                        "Se o e-mail estiver cadastrado e apto, você receberá as instruções de recuperação"));

        SolicitacaoRecuperacaoSenha solicitacao = solicitacaoRepository.findAll().get(0);
        assertNull(solicitacao.getUsuario());
        assertNull(solicitacao.getTokenHash());
        verify(envioEmail, never()).enviarRecuperacaoSenha(anyString(), anyString(), anyString());
    }

    @Test
    void deveLimitarSolicitacoesSemDiferenciarExistenciaDaConta() throws Exception {
        solicitar("inexistente@example.com").andExpect(status().isAccepted());
        solicitar("inexistente@example.com").andExpect(status().isAccepted());
        solicitar("inexistente@example.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensagem").value(
                        "Muitas solicitações de recuperação. Aguarde antes de tentar novamente"));
    }

    @Test
    void deveResponderIgualQuandoEmailEstiverDesabilitado() throws Exception {
        when(envioEmail.disponivel()).thenReturn(false);
        solicitar("qualquer@example.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensagem").exists());
        assertEquals(1, solicitacaoRepository.count());
    }

    @Test
    void deveResponder202QuandoSmtpFalharParaContaExistente() throws Exception {
        salvarUsuario("aluno@example.com", "senha-antiga");
        doThrow(new IllegalStateException("falha SMTP simulada"))
                .when(envioEmail).enviarRecuperacaoSenha(
                        anyString(), anyString(), anyString());

        solicitar("aluno@example.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void deveManterCompatibilidadeDeLoginComSenhaAntigaForaDaNovaPolitica() throws Exception {
        salvarUsuario("legado@example.com", "curta-antiga");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"legado@example.com\",\"senha\":\"curta-antiga\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    private org.springframework.test.web.servlet.ResultActions solicitar(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/senha/recuperacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private Usuario salvarUsuario(String email, String senha) {
        Usuario usuario = new Usuario();
        usuario.setNome("Aluno");
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil("ALUNO");
        usuario.setAtivo(true);
        usuario.setAtivado(true);
        usuario.setTermosAceitos(true);
        return usuarioRepository.save(usuario);
    }

    private String redefinicao(String token, String senha) {
        return "{\"token\":\"" + token + "\",\"senha\":\"" + senha
                + "\",\"confirmacaoSenha\":\"" + senha + "\"}";
    }

    private String parametroFragmento(String url, String nome) {
        String fragmento = URI.create(url).getFragment();
        for (String parametro : fragmento.split("&")) {
            String[] partes = parametro.split("=", 2);
            if (partes.length == 2 && nome.equals(partes[0])) {
                return partes[1];
            }
        }
        throw new AssertionError("Parâmetro ausente no fragmento: " + nome);
    }
}
