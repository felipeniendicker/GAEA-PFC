package Gaea.Api.security;

import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.SolicitacaoPrivacidadeRepository;
import Gaea.Api.repository.TokenVerificacaoEmailRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EncerramentoContaRollbackIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SolicitacaoPrivacidadeRepository solicitacoes;
    @Autowired LogAuditoriaRepository auditoria;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @MockitoBean TokenVerificacaoEmailRepository tokensVerificacao;

    @AfterEach
    void limparDadosCriados() {
        solicitacoes.deleteAll();
        auditoria.deleteAll();
        usuarios.deleteAll();
    }

    @Test
    void deveReverterOperacaoEManterAuditoriaDaFalha() throws Exception {
        Usuario titular = usuario("rollback-titular@example.com", "ALUNO");
        Usuario admin = usuario("rollback-admin@example.com", "ADMIN_GAEA");

        mockMvc.perform(post("/api/privacidade/solicitacoes")
                        .header("Authorization", bearer(titular))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"ENCERRAMENTO_CONTA",
                                 "descricao":"Solicito encerrar minha conta",
                                 "senhaAtual":"Senha-segura-123!"}
                                """))
                .andExpect(status().isOk());
        Long id = solicitacoes.findAll().get(0).getId();
        mockMvc.perform(put("/api/admin/privacidade/solicitacoes/{id}/analise", id)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        when(tokensVerificacao
                .findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(anyLong()))
                .thenThrow(new IllegalStateException("falha transacional simulada"));

        mockMvc.perform(post("/api/admin/privacidade/solicitacoes/{id}/concluir", id)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\":\"Encerramento aprovado pela administração.\"}"))
                .andExpect(status().isInternalServerError());

        assertTrue(usuarios.findById(titular.getId()).orElseThrow().getAtivo());
        assertTrue(solicitacoes.findById(id).orElseThrow().getSituacao()
                == SituacaoSolicitacaoPrivacidade.EM_ANALISE);
        assertFalse(auditoria.findAll().stream()
                .anyMatch(log -> "EXECUCAO_PRIVACIDADE".equals(log.getAcao())));
        assertTrue(auditoria.findAll().stream()
                .anyMatch(log -> "FALHA_EXECUCAO_PRIVACIDADE".equals(log.getAcao())));
    }

    private Usuario usuario(String email, String perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário rollback");
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode("Senha-segura-123!"));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuario.setAtivado(true);
        usuario.setTermosAceitos(true);
        return usuarios.saveAndFlush(usuario);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }
}
