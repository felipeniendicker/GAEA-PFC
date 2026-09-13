package Gaea.Api.service;

import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessoEstagioService {

    private final ProcessoEstagioRepository repository;

    public ProcessoEstagioService(ProcessoEstagioRepository repository) {
        this.repository = repository;
    }

    public ProcessoEstagio cadastrar(ProcessoEstagio processo) {
        processo.setStatus("PENDENTE");
        return repository.save(processo);
    }

    public List<ProcessoEstagio> listarTodos() {
        return repository.findAll();
    }
    public ProcessoEstagio alterarStatus(Long id, String novoStatus) {

        if (!novoStatus.equals("PENDENTE")
            && !novoStatus.equals("EM_ANALISE")
            && !novoStatus.equals("APROVADO")
            && !novoStatus.equals("REPROVADO")) {

        throw new RuntimeException("Status inválido");
        }

        ProcessoEstagio processo = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Processo não encontrado"));

        processo.setStatus(novoStatus);

        return repository.save(processo);
    }
}