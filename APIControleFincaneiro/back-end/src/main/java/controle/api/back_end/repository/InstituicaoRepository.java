package controle.api.back_end.repository;

import controle.api.back_end.model.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituicaoRepository extends JpaRepository <Instituicao, Integer> {
}
