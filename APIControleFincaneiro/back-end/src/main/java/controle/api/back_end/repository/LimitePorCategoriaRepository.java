package controle.api.back_end.repository;

import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LimitePorCategoriaRepository extends JpaRepository<LimitePorCategoria, CategoriaUsuario> {
}
