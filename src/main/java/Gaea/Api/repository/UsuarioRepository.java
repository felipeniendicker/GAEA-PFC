package Gaea.Api.repository;

import Gaea.Api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    @Query("select u from Usuario u where lower(trim(u.email)) = :email")
    Optional<Usuario> findByEmailNormalizado(@Param("email") String email);

    @Query("select count(u) > 0 from Usuario u where lower(trim(u.email)) = :email")
    boolean existsByEmailNormalizado(@Param("email") String email);
}
