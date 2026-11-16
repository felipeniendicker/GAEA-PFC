package Gaea.Api.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class PlanoAtividades {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false, unique = true)
    private ProcessoEstagio processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_usuario_id", nullable = false)
    private Usuario criadoPor;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal cargaHorariaPlanejada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoPlanoAtividades situacao;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliado_por_usuario_id")
    private Usuario avaliadoPor;

    private LocalDateTime avaliadoEm;

    @Column(length = 1000)
    private String justificativaRejeicao;

    @OneToMany(mappedBy = "plano", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<AtividadePlano> atividades = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private Long versao;

    @PrePersist
    void prepararInclusao() {
        if (situacao == null) {
            situacao = SituacaoPlanoAtividades.PENDENTE;
        }
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public ProcessoEstagio getProcesso() { return processo; }
    public void setProcesso(ProcessoEstagio processo) { this.processo = processo; }
    public Usuario getCriadoPor() { return criadoPor; }
    public void setCriadoPor(Usuario criadoPor) { this.criadoPor = criadoPor; }
    public BigDecimal getCargaHorariaPlanejada() { return cargaHorariaPlanejada; }
    public void setCargaHorariaPlanejada(BigDecimal cargaHorariaPlanejada) {
        this.cargaHorariaPlanejada = cargaHorariaPlanejada;
    }
    public SituacaoPlanoAtividades getSituacao() { return situacao; }
    public void setSituacao(SituacaoPlanoAtividades situacao) { this.situacao = situacao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Usuario getAvaliadoPor() { return avaliadoPor; }
    public void setAvaliadoPor(Usuario avaliadoPor) { this.avaliadoPor = avaliadoPor; }
    public LocalDateTime getAvaliadoEm() { return avaliadoEm; }
    public void setAvaliadoEm(LocalDateTime avaliadoEm) { this.avaliadoEm = avaliadoEm; }
    public String getJustificativaRejeicao() { return justificativaRejeicao; }
    public void setJustificativaRejeicao(String justificativaRejeicao) {
        this.justificativaRejeicao = justificativaRejeicao;
    }
    public List<AtividadePlano> getAtividades() { return atividades; }
    public Long getVersao() { return versao; }

    public void adicionarAtividade(String descricao, int ordem) {
        AtividadePlano atividade = new AtividadePlano();
        atividade.setPlano(this);
        atividade.setDescricao(descricao);
        atividade.setOrdem(ordem);
        atividades.add(atividade);
    }
}
