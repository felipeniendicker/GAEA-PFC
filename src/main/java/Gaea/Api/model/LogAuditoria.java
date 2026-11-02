package Gaea.Api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuario;
    private String acao;
    private String recurso;
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    public LogAuditoria() {
    }

    public LogAuditoria(String usuario, String acao, String recurso) {
        this(usuario, acao, recurso, null);
    }

    public LogAuditoria(String usuario, String acao, String recurso, Instituicao instituicao) {
        this.usuario = usuario;
        this.acao = acao;
        this.recurso = recurso;
        this.dataHora = LocalDateTime.now();
        this.instituicao = instituicao;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(Instituicao instituicao) {
        this.instituicao = instituicao;
    }
}
