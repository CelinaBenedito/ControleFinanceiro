package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface EventoFinanceiroRepository extends JpaRepository<EventoFinanceiro, UUID>,
                                                    JpaSpecificationExecutor<EventoFinanceiro> {
    List<EventoFinanceiro> getEventoFinanceirosByUsuario_id(UUID fkUsuarioId);
}
