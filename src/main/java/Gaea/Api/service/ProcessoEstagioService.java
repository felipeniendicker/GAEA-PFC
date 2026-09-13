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
}