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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
public class DocumentoProcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private ProcessoEstagio processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enviado_por_usuario_id", nullable = false)
    private Usuario enviadoPor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_anterior_id", unique = true)
    private DocumentoProcesso documentoAnterior;

    @Column(nullable = false)
    private String nomeOriginal;

    @Column(nullable = false, unique = true)
    private String chaveArmazenamento;

    @Column(nullable = false, length = 100)
    private String tipoConteudo;

    @Column(nullable = false)
    private Long tamanhoBytes;

    @Column(nullable = false, length = 64)
    private String hashSha256;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoDocumento situacao;

    @Column(nullable = false)
    private LocalDateTime enviadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analisado_por_usuario_id")
    private Usuario analisadoPor;

    private LocalDateTime analisadoEm;

    @Column(length = 1000)
    private String justificativaReprovacao;

    @Version
    @Column(nullable = false)
    private Long versao;

    @PrePersist
    void prepararInclusao() {
        if (situacao == null) {
            situacao = SituacaoDocumento.PENDENTE;
        }
        if (enviadoEm == null) {
            enviadoEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public ProcessoEstagio getProcesso() { return processo; }
    public void setProcesso(ProcessoEstagio processo) { this.processo = processo; }
    public Usuario getEnviadoPor() { return enviadoPor; }
    public void setEnviadoPor(Usuario enviadoPor) { this.enviadoPor = enviadoPor; }
    public DocumentoProcesso getDocumentoAnterior() { return documentoAnterior; }
    public void setDocumentoAnterior(DocumentoProcesso documentoAnterior) { this.documentoAnterior = documentoAnterior; }
    public String getNomeOriginal() { return nomeOriginal; }
    public void setNomeOriginal(String nomeOriginal) { this.nomeOriginal = nomeOriginal; }
    public String getChaveArmazenamento() { return chaveArmazenamento; }
    public void setChaveArmazenamento(String chaveArmazenamento) { this.chaveArmazenamento = chaveArmazenamento; }
    public String getTipoConteudo() { return tipoConteudo; }
    public void setTipoConteudo(String tipoConteudo) { this.tipoConteudo = tipoConteudo; }
    public Long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public String getHashSha256() { return hashSha256; }
    public void setHashSha256(String hashSha256) { this.hashSha256 = hashSha256; }
    public SituacaoDocumento getSituacao() { return situacao; }
    public void setSituacao(SituacaoDocumento situacao) { this.situacao = situacao; }
    public LocalDateTime getEnviadoEm() { return enviadoEm; }
    public Usuario getAnalisadoPor() { return analisadoPor; }
    public void setAnalisadoPor(Usuario analisadoPor) { this.analisadoPor = analisadoPor; }
    public LocalDateTime getAnalisadoEm() { return analisadoEm; }
    public void setAnalisadoEm(LocalDateTime analisadoEm) { this.analisadoEm = analisadoEm; }
    public String getJustificativaReprovacao() { return justificativaReprovacao; }
    public void setJustificativaReprovacao(String justificativaReprovacao) { this.justificativaReprovacao = justificativaReprovacao; }
    public Long getVersao() { return versao; }
}
