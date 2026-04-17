package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.GastoDetalhe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoDetalheRepository extends JpaRepository<GastoDetalhe, Long> {
}
