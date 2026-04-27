package controle.api.back_end.repository;

import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface EventoFinanceiroRepository extends JpaRepository<EventoFinanceiro, UUID>,
                                                    JpaSpecificationExecutor<EventoFinanceiro> {
    List<EventoFinanceiro> getEventoFinanceirosByUsuario_id(UUID fkUsuarioId);

    List<EventoFinanceiro> findEventoFinanceiroByEventoInstituicao_InstituicaoUsuario_Id(Integer eventoInstituicaoInstituicaoUsuarioId);

    List<EventoFinanceiro> findEventoFinanceiroByUsuario(Usuario usuario);

    void deleteEventoFinanceiroById(UUID id);

    EventoFinanceiro findEventoFinanceiroByEventoInstituicao_Id(Integer id);
}
