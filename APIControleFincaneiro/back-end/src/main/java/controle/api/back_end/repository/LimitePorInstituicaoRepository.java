package controle.api.back_end.repository;

import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LimitePorInstituicaoRepository extends JpaRepository<LimitePorInstituicao, UUID> {
}
