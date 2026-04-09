package controle.api.back_end.repository;

import controle.api.back_end.model.categoria.CategoriaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaUsuarioRepository extends JpaRepository<CategoriaUsuario, Integer> {
    List<CategoriaUsuario> findAllByUsuario_Id(UUID usuarioId);

    boolean existsByCategoria_IdAndUsuario_Id(Integer categoriaId, UUID usuarioId);

    boolean existsByCategoria_IdOrUsuario_Id(Integer categoriaId, UUID usuarioId);

    CategoriaUsuario findByUsuario_idAndCategoria_id(UUID usuarioId, Integer categoriaId);
}
