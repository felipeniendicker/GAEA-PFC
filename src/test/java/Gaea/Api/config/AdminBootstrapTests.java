package Gaea.Api.config;

import Gaea.Api.model.DocumentosLegais;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.PoliticaSenhaService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBootstrapTests {
    @Test void deveExigirConfirmacaoExplicita() {
        AdminBootstrap bootstrap=new AdminBootstrap(mock(UsuarioRepository.class),mock(PasswordEncoder.class),mock(PoliticaSenhaService.class),"Admin","admin@gaea.example","Senha-Forte1!","incorreta");
        assertThrows(IllegalStateException.class,()->bootstrap.run(new DefaultApplicationArguments()));
    }
    @Test void deveCriarAdminSemSenhaFixaQuandoExplicitamenteAutorizado() throws Exception {
        UsuarioRepository repositorio=mock(UsuarioRepository.class); PasswordEncoder encoder=mock(PasswordEncoder.class); PoliticaSenhaService politica=mock(PoliticaSenhaService.class);
        when(encoder.encode("Senha-Forte1!")).thenReturn("hash"); when(repositorio.save(any())).thenAnswer(i->i.getArgument(0));
        AdminBootstrap bootstrap=new AdminBootstrap(repositorio,encoder,politica,"Admin GAEA","ADMIN@GAEA.EXAMPLE","Senha-Forte1!",AdminBootstrap.CONFIRMACAO);
        bootstrap.run(new DefaultApplicationArguments());
        verify(politica).validar("Senha-Forte1!");
        verify(encoder).encode("Senha-Forte1!");
        verify(repositorio).save(argThat(usuario ->
                "ADMIN_GAEA".equals(usuario.getPerfil())
                        && "admin@gaea.example".equals(usuario.getEmail())
                        && DocumentosLegais.VERSAO_TERMOS_USO.equals(usuario.getTermosUsoVersao())
                        && DocumentosLegais.VERSAO_POLITICA_PRIVACIDADE.equals(
                                usuario.getPoliticaPrivacidadeVersao())
                        && usuario.getTermosAceitosEm() != null));
    }
}
