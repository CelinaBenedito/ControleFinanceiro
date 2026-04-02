package controle.api.back_end.repository;

import controle.api.back_end.model.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    List<Usuario> findUsuarioByEmailAndSenha(String email, String senha);
}
