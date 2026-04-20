package controle.api.back_end.repository;

import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.ScopedValue;
import java.util.List;
import java.util.UUID;

public interface LimitePorInstituicaoRepository extends JpaRepository<LimitePorInstituicao, UUID> {
    List<LimitePorInstituicao> findLimitePorInstituicaoByInstitucaoUsuario_Id(Integer institucaoUsuarioId);

    List<LimitePorInstituicao> findLimitePorInstituicaoByConfiguracoes_Id(UUID userId);

    LimitePorInstituicao findByConfiguracoesAndInstitucaoUsuario(Configuracoes configuracao, InstituicaoUsuario instituicaoUsuario);
}
