package controle.api.back_end.adapters.outbound.repository.instituicao;

import controle.api.back_end.domain.instituicao.Instituicao;
import controle.api.back_end.domain.instituicao.InstituicaoUsuario;
import controle.api.back_end.domain.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InstituicaoUsuarioRepository extends JpaRepository<InstituicaoUsuario, Integer> {

    boolean existsByUsuarioAndInstituicao(Usuario usuario, Instituicao instituicao);
    
    InstituicaoUsuario findByUsuario_IdAndInstituicao_Id(UUID userId, Integer instituicaoId);

    List<InstituicaoUsuario> findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(UUID idUser);

    List<InstituicaoUsuario> findInstituicaoUsuarioByEventoInstituicao_Id(Integer id);
}
