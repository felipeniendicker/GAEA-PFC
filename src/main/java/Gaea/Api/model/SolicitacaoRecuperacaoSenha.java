package Gaea.Api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacao_recuperacao_senha")
public class SolicitacaoRecuperacaoSenha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 64)
    private String identificadorHash;

    @Column(nullable = false, length = 64)
    private String origemHash;

    @Column(unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime expiraEm;
    private LocalDateTime utilizadoEm;
    private LocalDateTime canceladoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getIdentificadorHash() { return identificadorHash; }
    public void setIdentificadorHash(String identificadorHash) { this.identificadorHash = identificadorHash; }
    public String getOrigemHash() { return origemHash; }
    public void setOrigemHash(String origemHash) { this.origemHash = origemHash; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
    public LocalDateTime getUtilizadoEm() { return utilizadoEm; }
    public void setUtilizadoEm(LocalDateTime utilizadoEm) { this.utilizadoEm = utilizadoEm; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(LocalDateTime canceladoEm) { this.canceladoEm = canceladoEm; }
    public Long getVersao() { return versao; }
}
