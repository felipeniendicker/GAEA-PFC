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
import jakarta.persistence.Version;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_vinculo_supervisor_ativo",
        columnNames = {"processo_id", "ativo"}))
public class VinculoSupervisorProcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private ProcessoEstagio processo;

    @Column(nullable = false)
    private String emailConvidado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_usuario_id")
    private Usuario supervisor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoVinculoSupervisor situacao;

    @Column(length = 1000)
    private String justificativaRecusa;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime respondidoEm;

    private Boolean ativo;

    @Version
    @Column(nullable = false)
    private Long versao;

    @PrePersist
    void prepararInclusao() {
        if (situacao == null) {
            situacao = SituacaoVinculoSupervisor.PENDENTE;
        }
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        sincronizarAtivo();
    }

    public Long getId() {
        return id;
    }

    public ProcessoEstagio getProcesso() {
        return processo;
    }

    public void setProcesso(ProcessoEstagio processo) {
        this.processo = processo;
    }

    public String getEmailConvidado() {
        return emailConvidado;
    }

    public void setEmailConvidado(String emailConvidado) {
        this.emailConvidado = emailConvidado == null
                ? null
                : emailConvidado.trim().toLowerCase(Locale.ROOT);
    }

    public Usuario getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Usuario supervisor) {
        this.supervisor = supervisor;
    }

    public SituacaoVinculoSupervisor getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoVinculoSupervisor situacao) {
        this.situacao = situacao;
        sincronizarAtivo();
    }

    public String getJustificativaRecusa() {
        return justificativaRecusa;
    }

    public void setJustificativaRecusa(String justificativaRecusa) {
        this.justificativaRecusa = justificativaRecusa;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getRespondidoEm() {
        return respondidoEm;
    }

    public void setRespondidoEm(LocalDateTime respondidoEm) {
        this.respondidoEm = respondidoEm;
    }

    public Long getVersao() {
        return versao;
    }

    public Boolean getAtivo() { return ativo; }

    private void sincronizarAtivo() {
        ativo = situacao == SituacaoVinculoSupervisor.PENDENTE
                || situacao == SituacaoVinculoSupervisor.CONFIRMADO
                ? Boolean.TRUE
                : null;
    }
}
