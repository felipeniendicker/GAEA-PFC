package Gaea.Api.config;

import Gaea.Api.model.Usuario;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.PoliticaSenhaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Locale;

@Component
@ConditionalOnProperty(name="app.admin-bootstrap.enabled", havingValue="true")
public class AdminBootstrap implements ApplicationRunner {
    static final String CONFIRMACAO="GAEA_CREATE_INITIAL_ADMIN";
    private final UsuarioRepository usuarios; private final PasswordEncoder encoder; private final PoliticaSenhaService politica;
    private final String nome,email,senha,confirmacao;
    public AdminBootstrap(UsuarioRepository usuarios, PasswordEncoder encoder, PoliticaSenhaService politica,
            @Value("${app.admin-bootstrap.nome:}") String nome, @Value("${app.admin-bootstrap.email:}") String email,
            @Value("${app.admin-bootstrap.senha:}") String senha, @Value("${app.admin-bootstrap.confirmacao:}") String confirmacao) {
        this.usuarios=usuarios; this.encoder=encoder; this.politica=politica; this.nome=nome; this.email=email; this.senha=senha; this.confirmacao=confirmacao;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if (!CONFIRMACAO.equals(confirmacao)) throw new IllegalStateException("Bootstrap administrativo sem confirmação explícita");
        if (nome==null||nome.isBlank()||email==null||email.isBlank()||senha==null) throw new IllegalStateException("Dados do administrador inicial são obrigatórios");
        politica.validar(senha); String normalizado=email.trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmailNormalizado(normalizado)) return;
        if (usuarios.findAll().stream().anyMatch(u -> "ADMIN_GAEA".equals(u.getPerfil()))) throw new IllegalStateException("Já existe administrador GAEA; bootstrap recusado");
        Usuario u=new Usuario(); u.setNome(nome.trim()); u.setEmail(normalizado); u.setSenha(encoder.encode(senha)); u.setPerfil("ADMIN_GAEA");
        u.setAtivo(true); u.setAtivado(true); u.setTermosAceitos(true); u.setTermosAceitosEm(LocalDateTime.now()); u.setEmailVerificadoEm(LocalDateTime.now()); usuarios.save(u);
    }
}
