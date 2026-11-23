package Gaea.Api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacao_instituicao")
public class SolicitacaoInstituicao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String nomeInstitucional;
    @Column(nullable = false, unique = true, length = 14) private String cnpj;
    @Column(nullable = false) private String responsavelNome;
    @Column(nullable = false, unique = true) private String emailInstitucional;
    @Column(nullable = false, length = 64) private String origemHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatusSolicitacaoInstituicao status;
    private LocalDateTime emailVerificadoEm;
    @Column(length = 1000) private String justificativa;
    private LocalDateTime analisadaEm;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "analisada_por_usuario_id") private Usuario analisadaPor;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "instituicao_criada_id") private Instituicao instituicaoCriada;
    @Column(nullable = false) private LocalDateTime criadaEm;
    @Column(nullable = false) private LocalDateTime atualizadaEm;
    @Version @Column(nullable = false) private Long versao;

    public Long getId() { return id; }
    public String getNomeInstitucional() { return nomeInstitucional; }
    public void setNomeInstitucional(String v) { nomeInstitucional = v; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String v) { cnpj = v; }
    public String getResponsavelNome() { return responsavelNome; }
    public void setResponsavelNome(String v) { responsavelNome = v; }
    public String getEmailInstitucional() { return emailInstitucional; }
    public void setEmailInstitucional(String v) { emailInstitucional = v; }
    public String getOrigemHash() { return origemHash; }
    public void setOrigemHash(String v) { origemHash = v; }
    public StatusSolicitacaoInstituicao getStatus() { return status; }
    public void setStatus(StatusSolicitacaoInstituicao v) { status = v; }
    public LocalDateTime getEmailVerificadoEm() { return emailVerificadoEm; }
    public void setEmailVerificadoEm(LocalDateTime v) { emailVerificadoEm = v; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String v) { justificativa = v; }
    public LocalDateTime getAnalisadaEm() { return analisadaEm; }
    public void setAnalisadaEm(LocalDateTime v) { analisadaEm = v; }
    public Usuario getAnalisadaPor() { return analisadaPor; }
    public void setAnalisadaPor(Usuario v) { analisadaPor = v; }
    public Instituicao getInstituicaoCriada() { return instituicaoCriada; }
    public void setInstituicaoCriada(Instituicao v) { instituicaoCriada = v; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDateTime v) { criadaEm = v; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public void setAtualizadaEm(LocalDateTime v) { atualizadaEm = v; }
}
