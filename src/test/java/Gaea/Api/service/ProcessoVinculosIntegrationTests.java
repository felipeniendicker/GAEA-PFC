package Gaea.Api.service;

import Gaea.Api.exception.RecursoNaoEncontradoException;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ProcessoVinculosIntegrationTests {

    @Autowired
    private ProcessoAutorizacaoService autorizacaoService;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoEstagioRepository processoRepository;

    @Autowired
    private VinculoSupervisorProcessoRepository vinculoRepository;

    @Autowired
    private HistoricoStatusProcessoRepository historicoRepository;

    @BeforeEach
    void limparBanco() {
        vinculoRepository.deleteAll();
        historicoRepository.deleteAll();
        processoRepository.deleteAll();
        usuarioRepository.deleteAll();
        instituicaoRepository.deleteAll();
    }

    @Test
    void devePersistirEAutorizarSomenteOsResponsaveisVinculados() {
        Instituicao instituicao = salvarInstituicao("Faculdade A", "11222333000181");
        Instituicao outraInstituicao = salvarInstituicao("Faculdade B", "22333444000192");
        Usuario aluno = salvarUsuario("aluno@example.com", "ALUNO", instituicao);
        Usuario outroAluno = salvarUsuario("outro@example.com", "ALUNO", instituicao);
        Usuario responsavel = salvarUsuario("instituicao@example.com", "INSTITUICAO", instituicao);
        Usuario outroResponsavel = salvarUsuario(
                "outra.instituicao@example.com", "INSTITUICAO", outraInstituicao);
        Usuario supervisor = salvarUsuario("supervisor@example.com", "EMPRESA", null);
        Usuario outroSupervisor = salvarUsuario("outro.supervisor@example.com", "EMPRESA", null);

        ProcessoEstagio processo = salvarProcesso(aluno, instituicao);
        salvarVinculoConfirmado(processo, supervisor);

        assertDoesNotThrow(() -> autorizacaoService.validarAcesso(processo, aluno));
        assertDoesNotThrow(() -> autorizacaoService.validarAcesso(processo, responsavel));
        assertDoesNotThrow(() -> autorizacaoService.validarAcesso(processo, supervisor));
        assertDoesNotThrow(() -> autorizacaoService.validarGestaoInstitucional(processo, responsavel));

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> autorizacaoService.validarAcesso(processo, outroAluno));
        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> autorizacaoService.validarAcesso(processo, outroResponsavel));
        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> autorizacaoService.validarAcesso(processo, outroSupervisor));

        assertEquals(1, processoRepository.findAllByAlunoId(aluno.getId()).size());
        assertEquals(0, processoRepository.findAllByAlunoId(outroAluno.getId()).size());
        assertEquals(1, processoRepository.findAllByInstituicaoId(instituicao.getId()).size());
        assertEquals(0, processoRepository.findAllByInstituicaoId(outraInstituicao.getId()).size());
        assertEquals(1, processoRepository.findAllBySupervisorConfirmadoId(supervisor.getId()).size());
        assertEquals(0, processoRepository.findAllBySupervisorConfirmadoId(outroSupervisor.getId()).size());
    }

    @Test
    void devePreservarRegistroLegadoForaDasConsultasVinculadas() {
        ProcessoEstagio legado = new ProcessoEstagio();
        legado.setNomeAluno("Aluno Legado");
        legado.setEmailAluno("legado@example.com");
        legado.setNomeEmpresa("Empresa Legada");
        legado.setCnpjEmpresa("11222333000181");
        legado.setStatus("PENDENTE");
        ProcessoEstagio legadoSalvo = processoRepository.save(legado);

        Instituicao instituicao = salvarInstituicao("Faculdade A", "11222333000181");
        Usuario aluno = salvarUsuario("legado@example.com", "ALUNO", instituicao);

        assertNull(legadoSalvo.getAluno());
        assertNull(legadoSalvo.getInstituicao());
        assertEquals(0, processoRepository.findAllByAlunoId(aluno.getId()).size());
        assertEquals(0, processoRepository.findAllByInstituicaoId(instituicao.getId()).size());
        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> autorizacaoService.validarAcesso(legadoSalvo, aluno));
    }

    private Instituicao salvarInstituicao(String nome, String cnpj) {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome(nome);
        instituicao.setCnpj(cnpj);
        return instituicaoRepository.save(instituicao);
    }

    private Usuario salvarUsuario(String email, String perfil, Instituicao instituicao) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de Teste");
        usuario.setEmail(email);
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil(perfil);
        usuario.setTermosAceitos(true);
        usuario.setInstituicao(instituicao);
        return usuarioRepository.save(usuario);
    }

    private ProcessoEstagio salvarProcesso(Usuario aluno, Instituicao instituicao) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno(aluno.getNome());
        processo.setEmailAluno(aluno.getEmail());
        processo.setNomeEmpresa("Empresa de Teste");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus("PENDENTE");
        processo.setAluno(aluno);
        processo.setInstituicao(instituicao);
        return processoRepository.save(processo);
    }

    private void salvarVinculoConfirmado(ProcessoEstagio processo, Usuario supervisor) {
        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado(supervisor.getEmail());
        vinculo.setSupervisor(supervisor);
        vinculo.setSituacao(SituacaoVinculoSupervisor.CONFIRMADO);
        vinculoRepository.save(vinculo);
    }
}
