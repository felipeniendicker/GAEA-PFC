package Gaea.Api.security;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RelatorioEstagioIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired InstituicaoRepository instituicaoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ProcessoEstagioRepository processoRepository;
    @Autowired JwtService jwtService;

    @Test
    void instituicaoDeveConsultarSomenteSeusProcessos() throws Exception {
        Contexto contexto = prepararContexto();

        mockMvc.perform(get("/api/relatorios/estagios")
                        .header("Authorization", bearer(contexto.responsavelPrimeira())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.processos[0].aluno").value("Ana InstituiÃ§Ã£o Um"))
                .andExpect(jsonPath("$.processos[1].aluno").value("Carla InstituiÃ§Ã£o Um"))
                .andExpect(content().string(not(containsString("Bruno InstituiÃ§Ã£o Dois"))));
    }

    @Test
    void consultaECsvDevemAplicarOsMesmosFiltros() throws Exception {
        Contexto contexto = prepararContexto();
        String consulta = "?situacao=APROVADO&dataInicio=2026-03-01"
                + "&dataFim=2026-03-31&aluno=ana";

        mockMvc.perform(get("/api/relatorios/estagios" + consulta)
                        .header("Authorization", bearer(contexto.responsavelPrimeira())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.processos[0].aluno").value("Ana InstituiÃ§Ã£o Um"));

        mockMvc.perform(get("/api/relatorios/estagios/csv" + consulta)
                        .header("Authorization", bearer(contexto.responsavelPrimeira())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(new MediaType("text", "csv")))
                .andExpect(content().string(containsString("Ana InstituiÃ§Ã£o Um")))
                .andExpect(content().string(not(containsString("Carla InstituiÃ§Ã£o Um"))))
                .andExpect(content().string(not(containsString("Bruno InstituiÃ§Ã£o Dois"))));
    }

    @Test
    void deveBloquearAlunoEmpresaEUsuarioNaoAutenticadoNosDoisEndpoints() throws Exception {
        Contexto contexto = prepararContexto();
        String[] endpoints = {
                "/api/relatorios/estagios",
                "/api/relatorios/estagios/csv"
        };

        for (String endpoint : endpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get(endpoint)
                            .header("Authorization", bearer(contexto.aluno())))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(endpoint)
                            .header("Authorization", bearer(contexto.empresa())))
                    .andExpect(status().isForbidden());
        }
    }

    private Contexto prepararContexto() {
        Instituicao primeira = salvarInstituicao("InstituiÃ§Ã£o Um", "11111111000191");
        Instituicao segunda = salvarInstituicao("InstituiÃ§Ã£o Dois", "22222222000191");
        Usuario responsavelPrimeira = salvarUsuario(
                "responsavel.um@teste.example", "INSTITUICAO", primeira);
        Usuario aluno = salvarUsuario("aluno@teste.example", "ALUNO", primeira);
        Usuario empresa = salvarUsuario("empresa@teste.example", "EMPRESA", null);

        salvarProcesso(primeira, "Ana InstituiÃ§Ã£o Um", "APROVADO",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30));
        salvarProcesso(primeira, "Carla InstituiÃ§Ã£o Um", "PENDENTE",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15));
        salvarProcesso(segunda, "Bruno InstituiÃ§Ã£o Dois", "APROVADO",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30));
        return new Contexto(responsavelPrimeira, aluno, empresa);
    }

    private Instituicao salvarInstituicao(String nome, String cnpj) {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome(nome);
        instituicao.setCnpj(cnpj);
        return instituicaoRepository.save(instituicao);
    }

    private Usuario salvarUsuario(String email, String perfil, Instituicao instituicao) {
        Usuario usuario = new Usuario();
        usuario.setNome("UsuÃ¡rio de Teste");
        usuario.setEmail(email);
        usuario.setSenha("hash-nao-utilizado-no-teste");
        usuario.setPerfil(perfil);
        usuario.setInstituicao(instituicao);
        usuario.setTermosAceitos(true);
        return usuarioRepository.save(usuario);
    }

    private void salvarProcesso(
            Instituicao instituicao,
            String aluno,
            String situacao,
            LocalDate dataInicio,
            LocalDate dataFim) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno(aluno);
        processo.setEmailAluno(aluno.toLowerCase().replace(' ', '.') + "@teste.example");
        processo.setNomeEmpresa("Empresa de Teste");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus(situacao);
        processo.setInstituicao(instituicao);
        processo.setDataInicio(dataInicio);
        processo.setDataFim(dataFim);
        processoRepository.save(processo);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    private record Contexto(
            Usuario responsavelPrimeira,
            Usuario aluno,
            Usuario empresa) {
    }
}
