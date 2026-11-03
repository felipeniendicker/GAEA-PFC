package Gaea.Api.security;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import Gaea.Api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitacaoEstagioIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoEstagioRepository processoRepository;

    @Autowired
    private VinculoSupervisorProcessoRepository vinculoRepository;

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void deveRetornarDadosInstitucionaisDoAlunoNoLogin() throws Exception {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario aluno = usuario("Ana Aluna", "ana@example.com", "ALUNO", instituicao, "000123");
        aluno.setSenha(passwordEncoder.encode("senha-segura"));
        usuarioRepository.save(aluno);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","senha":"senha-segura"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Aluna"))
                .andExpect(jsonPath("$.matricula").value("000123"))
                .andExpect(jsonPath("$.instituicao.nome").value("Faculdade A"))
                .andExpect(jsonPath("$.instituicao.id").doesNotExist());
    }

    @Test
    void deveCadastrarSolicitacaoSomenteComIdentidadeDoAlunoAutenticado() throws Exception {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario aluno = usuario("Ana Aluna", "ana@example.com", "ALUNO", instituicao, "000123");

        mockMvc.perform(post("/api/processos")
                        .header("Authorization", bearer(aluno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeAluno").value("Ana Aluna"))
                .andExpect(jsonPath("$.emailAluno").value("ana@example.com"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.instituicao.nome").value("Faculdade A"))
                .andExpect(jsonPath("$.vinculoSupervisor.email").value("supervisor@example.com"))
                .andExpect(jsonPath("$.vinculoSupervisor.situacao").value("PENDENTE"));

        ProcessoEstagio processo = processoRepository.findAll().get(0);
        assertEquals(aluno.getId(), processo.getAluno().getId());
        assertEquals(instituicao.getId(), processo.getInstituicao().getId());
        assertEquals("Ana Aluna", processo.getNomeAluno());
        assertEquals("ana@example.com", processo.getEmailAluno());
        assertEquals("01001000", processo.getCepEmpresa());

        var vinculo = vinculoRepository.findByProcessoId(processo.getId()).orElseThrow();
        assertEquals(SituacaoVinculoSupervisor.PENDENTE, vinculo.getSituacao());
        assertNull(vinculo.getSupervisor());
        assertTrue(logAuditoriaRepository.findAll().stream().anyMatch(log ->
                "SOLICITACAO_ESTAGIO".equals(log.getAcao())
                        && log.getInstituicao().getId().equals(instituicao.getId())));
    }

    @Test
    void deveBloquearCadastroPorInstituicaoEEmpresa() throws Exception {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario responsavel = usuario(
                "Responsável", "instituicao@example.com", "INSTITUICAO", instituicao, null);
        Usuario empresa = usuario(
                "Supervisor", "empresa@example.com", "EMPRESA", null, null);

        solicitar(responsavel, requisicaoValida()).andExpect(status().isForbidden());
        solicitar(empresa, requisicaoValida()).andExpect(status().isForbidden());
        assertEquals(0, processoRepository.count());
    }

    @Test
    void deveRejeitarAlunoSemInstituicaoOuMatricula() throws Exception {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario semInstituicao = usuario(
                "Sem Instituição", "sem.instituicao@example.com", "ALUNO", null, "RGM-1");
        Usuario semMatricula = usuario(
                "Sem Matrícula", "sem.matricula@example.com", "ALUNO", instituicao, null);

        solicitar(semInstituicao, requisicaoValida()).andExpect(status().isConflict());
        solicitar(semMatricula, requisicaoValida()).andExpect(status().isConflict());
        assertEquals(0, processoRepository.count());
    }

    @Test
    void deveBloquearAlunoDesativadoOuNaoAtivado() throws Exception {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario desativado = usuario(
                "Desativado", "desativado@example.com", "ALUNO", instituicao, "RGM-1");
        desativado.setAtivo(false);
        usuarioRepository.save(desativado);
        Usuario naoAtivado = usuario(
                "Não ativado", "nao.ativado@example.com", "ALUNO", instituicao, "RGM-2");
        naoAtivado.setAtivado(false);
        usuarioRepository.save(naoAtivado);

        solicitar(desativado, requisicaoValida()).andExpect(status().isUnauthorized());
        solicitar(naoAtivado, requisicaoValida()).andExpect(status().isUnauthorized());
        assertEquals(0, processoRepository.count());
    }

    @Test
    void deveRejeitarIdsDeAlunoOuInstituicaoEnviadosPeloCliente() throws Exception {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario aluno = usuario("Aluno", "aluno@example.com", "ALUNO", instituicao, "RGM-1");
        String tentativa = requisicaoValida().replace(
                "\"nomeEmpresa\"", "\"alunoId\":999,\"instituicaoId\":999,\"nomeEmpresa\"");

        solicitar(aluno, tentativa).andExpect(status().isBadRequest());
        assertEquals(0, processoRepository.count());
    }

    @Test
    void deveRejeitarPeriodoIncoerente() throws Exception {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario aluno = usuario("Aluno", "aluno@example.com", "ALUNO", instituicao, "RGM-1");
        String datasInvalidas = requisicaoValida()
                .replace("2026-11-10", "2027-06-01")
                .replace("2027-05-10", "2027-05-01");

        solicitar(aluno, datasInvalidas)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos"));
    }

    @Test
    void deveIsolarSolicitacaoEntreInstituicoes() throws Exception {
        Instituicao primeira = instituicao("Faculdade A", "11222333000181");
        Instituicao segunda = instituicao("Faculdade B", "22333444000160");
        Usuario aluno = usuario("Aluno", "aluno@example.com", "ALUNO", primeira, "RGM-1");
        Usuario responsavelPrimeira = usuario(
                "Responsável A", "inst.a@example.com", "INSTITUICAO", primeira, null);
        Usuario responsavelSegunda = usuario(
                "Responsável B", "inst.b@example.com", "INSTITUICAO", segunda, null);

        solicitar(aluno, requisicaoValida()).andExpect(status().isOk());

        mockMvc.perform(get("/api/processos").header("Authorization", bearer(responsavelPrimeira)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/processos").header("Authorization", bearer(responsavelSegunda)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveBloquearAnaliseEnquantoSupervisorNaoConfirmar() throws Exception {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario aluno = usuario("Aluno", "aluno@example.com", "ALUNO", instituicao, "RGM-1");
        Usuario responsavel = usuario(
                "Responsável", "instituicao@example.com", "INSTITUICAO", instituicao, null);
        solicitar(aluno, requisicaoValida()).andExpect(status().isOk());
        Long processoId = processoRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/processos/{id}/status", processoId)
                        .header("Authorization", bearer(responsavel))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("EM_ANALISE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "A análise só pode ser iniciada após a confirmação do supervisor"));
        assertEquals("PENDENTE", processoRepository.findById(processoId).orElseThrow().getStatus());
    }

    @Test
    void devePreservarProcessoLegadoAoCriarNovaSolicitacao() throws Exception {
        ProcessoEstagio legado = new ProcessoEstagio();
        legado.setNomeAluno("Aluno Legado");
        legado.setEmailAluno("legado@example.com");
        legado.setNomeEmpresa("Empresa Legada");
        legado.setCnpjEmpresa("11222333000181");
        legado.setStatus("PENDENTE");
        legado = processoRepository.save(legado);

        Instituicao instituicao = instituicao("Faculdade A", "22333444000160");
        Usuario aluno = usuario("Aluno", "aluno@example.com", "ALUNO", instituicao, "RGM-1");
        solicitar(aluno, requisicaoValida()).andExpect(status().isOk());

        ProcessoEstagio preservado = processoRepository.findById(legado.getId()).orElseThrow();
        assertNull(preservado.getAluno());
        assertNull(preservado.getInstituicao());
        assertNull(preservado.getCepEmpresa());
        assertEquals(2, processoRepository.count());
    }

    private org.springframework.test.web.servlet.ResultActions solicitar(
            Usuario usuario,
            String conteudo) throws Exception {
        return mockMvc.perform(post("/api/processos")
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(conteudo));
    }

    private Instituicao instituicao(String nome, String cnpj) {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome(nome);
        instituicao.setCnpj(cnpj);
        return instituicaoRepository.save(instituicao);
    }

    private Usuario usuario(
            String nome,
            String email,
            String perfil,
            Instituicao instituicao,
            String matricula) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setInstituicao(instituicao);
        usuario.setMatricula(matricula);
        usuario.setAtivo(true);
        usuario.setAtivado(true);
        usuario.setTermosAceitos(true);
        return usuarioRepository.save(usuario);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    private String requisicaoValida() {
        return """
                {
                  "nomeEmpresa":"Empresa Exemplo",
                  "cnpjEmpresa":"11.222.333/0001-81",
                  "cepEmpresa":"01001-000",
                  "enderecoEmpresa":"Praça da Sé, 100 - São Paulo/SP",
                  "dataInicio":"2026-11-10",
                  "dataFim":"2027-05-10",
                  "emailSupervisor":" SUPERVISOR@EXAMPLE.COM "
                }
                """;
    }
}
