package controle.api.back_end.repository.categoria;

import controle.api.back_end.model.categoria.CategoriaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaUsuarioRepository extends JpaRepository<CategoriaUsuario, Integer> {
    List<CategoriaUsuario> findAllByUsuario_Id(UUID usuarioId);

    boolean existsByCategoria_IdAndUsuario_Id(Integer categoriaId, UUID usuarioId);

    boolean existsByCategoria_IdOrUsuario_Id(Integer categoriaId, UUID usuarioId);

    CategoriaUsuario findByUsuario_idAndCategoria_id(UUID usuarioId, Integer categoriaId);

    List<CategoriaUsuario> findAllByUsuario_IdAndIsAtivoIsTrue(UUID userId);

    Optional<CategoriaUsuario> findByUsuario_IdAndCategoria_Titulo(UUID userId, String titulo);
}
