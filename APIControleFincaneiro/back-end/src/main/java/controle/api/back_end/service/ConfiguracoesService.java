package controle.api.back_end.service;

import controle.api.back_end.dto.configuracoes.ConfiguracaoEditDTO;
import controle.api.back_end.dto.configuracoes.ConfiguracoesCreateDTO;
import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConfiguracoesService {

    private final ConfiguracoesRepository configuracoesRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;
    private final LimitePorCategoriaRepository limitePorCategoriaRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final LimitePorInstituicaoRepository limitePorInstiuicaoRepository;

    public ConfiguracoesService(ConfiguracoesRepository configuracoesRepository,
                                UsuarioRepository usuarioRepository,
                                CategoriaUsuarioRepository categoriaUsuarioRepository,
                                LimitePorCategoriaRepository limitePorCategoriaRepository,
                                InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                                LimitePorInstituicaoRepository limitePorInstiuicaoRepository) {
        this.configuracoesRepository = configuracoesRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
        this.limitePorCategoriaRepository = limitePorCategoriaRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.limitePorInstiuicaoRepository = limitePorInstiuicaoRepository;
    }

    public List<Configuracoes> getConfiguracoes() {
        return configuracoesRepository.findAll();
    }

    public Configuracoes getConfiguracoesById(UUID id) {
        return configuracoesRepository.findById(id)
                .orElseThrow(()->
                        new EntidadeNaoEncontradaException(
                                "Configuração de id: %s não encontrada"
                                        .formatted(id)
                        )
                );
    }

    public Configuracoes createConfiguracao(Configuracoes entity, ConfiguracoesCreateDTO createDto) {
        Usuario user = usuarioRepository.findById(createDto.getFkUsuario())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado"));

        if (configuracoesRepository.existsConfiguracoesByUsuario_Id(createDto.getFkUsuario())) {
            throw new EntidadeJaExisteException("Já existe uma configuração associada ao usuário informado");
        }

        entity.setUsuario(user);
        entity.setUltimaAtualizacao(LocalDate.now());

        return configuracoesRepository.save(entity);
    }


    public Configuracoes editConfiguracao(Configuracoes entity,
                                          @Valid ConfiguracaoEditDTO editDTO,
                                          UUID id) {

        Configuracoes user = configuracoesRepository.findById(id)
                .orElseThrow(() ->
                                new EntidadeNaoEncontradaException(
                                        "Configuração de ID: %d não encontrada."
                                                .formatted(id)
                                )
                );

        entity.setId(id);
        entity.setUsuario(user.getUsuario());

        // Limites por categoria
        if (editDTO.getLimitesCategoria() != null) {
            List<LimitePorCategoria> limitesCategoria = new ArrayList<>();
            for (ConfiguracaoEditDTO.LimiteCategoriaEditDTO limiteDTO : editDTO.getLimitesCategoria()) {
                CategoriaUsuario categoriaUsuario = categoriaUsuarioRepository.findByUsuario_idAndCategoria_id(
                        user.getId(), limiteDTO.getCategoriaId());

                LimitePorCategoria limite = new LimitePorCategoria();
                limite.setCategoriaUsuario(categoriaUsuario);
                limite.setLimiteDesejado(limiteDTO.getValor());
                limitePorCategoriaRepository.save(limite);

                limitesCategoria.add(limite);
            }
            entity.setLimitePorCategoria(limitesCategoria);
        }

        // Limites por instituição
        if (editDTO.getLimitesInstituicao() != null) {
            List<LimitePorInstituicao> limitesInstituicao = new ArrayList<>();
            for (ConfiguracaoEditDTO.LimiteInstituicaoEditDTO limiteDTO : editDTO.getLimitesInstituicao()) {
                InstituicaoUsuario instituicaoUsuario = instituicaoUsuarioRepository.findByUsuario_IdAndInstituicao_Id(
                        user.getId(), limiteDTO.getInstituicaoId());

                LimitePorInstituicao limite = new LimitePorInstituicao();
                limite.setInstitucaoUsuario(instituicaoUsuario);
                limite.setLimiteDesejado(limiteDTO.getValor());
                limitePorInstiuicaoRepository.save(limite);

                limitesInstituicao.add(limite);
            }
            entity.setLimitePorInstituicao(limitesInstituicao);
        }

        return configuracoesRepository.save(entity);
    }

    public LimitePorCategoria createLimitePorCategoria(CategoriaUsuario categoriaUsuario, Double valor){
        LimitePorCategoria limiteCategoria = new LimitePorCategoria();
        limiteCategoria.setCategoriaUsuario(categoriaUsuario);
        limiteCategoria.setLimiteDesejado(valor);
        return limitePorCategoriaRepository.save(limiteCategoria);
    }

    public List<LimitePorCategoria> createLimitePorCategoria(List<CategoriaUsuario> categorias, List<Double> valores){
       List<LimitePorCategoria> limites = new ArrayList<>();
        for (int c = 0;categorias.size()>c;c++){
            LimitePorCategoria limitePorCategoria = new LimitePorCategoria();
            limitePorCategoria.setCategoriaUsuario(categorias.get(c));
            limitePorCategoria.setLimiteDesejado(valores.get(c));

            limites.add(limitePorCategoria);
        }
        return limites;
    }

    public LimitePorInstituicao createLimitePorInstituicao(InstituicaoUsuario instituicaoUsuario, Double valor){
        LimitePorInstituicao limiteInstituicao = new LimitePorInstituicao();
        limiteInstituicao.setInstitucaoUsuario(instituicaoUsuario);
        limiteInstituicao.setLimiteDesejado(valor);
        return limitePorInstiuicaoRepository.save(limiteInstituicao);
    }

}
