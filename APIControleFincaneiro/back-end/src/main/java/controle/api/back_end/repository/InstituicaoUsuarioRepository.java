package controle.api.back_end.repository;

import controle.api.back_end.model.InstituicaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituicaoUsuarioRepository extends JpaRepository<InstituicaoUsuario, Integer> {
}
