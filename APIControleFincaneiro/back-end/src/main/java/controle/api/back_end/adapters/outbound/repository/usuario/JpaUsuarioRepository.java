package controle.api.back_end.adapters.outbound.repository.usuario;

import controle.api.back_end.adapters.outbound.entitys.JpaUsuarioEntity;
import controle.api.back_end.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaUsuarioRepository extends JpaRepository<JpaUsuarioEntity, UUID> {
    List<Usuario> findUsuarioByEmailAndSenha(String email, String senha);
}
