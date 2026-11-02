package Gaea.Api.service;

import Gaea.Api.dto.CadastroUsuarioRequest;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario cadastrar(CadastroUsuarioRequest dados) {
        String email = normalizarEmail(dados.email());

        if (usuarioRepository.existsByEmailNormalizado(email)) {
            throw new ConflitoDadosException("E-mail já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dados.nome().trim());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dados.senha()));
        usuario.setPerfil(dados.perfil());
        usuario.setTermosAceitos(true);
        usuario.setTermosAceitosEm(LocalDateTime.now());

        return usuarioRepository.save(usuario);
    }

    public String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
