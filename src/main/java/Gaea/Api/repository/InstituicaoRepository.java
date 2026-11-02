package Gaea.Api.repository;

import Gaea.Api.model.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstituicaoRepository extends JpaRepository<Instituicao, Long> {

    Optional<Instituicao> findByCnpj(String cnpj);
}
