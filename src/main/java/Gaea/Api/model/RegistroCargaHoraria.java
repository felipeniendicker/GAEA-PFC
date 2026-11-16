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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_carga_horaria_duplicidade",
        columnNames = {"processo_id", "data_atividade", "duracao_minutos", "atividade_hash"}))
public class RegistroCargaHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private ProcessoEstagio processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_usuario_id", nullable = false)
    private Usuario registradoPor;

    @Column(nullable = false)
    private LocalDate dataAtividade;

    @Column(nullable = false)
    private Integer duracaoMinutos;

    @Column(nullable = false, length = 1000)
    private String atividade;

    @Column(nullable = false, length = 64)
    private String atividadeHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoCargaHoraria situacao;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliado_por_usuario_id")
    private Usuario avaliadoPor;

    private LocalDateTime avaliadoEm;

    @Column(length = 1000)
    private String justificativaRejeicao;

    @Version
    @Column(nullable = false)
    private Long versao;

    @PrePersist
    void prepararInclusao() {
        if (situacao == null) {
            situacao = SituacaoCargaHoraria.PENDENTE;
        }
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public ProcessoEstagio getProcesso() { return processo; }
    public void setProcesso(ProcessoEstagio processo) { this.processo = processo; }
    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario registradoPor) { this.registradoPor = registradoPor; }
    public LocalDate getDataAtividade() { return dataAtividade; }
    public void setDataAtividade(LocalDate dataAtividade) { this.dataAtividade = dataAtividade; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public String getAtividade() { return atividade; }
    public void setAtividade(String atividade) { this.atividade = atividade; }
    public String getAtividadeHash() { return atividadeHash; }
    public void setAtividadeHash(String atividadeHash) { this.atividadeHash = atividadeHash; }
    public SituacaoCargaHoraria getSituacao() { return situacao; }
    public void setSituacao(SituacaoCargaHoraria situacao) { this.situacao = situacao; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Usuario getAvaliadoPor() { return avaliadoPor; }
    public void setAvaliadoPor(Usuario avaliadoPor) { this.avaliadoPor = avaliadoPor; }
    public LocalDateTime getAvaliadoEm() { return avaliadoEm; }
    public void setAvaliadoEm(LocalDateTime avaliadoEm) { this.avaliadoEm = avaliadoEm; }
    public String getJustificativaRejeicao() { return justificativaRejeicao; }
    public void setJustificativaRejeicao(String justificativaRejeicao) {
        this.justificativaRejeicao = justificativaRejeicao;
    }
    public Long getVersao() { return versao; }
}
