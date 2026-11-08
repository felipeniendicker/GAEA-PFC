package Gaea.Api.service;

import Gaea.Api.dto.DashboardResumoResponse;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DashboardServiceTests {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ProcessoEstagioRepository processoRepository;

    @Autowired
    private HistoricoStatusProcessoRepository historicoRepository;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VinculoSupervisorProcessoRepository vinculoRepository;

    private Instituicao instituicao;
    private Usuario usuarioInstituicao;

    @BeforeEach
    void limparBanco() {
        vinculoRepository.deleteAll();
        historicoRepository.deleteAll();
        processoRepository.deleteAll();
        usuarioRepository.deleteAll();
        instituicaoRepository.deleteAll();

        instituicao = novaInstituicao("Faculdade de Teste", "11222333000181");
        usuarioInstituicao = new Usuario();
        usuarioInstituicao.setPerfil("INSTITUICAO");
        usuarioInstituicao.setInstituicao(instituicao);
    }

    @Test
    void deveRetornarResumoZeradoQuandoNaoExistiremProcessos() {
        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(0, resumo.totalProcessos());
        assertEquals(0, resumo.pendentes());
        assertEquals(0, resumo.emAnalise());
        assertEquals(0, resumo.aprovados());
        assertEquals(0, resumo.reprovados());
        assertEquals(new BigDecimal("0.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveContarProcessosEmTodosOsStatusECalcularPercentual() {
        salvarProcessos("PENDENTE", 1);
        salvarProcessos("EM_ANALISE", 2);
        salvarProcessos("APROVADO", 3);
        salvarProcessos("REPROVADO", 1);

        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(7, resumo.totalProcessos());
        assertEquals(1, resumo.pendentes());
        assertEquals(2, resumo.emAnalise());
        assertEquals(3, resumo.aprovados());
        assertEquals(1, resumo.reprovados());
        assertEquals(new BigDecimal("75.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveRetornarPercentualZeroQuandoNaoExistiremDecisoesFinais() {
        salvarProcessos("PENDENTE", 2);
        salvarProcessos("EM_ANALISE", 1);

        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(3, resumo.totalProcessos());
        assertEquals(0, resumo.aprovados());
        assertEquals(0, resumo.reprovados());
        assertEquals(new BigDecimal("0.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveArredondarPercentualDeAprovacaoParaDuasCasasDecimais() {
        salvarProcessos("APROVADO", 1);
        salvarProcessos("REPROVADO", 2);

        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(new BigDecimal("33.33"), resumo.percentualAprovacao());
    }

    @Test
    void deveIgnorarProcessosDeOutraInstituicao() {
        salvarProcessos("PENDENTE", 1);
        Instituicao outraInstituicao = novaInstituicao(
                "Outra Faculdade",
                "22333444000160");
        salvarProcesso("APROVADO", outraInstituicao);

        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(1, resumo.totalProcessos());
        assertEquals(1, resumo.pendentes());
        assertEquals(0, resumo.aprovados());
    }

    @Test
    void deveContarSomenteProcessosDoAlunoAutenticado() {
        Usuario aluno = salvarUsuario("aluno@example.com", "ALUNO");
        Usuario outroAluno = salvarUsuario("outro.aluno@example.com", "ALUNO");
        salvarProcesso("PENDENTE", instituicao, aluno);
        salvarProcesso("APROVADO", instituicao, aluno);
        salvarProcesso("REPROVADO", instituicao, outroAluno);

        DashboardResumoResponse resumo = dashboardService.obterResumo(aluno);

        assertEquals(2, resumo.totalProcessos());
        assertEquals(1, resumo.pendentes());
        assertEquals(1, resumo.aprovados());
        assertEquals(0, resumo.reprovados());
        assertEquals(new BigDecimal("100.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveRetornarResumoZeradoParaAlunoSemProcessos() {
        Usuario aluno = salvarUsuario("aluno@example.com", "ALUNO");

        DashboardResumoResponse resumo = dashboardService.obterResumo(aluno);

        assertEquals(0, resumo.totalProcessos());
        assertEquals(new BigDecimal("0.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveContarSomenteSupervisoesAtivasEConfirmadasSemDuplicidade() {
        Usuario supervisor = salvarUsuario("supervisor@example.com", "EMPRESA");
        Usuario outroSupervisor = salvarUsuario("outro.supervisor@example.com", "EMPRESA");
        ProcessoEstagio confirmado = salvarProcesso("APROVADO", instituicao);
        ProcessoEstagio pendente = salvarProcesso("PENDENTE", instituicao);
        ProcessoEstagio recusado = salvarProcesso("REPROVADO", instituicao);
        ProcessoEstagio cancelado = salvarProcesso("EM_ANALISE", instituicao);
        ProcessoEstagio deOutroSupervisor = salvarProcesso("PENDENTE", instituicao);

        salvarVinculo(confirmado, supervisor, SituacaoVinculoSupervisor.CANCELADO);
        salvarVinculo(confirmado, supervisor, SituacaoVinculoSupervisor.CONFIRMADO);
        salvarVinculo(pendente, supervisor, SituacaoVinculoSupervisor.PENDENTE);
        salvarVinculo(recusado, supervisor, SituacaoVinculoSupervisor.RECUSADO);
        salvarVinculo(cancelado, supervisor, SituacaoVinculoSupervisor.CANCELADO);
        salvarVinculo(deOutroSupervisor, outroSupervisor, SituacaoVinculoSupervisor.CONFIRMADO);

        DashboardResumoResponse resumo = dashboardService.obterResumo(supervisor);

        assertEquals(1, resumo.totalProcessos());
        assertEquals(1, resumo.aprovados());
        assertEquals(0, resumo.pendentes());
        assertEquals(0, resumo.emAnalise());
        assertEquals(0, resumo.reprovados());
        assertEquals(new BigDecimal("100.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveRetornarResumoZeradoParaEmpresaSemVinculosConfirmados() {
        Usuario supervisor = salvarUsuario("supervisor@example.com", "EMPRESA");
        ProcessoEstagio processo = salvarProcesso("PENDENTE", instituicao);
        salvarVinculo(processo, supervisor, SituacaoVinculoSupervisor.PENDENTE);

        DashboardResumoResponse resumo = dashboardService.obterResumo(supervisor);

        assertEquals(0, resumo.totalProcessos());
        assertEquals(new BigDecimal("0.00"), resumo.percentualAprovacao());
    }

    private void salvarProcessos(String status, int quantidade) {
        for (int indice = 0; indice < quantidade; indice += 1) {
            salvarProcesso(status, instituicao);
        }
    }

    private ProcessoEstagio salvarProcesso(String status, Instituicao instituicaoDoProcesso) {
        return salvarProcesso(status, instituicaoDoProcesso, null);
    }

    private ProcessoEstagio salvarProcesso(
            String status,
            Instituicao instituicaoDoProcesso,
            Usuario aluno) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno("Aluno " + status);
        processo.setEmailAluno("aluno" + status + "@example.com");
        processo.setNomeEmpresa("Empresa de Teste");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus(status);
        processo.setInstituicao(instituicaoDoProcesso);
        processo.setAluno(aluno);
        return processoRepository.save(processo);
    }

    private Usuario salvarUsuario(String email, String perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de Teste");
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        usuario.setTermosAceitos(true);
        return usuarioRepository.save(usuario);
    }

    private void salvarVinculo(
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

    private Instituicao novaInstituicao(String nome, String cnpj) {
        Instituicao novaInstituicao = new Instituicao();
        novaInstituicao.setNome(nome);
        novaInstituicao.setCnpj(cnpj);
        return instituicaoRepository.save(novaInstituicao);
    }
}
