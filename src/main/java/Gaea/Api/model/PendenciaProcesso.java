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

import java.time.LocalDateTime;

@Entity
public class PendenciaProcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private ProcessoEstagio processo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoPendencia situacao;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    private LocalDateTime resolvidaEm;

    @Column(nullable = false)
    private String responsavelRegistro;

    @Version
    @Column(nullable = false)
    private Long versao;

    @PrePersist
    void prepararInclusao() {
        if (situacao == null) {
            situacao = SituacaoPendencia.ABERTA;
        }
        if (criadaEm == null) {
            criadaEm = LocalDateTime.now();
        }
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

    public SituacaoPendencia getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoPendencia situacao) {
        this.situacao = situacao;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getResolvidaEm() {
        return resolvidaEm;
    }

    public void setResolvidaEm(LocalDateTime resolvidaEm) {
        this.resolvidaEm = resolvidaEm;
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
