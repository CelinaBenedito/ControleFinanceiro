package controle.api.back_end.repository;

import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LimitePorCategoriaRepository extends JpaRepository<LimitePorCategoria, UUID> {
}
