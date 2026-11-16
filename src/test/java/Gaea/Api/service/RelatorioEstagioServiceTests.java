package Gaea.Api.service;

import Gaea.Api.dto.RelatorioEstagioResponse;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelatorioEstagioServiceTests {

    @Mock ProcessoEstagioRepository processoRepository;
    @Mock VinculoSupervisorProcessoRepository vinculoRepository;

    private RelatorioEstagioService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        service = new RelatorioEstagioService(processoRepository, vinculoRepository);
        Instituicao instituicao = mock(Instituicao.class);
        when(instituicao.getId()).thenReturn(7L);
        usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(1L);
        when(usuario.getPerfil()).thenReturn("INSTITUICAO");
        when(usuario.getInstituicao()).thenReturn(instituicao);
        when(vinculoRepository.findFirstByProcessoIdOrderByCriadoEmDescIdDesc(
                org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void deveFiltrarPorInstituicaoStatusPeriodoEAlunoECalcularIndicadores() {
        ProcessoEstagio aprovado = processo(10L, "Ana Silva", "APROVADO",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30));
        ProcessoEstagio reprovado = processo(11L, "Bruno", "REPROVADO",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30));
        when(processoRepository.findAllByInstituicaoId(7L))
                .thenReturn(List.of(aprovado, reprovado));

        RelatorioEstagioResponse resposta = service.gerar(
                usuario, "APROVADO", LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31), "ana");

        assertEquals(1, resposta.total());
        assertEquals(1L, resposta.porSituacao().get("APROVADO"));
        assertEquals(0L, resposta.porSituacao().get("REPROVADO"));
        assertEquals(10L, resposta.processos().get(0).processoId());
    }

    @Test
    void deveBloquearUsuarioSemVinculoInstitucional() {
        Usuario semInstituicao = mock(Usuario.class);
        when(semInstituicao.getPerfil()).thenReturn("INSTITUICAO");
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.gerar(semInstituicao, null, null, null, null));
    }

    @Test
    void deveRetornarVazioSemExporProcessosDeOutraInstituicao() {
        when(processoRepository.findAllByInstituicaoId(7L)).thenReturn(List.of());
        assertEquals(0, service.gerar(usuario, null, null, null, null).total());
    }

    @Test
    void devePreservarProcessoSemDatasQuandoNaoHaFiltroDePeriodo() {
        ProcessoEstagio semDatas = processo(12L, "Aluno legado", "PENDENTE", null, null);
        when(processoRepository.findAllByInstituicaoId(7L)).thenReturn(List.of(semDatas));

        assertEquals(1, service.gerar(usuario, null, null, null, null).total());
    }

    @Test
    void deveExcluirProcessosSemDataNecessariaNosFiltrosIndividuais() {
        ProcessoEstagio semInicio = processo(
                12L, "Sem inÃ­cio", "PENDENTE", null, LocalDate.of(2026, 6, 30));
        ProcessoEstagio semFim = processo(
                13L, "Sem fim", "PENDENTE", LocalDate.of(2026, 2, 1), null);
        ProcessoEstagio completo = processo(
                14L, "Completo", "PENDENTE",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30));
        when(processoRepository.findAllByInstituicaoId(7L))
                .thenReturn(List.of(semInicio, semFim, completo));

        RelatorioEstagioResponse aPartirDeMarco = service.gerar(
                usuario, null, LocalDate.of(2026, 3, 1), null, null);
        RelatorioEstagioResponse ateMarco = service.gerar(
                usuario, null, null, LocalDate.of(2026, 3, 31), null);

        assertEquals(List.of(14L, 12L), aPartirDeMarco.processos().stream()
                .map(Gaea.Api.dto.RelatorioProcessoResponse::processoId).toList());
        assertEquals(List.of(13L, 14L), ateMarco.processos().stream()
                .map(Gaea.Api.dto.RelatorioProcessoResponse::processoId).toList());
    }

    @Test
    void deveExcluirDatasAusentesECombinarTodosOsFiltros() {
        ProcessoEstagio semDatas = processo(12L, "Ana sem datas", "APROVADO", null, null);
        ProcessoEstagio dentro = processo(13L, "Ana Silva", "APROVADO",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30));
        ProcessoEstagio outroStatus = processo(14L, "Ana Costa", "REPROVADO",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30));
        when(processoRepository.findAllByInstituicaoId(7L))
                .thenReturn(List.of(semDatas, dentro, outroStatus));

        RelatorioEstagioResponse resposta = service.gerar(
                usuario, "APROVADO", LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31), "ana");

        assertEquals(1, resposta.total());
        assertEquals(13L, resposta.processos().get(0).processoId());
    }

    @Test
    void deveEscaparCsvPreservarConteudoEImpedirFormulasOfuscadas() {
        RelatorioEstagioResponse relatorio = new RelatorioEstagioResponse(
                4,
                java.util.Map.of("PENDENTE", 4L),
                List.of(
                        new Gaea.Api.dto.RelatorioProcessoResponse(
                                1L, "=PERIGO", "Empresa;Teste", null, "PENDENTE", null, null),
                        new Gaea.Api.dto.RelatorioProcessoResponse(
                                2L, "  +SOMA(A1:A2)", "Empresa comum", null,
                                "PENDENTE", null, null),
                        new Gaea.Api.dto.RelatorioProcessoResponse(
                                3L, "\t-CMD", "Texto, com \"aspas\"\ne quebra", null,
                                "PENDENTE", null, null),
                        new Gaea.Api.dto.RelatorioProcessoResponse(
                                4L, "\r\n@IMPORT", "Valor normal", null,
                                "PENDENTE", null, null)));

        String csv = new String(service.gerarCsv(relatorio), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"'=PERIGO\""));
        assertTrue(csv.contains("\"Empresa;Teste\""));
        assertTrue(csv.contains("\"'  +SOMA(A1:A2)\""));
        assertTrue(csv.contains("\"'\t-CMD\""));
        assertTrue(csv.contains("\"'\r\n@IMPORT\""));
        assertTrue(csv.contains("\"Texto, com \"\"aspas\"\"\ne quebra\""));
        assertTrue(csv.contains("\"Empresa comum\""));
        assertTrue(csv.contains("Processo;Aluno;Empresa"));
        assertTrue(csv.startsWith("\uFEFF"));
    }

    private ProcessoEstagio processo(
            Long id, String aluno, String status, LocalDate inicio, LocalDate fim) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setId(id);
        processo.setNomeAluno(aluno);
        processo.setStatus(status);
        processo.setDataInicio(inicio);
        processo.setDataFim(fim);
        return processo;
    }
}
