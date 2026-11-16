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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao", uniqueConstraints = @UniqueConstraint(
        name = "uk_notificacao_destinatario_chave",
        columnNames = {"destinatario_usuario_id", "chave_idempotencia"}))
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario destinatario;

    @Column(nullable = false, length = 60)
    private String tipoEvento;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensagem;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    @Column(nullable = false)
    private boolean lida;

    private LocalDateTime lidaEm;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(nullable = false, length = 255)
    private String chaveIdempotencia;

    @PrePersist
    void prepararInclusao() {
        if (criadaEm == null) {
            criadaEm = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Usuario getDestinatario() { return destinatario; }
    public void setDestinatario(Usuario destinatario) { this.destinatario = destinatario; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public boolean isLida() { return lida; }
    public LocalDateTime getLidaEm() { return lidaEm; }
    public Long getProcessoId() { return processoId; }
    public void setProcessoId(Long processoId) { this.processoId = processoId; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }

    public void marcarComoLida() {
        if (!lida) {
            lida = true;
            lidaEm = LocalDateTime.now();
        }
    }
}
