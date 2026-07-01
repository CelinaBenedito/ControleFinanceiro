package controle.api.back_end.repository.poupanca;

import controle.api.back_end.model.poupanca.CaixinhaInstituicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaixinhaInstituicaoRepository extends JpaRepository<CaixinhaInstituicao, Integer> {

    List<CaixinhaInstituicao> findAllByCaixinha_Id(UUID caixinhaId);

    boolean existsByCaixinha_IdAndInstituicaoUsuario_Id(UUID caixinhaId, Integer instId);
}

