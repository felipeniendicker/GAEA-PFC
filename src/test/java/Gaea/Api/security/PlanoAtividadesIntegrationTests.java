package Gaea.Api.security;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.PlanoAtividades;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoPlanoAtividades;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.PlanoAtividadesRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
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

import java.math.BigDecimal;

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
class PlanoAtividadesIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired InstituicaoRepository instituicaoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ProcessoEstagioRepository processoRepository;
    @Autowired VinculoSupervisorProcessoRepository vinculoRepository;
    @Autowired PlanoAtividadesRepository planoRepository;
    @Autowired LogAuditoriaRepository auditoriaRepository;
    @Autowired JwtService jwtService;

    @Test
    void alunoDeveCadastrarPlanoValidoDoProprioProcesso() throws Exception {
        Contexto contexto = contexto();

        cadastrar(contexto.processo(), contexto.aluno(), planoJson())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.cargaHorariaPlanejada").value(120))
                .andExpect(jsonPath("$.atividades.length()").value(2))
                .andExpect(jsonPath("$.atividades[0].descricao").value("Desenvolver relatórios"));

        PlanoAtividades salvo = planoRepository.findByProcessoId(contexto.processo().getId())
                .orElseThrow();
        assertEquals(contexto.aluno().getId(), salvo.getCriadoPor().getId());
        assertEquals(2, salvo.getAtividades().size());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "CADASTRO_PLANO_ATIVIDADES".equals(log.getAcao())
                        && contexto.instituicao().getId().equals(log.getInstituicao().getId())));
    }

    @Test
    void deveBloquearCadastroEmProcessoAlheioEPorOutrosPerfis() throws Exception {
        Contexto contexto = contexto();
        Usuario outroAluno = aluno("Outro aluno", "outro.aluno@plano.test", contexto.instituicao(), "RGM-2");

        cadastrar(contexto.processo(), outroAluno, planoJson()).andExpect(status().isNotFound());
        cadastrar(contexto.processo(), contexto.responsavel(), planoJson()).andExpect(status().isForbidden());
        cadastrar(contexto.processo(), contexto.supervisor(), planoJson()).andExpect(status().isForbidden());
        assertEquals(0, planoRepository.count());
    }

    @Test
    void deveRejeitarCamposObrigatoriosECargaHorariaInvalida() throws Exception {
        Contexto contexto = contexto();

        cadastrar(contexto.processo(), contexto.aluno(),
                "{\"atividades\":[],\"cargaHorariaPlanejada\":120}")
                .andExpect(status().isBadRequest());
        cadastrar(contexto.processo(), contexto.aluno(),
                "{\"atividades\":[{\"descricao\":\"   \"}],\"cargaHorariaPlanejada\":120}")
                .andExpect(status().isBadRequest());
        cadastrar(contexto.processo(), contexto.aluno(),
                "{\"atividades\":[{\"descricao\":\"Atividade\"}],\"cargaHorariaPlanejada\":0}")
                .andExpect(status().isBadRequest());
        assertEquals(0, planoRepository.count());
    }

    @Test
    void deveImpedirSegundoPlanoNoMesmoProcesso() throws Exception {
        Contexto contexto = contexto();
        cadastrar(contexto.processo(), contexto.aluno(), planoJson()).andExpect(status().isOk());
        cadastrar(contexto.processo(), contexto.aluno(), planoJson()).andExpect(status().isConflict());
        assertEquals(1, planoRepository.count());
    }

    @Test
    void tresPerfisAutorizadosDevemConsultarPlano() throws Exception {
        Contexto contexto = contexto();
        cadastrar(contexto.processo(), contexto.aluno(), planoJson()).andExpect(status().isOk());

        for (Usuario usuario : new Usuario[]{
                contexto.aluno(), contexto.responsavel(), contexto.supervisor()}) {
            consultar(contexto.processo(), usuario)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.processoId").value(contexto.processo().getId()))
                    .andExpect(jsonPath("$.situacao").value("PENDENTE"));
        }
    }

    @Test
    void deveIsolarAlunoEInstituicaoDeOutroProcesso() throws Exception {
        Contexto contexto = contexto();
        cadastrar(contexto.processo(), contexto.aluno(), planoJson()).andExpect(status().isOk());
        Instituicao outraInstituicao = instituicao("Outra Faculdade", "22333444000160");
        Usuario outroAluno = aluno("Aluno externo", "externo@plano.test", outraInstituicao, "RGM-1");
        Usuario outroResponsavel = usuario(
                "Responsável externo", "responsavel.externo@plano.test", "INSTITUICAO", outraInstituicao);

        consultar(contexto.processo(), outroAluno).andExpect(status().isNotFound());
        consultar(contexto.processo(), outroResponsavel).andExpect(status().isNotFound());
    }

    @Test
    void somenteSupervisorAtivoEConfirmadoDeveConsultar() throws Exception {
        for (SituacaoVinculoSupervisor situacao : new SituacaoVinculoSupervisor[]{
                SituacaoVinculoSupervisor.PENDENTE,
                SituacaoVinculoSupervisor.RECUSADO,
                SituacaoVinculoSupervisor.CANCELADO}) {
            Contexto contexto = contextoSemVinculo(situacao.name().toLowerCase());
            Usuario supervisor = usuario(
                    "Supervisor " + situacao,
                    situacao.name().toLowerCase() + "@plano.test", "EMPRESA", null);
            vinculo(contexto.processo(), supervisor, situacao);
            salvarPlano(contexto.processo(), contexto.aluno());

            consultar(contexto.processo(), supervisor).andExpect(status().isNotFound());
        }
    }

    @Test
    void supervisorConfirmadoDeveAprovarPlanoERegistrarAuditoria() throws Exception {
        Contexto contexto = contexto();
        cadastrar(contexto.processo(), contexto.aluno(), planoJson()).andExpect(status().isOk());

        avaliar(contexto, contexto.supervisor(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("APROVADO"))
                .andExpect(jsonPath("$.avaliadoPor").value(contexto.supervisor().getNome()))
                .andExpect(jsonPath("$.avaliadoEm").exists())
                .andExpect(jsonPath("$.justificativaRejeicao").doesNotExist());

        PlanoAtividades plano = planoRepository.findByProcessoId(contexto.processo().getId()).orElseThrow();
        assertNotNull(plano.getAvaliadoEm());
        assertEquals(SituacaoPlanoAtividades.APROVADO, plano.getSituacao());
        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "APROVACAO_PLANO_ATIVIDADES".equals(log.getAcao())));
    }

    @Test
    void supervisorConfirmadoDeveRejeitarComJustificativa() throws Exception {
        Contexto contexto = contexto();
        cadastrar(contexto.processo(), contexto.aluno(), planoJson()).andExpect(status().isOk());

        avaliar(contexto, contexto.supervisor(),
                "{\"resultado\":\"REJEITADO\",\"justificativa\":\"  Ajustar atividades  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("REJEITADO"))
                .andExpect(jsonPath("$.justificativaRejeicao").value("Ajustar atividades"));

        assertTrue(auditoriaRepository.findAll().stream().anyMatch(log ->
                "REJEICAO_PLANO_ATIVIDADES".equals(log.getAcao())));
    }

    @Test
    void deveBloquearRejeicaoSemJustificativaEAvaliacaoDuplicada() throws Exception {
        Contexto contexto = contexto();
        cadastrar(contexto.processo(), contexto.aluno(), planoJson()).andExpect(status().isOk());

        avaliar(contexto, contexto.supervisor(),
                "{\"resultado\":\"REJEITADO\",\"justificativa\":\"   \"}")
                .andExpect(status().isBadRequest());
        avaliar(contexto, contexto.supervisor(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isOk());
        avaliar(contexto, contexto.supervisor(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void alunoInstituicaoESupervisorNaoVinculadoNaoDevemAvaliar() throws Exception {
        Contexto contexto = contexto();
        cadastrar(contexto.processo(), contexto.aluno(), planoJson()).andExpect(status().isOk());
        Usuario naoVinculado = usuario(
                "Supervisor externo", "supervisor.externo@plano.test", "EMPRESA", null);

        avaliar(contexto, contexto.aluno(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isForbidden());
        avaliar(contexto, contexto.responsavel(), "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isForbidden());
        avaliar(contexto, naoVinculado, "{\"resultado\":\"APROVADO\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void endpointsDevemExigirAutenticacao() throws Exception {
        Contexto contexto = contexto();

        mockMvc.perform(post(url(contexto.processo()))
                        .contentType(MediaType.APPLICATION_JSON).content(planoJson()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(url(contexto.processo())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(url(contexto.processo()) + "/avaliacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"APROVADO\"}"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions cadastrar(
            ProcessoEstagio processo, Usuario usuario, String json) throws Exception {
        return mockMvc.perform(post(url(processo))
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private org.springframework.test.web.servlet.ResultActions consultar(
            ProcessoEstagio processo, Usuario usuario) throws Exception {
        return mockMvc.perform(get(url(processo)).header("Authorization", bearer(usuario)));
    }

    private org.springframework.test.web.servlet.ResultActions avaliar(
            Contexto contexto, Usuario usuario, String json) throws Exception {
        return mockMvc.perform(put(url(contexto.processo()) + "/avaliacao")
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String url(ProcessoEstagio processo) {
        return "/api/processos/" + processo.getId() + "/plano-atividades";
    }

    private String planoJson() {
        return """
                {"atividades":[
                    {"descricao":"  Desenvolver relatórios  "},
                    {"descricao":"Participar de reuniões"}
                ],"cargaHorariaPlanejada":120}
                """;
    }

    private Contexto contexto() {
        Contexto contexto = contextoSemVinculo("principal");
        Usuario supervisor = usuario(
                "Supervisor confirmado", "supervisor.confirmado@plano.test", "EMPRESA", null);
        vinculo(contexto.processo(), supervisor, SituacaoVinculoSupervisor.CONFIRMADO);
        return new Contexto(
                contexto.instituicao(), contexto.aluno(), contexto.responsavel(), supervisor,
                contexto.processo());
    }

    private Contexto contextoSemVinculo(String sufixo) {
        Instituicao instituicao = instituicao(
                "Faculdade " + sufixo,
                "11222333" + String.format("%06d", Math.abs(sufixo.hashCode()) % 1_000_000));
        Usuario aluno = aluno(
                "Ana Aluna", "aluno." + sufixo + "@plano.test", instituicao, "RGM-" + sufixo);
        Usuario responsavel = usuario(
                "Responsável", "instituicao." + sufixo + "@plano.test", "INSTITUICAO", instituicao);
        return new Contexto(instituicao, aluno, responsavel, null, processo(aluno, instituicao));
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
            ProcessoEstagio processo, Usuario supervisor, SituacaoVinculoSupervisor situacao) {
        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado(supervisor.getEmail());
        vinculo.setSupervisor(supervisor);
        vinculo.setSituacao(situacao);
        vinculoRepository.save(vinculo);
    }

    private void salvarPlano(ProcessoEstagio processo, Usuario aluno) {
        PlanoAtividades plano = new PlanoAtividades();
        plano.setProcesso(processo);
        plano.setCriadoPor(aluno);
        plano.setCargaHorariaPlanejada(new BigDecimal("80"));
        plano.setSituacao(SituacaoPlanoAtividades.PENDENTE);
        plano.adicionarAtividade("Atividade prevista", 1);
        planoRepository.save(plano);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    private record Contexto(
            Instituicao instituicao,
            Usuario aluno,
            Usuario responsavel,
            Usuario supervisor,
            ProcessoEstagio processo) {
    }
}
