package controle.api.back_end.repository;

import controle.api.back_end.model.configuracoes.Configuracoes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConfiguracoesRepository extends JpaRepository<Configuracoes, UUID> {

    boolean existsConfiguracoesByUsuario_Id(UUID fkUsuarioId);
}
