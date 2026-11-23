package Gaea.Api.security;

import Gaea.Api.model.Usuario;
import Gaea.Api.model.Instituicao;
import Gaea.Api.repository.*;
import Gaea.Api.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdministracaoInstituicoesIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired InstituicaoRepository instituicoes;
    @Autowired SolicitacaoInstituicaoRepository solicitacoes;
    @Autowired JwtService jwt;
    @MockitoBean EnvioEmailAplicacao email;

    @Test
    void deveVerificarAprovarAtivarEManterAprovacaoIdempotente() throws Exception {
        when(email.disponivel()).thenReturn(true);
        solicitar("Faculdade Horizonte", "11.222.333/0001-81", "Ana Responsável", "ana@horizonte.example")
                .andExpect(status().isCreated());
        ArgumentCaptor<String> verificacao=ArgumentCaptor.forClass(String.class);
        verify(email).enviarVerificacaoInstituicao(eq("ana@horizonte.example"), anyString(), verificacao.capture());
        String tokenVerificacao=fragmento(verificacao.getValue(), "verificarInstituicao");
        mockMvc.perform(post("/api/instituicoes/solicitacoes/verificar-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+tokenVerificacao+"\"}")) .andExpect(status().isOk());

        Usuario admin=admin(); Long id=solicitacoes.findAll().get(0).getId(); String bearer=bearer(admin);
        mockMvc.perform(put("/api/admin/instituicoes/solicitacoes/{id}/analise",id).header("Authorization",bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ANALISE"));
        mockMvc.perform(post("/api/admin/instituicoes/solicitacoes/{id}/aprovar",id).header("Authorization",bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sigla\":\"FHL\",\"ficticia\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
        mockMvc.perform(post("/api/admin/instituicoes/solicitacoes/{id}/aprovar",id).header("Authorization",bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sigla\":\"FHL\",\"ficticia\":true}"))
                .andExpect(status().isOk());
        assertEquals(1, instituicoes.findAll().stream().filter(i -> "11222333000181".equals(i.getCnpj())).count());
        Instituicao instituicao=instituicoes.findByCnpj("11222333000181").orElseThrow();
        assertEquals("FHL",instituicao.getSigla());
        assertTrue(instituicao.getFicticia());
        assertEquals(1, usuarios.findAll().stream().filter(u -> "INSTITUICAO".equals(u.getPerfil())).count());
        Usuario responsavel=usuarios.findByEmailNormalizado("ana@horizonte.example").orElseThrow();
        assertFalse(responsavel.getAtivado()); assertNull(responsavel.getSenha());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@horizonte.example\",\"senha\":\"Senha-Forte1!\"}"))
                .andExpect(status().isUnauthorized());
        ArgumentCaptor<String> ativacao=ArgumentCaptor.forClass(String.class);
        verify(email).enviarAtivacaoInstituicao(eq("ana@horizonte.example"), anyString(), ativacao.capture());
        String tokenAtivacao=fragmento(ativacao.getValue(), "ativarInstituicao");
        mockMvc.perform(post("/api/instituicoes/ativacoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+tokenAtivacao+"\",\"senha\":\"Senha-Forte1!\",\"confirmacaoSenha\":\"Senha-Forte1!\",\"termosAceitos\":true}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@horizonte.example\",\"senha\":\"Senha-Forte1!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").value("INSTITUICAO"))
                .andExpect(jsonPath("$.instituicao.sigla").value("FHL"))
                .andExpect(jsonPath("$.instituicao.ficticia").doesNotExist());
    }

    @Test
    void deveProtegerAdministracaoBloquearElevacaoERejeitarComJustificativa() throws Exception {
        when(email.disponivel()).thenReturn(true);
        mockMvc.perform(get("/api/admin/instituicoes/dashboard")).andExpect(status().isUnauthorized());
        Usuario empresa=usuario("Empresa", "empresa@example.com", "EMPRESA");
        mockMvc.perform(get("/api/admin/instituicoes/dashboard").header("Authorization",bearer(empresa)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Intruso","email":"intruso@example.com","senha":"Senha-Forte1!","perfil":"ADMIN_GAEA","termosAceitos":true}
                """)).andExpect(status().isBadRequest());

        solicitar("Instituto Vale", "04.252.011/0001-10", "Bruno Gestor", "bruno@vale.example").andExpect(status().isCreated());
        ArgumentCaptor<String> link=ArgumentCaptor.forClass(String.class); verify(email).enviarVerificacaoInstituicao(anyString(),anyString(),link.capture());
        mockMvc.perform(post("/api/instituicoes/solicitacoes/verificar-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+fragmento(link.getValue(),"verificarInstituicao")+"\"}")) .andExpect(status().isOk());
        Usuario admin=admin(); Long id=solicitacoes.findAll().get(0).getId(); String bearer=bearer(admin);
        mockMvc.perform(put("/api/admin/instituicoes/solicitacoes/{id}/analise",id).header("Authorization",bearer)).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/instituicoes/solicitacoes/{id}/rejeitar",id).header("Authorization",bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Documentação institucional insuficiente\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADA"));
        mockMvc.perform(get("/api/processos").header("Authorization",bearer)).andExpect(status().isForbidden());
    }

    @Test
    void deveExigirSiglaEFicticiaNaAprovacao() throws Exception {
        when(email.disponivel()).thenReturn(true);
        solicitar("Instituto Metadados", "04.252.011/0001-10", "Carlos Gestor", "carlos@metadados.example")
                .andExpect(status().isCreated());
        ArgumentCaptor<String> link=ArgumentCaptor.forClass(String.class);
        verify(email).enviarVerificacaoInstituicao(anyString(),anyString(),link.capture());
        mockMvc.perform(post("/api/instituicoes/solicitacoes/verificar-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+fragmento(link.getValue(),"verificarInstituicao")+"\"}"))
                .andExpect(status().isOk());

        Usuario admin=admin();
        Long id=solicitacoes.findAll().get(0).getId();
        String bearer=bearer(admin);
        mockMvc.perform(put("/api/admin/instituicoes/solicitacoes/{id}/analise",id).header("Authorization",bearer))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/instituicoes/solicitacoes/{id}/aprovar",id).header("Authorization",bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"sigla\":\"A\",\"ficticia\":false}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/instituicoes/solicitacoes/{id}/aprovar",id).header("Authorization",bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"sigla\":\"METADADOS\",\"ficticia\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveBloquearDuplicidadesEAbuso() throws Exception {
        when(email.disponivel()).thenReturn(true);
        solicitar("Faculdade Horizonte", "11.222.333/0001-81", "Ana", "ana@horizonte.example").andExpect(status().isCreated());
        solicitar("Outra", "11.222.333/0001-81", "Outra", "outra@example.com").andExpect(status().isConflict());
        solicitar("Outra", "04.252.011/0001-10", "Outra", "ana@horizonte.example").andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.ResultActions solicitar(String nome,String cnpj,String responsavel,String emailInstitucional) throws Exception {
        return mockMvc.perform(post("/api/instituicoes/solicitacoes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nomeInstitucional\":\""+nome+"\",\"cnpj\":\""+cnpj+"\",\"responsavelNome\":\""+responsavel+"\",\"emailInstitucional\":\""+emailInstitucional+"\",\"termosAceitos\":true}"));
    }
    private Usuario admin(){ return usuario("Administrador", "admin@gaea.example", "ADMIN_GAEA"); }
    private Usuario usuario(String nome,String email,String perfil){ Usuario u=new Usuario();u.setNome(nome);u.setEmail(email);u.setSenha("hash");u.setPerfil(perfil);u.setAtivo(true);u.setAtivado(true);u.setTermosAceitos(true);u.setEmailVerificadoEm(LocalDateTime.now());return usuarios.save(u); }
    private String bearer(Usuario u){ return "Bearer "+jwt.gerarToken(u); }
    private String fragmento(String url,String nome){ for(String p: URI.create(url).getFragment().split("&")){String[] v=p.split("=",2);if(v[0].equals(nome))return v[1];}throw new AssertionError(); }
}
