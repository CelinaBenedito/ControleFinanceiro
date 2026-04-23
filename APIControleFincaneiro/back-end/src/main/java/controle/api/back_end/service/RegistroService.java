package controle.api.back_end.service;

import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.factory.MovimentoFactory;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.*;
import controle.api.back_end.specifications.EventoFinanceiroSpecifications;
import controle.api.back_end.strategy.movimento.MovimentoResultado;
import controle.api.back_end.strategy.movimento.MovimentoStrategy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RegistroService {
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final GastoDetalheRepository gastoDetalheRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final MovimentoFactory movimentoFactory;

    public RegistroService(EventoFinanceiroRepository eventoFinanceiroRepository,
                           EventoInstituicaoRepository eventoInstituicaoRepository,
                           GastoDetalheRepository gastoDetalheRepository,
                           CategoriaUsuarioRepository categoriaUsuarioRepository,
                           UsuarioRepository usuarioRepository,
                           InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                           MovimentoFactory movimentoFactory) {
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.gastoDetalheRepository = gastoDetalheRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.movimentoFactory = movimentoFactory;
    }

    public EventoFinanceiro createEventoFinanceiro(EventoFinanceiro entity) {

        Usuario user = usuarioRepository
                .findById(entity.getUsuario().getId())
                        .orElseThrow(() ->
                                new EntidadeNaoEncontradaException(
                                        "Usuário de id: %s não encontrado."
                                        .formatted(entity.getUsuario().getId()
                                        )
                                )
                        );

        entity.setUsuario(user);
        entity.setDataRegistro(LocalDate.now());
        return eventoFinanceiroRepository.save(entity);
    }

    public EventoInstituicao createEventoInstituicao(EventoInstituicao entity,
                                                     EventoFinanceiro eventoFinanceiro){
        if (!eventoFinanceiroRepository.existsById(eventoFinanceiro.getId())){
            throw new EntidadeNaoEncontradaException(
                    "Evento Financeiro de id: %s não encontrado"
                            .formatted(eventoFinanceiro.getId())
            );
        }
       InstituicaoUsuario instituicaoUsuario = instituicaoUsuarioRepository
               .findById(entity.getInstituicaoUsuario().getId())
               .orElseThrow(()->
                       new EntidadeNaoEncontradaException(
                               "Instituição associada ao Usuário não encontrada."
                       )
               );
        Map<String, Object> params = new HashMap<>();

        MovimentoStrategy strategy = movimentoFactory.getStrategy(entity.getTipoMovimento(), params);
        strategy.validar(entity.getInstituicaoUsuario());
        MovimentoResultado resultado = strategy.processar(entity);

        if (resultado.getParcelas() > 1) {
            // lógica de salvar parcelas
        } else {
            eventoInstituicaoRepository.save(resultado.getEvento());
        }


        entity.setInstituicaoUsuario(instituicaoUsuario);
       entity.setEventoFinanceiro(eventoFinanceiro);
       return eventoInstituicaoRepository.save(entity);
    }

    public GastoDetalhe createGastoDetalhe(GastoDetalhe entity,
                                           EventoFinanceiro eventoFinanceiro){
       if (!eventoFinanceiroRepository.existsById(eventoFinanceiro.getId())){
           throw new EntidadeNaoEncontradaException(
                   "Evento Financeiro de id: %s não encontrado"
                           .formatted(eventoFinanceiro.getId())
           );
       }
        CategoriaUsuario categoriaUsuario = categoriaUsuarioRepository
                .findById(entity.getCategoriaUsuario().getId())
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException("Categoria Usuário de id: %d não encontrada."
                                .formatted(entity.getCategoriaUsuario().getId()))
                );
       entity.setEventoFinanceiro(eventoFinanceiro);
       entity.setCategoriaUsuario(categoriaUsuario);

       return gastoDetalheRepository.save(entity);
    }

    public List<EventoFinanceiro> getEventosFinanceirosByUser(UUID userId) {
        if(!usuarioRepository.existsById(userId)){
            throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado."
                    .formatted(userId)
            );
        }

        return eventoFinanceiroRepository.getEventoFinanceirosByUsuario_id(userId);
    }

    public List<EventoInstituicao> getEventosInstituicoesByEventoFinanceiro(
            List<EventoFinanceiro> eventosFinanceiros) {
        List<EventoInstituicao> instituicoes = new ArrayList<>();
    for (EventoFinanceiro evento : eventosFinanceiros){
       if(!eventoFinanceiroRepository.existsById(evento.getId())){
          throw new EntidadeNaoEncontradaException("Evento financeiro de id: %s não encontrado."
                  .formatted(evento.getId()
                  )
          );
        }
        EventoInstituicao eventoInstituicaoByEventoId = eventoInstituicaoRepository.
                findEventoInstituicaoByEventoFinanceiro_Id(
                        evento.getId()
                );

        instituicoes.add(eventoInstituicaoByEventoId);
    }
    return instituicoes;
    }

    public List<GastoDetalhe> getGastosDetalhesByEventoFinanceiro(
            List<EventoFinanceiro> eventosFinanceiros) {
        List<GastoDetalhe> gastoDetalhes = new ArrayList<>();
        for (EventoFinanceiro evento : eventosFinanceiros){
            if(!eventoFinanceiroRepository.existsById(evento.getId())){
                throw new EntidadeNaoEncontradaException("Evento financeiro de id: %s não encontrado."
                        .formatted(evento.getId()
                        )
                );
            }
            GastoDetalhe gastoDetalheByEventoFinanceiro = gastoDetalheRepository.findGastoDetalheByEventoFinanceiro(evento);

            gastoDetalhes.add(gastoDetalheByEventoFinanceiro);
        }
        return gastoDetalhes;
    }

    public List<RegistroResponseDto> getByFilter(Double valor, TipoMovimento tipoMovimento, Tipo tipo,
                                                 LocalDate dataEvento, InstituicaoUsuario instituicao,
                                                 CategoriaUsuario categoria, String descricao, String titulo){

        Specification<EventoFinanceiro> spec = EventoFinanceiroSpecifications.porFiltros(
                valor, tipo, dataEvento, descricao, tipoMovimento, instituicao, categoria, titulo
        );

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAll(spec);

        return eventos.stream()
                .map(e -> RegistrosMapper.toResponse(e, e.getEventoInstituicao(), e.getGastoDetalhe()))
                .collect(Collectors.toList());

    }

    public EventoFinanceiro editEventoFinanceiro(UUID eventoId, EventoFinanceiro entity) {
        EventoFinanceiro financeiro = eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento financeiro de id: %s não encontrado"
                                        .formatted(eventoId))
                );

        if(entity.getDataEvento() != financeiro.getDataEvento()){
            financeiro.setDataEvento(entity.getDataEvento());
        }
        if (!Objects.equals(entity.getDescricao(), financeiro.getDescricao())){
            financeiro.setDescricao(entity.getDescricao());
        }
        if (entity.getTipo() != financeiro.getTipo()){
            financeiro.setTipo(entity.getTipo());
        }
        return eventoFinanceiroRepository.save(financeiro);
    }

    public EventoInstituicao editEventoInstituicao(UUID eventoId, EventoInstituicao entity){
        EventoFinanceiro financeiro = eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento financeiro de id: %s não encontrado"
                                        .formatted(eventoId))
                );

        EventoInstituicao eventoInstituicao = eventoInstituicaoRepository
                .findEventoInstituicaoByEventoFinanceiro_Id(eventoId);

        if (entity.getInstituicaoUsuario() != eventoInstituicao.getInstituicaoUsuario()){
            eventoInstituicao.setInstituicaoUsuario(entity.getInstituicaoUsuario());
        }
        if (!Objects.equals(entity.getValor(), eventoInstituicao.getValor())){
            eventoInstituicao.setValor(entity.getValor());
        }
        eventoInstituicao.setEventoFinanceiro(financeiro);

        return eventoInstituicaoRepository.save(eventoInstituicao);
    }

    public GastoDetalhe editGastoDetalhe(UUID eventoId, GastoDetalhe entity){
        EventoFinanceiro financeiro = eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento financeiro de id: %s não encontrado"
                                        .formatted(eventoId))
                );

        GastoDetalhe gastoDetalhe = gastoDetalheRepository.findGastoDetalheByEventoFinanceiro_Id(eventoId);

        if (entity.getCategoriaUsuario() != gastoDetalhe.getCategoriaUsuario()){
            gastoDetalhe.setCategoriaUsuario(entity.getCategoriaUsuario());
        }
        if (!Objects.equals(entity.getTituloGasto(), gastoDetalhe.getTituloGasto())){
            gastoDetalhe.setTituloGasto(entity.getTituloGasto());
        }
        gastoDetalhe.setEventoFinanceiro(financeiro);

        return gastoDetalheRepository.save(gastoDetalhe);
    }

    public void deleteRegistroByEventoFinanceiro_Id(UUID eventoId) {
        eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento Financeiro de id: %s não encontrado"
                                        .formatted(eventoId)
                        )
                );
        if(!gastoDetalheRepository.existsGastoDetalheByEventoFinanceiro_Id(eventoId)){
            throw new EntidadeNaoEncontradaException("Detalhe do gasto não encontrado.");
        }
        gastoDetalheRepository.deleteGastoDetalheByEventoFinanceiro_Id(eventoId);

        if(!eventoInstituicaoRepository.existsEventoInstituicaoByEventoFinanceiro_Id(eventoId)){
            throw new EntidadeNaoEncontradaException("Evento Instituição não encontrado.");
        }
        eventoInstituicaoRepository.deleteEventoInstituicaoByEventoFinanceiro_Id(eventoId);

        eventoFinanceiroRepository.deleteEventoFinanceiroById(eventoId);
    }
}
