package controle.api.back_end.domain.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {
    Usuario save(Usuario usuario);

    Optional<Usuario> findById(UUID id);

    List<Usuario> findAll();

    void deleteById(UUID id);
}
