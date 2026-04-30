package controle.api.back_end.service;

import controle.api.back_end.dto.configuracoes.in.ConfiguracaoEditDTO;
import controle.api.back_end.dto.configuracoes.in.PeriodoTempoRequestDto;
import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.GastoDetalhe;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConfiguracoesService {

    private final ConfiguracoesRepository configuracoesRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;
    private final LimitePorCategoriaRepository limitePorCategoriaRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final LimitePorInstituicaoRepository limitePorInstiuicaoRepository;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final GastoDetalheRepository gastoDetalheRepository;

    public ConfiguracoesService(ConfiguracoesRepository configuracoesRepository,
                                UsuarioRepository usuarioRepository,
                                CategoriaUsuarioRepository categoriaUsuarioRepository,
                                LimitePorCategoriaRepository limitePorCategoriaRepository,
                                InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                                LimitePorInstituicaoRepository limitePorInstiuicaoRepository,
                                EventoFinanceiroRepository eventoFinanceiroRepository,
                                EventoInstituicaoRepository eventoInstituicaoRepository,
                                GastoDetalheRepository gastoDetalheRepository) {
        this.configuracoesRepository = configuracoesRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
        this.limitePorCategoriaRepository = limitePorCategoriaRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.limitePorInstiuicaoRepository = limitePorInstiuicaoRepository;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.gastoDetalheRepository = gastoDetalheRepository;
    }

    public List<Configuracoes> getConfiguracoes() {
        return configuracoesRepository.findAll();
    }



    public Configuracoes createConfiguracao(Configuracoes entity, UUID idUsuario) {
        Usuario user = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado"));

        if (configuracoesRepository.existsConfiguracoesByUsuario_Id(idUsuario)) {
            throw new EntidadeJaExisteException("Já existe uma configuração associada ao usuário informado");
        }

        entity.setUsuario(user);
        entity.setUltimaAtualizacao(LocalDate.now());

        return configuracoesRepository.save(entity);
    }


    public Configuracoes editConfiguracao(Configuracoes entity,
                                          @Valid ConfiguracaoEditDTO editDTO,
                                          UUID id) {

        Configuracoes configuracao = configuracoesRepository.findById(id)
                .orElseThrow(() ->
                                new EntidadeNaoEncontradaException(
                                        "Configuração de Id: %s não encontrada."
                                                .formatted(id)
                                )
                );

        entity.setId(configuracao.getId());
        entity.setUsuario(configuracao.getUsuario());

        entity.setUltimaAtualizacao(LocalDate.now());
        // Limites por categoria
        if (editDTO.getLimitesCategoria() != null) {
            List<LimitePorCategoria> limitesCategoria = new ArrayList<>();
            for (ConfiguracaoEditDTO.LimiteCategoriaEditDTO limiteDTO : editDTO.getLimitesCategoria()) {
                CategoriaUsuario categoriaUsuario = categoriaUsuarioRepository
                        .findByUsuario_idAndCategoria_id(configuracao.getUsuario().getId(), limiteDTO.getCategoriaId());

                LimitePorCategoria limite = limitePorCategoriaRepository
                        .findByConfiguracoesAndCategoriaUsuario(configuracao, categoriaUsuario);

                if (limite == null) {
                    limite = new LimitePorCategoria();
                    limite.setConfiguracoes(configuracao);
                    limite.setCategoriaUsuario(categoriaUsuario);
                }

                limite.setLimiteDesejado(limiteDTO.getValor());
                LimitePorCategoria save = limitePorCategoriaRepository.save(limite);
                limitesCategoria.add(save);
            }
            entity.setLimitePorCategoria(limitesCategoria);
        }

        // Limites por instituição
        if (editDTO.getLimitesInstituicao() != null) {
            List<LimitePorInstituicao> limitesInstituicao = new ArrayList<>();
            for (ConfiguracaoEditDTO.LimiteInstituicaoEditDTO limiteDTO : editDTO.getLimitesInstituicao()) {
                InstituicaoUsuario instituicaoUsuario = instituicaoUsuarioRepository
                        .findByUsuario_IdAndInstituicao_Id(configuracao.getUsuario().getId(), limiteDTO.getInstituicaoId());

                LimitePorInstituicao limite = limitePorInstiuicaoRepository
                        .findByConfiguracoesAndInstitucaoUsuario(configuracao, instituicaoUsuario);

                if (limite == null) {
                    limite = new LimitePorInstituicao();
                    limite.setConfiguracoes(configuracao);
                    limite.setInstitucaoUsuario(instituicaoUsuario);
                }

                limite.setLimiteDesejado(limiteDTO.getValor());
                LimitePorInstituicao save = limitePorInstiuicaoRepository.save(limite);
                limitesInstituicao.add(save);
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
        List<LimitePorInstituicao> limitePorInstituicaoByInstitucaoUsuarioId = limitePorInstiuicaoRepository
                .findLimitePorInstituicaoByInstitucaoUsuario_Id(
                        instituicaoUsuario.getId()
                );

        limiteInstituicao.setInstitucaoUsuario(instituicaoUsuario);
        limiteInstituicao.setLimiteDesejado(valor);
        return limitePorInstiuicaoRepository.save(limiteInstituicao);
    }

    public InstituicaoUsuario findInstituicaoUsuario(Integer institucaoUsuarioId) {
        return instituicaoUsuarioRepository.findById(institucaoUsuarioId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Associação de instituição e usuário com o id: %d não encontrada."
                                        .formatted(institucaoUsuarioId)
                        )
                );
    }
    public CategoriaUsuario findCategoriaUsuario(Integer categoriaUsuarioId) {
        return categoriaUsuarioRepository.findById(categoriaUsuarioId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Associação de categoria e usuário com o id: %d não encontrada."
                                        .formatted(categoriaUsuarioId)
                        )
                );
    }

    public Configuracoes updateLimiteInstituicao(UUID id, List<LimitePorInstituicao> limites) {
        Configuracoes configuracoes = configuracoesRepository.findById(id)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Configuração de id: %s não encontrado."
                                        .formatted(id)
                        )
                );
        configuracoes.setLimitePorInstituicao(limites);
        return configuracoesRepository.save(configuracoes);
    }


    public Configuracoes updateLimiteCategoria(UUID id, List<LimitePorCategoria> limites) {
        Configuracoes configuracoes = configuracoesRepository.findById(id)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Configuração de id: %s não encontrado."
                                        .formatted(id)
                        )
                );
        configuracoes.setLimitePorCategoria(limites);
        return configuracoesRepository.save(configuracoes);
    }

    @Transactional
    public void deleteByPeriodoDeTempo(UUID id,@Valid PeriodoTempoRequestDto tempoDto) {
       Configuracoes config = getConfiguracoesById(id);

        UUID idUsuario = config.getUsuario().getId();

        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository
                .findEventoFinanceiroByUsuario_Id(idUsuario)
                .stream()
                .filter(evento -> !evento.getDataEvento().isBefore(tempoDto.getDataInical())
                        && !evento.getDataEvento().isAfter(tempoDto.getDataFinal()))
                .toList();

        if (eventosFinanceiros.isEmpty()) {
            throw new EntidadeNaoEncontradaException("Nenhum evento encontrado no período informado.");
        }

        for (EventoFinanceiro evento : eventosFinanceiros) {
            if (evento.getGastoDetalhe() != null) {
                evento.getGastoDetalhe().getCategoriaUsuario().clear(); // limpa vínculos
                evento.setGastoDetalhe(null);
            }
            if (evento.getEventoInstituicao() != null) {
                evento.getEventoInstituicao().clear();
            }
        }
        eventoFinanceiroRepository.deleteAll(eventosFinanceiros);

    }

    @Transactional
    public void deleteAllByUsuario(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Usuário de id: %s não encontrado.".formatted(usuarioId)
                        )
                );

        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findAllByUsuario(usuario);

        if (eventosFinanceiros.isEmpty()) {
            throw new EntidadeNaoEncontradaException("Nenhum evento encontrado para o usuário informado.");
        }

        eventoFinanceiroRepository.deleteAll(eventosFinanceiros);
    }

    public Configuracoes getConfiguracaoByUserId(UUID userId) {

       Configuracoes config = configuracoesRepository.findConfiguracoesByUsuario_Id(userId)
               .orElseThrow(()->
                       new EntidadeNaoEncontradaException("Não foi possível encontrar a configuração correspondente" +
                               " ao usuário de id: %s"
                               .formatted(userId)
                       )
               );
        List<LimitePorCategoria> limitePorCategoriaByConfiguracoesId = limitePorCategoriaRepository.findLimitePorCategoriaByConfiguracoes_Id(config.getId());
        List<LimitePorInstituicao> limitePorInstituicaoByConfiguracoesId = limitePorInstiuicaoRepository.findLimitePorInstituicaoByConfiguracoes_Id(config.getId());

        config.setLimitePorCategoria(limitePorCategoriaByConfiguracoesId);
        config.setLimitePorInstituicao(limitePorInstituicaoByConfiguracoesId);

        return config;
    }

    public Configuracoes getConfiguracoesById(UUID configId){
        Configuracoes config = configuracoesRepository.findById(configId)
                .orElseThrow(() ->
                    new EntidadeNaoEncontradaException(
                            "Configurações de id: %s não encontrada."
                                    .formatted(configId)
                    )
                );
        List<LimitePorCategoria> limitePorCategoriaByConfiguracoesId = limitePorCategoriaRepository.findLimitePorCategoriaByConfiguracoes_Id(config.getId());
        List<LimitePorInstituicao> limitePorInstituicaoByConfiguracoesId = limitePorInstiuicaoRepository.findLimitePorInstituicaoByConfiguracoes_Id(config.getId());

        config.setLimitePorCategoria(limitePorCategoriaByConfiguracoesId);
        config.setLimitePorInstituicao(limitePorInstituicaoByConfiguracoesId);
        return config;
    }
}
