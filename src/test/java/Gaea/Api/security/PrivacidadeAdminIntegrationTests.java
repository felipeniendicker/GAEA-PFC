package Gaea.Api.security;

import Gaea.Api.model.Usuario;
import Gaea.Api.repository.SolicitacaoPrivacidadeRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrivacidadeAdminIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired SolicitacaoPrivacidadeRepository solicitacoes;
    @Autowired JwtService jwtService;

    @Test
    void deveAtenderSolicitacaoComAtribuicaoDecisaoEvidenciaEAuditoria() throws Exception {
        Usuario titular = usuario("Titular", "titular@example.com", "ALUNO");
        Usuario admin = usuario("Admin", "admin@example.com", "ADMIN_GAEA");

        mockMvc.perform(post("/api/privacidade/solicitacoes")
                        .header("Authorization", bearer(titular))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"EXPORTACAO","descricao":"Solicito cópia dos meus dados pessoais."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PENDENTE"));

        mockMvc.perform(post("/api/privacidade/solicitacoes")
                        .header("Authorization", bearer(titular))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"EXPORTACAO","descricao":"Solicitação duplicada em aberto."}
                                """))
                .andExpect(status().isConflict());

        Long id = solicitacoes.findAll().get(0).getId();
        mockMvc.perform(put("/api/admin/privacidade/solicitacoes/{id}/analise", id)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("EM_ANALISE"))
                .andExpect(jsonPath("$.responsavel").value("admin@example.com"));

        mockMvc.perform(post("/api/admin/privacidade/solicitacoes/{id}/concluir", id)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decisao":"Exportação disponibilizada ao titular.",
                                 "evidenciaExecucao":"Arquivo gerado e entregue pelo canal autenticado."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CONCLUIDA"));

        mockMvc.perform(get("/api/privacidade/solicitacoes")
                        .header("Authorization", bearer(titular)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].decisao")
                        .value("Exportação disponibilizada ao titular."));
        assertTrue(usuarios.existsByEmailNormalizado("titular@example.com"));
    }

    @Test
    void deveRestringirAdministracaoEImpedirConclusaoSemEvidencia() throws Exception {
        Usuario titular = usuario("Titular", "titular2@example.com", "EMPRESA");
        Usuario admin = usuario("Admin", "admin2@example.com", "ADMIN_GAEA");
        mockMvc.perform(post("/api/privacidade/solicitacoes")
                        .header("Authorization", bearer(titular))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"ENCERRAMENTO_CONTA","descricao":"Solicito encerrar o meu acesso."}
                                """))
                .andExpect(status().isOk());
        Long id = solicitacoes.findAll().get(0).getId();

        mockMvc.perform(get("/api/admin/privacidade/solicitacoes")
                        .header("Authorization", bearer(titular)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/privacidade/solicitacoes"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/admin/privacidade/solicitacoes/{id}/analise", id)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/privacidade/solicitacoes/{id}/concluir", id)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decisao":"Encerramento analisado e aprovado.","evidenciaExecucao":""}
                                """))
                .andExpect(status().isConflict());
    }

    private Usuario usuario(String nome, String email, String perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
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
