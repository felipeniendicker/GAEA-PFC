package Gaea.Api.security;

import Gaea.Api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gaea-admin-bootstrap-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.admin-bootstrap.enabled=true",
        "app.admin-bootstrap.nome=Administrador de Homologacao",
        "app.admin-bootstrap.email=admin.homologacao@example.com",
        "app.admin-bootstrap.senha=Senha-Fixture-Admin-2026!",
        "app.admin-bootstrap.confirmacao=GAEA_CREATE_INITIAL_ADMIN"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminBootstrapIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarios;

    @Test
    void deveCriarEAutenticarPrimeiroAdministradorSemExporCredencial() throws Exception {
        assertThat(usuarios.findAll().stream()
                .filter(usuario -> "ADMIN_GAEA".equals(usuario.getPerfil())))
                .hasSize(1);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin.homologacao@example.com",
                                  "senha": "Senha-Fixture-Admin-2026!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.perfil").value("ADMIN_GAEA"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
