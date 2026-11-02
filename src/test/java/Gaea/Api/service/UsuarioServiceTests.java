package Gaea.Api.service;

import Gaea.Api.dto.CadastroUsuarioRequest;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTests {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveCadastrarUsuarioValidoComEmailNormalizadoESenhaProtegida() {
        CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                "  Ana Souza  ",
                "  ANA@EXAMPLE.COM ",
                "senha-segura",
                "ALUNO",
                true);

        when(usuarioRepository.existsByEmailNormalizado("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha-segura")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = usuarioService.cadastrar(request);

        assertEquals("Ana Souza", usuario.getNome());
        assertEquals("ana@example.com", usuario.getEmail());
        assertEquals("hash-bcrypt", usuario.getSenha());
        assertEquals("ALUNO", usuario.getPerfil());
        assertEquals(true, usuario.getTermosAceitos());
        assertNotNull(usuario.getTermosAceitosEm());
        verify(passwordEncoder).encode("senha-segura");
    }

    @Test
    void deveRejeitarEmailDuplicado() {
        CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                "Ana Souza",
                "ANA@example.com",
                "senha-segura",
                "ALUNO",
                true);

        when(usuarioRepository.existsByEmailNormalizado("ana@example.com")).thenReturn(true);

        assertThrows(ConflitoDadosException.class, () -> usuarioService.cadastrar(request));
        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
