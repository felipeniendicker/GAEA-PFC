package Gaea.Api.repository;

import Gaea.Api.model.ProcessoEstagio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessoEstagioRepository extends JpaRepository<ProcessoEstagio, Long> {

    List<ProcessoEstagio> findByEmailAluno(String emailAluno);
}
