package controle.api.back_end.repository.instituicao;

import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstituicaoUsuarioRepository extends JpaRepository<InstituicaoUsuario, Integer> {

    boolean existsByUsuarioAndInstituicao(Usuario usuario, Instituicao instituicao);
    
    InstituicaoUsuario findByUsuario_IdAndInstituicao_Id(UUID userId, Integer instituicaoId);

    List<InstituicaoUsuario> findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(UUID idUser);

    Page<InstituicaoUsuario> findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(UUID idUser, Pageable pageable);

    List<InstituicaoUsuario> findInstituicaoUsuarioByEventoInstituicao_Id(Integer id);

    Optional<InstituicaoUsuario> findByUsuario_IdAndInstituicao_Nome(UUID userId, String nome);
}
