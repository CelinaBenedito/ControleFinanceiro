package controle.api.back_end.adapters.outbound.repository.instituicao;

import controle.api.back_end.domain.instituicao.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituicaoRepository extends JpaRepository <Instituicao, Integer> {
    void deleteInstituicaoById(Integer id);

    Instituicao findInstituicaoByNomeContainingIgnoreCase(String nome);
}
