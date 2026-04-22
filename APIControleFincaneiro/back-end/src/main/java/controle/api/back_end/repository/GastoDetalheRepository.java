package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.GastoDetalhe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GastoDetalheRepository extends JpaRepository<GastoDetalhe, Long> {
    GastoDetalhe findGastoDetalheByEventoFinanceiro(EventoFinanceiro eventoFinanceiro);

    GastoDetalhe findGastoDetalheByEventoFinanceiro_Id(UUID eventoFinanceiroId);

    boolean existsGastoDetalheByEventoFinanceiro_Id(UUID eventoFinanceiroId);

    void deleteGastoDetalheByEventoFinanceiro_Id(UUID eventoId);
}
