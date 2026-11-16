package Gaea.Api.security;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.RegistroCargaHoraria;
import Gaea.Api.model.SituacaoCargaHoraria;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.RegistroCargaHorariaRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import Gaea.Api.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistroCargaHorariaIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired InstituicaoRepository instituicaoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ProcessoEstagioRepository processoRepository;
    @Autowired VinculoSupervisorProcessoRepository vinculoRepository;
    @Autowired RegistroCargaHorariaRepository registroRepository;
    @Autowired LogAuditoriaRepository auditoriaRepository;
    @Autowired JwtService jwtService;

    @Test
    void alunoDeveCadastrarRegistroPendenteNormalizadoEAuditado() throws Exception {
        Contexto contexto = contexto("cadastro");

        cadastrar(contexto.processo(), contexto.aluno(),
                json("2026-11-16", 90, "  Elaborar   relatórios  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.duracaoMinutos").value(90))
                .andExpect(jsonPath("$.atividade").value("Elaborar relatórios"));

        RegistroCargaHoraria salvo = registroRepository.findAll().get(0);
        assertEquals(contexto.aluno().getId(), salvo.getRegistradoPor().getId());
        assertEquals(64, salvo.getAtividadeHash().length());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "REGISTRO_CARGA_HORARIA".equals(log.getAcao())
                        && contexto.instituicao().getId().equals(log.getInstituicao().getId())));
    }

    @Test
    void deveBloquearCadastroPorOutroAlunoEOutrosPerfis() throws Exception {
        Contexto contexto = contexto("perfis");
        Usuario outroAluno = aluno("Outro aluno", "outro.carga@test", contexto.instituicao(), "RGM-2");

        cadastrar(contexto.processo(), outroAluno, jsonValido()).andExpect(status().isNotFound());
        cadastrar(contexto.processo(), contexto.responsavel(), jsonValido()).andExpect(status().isForbidden());
        cadastrar(contexto.processo(), contexto.supervisor(), jsonValido()).andExpect(status().isForbidden());
        assertEquals(0, registroRepository.count());
    }

    @Test
    void deveRejeitarCamposInvalidos() throws Exception {
        Contexto contexto = contexto("invalidos");

        cadastrar(contexto.processo(), contexto.aluno(),
                "{\"dataAtividade\":null,\"duracaoMinutos\":60,\"atividade\":\"Atividade\"}")
                .andExpect(status().isBadRequest());
        cadastrar(contexto.processo(), contexto.aluno(),
                json("2026-11-16", 0, "Atividade"))
                .andExpect(status().isBadRequest());
        cadastrar(contexto.processo(), contexto.aluno(),
                json("2026-11-16", 60, "   "))
                .andExpect(status().isBadRequest());
        assertEquals(0, registroRepository.count());
    }

    @Test
    void deveDetectarDuplicidadeAposNormalizacaoDeterministica() throws Exception {
        Contexto contexto = contexto("duplicidade");

        cadastrar(contexto.processo(), contexto.aluno(),
                json("2026-11-16", 60, "Revisar   documentação"))
                .andExpect(status().isOk());
        cadastrar(contexto.processo(), contexto.aluno(),
                json("2026-11-16", 60, "  REVISAR documentação  "))
                .andExpect(status().isConflict());
        assertEquals(1, registroRepository.count());
    }

    @Test
    void tresPerfisAutorizadosDevemConsultarRegistrosEResumo() throws Exception {
        Contexto contexto = contexto("consulta");
        cadastrar(contexto.processo(), contexto.aluno(), jsonValido()).andExpect(status().isOk());

        for (Usuario usuario : new Usuario[]{
                contexto.aluno(), contexto.responsavel(), contexto.supervisor()}) {
            listar(contexto.processo(), usuario)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].processoId").value(contexto.processo().getId()));
            resumir(contexto.processo(), usuario)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRegistradoMinutos").value(60))
                    .andExpect(jsonPath("$.totalPendenteMinutos").value(60));
        }
    }

    @Test
    void deveIsolarAlunoInstituicaoESupervisorSemVinculo() throws Exception {
        Contexto contexto = contexto("isolamento");
        cadastrar(contexto.processo(), contexto.aluno(), jsonValido()).andExpect(status().isOk());
        Instituicao outra = instituicao("Outra instituição", "22333444000160");
        Usuario alunoExterno = aluno("Aluno externo", "aluno.externo.carga@test", outra, "RGM-1");
        Usuario responsavelExterno = usuario(
                "Responsável externo", "inst.externa.carga@test", "INSTITUICAO", outra);
        Usuario supervisorExterno = usuario(
                "Supervisor externo", "supervisor.externo.carga@test", "EMPRESA", null);

        listar(contexto.processo(), alunoExterno).andExpect(status().isNotFound());
        listar(contexto.processo(), responsavelExterno).andExpect(status().isNotFound());
        listar(contexto.processo(), supervisorExterno).andExpect(status().isNotFound());
    }

    @Test
    void vinculosNaoConfirmadosOuInativosNaoDevemDarAcesso() throws Exception {
        for (SituacaoVinculoSupervisor situacao : new SituacaoVinculoSupervisor[]{
                SituacaoVinculoSupervisor.PENDENTE,
                SituacaoVinculoSupervisor.RECUSADO,
                SituacaoVinculoSupervisor.CANCELADO}) {
            String sufixo = situacao.name().toLowerCase();
            Contexto contexto = contextoSemVinculo(sufixo);
            Usuario supervisor = usuario(
                    "Supervisor " + situacao, sufixo + ".carga@test", "EMPRESA", null);
            vinculo(contexto.processo(), supervisor, situacao);
            salvarRegistro(contexto, 30, SituacaoCargaHoraria.PENDENTE);

            listar(contexto.processo(), supervisor).andExpect(status().isNotFound());
        }
    }

    @Test
    void supervisorConfirmadoDeveAprovarERejeitarComAuditoria() throws Exception {
        Contexto contexto = contexto("avaliacao");
        Long aprovadoId = idDoCadastro(contexto, json("2026-11-15", 80, "Atividade A"));
        Long rejeitadoId = idDoCadastro(contexto, json("2026-11-16", 40, "Atividade B"));

        avaliar(contexto.processo(), aprovadoId, contexto.supervisor(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("APROVADO"))
                .andExpect(jsonPath("$.avaliadoEm").exists());
        avaliar(contexto.processo(), rejeitadoId, contexto.supervisor(),
                "{\"resultado\":\"REJEITADO\",\"justificativa\":\"  Detalhar a entrega  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("REJEITADO"))
                .andExpect(jsonPath("$.justificativaRejeicao").value("Detalhar a entrega"));

        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "APROVACAO_CARGA_HORARIA".equals(log.getAcao())));
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "REJEICAO_CARGA_HORARIA".equals(log.getAcao())));
    }

    @Test
    void deveBloquearRejeicaoSemJustificativaEAvaliacaoDuplicada() throws Exception {
        Contexto contexto = contexto("conflito");
        Long id = idDoCadastro(contexto, jsonValido());

        avaliar(contexto.processo(), id, contexto.supervisor(),
                "{\"resultado\":\"REJEITADO\",\"justificativa\":\"   \"}")
                .andExpect(status().isBadRequest());
        avaliar(contexto.processo(), id, contexto.supervisor(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isOk());
        avaliar(contexto.processo(), id, contexto.supervisor(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void deveBloquearAvaliacaoPorAlunoInstituicaoESupervisorNaoVinculado() throws Exception {
        Contexto contexto = contexto("bloqueio-avaliacao");
        Long id = idDoCadastro(contexto, jsonValido());
        Usuario externo = usuario("Supervisor externo", "nao.vinculado.carga@test", "EMPRESA", null);

        avaliar(contexto.processo(), id, contexto.aluno(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isForbidden());
        avaliar(contexto.processo(), id, contexto.responsavel(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isForbidden());
        avaliar(contexto.processo(), id, externo, "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void resumoDeveManterInvarianteEntreTodosOsEstados() throws Exception {
        Contexto contexto = contexto("totais");
        Long aprovado = idDoCadastro(contexto, json("2026-11-14", 90, "Aprovada"));
        Long rejeitado = idDoCadastro(contexto, json("2026-11-15", 30, "Rejeitada"));
        idDoCadastro(contexto, json("2026-11-16", 60, "Pendente"));
        avaliar(contexto.processo(), aprovado, contexto.supervisor(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isOk());
        avaliar(contexto.processo(), rejeitado, contexto.supervisor(),
                "{\"resultado\":\"REJEITADO\",\"justificativa\":\"Não comprovada\"}")
                .andExpect(status().isOk());

        resumir(contexto.processo(), contexto.aluno())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRegistradoMinutos").value(180))
                .andExpect(jsonPath("$.totalAprovadoMinutos").value(90))
                .andExpect(jsonPath("$.totalPendenteMinutos").value(60))
                .andExpect(jsonPath("$.totalRejeitadoMinutos").value(30));
    }

    @Test
    void endpointsDevemExigirAutenticacao() throws Exception {
        Contexto contexto = contexto("sem-auth");
        mockMvc.perform(post(url(contexto.processo()))
                        .contentType(MediaType.APPLICATION_JSON).content(jsonValido()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(url(contexto.processo()))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(url(contexto.processo()) + "/resumo")).andExpect(status().isUnauthorized());
        mockMvc.perform(put(url(contexto.processo()) + "/1/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"APROVADO\"}"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions cadastrar(
            ProcessoEstagio processo, Usuario usuario, String json) throws Exception {
        return mockMvc.perform(post(url(processo)).header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private org.springframework.test.web.servlet.ResultActions listar(
            ProcessoEstagio processo, Usuario usuario) throws Exception {
        return mockMvc.perform(get(url(processo)).header("Authorization", bearer(usuario)));
    }

    private org.springframework.test.web.servlet.ResultActions resumir(
            ProcessoEstagio processo, Usuario usuario) throws Exception {
        return mockMvc.perform(get(url(processo) + "/resumo")
                .header("Authorization", bearer(usuario)));
    }

    private org.springframework.test.web.servlet.ResultActions avaliar(
            ProcessoEstagio processo, Long registroId, Usuario usuario, String json) throws Exception {
        return mockMvc.perform(put(url(processo) + "/" + registroId + "/avaliacao")
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private Long idDoCadastro(Contexto contexto, String json) throws Exception {
        cadastrar(contexto.processo(), contexto.aluno(), json).andExpect(status().isOk());
        return registroRepository.findAll().stream()
                .filter(r -> r.getProcesso().getId().equals(contexto.processo().getId()))
                .map(RegistroCargaHoraria::getId).max(Long::compareTo).orElseThrow();
    }

    private String url(ProcessoEstagio processo) {
        return "/api/processos/" + processo.getId() + "/cargas-horarias";
    }

    private String jsonValido() {
        return json("2026-11-16", 60, "Registrar atividade desenvolvida");
    }

    private String json(String data, int minutos, String atividade) {
        return "{\"dataAtividade\":\"" + data + "\",\"duracaoMinutos\":" + minutos
                + ",\"atividade\":\"" + atividade + "\"}";
    }

    private Contexto contexto(String sufixo) {
        Contexto base = contextoSemVinculo(sufixo);
        Usuario supervisor = usuario(
                "Supervisor confirmado", "supervisor." + sufixo + "@carga.test", "EMPRESA", null);
        vinculo(base.processo(), supervisor, SituacaoVinculoSupervisor.CONFIRMADO);
        return new Contexto(base.instituicao(), base.aluno(), base.responsavel(), supervisor, base.processo());
    }

    private Contexto contextoSemVinculo(String sufixo) {
        String numero = String.format("%06d", Math.abs(sufixo.hashCode()) % 1_000_000);
        Instituicao instituicao = instituicao("Faculdade " + sufixo, "11222333" + numero);
        Usuario aluno = aluno("Ana Aluna", "aluno." + sufixo + "@carga.test", instituicao, "RGM-" + sufixo);
        Usuario responsavel = usuario(
                "Responsável", "instituicao." + sufixo + "@carga.test", "INSTITUICAO", instituicao);
        return new Contexto(instituicao, aluno, responsavel, null, processo(aluno, instituicao, sufixo));
    }

    private Instituicao instituicao(String nome, String cnpj) {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome(nome);
        instituicao.setCnpj(cnpj);
        return instituicaoRepository.save(instituicao);
    }

    private Usuario aluno(String nome, String email, Instituicao instituicao, String matricula) {
        Usuario aluno = usuario(nome, email, "ALUNO", instituicao);
        aluno.setMatricula(matricula);
        return usuarioRepository.save(aluno);
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

    private ProcessoEstagio processo(Usuario aluno, Instituicao instituicao, String sufixo) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno(aluno.getNome());
        processo.setEmailAluno(aluno.getEmail());
        processo.setNomeEmpresa("Empresa Exemplo");
        processo.setCnpjEmpresa("55" + String.format("%012d", Math.abs(sufixo.hashCode())));
        processo.setStatus("PENDENTE");
        processo.setAluno(aluno);
        processo.setInstituicao(instituicao);
        return processoRepository.save(processo);
    }

    private void vinculo(ProcessoEstagio processo, Usuario supervisor,
                         SituacaoVinculoSupervisor situacao) {
        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado(supervisor.getEmail());
        vinculo.setSupervisor(supervisor);
        vinculo.setSituacao(situacao);
        vinculoRepository.save(vinculo);
    }

    private void salvarRegistro(Contexto contexto, int minutos, SituacaoCargaHoraria situacao) {
        RegistroCargaHoraria registro = new RegistroCargaHoraria();
        registro.setProcesso(contexto.processo());
        registro.setRegistradoPor(contexto.aluno());
        registro.setDataAtividade(LocalDate.of(2026, 11, 16));
        registro.setDuracaoMinutos(minutos);
        registro.setAtividade("Atividade de teste");
        registro.setAtividadeHash("a".repeat(64));
        registro.setSituacao(situacao);
        registroRepository.save(registro);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    private record Contexto(Instituicao instituicao, Usuario aluno, Usuario responsavel,
                            Usuario supervisor, ProcessoEstagio processo) {
    }
}
