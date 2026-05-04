package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.usuario.Usuario;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventoFinanceiroRepository extends JpaRepository<EventoFinanceiro, UUID>,
                                                    JpaSpecificationExecutor<EventoFinanceiro> {
    List<EventoFinanceiro> getEventoFinanceirosByUsuario_id(UUID fkUsuarioId);

    List<EventoFinanceiro> findEventoFinanceiroByEventoInstituicao_InstituicaoUsuario_Id(Integer eventoInstituicaoInstituicaoUsuarioId);

    List<EventoFinanceiro> findEventoFinanceiroByUsuario(Usuario usuario);

    void deleteEventoFinanceiroById(UUID id);

    EventoFinanceiro findEventoFinanceiroByEventoInstituicao_Id(Integer id);

    List<EventoFinanceiro> findEventoFinanceiroByUsuarioOrderByDataEventoDesc(Usuario usuario);

    List<EventoFinanceiro> findAllByUsuario(Usuario usuario);

    List<EventoFinanceiro> findEventoFinanceiroByUsuario_Id(UUID usuarioId);

    @Query("SELECT e FROM EventoFinanceiro e WHERE e.usuario.id = :userId AND e.dataEvento BETWEEN :inicio AND :fim  ORDER BY e.dataEvento ASC")
    List<EventoFinanceiro> findByUsuarioAndPeriodoFiscal(@Param("userId") UUID userId,
                                                         @Param("inicio") LocalDate inicio,
                                                         @Param("fim") LocalDate fim);

    List<EventoFinanceiro> findAllByUsuario_Id(UUID usuarioId);

    List<EventoFinanceiro> findEventoFinanceiroByUsuario_IdAndDataEventoBetween(UUID usuarioId, LocalDate inicio, LocalDate fim);
}
