package Gaea.Api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_fluxo_instituicao")
public class TokenFluxoInstituicao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "solicitacao_id") private SolicitacaoInstituicao solicitacao;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id") private Usuario usuario;
    @Column(nullable = false, length = 20) private String tipo;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private LocalDateTime criadoEm;
    @Column(nullable = false) private LocalDateTime expiraEm;
    private LocalDateTime utilizadoEm;
    private LocalDateTime canceladoEm;
    @Version @Column(nullable = false) private Long versao;
    public Long getId() { return id; }
    public SolicitacaoInstituicao getSolicitacao() { return solicitacao; }
    public void setSolicitacao(SolicitacaoInstituicao v) { solicitacao = v; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario v) { usuario = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String v) { tokenHash = v; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime v) { criadoEm = v; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime v) { expiraEm = v; }
    public LocalDateTime getUtilizadoEm() { return utilizadoEm; }
    public void setUtilizadoEm(LocalDateTime v) { utilizadoEm = v; }
    public LocalDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(LocalDateTime v) { canceladoEm = v; }
}
