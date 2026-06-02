package controle.api.back_end.adapters.outbound.repository.configuracoes;

import controle.api.back_end.domain.configuracoes.Configuracoes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfiguracoesRepository extends JpaRepository<Configuracoes, UUID> {

    boolean existsConfiguracoesByUsuario_Id(UUID fkUsuarioId);

    Optional<Configuracoes> findConfiguracoesByUsuario_Id(UUID userId);


    List<Configuracoes> findAllByUsuario_Id(UUID userId);
}
