package Gaea.Api.service;

import Gaea.Api.dto.CadastroProcessoRequest;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessoEstagioServiceTests {

    @Mock
    private ProcessoEstagioRepository repository;

    @InjectMocks
    private ProcessoEstagioService service;

    @Test
    void deveDefinirStatusInicialNoBackend() {
        CadastroProcessoRequest request = new CadastroProcessoRequest(
                "  Ana Souza  ",
                "  ANA@EXAMPLE.COM ",
                "  Empresa Exemplo  ",
                "11.222.333/0001-81",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 4, 30));

        when(repository.save(any(ProcessoEstagio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProcessoEstagio processo = service.cadastrar(request);

        assertNull(processo.getId());
        assertEquals("PENDENTE", processo.getStatus());
        assertEquals("Ana Souza", processo.getNomeAluno());
        assertEquals("ana@example.com", processo.getEmailAluno());
        assertEquals("Empresa Exemplo", processo.getNomeEmpresa());
        assertEquals("11222333000181", processo.getCnpjEmpresa());
    }
}
