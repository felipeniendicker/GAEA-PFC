package Gaea.Api.service;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.Notificacao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.NotificacaoRepository;
import Gaea.Api.repository.PrazoProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacaoServiceTests {

    @Mock NotificacaoRepository notificacaoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ProcessoEstagioRepository processoRepository;
    @Mock VinculoSupervisorProcessoRepository vinculoRepository;
    @Mock PrazoProcessoRepository prazoRepository;
    @Mock ProcessoAutorizacaoService autorizacaoService;

    private NotificacaoService service;
    private ProcessoEstagio processo;
    private Usuario aluno;

    @BeforeEach
    void preparar() {
        service = new NotificacaoService(
                notificacaoRepository,
                usuarioRepository,
                processoRepository,
                vinculoRepository,
                prazoRepository,
                autorizacaoService);
        processo = new ProcessoEstagio();
        processo.setId(10L);
        aluno = usuario(1L, "ALUNO");
        processo.setAluno(aluno);
        Instituicao instituicao = mock(Instituicao.class);
        when(instituicao.getId()).thenReturn(7L);
        processo.setInstituicao(instituicao);
        when(prazoRepository.findAllByDataVencimentoBetween(any(), any()))
                .thenReturn(List.of());
        when(usuarioRepository.findAllByInstituicaoId(7L)).thenReturn(List.of());
        when(vinculoRepository.findFirstByProcessoIdOrderByCriadoEmDescIdDesc(10L))
                .thenReturn(Optional.empty());
        when(notificacaoRepository.existsByDestinatarioIdAndChaveIdempotencia(
                any(Long.class), anyString())).thenReturn(false);
    }

    @Test
    void deveNotificarAlunoInstituicaoESupervisorConfirmado() {
        Usuario instituicaoUsuario = usuario(2L, "INSTITUICAO");
        Usuario supervisor = usuario(3L, "EMPRESA");
        VinculoSupervisorProcesso vinculo = new VinculoSupervisorProcesso();
        vinculo.setProcesso(processo);
        vinculo.setSupervisor(supervisor);
        vinculo.setSituacao(SituacaoVinculoSupervisor.CONFIRMADO);

        when(usuarioRepository.findAllByInstituicaoId(7L)).thenReturn(List.of(instituicaoUsuario));
        when(vinculoRepository.findFirstByProcessoIdOrderByCriadoEmDescIdDesc(10L))
                .thenReturn(Optional.of(vinculo));

        service.notificarAlteracaoStatus(processo, "PENDENTE", "EM_ANALISE");

        verify(notificacaoRepository, org.mockito.Mockito.times(3)).save(any(Notificacao.class));
    }

    @Test
    void deveIgnorarEventoDuplicadoParaMesmoDestinatario() {
        when(notificacaoRepository.existsByDestinatarioIdAndChaveIdempotencia(
                1L, "STATUS:10:PENDENTE:EM_ANALISE")).thenReturn(true);

        service.notificarAlteracaoStatus(processo, "PENDENTE", "EM_ANALISE");

        verify(notificacaoRepository, never()).save(any(Notificacao.class));
    }

    @Test
    void deveOcultarNotificacaoQuandoAcessoAoProcessoFoiRevogado() {
        Notificacao notificacao = new Notificacao();
        notificacao.setDestinatario(aluno);
        notificacao.setProcessoId(10L);
        when(notificacaoRepository.findAllByDestinatarioIdOrderByCriadaEmDescIdDesc(1L))
                .thenReturn(List.of(notificacao));
        when(autorizacaoService.possuiAcesso(processo, aluno)).thenReturn(false);
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));

        assertEquals(0, service.listar(aluno).size());
    }

    @Test
    void deveContarSomenteNotificacoesNaoLidasDoUsuario() {
        Notificacao naoLida = new Notificacao();
        naoLida.setDestinatario(aluno);
        Notificacao lida = new Notificacao();
        lida.setDestinatario(aluno);
        lida.marcarComoLida();
        when(notificacaoRepository.findAllByDestinatarioIdOrderByCriadaEmDescIdDesc(1L))
                .thenReturn(List.of(naoLida, lida));
        when(autorizacaoService.possuiAcesso(any(), any())).thenReturn(true);
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));

        assertEquals(1, service.contarNaoLidas(aluno));
    }

    private Usuario usuario(Long id, String perfil) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        when(usuario.getPerfil()).thenReturn(perfil);
        return usuario;
    }
}
