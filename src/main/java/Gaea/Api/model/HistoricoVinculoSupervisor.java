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
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;

@Entity
public class HistoricoVinculoSupervisor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinculo_id", nullable = false)
    private VinculoSupervisorProcesso vinculo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SituacaoVinculoSupervisor situacaoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoVinculoSupervisor situacaoNova;

    @Column(nullable = false)
    private String usuarioResponsavel;

    @Column(length = 1000)
    private String justificativa;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @PrePersist
    void prepararInclusao() {
        if (dataHora == null) dataHora = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public VinculoSupervisorProcesso getVinculo() { return vinculo; }
    public void setVinculo(VinculoSupervisorProcesso vinculo) { this.vinculo = vinculo; }
    public SituacaoVinculoSupervisor getSituacaoAnterior() { return situacaoAnterior; }
    public void setSituacaoAnterior(SituacaoVinculoSupervisor situacaoAnterior) { this.situacaoAnterior = situacaoAnterior; }
    public SituacaoVinculoSupervisor getSituacaoNova() { return situacaoNova; }
    public void setSituacaoNova(SituacaoVinculoSupervisor situacaoNova) { this.situacaoNova = situacaoNova; }
    public String getUsuarioResponsavel() { return usuarioResponsavel; }
    public void setUsuarioResponsavel(String usuarioResponsavel) { this.usuarioResponsavel = usuarioResponsavel; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    public LocalDateTime getDataHora() { return dataHora; }
}
