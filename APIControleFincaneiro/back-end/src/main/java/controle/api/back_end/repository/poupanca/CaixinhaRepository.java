package controle.api.back_end.repository.poupanca;

import controle.api.back_end.model.poupanca.Caixinha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CaixinhaRepository extends JpaRepository<Caixinha, UUID> {

    List<Caixinha> findAllByUsuario_Id(UUID usuarioId);

    List<Caixinha> findAllByUsuario_IdAndIsAtivaTrue(UUID usuarioId);

    /** Caixinhas ativas de um usuário em uma instituição específica. */
    @Query("SELECT c FROM Caixinha c JOIN c.caixinhaInstituicoes ci WHERE c.usuario.id = :userId AND ci.instituicaoUsuario.id = :instId AND c.isAtiva = true")
    List<Caixinha> findAtivasByUsuarioAndInstituicao(@Param("userId") UUID userId, @Param("instId") Integer instId);
}

