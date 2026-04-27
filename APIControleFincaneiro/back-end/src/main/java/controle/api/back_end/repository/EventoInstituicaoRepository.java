package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoInstituicaoRepository extends JpaRepository<EventoInstituicao, Integer> {
    List<EventoInstituicao> findEventoInstituicaoByEventoFinanceiro_Id(UUID eventoId);

    boolean existsEventoInstituicaoByEventoFinanceiro_Id(UUID eventoId);

    void deleteEventoInstituicaoByEventoFinanceiro_Id(UUID eventoFinanceiroId);

    List<EventoInstituicao> findByInstituicaoUsuario_Id(Integer instituicaoUsuarioId);
}
