package controle.api.back_end.repository;

import controle.api.back_end.model.Instituicao;
import controle.api.back_end.model.InstituicaoUsuario;
import controle.api.back_end.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InstituicaoUsuarioRepository extends JpaRepository<InstituicaoUsuario, Integer> {
    List<InstituicaoUsuario> findInstituicaoUsuarioByFkUsuario_Id(UUID idUser);

    boolean existsByFkUsuarioAndFkInstituicao(Usuario usuario, Instituicao instituicao);
    
    InstituicaoUsuario findByFkUsuario_IdAndFkInstituicao_Id(UUID userId, Integer instituicaoId);

    List<InstituicaoUsuario> findInstituicaoUsuarioByFkUsuario_IdAndIsAtivoIsTrue(UUID idUser);
}
