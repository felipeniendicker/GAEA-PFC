package Gaea.Api.repository;

import Gaea.Api.model.RegistroCargaHoraria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegistroCargaHorariaRepository
        extends JpaRepository<RegistroCargaHoraria, Long> {

    boolean existsByProcessoIdAndDataAtividadeAndDuracaoMinutosAndAtividadeHash(
            Long processoId,
            LocalDate dataAtividade,
            Integer duracaoMinutos,
            String atividadeHash);

    List<RegistroCargaHoraria>
            findAllByProcessoIdOrderByDataAtividadeDescCriadoEmDescIdDesc(Long processoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from RegistroCargaHoraria r
            where r.id = :registroId and r.processo.id = :processoId
            """)
    Optional<RegistroCargaHoraria> findByIdAndProcessoIdParaAtualizacao(
            @Param("registroId") Long registroId,
            @Param("processoId") Long processoId);

    @Query("""
            select
                coalesce(sum(r.duracaoMinutos), 0),
                coalesce(sum(case when r.situacao = Gaea.Api.model.SituacaoCargaHoraria.APROVADO
                    then r.duracaoMinutos else 0 end), 0),
                coalesce(sum(case when r.situacao = Gaea.Api.model.SituacaoCargaHoraria.PENDENTE
                    then r.duracaoMinutos else 0 end), 0),
                coalesce(sum(case when r.situacao = Gaea.Api.model.SituacaoCargaHoraria.REJEITADO
                    then r.duracaoMinutos else 0 end), 0)
            from RegistroCargaHoraria r
            where r.processo.id = :processoId
            """)
    List<Object[]> calcularTotais(@Param("processoId") Long processoId);
}
