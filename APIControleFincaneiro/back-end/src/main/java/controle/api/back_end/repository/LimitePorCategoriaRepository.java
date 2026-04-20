package controle.api.back_end.repository;

import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LimitePorCategoriaRepository extends JpaRepository<LimitePorCategoria, UUID> {
    List<LimitePorCategoria> findLimitePorCategoriaByConfiguracoes_Id(UUID configuracoesId);

    LimitePorCategoria findByConfiguracoesAndCategoriaUsuario(Configuracoes configuracao, CategoriaUsuario categoriaUsuario);

}
