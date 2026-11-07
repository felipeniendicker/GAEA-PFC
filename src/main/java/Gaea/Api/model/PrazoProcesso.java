package Gaea.Api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class PrazoProcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private ProcessoEstagio processo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false)
    private LocalDate dataVencimento;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    @Column(nullable = false)
    private LocalDateTime atualizadaEm;

    @Column(nullable = false)
    private String responsavelRegistro;

    @Version
    @Column(nullable = false)
    private Long versao;

    @PrePersist
    void prepararInclusao() {
        LocalDateTime agora = LocalDateTime.now();
        if (criadaEm == null) {
            criadaEm = agora;
        }
        if (atualizadaEm == null) {
            atualizadaEm = agora;
        }
    }

    @PreUpdate
    void prepararAtualizacao() {
        atualizadaEm = LocalDateTime.now();
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao == null ? null : descricao.trim();
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getAtualizadaEm() {
        return atualizadaEm;
    }

    public void setAtualizadaEm(LocalDateTime atualizadaEm) {
        this.atualizadaEm = atualizadaEm;
    }

    public String getResponsavelRegistro() {
        return responsavelRegistro;
    }

    public void setResponsavelRegistro(String responsavelRegistro) {
        this.responsavelRegistro = responsavelRegistro;
    }

    public Long getVersao() {
        return versao;
    }
}
