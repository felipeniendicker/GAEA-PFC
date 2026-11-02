package Gaea.Api.service;

import Gaea.Api.dto.CadastroProcessoRequest;
import Gaea.Api.dto.ReaberturaProcessoRequest;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.model.HistoricoStatusProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.TipoOperacaoStatus;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessoEstagioServiceTests {

    @Mock
    private ProcessoEstagioRepository repository;

    @Mock
    private HistoricoStatusProcessoRepository historicoRepository;

    @Mock
    private LogAuditoriaService logAuditoriaService;

    @InjectMocks
    private ProcessoEstagioService service;

    @Test
    void deveDefinirStatusInicialNoBackend() {
        CadastroProcessoRequest request = new CadastroProcessoRequest(
                "  Ana Souza  ",
                "  ANA@EXAMPLE.COM ",
                "  Empresa Exemplo  ",
                "11.222.333/0001-81",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 4, 30));

        when(repository.save(any(ProcessoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcessoEstagio processo = service.cadastrar(request);

        assertNull(processo.getId());
        assertEquals("PENDENTE", processo.getStatus());
        assertEquals("Ana Souza", processo.getNomeAluno());
        assertEquals("ana@example.com", processo.getEmailAluno());
        assertEquals("Empresa Exemplo", processo.getNomeEmpresa());
        assertEquals("11222333000181", processo.getCnpjEmpresa());
    }

    @ParameterizedTest
    @CsvSource({
            "PENDENTE, EM_ANALISE",
            "EM_ANALISE, APROVADO",
            "EM_ANALISE, REPROVADO"
    })
    void devePermitirTransicoesNormaisValidas(String statusAnterior, String statusNovo) {
        ProcessoEstagio processo = processo(1L, "aluno@example.com", statusAnterior);
        when(repository.findById(1L)).thenReturn(Optional.of(processo));
        when(repository.save(processo)).thenReturn(processo);

        ProcessoEstagio atualizado = service.alterarStatus(1L, statusNovo, "instituicao@example.com");

        assertEquals(statusNovo, atualizado.getStatus());
        ArgumentCaptor<HistoricoStatusProcesso> captor =
                ArgumentCaptor.forClass(HistoricoStatusProcesso.class);
        verify(historicoRepository).save(captor.capture());
        assertEquals(statusAnterior, captor.getValue().getStatusAnterior());
        assertEquals(statusNovo, captor.getValue().getStatusNovo());
        assertEquals(TipoOperacaoStatus.ALTERACAO_NORMAL, captor.getValue().getTipoOperacao());
        assertNull(captor.getValue().getJustificativa());
        verify(logAuditoriaService).registrar(
                "instituicao@example.com",
                "ALTERACAO_STATUS",
                "Processo 1: " + statusAnterior + " -> " + statusNovo);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDENTE, PENDENTE",
            "PENDENTE, APROVADO",
            "PENDENTE, REPROVADO",
            "EM_ANALISE, EM_ANALISE",
            "EM_ANALISE, PENDENTE",
            "APROVADO, APROVADO",
            "APROVADO, PENDENTE",
            "APROVADO, EM_ANALISE",
            "APROVADO, REPROVADO",
            "REPROVADO, REPROVADO",
            "REPROVADO, PENDENTE",
            "REPROVADO, EM_ANALISE",
            "REPROVADO, APROVADO"
    })
    void deveRejeitarTransicoesNormaisInvalidas(String statusAnterior, String statusNovo) {
        ProcessoEstagio processo = processo(1L, "aluno@example.com", statusAnterior);
        when(repository.findById(1L)).thenReturn(Optional.of(processo));

        assertThrows(
                ConflitoDadosException.class,
                () -> service.alterarStatus(1L, statusNovo, "instituicao@example.com"));

        verify(repository, never()).save(any());
        verify(historicoRepository, never()).save(any());
        verify(logAuditoriaService, never()).registrar(any(), any(), any());
    }

    @Test
    void deveRejeitarStatusDesconhecido() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(processo(1L, "aluno@example.com", "PENDENTE")));

        assertThrows(
                ConflitoDadosException.class,
                () -> service.alterarStatus(1L, "CANCELADO", "instituicao@example.com"));
    }

    @ParameterizedTest
    @CsvSource({"APROVADO", "REPROVADO"})
    void deveReabrirProcessoFinalizadoComJustificativa(String statusAnterior) {
        ProcessoEstagio processo = processo(1L, "aluno@example.com", statusAnterior);
        when(repository.findById(1L)).thenReturn(Optional.of(processo));
        when(repository.save(processo)).thenReturn(processo);

        ProcessoEstagio atualizado = service.reabrir(
                1L,
                new ReaberturaProcessoRequest("Novos documentos foram recebidos"),
                "instituicao@example.com");

        assertEquals("EM_ANALISE", atualizado.getStatus());
        ArgumentCaptor<HistoricoStatusProcesso> captor =
                ArgumentCaptor.forClass(HistoricoStatusProcesso.class);
        verify(historicoRepository).save(captor.capture());
        assertEquals(statusAnterior, captor.getValue().getStatusAnterior());
        assertEquals("EM_ANALISE", captor.getValue().getStatusNovo());
        assertEquals("Novos documentos foram recebidos", captor.getValue().getJustificativa());
        assertEquals(TipoOperacaoStatus.REABERTURA, captor.getValue().getTipoOperacao());
    }

    @Test
    void deveRejeitarReaberturaSemJustificativaSignificativa() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(processo(1L, "aluno@example.com", "APROVADO")));

        assertThrows(
                ConflitoDadosException.class,
                () -> service.reabrir(
                        1L,
                        new ReaberturaProcessoRequest("curta"),
                        "instituicao@example.com"));
    }

    @ParameterizedTest
    @CsvSource({"PENDENTE", "EM_ANALISE"})
    void deveRejeitarReaberturaDeProcessoNaoFinalizado(String status) {
        when(repository.findById(1L))
                .thenReturn(Optional.of(processo(1L, "aluno@example.com", status)));

        assertThrows(
                ConflitoDadosException.class,
                () -> service.reabrir(
                        1L,
                        new ReaberturaProcessoRequest("Justificativa suficiente"),
                        "instituicao@example.com"));
    }

    @Test
    void devePermitirConsultaDoHistoricoPeloAlunoProprietario() {
        Usuario aluno = usuario("aluno@example.com", "ALUNO");
        when(repository.findById(1L))
                .thenReturn(Optional.of(processo(1L, "aluno@example.com", "EM_ANALISE")));
        when(historicoRepository.findByProcessoIdOrderByDataHoraAscIdAsc(1L))
                .thenReturn(List.of());

        assertEquals(0, service.listarHistorico(1L, aluno).size());
    }

    @Test
    void deveNegarConsultaDoHistoricoDeOutroAluno() {
        Usuario aluno = usuario("outro@example.com", "ALUNO");
        when(repository.findById(1L))
                .thenReturn(Optional.of(processo(1L, "aluno@example.com", "EM_ANALISE")));

        assertThrows(AccessDeniedException.class, () -> service.listarHistorico(1L, aluno));
        verify(historicoRepository, never()).findByProcessoIdOrderByDataHoraAscIdAsc(any());
    }

    private ProcessoEstagio processo(Long id, String emailAluno, String status) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setId(id);
        processo.setEmailAluno(emailAluno);
        processo.setStatus(status);
        return processo;
    }

    private Usuario usuario(String email, String perfil) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        return usuario;
    }
}
