package controle.api.back_end.repository.eventoFinanceiro;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.usuario.Usuario;
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

    List<EventoFinanceiro> findEventoFinanceiroByEventoInstituicoes_InstituicaoUsuario_Id(Integer eventoInstituicaoInstituicaoUsuarioId);

    List<EventoFinanceiro> findEventoFinanceiroByUsuario(Usuario usuario);

    void deleteEventoFinanceiroById(UUID id);

    EventoFinanceiro findEventoFinanceiroByEventoInstituicoes_Id(Integer id);

    List<EventoFinanceiro> findEventoFinanceiroByUsuarioOrderByDataEventoDesc(Usuario usuario);

    List<EventoFinanceiro> findAllByUsuario(Usuario usuario);

    List<EventoFinanceiro> findEventoFinanceiroByUsuario_Id(UUID usuarioId);

    @Query("SELECT e FROM EventoFinanceiro e WHERE e.usuario.id = :userId AND e.dataEvento BETWEEN :inicio AND :fim  ORDER BY e.dataEvento ASC")
    List<EventoFinanceiro> findByUsuarioAndPeriodoFiscal(@Param("userId") UUID userId,
                                                         @Param("inicio") LocalDate inicio,
                                                         @Param("fim") LocalDate fim);

    List<EventoFinanceiro> findAllByUsuario_Id(UUID usuarioId);

    List<EventoFinanceiro> findEventoFinanceiroByUsuario_IdAndDataEventoBetween(UUID usuarioId, LocalDate inicio, LocalDate fim);

    // ── Navegação por calendário ──────────────────────────────────────────────

    /** Anos distintos que o usuário possui registros (mais recente primeiro). */
    @Query("SELECT DISTINCT YEAR(e.dataEvento) FROM EventoFinanceiro e WHERE e.usuario.id = :userId ORDER BY YEAR(e.dataEvento) DESC")
    List<Integer> findDistinctAnosByUserId(@Param("userId") UUID userId);

    /** Meses distintos que o usuário possui registros em determinado ano (ordem crescente). */
    @Query("SELECT DISTINCT MONTH(e.dataEvento) FROM EventoFinanceiro e WHERE e.usuario.id = :userId AND YEAR(e.dataEvento) = :ano ORDER BY MONTH(e.dataEvento) ASC")
    List<Integer> findDistinctMesesByUserIdAndAno(@Param("userId") UUID userId, @Param("ano") int ano);

    /**
     * Todos os eventos de um mês/ano com EventoDetalhe pré-carregado (JOIN FETCH),
     * evitando N+1 na ordenação por título.
     */
    @Query("SELECT e FROM EventoFinanceiro e LEFT JOIN FETCH e.eventoDetalhe WHERE e.usuario.id = :userId AND YEAR(e.dataEvento) = :ano AND MONTH(e.dataEvento) = :mes")
    List<EventoFinanceiro> findByUserIdAndAnoAndMes(@Param("userId") UUID userId, @Param("ano") int ano, @Param("mes") int mes);

    /** Soma dos valores de eventos do tipo Poupança vinculados a uma caixinha específica. */
    @Query("SELECT COALESCE(SUM(e.valor), 0) FROM EventoFinanceiro e WHERE e.caixinha.id = :caixinhaId AND e.tipo = controle.api.back_end.model.eventoFinanceiro.Tipo.Poupanca")
    java.math.BigDecimal sumValorByCaixinha(@Param("caixinhaId") java.util.UUID caixinhaId);

    /** Eventos do tipo Poupança vinculados a uma caixinha. */
    List<EventoFinanceiro> findAllByCaixinha_Id(java.util.UUID caixinhaId);
}
