package Gaea.Api.security;

import Gaea.Api.model.Usuario;
import Gaea.Api.model.ViaCepResponse;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import Gaea.Api.service.ViaCepService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ViaCepSecurityIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired JwtService jwtService;

    @MockitoBean
    ViaCepService viaCepService;

    @ParameterizedTest
    @ValueSource(strings = {"ALUNO", "INSTITUICAO", "EMPRESA"})
    void devePermitirConsultaAosTresPerfisOficiais(String perfil) throws Exception {
        Usuario usuario = salvarUsuario(perfil);
        ViaCepResponse resposta = new ViaCepResponse();
        resposta.setCep("01001-000");
        resposta.setLocalidade("São Paulo");
        resposta.setUf("SP");
        when(viaCepService.buscarCep("01001000")).thenReturn(resposta);

        mockMvc.perform(get("/api/cep/01001000")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("01001-000"))
                .andExpect(jsonPath("$.localidade").value("São Paulo"));
    }

    @Test
    void deveNegarConsultaSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/cep/01001000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveNegarConsultaParaPerfilNaoAutorizado() throws Exception {
        Usuario usuario = salvarUsuario("OUTRO");

        mockMvc.perform(get("/api/cep/01001000")
                        .header("Authorization", "Bearer " + jwtService.gerarToken(usuario)))
                .andExpect(status().isForbidden());
    }

    private Usuario salvarUsuario(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário " + perfil);
        usuario.setEmail(perfil.toLowerCase() + "-cep@example.com");
        usuario.setSenha("hash-nao-utilizado");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuario.setAtivado(true);
        usuario.setTermosAceitos(true);
        return usuarioRepository.saveAndFlush(usuario);
    }
}
