package controle.api.back_end.repository;

import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LimitePorInstituicaoRepository extends JpaRepository<LimitePorInstituicao, UUID> {
    List<LimitePorInstituicao> findLimitePorInstituicaoByInstituicaoUsuario_Id(Integer institucaoUsuarioId);

    List<LimitePorInstituicao> findLimitePorInstituicaoByConfiguracoes_Id(UUID userId);

    LimitePorInstituicao findByConfiguracoesAndInstituicaoUsuario(Configuracoes configuracao, InstituicaoUsuario instituicaoUsuario);

    List<LimitePorInstituicao> findByInstituicaoUsuario_Usuario_Id(UUID userId);
}
