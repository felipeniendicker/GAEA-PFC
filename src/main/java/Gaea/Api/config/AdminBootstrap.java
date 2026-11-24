package Gaea.Api.config;

import Gaea.Api.model.DocumentosLegais;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.PoliticaSenhaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "app.admin-bootstrap.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {
    static final String CONFIRMACAO = "GAEA_CREATE_INITIAL_ADMIN";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final PoliticaSenhaService politica;
    private final String nome;
    private final String email;
    private final String senha;
    private final String confirmacao;

    public AdminBootstrap(
            UsuarioRepository usuarios,
            PasswordEncoder encoder,
            PoliticaSenhaService politica,
            @Value("${app.admin-bootstrap.nome:}") String nome,
            @Value("${app.admin-bootstrap.email:}") String email,
            @Value("${app.admin-bootstrap.senha:}") String senha,
            @Value("${app.admin-bootstrap.confirmacao:}") String confirmacao) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.politica = politica;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.confirmacao = confirmacao;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validarConfiguracao();
        politica.validar(senha);

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmailNormalizado(emailNormalizado)) {
            return;
        }
        if (usuarios.findAll().stream().anyMatch(usuario -> "ADMIN_GAEA".equals(usuario.getPerfil()))) {
            throw new IllegalStateException("Já existe administrador GAEA; bootstrap recusado");
        }

        LocalDateTime agora = LocalDateTime.now();
        Usuario administrador = new Usuario();
        administrador.setNome(nome.trim());
        administrador.setEmail(emailNormalizado);
        administrador.setSenha(encoder.encode(senha));
        administrador.setPerfil("ADMIN_GAEA");
        administrador.setAtivo(true);
        administrador.setAtivado(true);
        administrador.setEmailVerificadoEm(agora);
        DocumentosLegais.registrarAceiteAtual(administrador, agora);
        usuarios.save(administrador);
    }

    private void validarConfiguracao() {
        if (!CONFIRMACAO.equals(confirmacao)) {
            throw new IllegalStateException("Bootstrap administrativo sem confirmação explícita");
        }
        if (nome == null || nome.isBlank() || email == null || email.isBlank() || senha == null) {
            throw new IllegalStateException("Dados do administrador inicial são obrigatórios");
        }
    }
}
