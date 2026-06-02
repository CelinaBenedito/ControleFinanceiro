package controle.api.back_end.adapters.outbound.repository.configuracoes;

import controle.api.back_end.domain.categoria.CategoriaUsuario;
import controle.api.back_end.domain.configuracoes.Configuracoes;
import controle.api.back_end.domain.configuracoes.LimitePorCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LimitePorCategoriaRepository extends JpaRepository<LimitePorCategoria, UUID> {
    List<LimitePorCategoria> findLimitePorCategoriaByConfiguracoes_Id(UUID configuracoesId);

    LimitePorCategoria findByConfiguracoesAndCategoriaUsuario(Configuracoes configuracao, CategoriaUsuario categoriaUsuario);

    List<LimitePorCategoria> findByCategoriaUsuario_Usuario_Id(UUID userId);
}
