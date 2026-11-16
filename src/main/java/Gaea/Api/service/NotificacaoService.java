package Gaea.Api.service;

import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.dto.NotificacaoResponse;
import Gaea.Api.model.Notificacao;
import Gaea.Api.model.PrazoProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.NotificacaoRepository;
import Gaea.Api.repository.PrazoProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificacaoService {

    private static final int JANELA_PRAZO_DIAS = 7;

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcessoEstagioRepository processoRepository;
    private final VinculoSupervisorProcessoRepository vinculoRepository;
    private final PrazoProcessoRepository prazoRepository;
    private final ProcessoAutorizacaoService autorizacaoService;

    public NotificacaoService(
            NotificacaoRepository notificacaoRepository,
            UsuarioRepository usuarioRepository,
            ProcessoEstagioRepository processoRepository,
            VinculoSupervisorProcessoRepository vinculoRepository,
            PrazoProcessoRepository prazoRepository,
            ProcessoAutorizacaoService autorizacaoService) {
        this.notificacaoRepository = notificacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.processoRepository = processoRepository;
        this.vinculoRepository = vinculoRepository;
        this.prazoRepository = prazoRepository;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional
    public List<NotificacaoResponse> listar(Usuario usuario) {
        gerarAvisosDePrazo(LocalDate.now());
        return visiveis(usuario).stream().map(NotificacaoResponse::from).toList();
    }

    @Transactional
    public long contarNaoLidas(Usuario usuario) {
        gerarAvisosDePrazo(LocalDate.now());
        return visiveis(usuario).stream().filter(notificacao -> !notificacao.isLida()).count();
    }

    @Transactional
    public void marcarComoLida(Long id, Usuario usuario) {
        Notificacao notificacao = notificacaoRepository
                .findByIdAndDestinatarioId(id, usuario.getId())
                .filter(this::processoAindaVisivel)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada"));
        notificacao.marcarComoLida();
    }

    @Transactional
    public void notificarAlteracaoStatus(
            ProcessoEstagio processo, String statusAnterior, String statusNovo) {
        String chave = "STATUS:" + processo.getId() + ":" + statusAnterior + ":" + statusNovo;
        criarParaProcesso(
                processo,
                "ALTERACAO_STATUS",
                "Situação do estágio atualizada",
                "O processo de estágio foi alterado de " + statusAnterior + " para " + statusNovo + ".",
                chave);
    }

    @Transactional
    public void notificarSupervisao(
            VinculoSupervisorProcesso vinculo, boolean confirmado) {
        ProcessoEstagio processo = vinculo.getProcesso();
        String evento = confirmado ? "CONFIRMACAO_SUPERVISOR" : "RECUSA_SUPERVISOR";
        String titulo = confirmado ? "Supervisor confirmado" : "Supervisor recusou o convite";
        String mensagem = confirmado
                ? "O supervisor confirmou o vínculo do processo de estágio."
                : "O supervisor recusou o vínculo do processo de estágio.";
        criarParaProcesso(processo, evento, titulo, mensagem,
                evento + ":" + vinculo.getId());
    }

    @Transactional
    public void notificarDocumento(
            ProcessoEstagio processo, Long documentoId, boolean aprovado) {
        String evento = aprovado ? "APROVACAO_DOCUMENTO" : "REPROVACAO_DOCUMENTO";
        String titulo = aprovado ? "Documento aprovado" : "Documento reprovado";
        String mensagem = aprovado
                ? "Um documento do seu processo foi aprovado."
                : "Um documento do seu processo foi reprovado e pode ser reenviado.";
        criarParaDestinatario(
                processo.getAluno(),
                processo,
                evento,
                titulo,
                mensagem,
                evento + ":" + documentoId);
    }

    @Transactional
    public void notificarPendencia(ProcessoEstagio processo, Long pendenciaId) {
        criarParaProcesso(
                processo,
                "CRIACAO_PENDENCIA",
                "Nova pendência no estágio",
                "Uma nova pendência foi registrada no seu processo de estágio.",
                "PENDENCIA:" + pendenciaId);
    }

    @Transactional
    public void gerarAvisosDePrazo(LocalDate hoje) {
        prazoRepository.findAllByDataVencimentoBetween(
                        hoje, hoje.plusDays(JANELA_PRAZO_DIAS))
                .forEach(prazo -> criarAvisoDePrazo(prazo, hoje));
    }

    private void criarAvisoDePrazo(PrazoProcesso prazo, LocalDate hoje) {
        ProcessoEstagio processo = prazo.getProcesso();
        long dias = hoje.until(prazo.getDataVencimento()).getDays();
        String quando = dias == 0 ? "vence hoje" : "vence em " + dias + " dias";
        criarParaProcesso(
                processo,
                "PRAZO_PROXIMO",
                "Prazo próximo",
                "O prazo \"" + prazo.getDescricao() + "\" " + quando + ".",
                "PRAZO:" + prazo.getId() + ":" + prazo.getDataVencimento());
    }

    private void criarParaProcesso(
            ProcessoEstagio processo,
            String tipo,
            String titulo,
            String mensagem,
            String chave) {
        for (Usuario destinatario : destinatariosAutorizados(processo)) {
            criarParaDestinatario(destinatario, processo, tipo, titulo, mensagem, chave);
        }
    }

    private void criarParaDestinatario(
            Usuario destinatario,
            ProcessoEstagio processo,
            String tipo,
            String titulo,
            String mensagem,
            String chave) {
        if (destinatario == null || destinatario.getId() == null
                || notificacaoRepository.existsByDestinatarioIdAndChaveIdempotencia(
                        destinatario.getId(), chave)) {
            return;
        }
        Notificacao notificacao = new Notificacao();
        notificacao.setDestinatario(destinatario);
        notificacao.setProcessoId(processo.getId());
        notificacao.setTipoEvento(tipo);
        notificacao.setTitulo(titulo);
        notificacao.setMensagem(mensagem);
        notificacao.setChaveIdempotencia(chave);
        notificacaoRepository.save(notificacao);
    }

    private List<Usuario> destinatariosAutorizados(ProcessoEstagio processo) {
        Map<Long, Usuario> destinatarios = new LinkedHashMap<>();
        adicionar(destinatarios, processo.getAluno());

        if (processo.getInstituicao() != null && processo.getInstituicao().getId() != null) {
            usuarioRepository.findAllByInstituicaoId(processo.getInstituicao().getId())
                    .forEach(usuario -> adicionar(destinatarios, usuario));
        }

        vinculoRepository.findFirstByProcessoIdOrderByCriadoEmDescIdDesc(processo.getId())
                .filter(this::vinculoConfirmado)
                .map(VinculoSupervisorProcesso::getSupervisor)
                .ifPresent(usuario -> adicionar(destinatarios, usuario));
        return new ArrayList<>(destinatarios.values());
    }

    private boolean vinculoConfirmado(VinculoSupervisorProcesso vinculo) {
        return vinculo.getSituacao() == SituacaoVinculoSupervisor.CONFIRMADO
                && Boolean.TRUE.equals(vinculo.getAtivo())
                && vinculo.getSupervisor() != null;
    }

    private void adicionar(Map<Long, Usuario> destinatarios, Usuario usuario) {
        if (usuario != null && usuario.getId() != null) {
            destinatarios.putIfAbsent(usuario.getId(), usuario);
        }
    }

    private List<Notificacao> visiveis(Usuario usuario) {
        return notificacaoRepository
                .findAllByDestinatarioIdOrderByCriadaEmDescIdDesc(usuario.getId())
                .stream()
                .filter(this::processoAindaVisivel)
                .toList();
    }

    private boolean processoAindaVisivel(Notificacao notificacao) {
        return notificacao.getProcessoId() == null
                || processoRepository.findById(notificacao.getProcessoId())
                .map(processo -> possuiAcessoSemExporErro(
                        processo, notificacao.getDestinatario()))
                .orElse(false);
    }

    private boolean possuiAcessoSemExporErro(
            ProcessoEstagio processo, Usuario usuario) {
        return autorizacaoService.possuiAcesso(processo, usuario);
    }

}
