package controle.api.back_end.repository.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.RecorrenciaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecorrenciaFinanceiraRepository extends JpaRepository<RecorrenciaFinanceira, UUID> {

    /** Todas as recorrências de um usuário */
    List<RecorrenciaFinanceira> findByUsuario_Id(UUID userId);

    /** Recorrências vinculadas a uma instituição específica do usuário */
    @Query("SELECT DISTINCT ei.recorrenciaFinanceira FROM EventoInstituicao ei " +
           "WHERE ei.recorrenciaFinanceira IS NOT NULL " +
           "AND ei.instituicaoUsuario.id = :instUsuarioId")
    List<RecorrenciaFinanceira> findByInstituicaoUsuarioId(@Param("instUsuarioId") Integer instUsuarioId);
}

