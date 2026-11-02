package Gaea.Api.service;

import Gaea.Api.dto.CadastroProcessoRequest;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ProcessoEstagioService {

    private final ProcessoEstagioRepository repository;

    public ProcessoEstagioService(ProcessoEstagioRepository repository) {
        this.repository = repository;
    }

    public ProcessoEstagio cadastrar(CadastroProcessoRequest dados) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno(dados.nomeAluno().trim());
        processo.setEmailAluno(dados.emailAluno().trim().toLowerCase(Locale.ROOT));
        processo.setNomeEmpresa(dados.nomeEmpresa().trim());
        processo.setCnpjEmpresa(dados.cnpjEmpresa().replaceAll("\\D", ""));
        processo.setDataInicio(dados.dataInicio());
        processo.setDataFim(dados.dataFim());
        processo.setStatus("PENDENTE");
        return repository.save(processo);
    }

    public List<ProcessoEstagio> listarTodos() {
        return repository.findAll();
    }

    public List<ProcessoEstagio> listarPorEmailAluno(String emailAluno) {
        return repository.findByEmailAluno(emailAluno);
    }

    public ProcessoEstagio alterarStatus(Long id, String novoStatus) {

        if (novoStatus == null || (!novoStatus.equals("PENDENTE")
            && !novoStatus.equals("EM_ANALISE")
            && !novoStatus.equals("APROVADO")
            && !novoStatus.equals("REPROVADO"))) {

        throw new DadosInvalidosException("Status inválido");
        }

        ProcessoEstagio processo = repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));

        processo.setStatus(novoStatus);

        return repository.save(processo);
    }
}
