package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoInstituicaoRepository extends JpaRepository<EventoInstituicao, Integer> {
}
