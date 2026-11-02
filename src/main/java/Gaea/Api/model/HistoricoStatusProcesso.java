package Gaea.Api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_status_processo")
public class HistoricoStatusProcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private ProcessoEstagio processo;

    @Column(nullable = false, length = 20)
    private String statusAnterior;

    @Column(nullable = false, length = 20)
    private String statusNovo;

    @Column(nullable = false)
    private String usuarioResponsavel;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 1000)
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoOperacaoStatus tipoOperacao;

    public HistoricoStatusProcesso() {
    }

    public HistoricoStatusProcesso(
            ProcessoEstagio processo,
            String statusAnterior,
            String statusNovo,
            String usuarioResponsavel,
            String justificativa,
            TipoOperacaoStatus tipoOperacao) {

        this.processo = processo;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.usuarioResponsavel = usuarioResponsavel;
        this.dataHora = LocalDateTime.now();
        this.justificativa = justificativa;
        this.tipoOperacao = tipoOperacao;
    }

    public Long getId() {
        return id;
    }

    public ProcessoEstagio getProcesso() {
        return processo;
    }

    public String getStatusAnterior() {
        return statusAnterior;
    }

    public String getStatusNovo() {
        return statusNovo;
    }

    public String getUsuarioResponsavel() {
        return usuarioResponsavel;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public TipoOperacaoStatus getTipoOperacao() {
        return tipoOperacao;
    }
}
