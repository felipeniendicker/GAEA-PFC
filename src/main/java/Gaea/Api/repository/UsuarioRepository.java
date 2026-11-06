package Gaea.Api.repository;

import Gaea.Api.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdParaAtualizacao(@Param("id") Long id);

    @EntityGraph(attributePaths = "instituicao")
    @Query("select u from Usuario u where lower(trim(u.email)) = :email")
    Optional<Usuario> findByEmailNormalizado(@Param("email") String email);

    @Query("select count(u) > 0 from Usuario u where lower(trim(u.email)) = :email")
    boolean existsByEmailNormalizado(@Param("email") String email);

    List<Usuario> findAllByPerfilAndInstituicaoIdOrderByNomeAsc(
            String perfil,
            Long instituicaoId);

    Optional<Usuario> findByIdAndPerfilAndInstituicaoId(
            Long id,
            String perfil,
            Long instituicaoId);

    boolean existsByInstituicaoIdAndMatricula(Long instituicaoId, String matricula);

    boolean existsByInstituicaoIdAndMatriculaAndIdNot(
            Long instituicaoId,
            String matricula,
            Long id);
}
