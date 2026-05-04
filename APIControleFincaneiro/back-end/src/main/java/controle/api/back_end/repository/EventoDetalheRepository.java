package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoDetalheRepository extends JpaRepository<EventoDetalhe, Long> {
    EventoDetalhe findGastoDetalheByEventoFinanceiro(EventoFinanceiro eventoFinanceiro);

    EventoDetalhe findGastoDetalheByEventoFinanceiro_Id(UUID eventoFinanceiroId);

    boolean existsGastoDetalheByEventoFinanceiro_Id(UUID eventoFinanceiroId);

    void deleteGastoDetalheByEventoFinanceiro_Id(UUID eventoId);
}
