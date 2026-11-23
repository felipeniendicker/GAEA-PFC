package Gaea.Api.security;

import Gaea.Api.model.DocumentosLegais;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentosLegaisIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void deveDisponibilizarVersoesVigentesSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/auth/documentos-legais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versaoTermosUso")
                        .value(DocumentosLegais.VERSAO_TERMOS_USO))
                .andExpect(jsonPath("$.termosUsoPublicadoEm").value("2026-09-27"))
                .andExpect(jsonPath("$.versaoPoliticaPrivacidade")
                        .value(DocumentosLegais.VERSAO_POLITICA_PRIVACIDADE))
                .andExpect(jsonPath("$.politicaPrivacidadePublicadaEm")
                        .value("2026-09-27"));
    }
}
