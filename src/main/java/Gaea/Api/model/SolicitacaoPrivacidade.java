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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacao_privacidade")
public class SolicitacaoPrivacidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSolicitacaoPrivacidade tipo;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoSolicitacaoPrivacidade situacao;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_usuario_id")
    private Usuario responsavel;

    @Column(length = 2000)
    private String decisao;

    @Column(length = 2000)
    private String evidenciaExecucao;

    @Version
    @Column(nullable = false)
    private Long versao;

    @PrePersist
    void prepararInclusao() {
        if (situacao == null) {
            situacao = SituacaoSolicitacaoPrivacidade.PENDENTE;
        }
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (atualizadoEm == null) {
            atualizadoEm = criadoEm;
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public TipoSolicitacaoPrivacidade getTipo() {
        return tipo;
    }

    public void setTipo(TipoSolicitacaoPrivacidade tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public SituacaoSolicitacaoPrivacidade getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoSolicitacaoPrivacidade situacao) {
        this.situacao = situacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Usuario responsavel) {
        this.responsavel = responsavel;
    }

    public String getDecisao() {
        return decisao;
    }

    public void setDecisao(String decisao) {
        this.decisao = decisao;
    }

    public String getEvidenciaExecucao() {
        return evidenciaExecucao;
    }

    public void setEvidenciaExecucao(String evidenciaExecucao) {
        this.evidenciaExecucao = evidenciaExecucao;
    }
}
