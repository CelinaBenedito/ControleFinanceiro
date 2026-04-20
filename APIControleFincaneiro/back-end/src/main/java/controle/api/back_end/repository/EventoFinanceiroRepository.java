package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoFinanceiroRepository extends JpaRepository<EventoFinanceiro, UUID> {
    List<EventoFinanceiro> getEventoFinanceirosByFkUsuario_Id(UUID fkUsuarioId);
}
