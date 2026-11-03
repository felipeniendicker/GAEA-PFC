package Gaea.Api.service;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.TipoOperacaoStatus;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.SituacaoVinculoSupervisor;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class ProcessoEstagioTransactionalTests {

    @Autowired
    private ProcessoEstagioService service;

    @Autowired
    private ProcessoEstagioRepository processoRepository;

    @Autowired
    private HistoricoStatusProcessoRepository historicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    @Autowired
    private VinculoSupervisorProcessoRepository vinculoRepository;

    @MockitoBean
    private LogAuditoriaService logAuditoriaService;

    private Usuario responsavelInstituicao;

    @BeforeEach
    void limparBanco() {
        vinculoRepository.deleteAll();
        historicoRepository.deleteAll();
        processoRepository.deleteAll();
        usuarioRepository.deleteAll();
        instituicaoRepository.deleteAll();
        responsavelInstituicao = salvarResponsavelInstituicao();
    }

    @Test
    void devePersistirHistoricoJuntoComAlteracaoDeStatus() {
        ProcessoEstagio processo = novoProcessoPendente();

        service.alterarStatus(
                processo.getId(),
                "EM_ANALISE",
                responsavelInstituicao);

        ProcessoEstagio processoAtualizado = processoRepository.findById(processo.getId()).orElseThrow();
        var historicos = historicoRepository.findByProcessoIdOrderByDataHoraAscIdAsc(processo.getId());

        assertEquals("EM_ANALISE", processoAtualizado.getStatus());
        assertEquals(1, historicos.size());
        assertEquals("PENDENTE", historicos.get(0).getStatusAnterior());
        assertEquals("EM_ANALISE", historicos.get(0).getStatusNovo());
        assertEquals("instituicao@example.com", historicos.get(0).getUsuarioResponsavel());
        assertEquals(TipoOperacaoStatus.ALTERACAO_NORMAL, historicos.get(0).getTipoOperacao());
    }

    @Test
    void deveDesfazerStatusEHistoricoQuandoAuditoriaFalhar() {
        ProcessoEstagio processo = novoProcessoPendente();

        Long processoId = processo.getId();
        doThrow(new RuntimeException("Falha simulada de auditoria"))
                .when(logAuditoriaService).registrar(anyString(), anyString(), anyString(), any());

        assertThrows(
                RuntimeException.class,
                () -> service.alterarStatus(
                        processoId,
                        "EM_ANALISE",
                        responsavelInstituicao));

        assertEquals("PENDENTE", processoRepository.findById(processoId).orElseThrow().getStatus());
        assertEquals(0, historicoRepository.count());
    }

    private ProcessoEstagio novoProcessoPendente() {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno("Ana Souza");
        processo.setEmailAluno("ana@example.com");
        processo.setNomeEmpresa("Empresa Exemplo");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus("PENDENTE");
        processo.setInstituicao(responsavelInstituicao.getInstituicao());
        processo = processoRepository.save(processo);

        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setEmailConvidado("supervisor@example.com");
        vinculo.setSituacao(SituacaoVinculoSupervisor.CONFIRMADO);
        vinculoRepository.save(vinculo);
        return processo;
    }

    private Usuario salvarResponsavelInstituicao() {
        Instituicao instituicao = new Instituicao();
        instituicao.setNome("Faculdade de Teste");
        instituicao.setCnpj("11222333000181");
        instituicao = instituicaoRepository.save(instituicao);

        Usuario usuario = new Usuario();
        usuario.setNome("Responsável Institucional");
        usuario.setEmail("instituicao@example.com");
        usuario.setSenha("hash-de-teste");
        usuario.setPerfil("INSTITUICAO");
        usuario.setInstituicao(instituicao);
        return usuarioRepository.save(usuario);
    }
}
