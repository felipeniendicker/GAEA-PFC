package Gaea.Api.security;

import Gaea.Api.model.DocumentoProcesso;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoDocumento;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.ConviteAtivacaoAlunoRepository;
import Gaea.Api.repository.DocumentoProcessoRepository;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.HistoricoVinculoSupervisorRepository;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.PendenciaProcessoRepository;
import Gaea.Api.repository.PrazoProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.TokenVerificacaoEmailRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import Gaea.Api.service.JwtService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.documentos.storage.diretorio=${java.io.tmpdir}/gaea-documentos-testes",
        "app.documentos.tamanho-maximo-bytes=2048"
})
@AutoConfigureMockMvc
class DocumentoProcessoIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired DocumentoProcessoRepository documentoRepository;
    @Autowired PendenciaProcessoRepository pendenciaRepository;
    @Autowired PrazoProcessoRepository prazoRepository;
    @Autowired HistoricoVinculoSupervisorRepository historicoVinculoRepository;
    @Autowired VinculoSupervisorProcessoRepository vinculoRepository;
    @Autowired HistoricoStatusProcessoRepository historicoStatusRepository;
    @Autowired ProcessoEstagioRepository processoRepository;
    @Autowired TokenVerificacaoEmailRepository tokenRepository;
    @Autowired ConviteAtivacaoAlunoRepository conviteRepository;
    @Autowired LogAuditoriaRepository auditoriaRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired InstituicaoRepository instituicaoRepository;
    @Autowired JwtService jwtService;

    @BeforeEach
    @AfterEach
    void limparBanco() {
        documentoRepository.deleteAll();
        pendenciaRepository.deleteAll();
        prazoRepository.deleteAll();
        historicoVinculoRepository.deleteAll();
        vinculoRepository.deleteAll();
        historicoStatusRepository.deleteAll();
        processoRepository.deleteAll();
        tokenRepository.deleteAll();
        conviteRepository.deleteAll();
        auditoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
        instituicaoRepository.deleteAll();
    }

    @Test
    void alunoDeveEnviarListarEBaixarPdfDoProprioProcesso() throws Exception {
        Contexto contexto = contexto();

        mockMvc.perform(multipart("/api/processos/{id}/documentos", contexto.processo().getId())
                        .file(pdf("termo.pdf"))
                        .header("Authorization", bearer(contexto.aluno())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeOriginal").value("termo.pdf"))
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.documentoAnteriorId").doesNotExist());

        DocumentoProcesso salvo = documentoRepository.findAll().get(0);
        assertEquals(contexto.processo().getId(), salvo.getProcesso().getId());
        assertEquals(contexto.aluno().getId(), salvo.getEnviadoPor().getId());
        assertEquals(64, salvo.getHashSha256().length());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "ENVIO_DOCUMENTO".equals(log.getAcao())
                        && contexto.instituicao().getId().equals(log.getInstituicao().getId())));

        mockMvc.perform(get("/api/processos/{id}/documentos", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.aluno())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(salvo.getId()));

        mockMvc.perform(get("/api/processos/{id}/documentos/{documentoId}/conteudo",
                        contexto.processo().getId(), salvo.getId())
                        .header("Authorization", bearer(contexto.aluno())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(result -> assertTrue(
                        new String(result.getResponse().getContentAsByteArray(), 0, 5,
                                StandardCharsets.US_ASCII).equals("%PDF-")));
    }

    @Test
    void deveRejeitarArquivoVazioExtensaoInvalidaConteudoInvalidoEExcessoDeTamanho() throws Exception {
        Contexto contexto = contexto();

        enviar(contexto, new MockMultipartFile("arquivo", "vazio.pdf", "application/pdf", new byte[0]))
                .andExpect(status().isBadRequest());
        enviar(contexto, new MockMultipartFile("arquivo", "arquivo.txt", "application/pdf", pdfBytes()))
                .andExpect(status().isBadRequest());
        enviar(contexto, new MockMultipartFile("arquivo", "falso.pdf", "application/pdf",
                        "não é pdf".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest());
        enviar(contexto, new MockMultipartFile("arquivo", "grande.pdf", "application/pdf", new byte[2049]))
                .andExpect(status().isBadRequest());

        assertEquals(0, documentoRepository.count());
    }

    @Test
    void deveImpedirUploadPorInstituicaoEmpresaEOutroAluno() throws Exception {
        Contexto contexto = contexto();
        Usuario empresa = usuario("Supervisor", "supervisor@example.com", "EMPRESA", null);
        Usuario outroAluno = usuario("Outro aluno", "outro@example.com", "ALUNO", contexto.instituicao());
        outroAluno.setMatricula("RGM-2");
        usuarioRepository.save(outroAluno);

        enviar(contexto, pdf("instituicao.pdf"), contexto.responsavel())
                .andExpect(status().isForbidden());
        enviar(contexto, pdf("empresa.pdf"), empresa)
                .andExpect(status().isForbidden());
        enviar(contexto, pdf("outro.pdf"), outroAluno)
                .andExpect(status().isNotFound());
        assertEquals(0, documentoRepository.count());
    }

    @Test
    void deveIsolarListagemEDownloadEntreAlunosEInstituicoes() throws Exception {
        Contexto contexto = contexto();
        enviar(contexto, pdf("protegido.pdf")).andExpect(status().isOk());
        Long documentoId = documentoRepository.findAll().get(0).getId();
        Instituicao outraInstituicao = instituicao("Outra Faculdade", "22333444000160");
        Usuario outroAluno = usuario("Outro aluno", "outro@example.com", "ALUNO", outraInstituicao);
        outroAluno.setMatricula("RGM-9");
        usuarioRepository.save(outroAluno);
        Usuario outroResponsavel = usuario(
                "Outro responsável", "outra.instituicao@example.com", "INSTITUICAO", outraInstituicao);

        for (Usuario usuario : new Usuario[]{outroAluno, outroResponsavel}) {
            mockMvc.perform(get("/api/processos/{id}/documentos", contexto.processo().getId())
                            .header("Authorization", bearer(usuario)))
                    .andExpect(status().isNotFound());
            mockMvc.perform(get("/api/processos/{id}/documentos/{documentoId}/conteudo",
                            contexto.processo().getId(), documentoId)
                            .header("Authorization", bearer(usuario)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void somenteSupervisorAtivoEConfirmadoDeveConsultarDocumentos() throws Exception {
        Contexto contexto = contexto();
        enviar(contexto, pdf("supervisao.pdf")).andExpect(status().isOk());
        Usuario confirmado = usuario("Confirmado", "confirmado@example.com", "EMPRESA", null);
        Usuario pendente = usuario("Pendente", "pendente@example.com", "EMPRESA", null);
        vinculo(contexto.processo(), confirmado, SituacaoVinculoSupervisor.CONFIRMADO);

        mockMvc.perform(get("/api/processos/{id}/documentos", contexto.processo().getId())
                        .header("Authorization", bearer(confirmado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/processos/{id}/documentos", contexto.processo().getId())
                        .header("Authorization", bearer(pendente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void instituicaoDeveAprovarDocumentoDoProprioProcesso() throws Exception {
        Contexto contexto = contexto();
        enviar(contexto, pdf("plano.pdf")).andExpect(status().isOk());
        DocumentoProcesso documento = documentoRepository.findAll().get(0);

        mockMvc.perform(put("/api/processos/{id}/documentos/{documentoId}/analise",
                        contexto.processo().getId(), documento.getId())
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"APROVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("APROVADO"))
                .andExpect(jsonPath("$.analisadoEm").exists())
                .andExpect(jsonPath("$.analisadoPor").value(contexto.responsavel().getNome()));

        DocumentoProcesso atualizado = documentoRepository.findById(documento.getId()).orElseThrow();
        assertEquals(SituacaoDocumento.APROVADO, atualizado.getSituacao());
        assertNotNull(atualizado.getAnalisadoEm());
        assertEquals("PENDENTE", processoRepository.findById(contexto.processo().getId())
                .orElseThrow().getStatus());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "APROVACAO_DOCUMENTO".equals(log.getAcao())));

        mockMvc.perform(get("/api/processos/{id}/historico-completo", contexto.processo().getId())
                        .header("Authorization", bearer(contexto.aluno())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("APROVACAO_DOCUMENTO"))
                .andExpect(jsonPath("$[1].tipo").value("ENVIO_DOCUMENTO"));
    }

    @Test
    void deveExigirJustificativaNaReprovacaoEBloquearNovaAnalise() throws Exception {
        Contexto contexto = contexto();
        enviar(contexto, pdf("relatorio.pdf")).andExpect(status().isOk());
        Long documentoId = documentoRepository.findAll().get(0).getId();

        analisar(contexto, documentoId, "{\"resultado\":\"REPROVADO\",\"justificativa\":\"   \"}")
                .andExpect(status().isBadRequest());
        analisar(contexto, documentoId,
                "{\"resultado\":\"REPROVADO\",\"justificativa\":\"Falta assinatura do supervisor\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("REPROVADO"))
                .andExpect(jsonPath("$.justificativaReprovacao")
                        .value("Falta assinatura do supervisor"));
        analisar(contexto, documentoId, "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void alunoDeveReenviarDocumentoReprovadoPreservandoAnterior() throws Exception {
        Contexto contexto = contexto();
        enviar(contexto, pdf("termo-v1.pdf")).andExpect(status().isOk());
        Long anteriorId = documentoRepository.findAll().get(0).getId();
        analisar(contexto, anteriorId,
                "{\"resultado\":\"REPROVADO\",\"justificativa\":\"Documento incompleto\"}")
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/api/processos/{id}/documentos", contexto.processo().getId())
                        .file(pdf("termo-v2.pdf"))
                        .param("documentoAnteriorId", anteriorId.toString())
                        .header("Authorization", bearer(contexto.aluno())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.documentoAnteriorId").value(anteriorId));

        assertEquals(2, documentoRepository.count());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "REENVIO_DOCUMENTO".equals(log.getAcao())));

        mockMvc.perform(multipart("/api/processos/{id}/documentos", contexto.processo().getId())
                        .file(pdf("termo-v3.pdf"))
                        .param("documentoAnteriorId", anteriorId.toString())
                        .header("Authorization", bearer(contexto.aluno())))
                .andExpect(status().isConflict());
    }

    @Test
    void deveImpedirAnalisePorAlunoEmpresaEInstituicaoDeOutroProcesso() throws Exception {
        Contexto contexto = contexto();
        enviar(contexto, pdf("protegido.pdf")).andExpect(status().isOk());
        Long documentoId = documentoRepository.findAll().get(0).getId();
        Usuario empresa = usuario("Supervisor", "supervisor@example.com", "EMPRESA", null);
        Instituicao outra = instituicao("Outra Faculdade", "33444555000140");
        Usuario outraInstituicao = usuario("Outro responsável", "outra@example.com", "INSTITUICAO", outra);

        for (Usuario usuario : new Usuario[]{contexto.aluno(), empresa}) {
            mockMvc.perform(put("/api/processos/{id}/documentos/{documentoId}/analise",
                            contexto.processo().getId(), documentoId)
                            .header("Authorization", bearer(usuario))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"resultado\":\"APROVADO\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put("/api/processos/{id}/documentos/{documentoId}/analise",
                        contexto.processo().getId(), documentoId)
                        .header("Authorization", bearer(outraInstituicao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"APROVADO\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void endpointsDevemExigirAutenticacao() throws Exception {
        Contexto contexto = contexto();
        mockMvc.perform(get("/api/processos/{id}/documentos", contexto.processo().getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void idDoDocumentoNaoDevePermitirAcessoPorOutroProcesso() throws Exception {
        Contexto contexto = contexto();
        enviar(contexto, pdf("isolado.pdf")).andExpect(status().isOk());
        Long documentoId = documentoRepository.findAll().get(0).getId();
        ProcessoEstagio outroProcesso = processo(contexto.aluno(), contexto.instituicao());

        mockMvc.perform(get("/api/processos/{id}/documentos/{documentoId}/conteudo",
                        outroProcesso.getId(), documentoId)
                        .header("Authorization", bearer(contexto.aluno())))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/processos/{id}/documentos/{documentoId}/analise",
                        outroProcesso.getId(), documentoId)
                        .header("Authorization", bearer(contexto.responsavel()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"APROVADO\"}"))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions enviar(
            Contexto contexto, MockMultipartFile arquivo) throws Exception {
        return enviar(contexto, arquivo, contexto.aluno());
    }

    private org.springframework.test.web.servlet.ResultActions enviar(
            Contexto contexto, MockMultipartFile arquivo, Usuario usuario) throws Exception {
        return mockMvc.perform(multipart("/api/processos/{id}/documentos", contexto.processo().getId())
                .file(arquivo)
                .header("Authorization", bearer(usuario)));
    }

    private org.springframework.test.web.servlet.ResultActions analisar(
            Contexto contexto, Long documentoId, String json) throws Exception {
        return mockMvc.perform(put("/api/processos/{id}/documentos/{documentoId}/analise",
                        contexto.processo().getId(), documentoId)
                .header("Authorization", bearer(contexto.responsavel()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Contexto contexto() {
        Instituicao instituicao = instituicao("Faculdade A", "11222333000181");
        Usuario aluno = usuario("Ana Aluna", "aluno@example.com", "ALUNO", instituicao);
        aluno.setMatricula("RGM-1");
        usuarioRepository.save(aluno);
        Usuario responsavel = usuario(
                "Responsável", "instituicao@example.com", "INSTITUICAO", instituicao);
        return new Contexto(instituicao, aluno, responsavel, processo(aluno, instituicao));
    }

    private Instituicao instituicao(String nome, String cnpj) {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome(nome);
        instituicao.setCnpj(cnpj);
        return instituicaoRepository.save(instituicao);
    }

    private Usuario usuario(String nome, String email, String perfil, Instituicao instituicao) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setInstituicao(instituicao);
        usuario.setAtivo(true);
        usuario.setAtivado(true);
        usuario.setTermosAceitos(true);
        return usuarioRepository.save(usuario);
    }

    private ProcessoEstagio processo(Usuario aluno, Instituicao instituicao) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno(aluno.getNome());
        processo.setEmailAluno(aluno.getEmail());
        processo.setNomeEmpresa("Empresa Exemplo");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus("PENDENTE");
        processo.setAluno(aluno);
        processo.setInstituicao(instituicao);
        return processoRepository.save(processo);
    }

    private void vinculo(
            ProcessoEstagio processo,
            Usuario supervisor,
            SituacaoVinculoSupervisor situacao) {
        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado(supervisor.getEmail());
        vinculo.setSupervisor(supervisor);
        vinculo.setSituacao(situacao);
        vinculoRepository.save(vinculo);
    }

    private MockMultipartFile pdf(String nome) throws Exception {
        return new MockMultipartFile("arquivo", nome, "application/pdf", pdfBytes());
    }

    private byte[] pdfBytes() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    private record Contexto(
            Instituicao instituicao,
            Usuario aluno,
            Usuario responsavel,
            ProcessoEstagio processo) {
    }
}
