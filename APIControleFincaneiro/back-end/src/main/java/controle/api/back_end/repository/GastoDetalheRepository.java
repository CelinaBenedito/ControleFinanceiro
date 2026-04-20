package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.GastoDetalhe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoDetalheRepository extends JpaRepository<GastoDetalhe, Long> {
    List<GastoDetalhe> findGastoDetalheByEventoFinanceiro(EventoFinanceiro eventoFinanceiro);
}
