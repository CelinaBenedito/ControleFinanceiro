package controle.api.back_end.service;

import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.exception.InstituicaoInativaException;
import controle.api.back_end.exception.SaldoInsuficienteException;
import controle.api.back_end.factory.EventoFinanceiroFactory;
import controle.api.back_end.factory.MovimentoFactory;
import controle.api.back_end.factory.RecorrenciaFactory;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.RecorrenciaFinanceira;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.categoria.CategoriaUsuarioRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import controle.api.back_end.specifications.EventoFinanceiroSpecifications;
import controle.api.back_end.strategy.eventoFinanceiro.EventoFinanceiroStrategy;
import controle.api.back_end.strategy.eventoFinanceiro.Registro;
import controle.api.back_end.strategy.movimento.MovimentoResultado;
import controle.api.back_end.strategy.movimento.MovimentoStrategy;
import controle.api.back_end.strategy.recorrenciaFinanceira.RecorrenciaStrategy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Serviço principal de registros financeiros.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Consultar registros do usuário (com ou sem filtros)</li>
 *   <li>Criar eventos únicos, em lote ou recorrentes</li>
 *   <li>Editar eventos já existentes</li>
 *   <li>Excluir eventos</li>
 * </ul>
 *
 * <p>Exportação de arquivos (JSON, SQL, Excel, PDF) é responsabilidade de
 * {@link RegistroExportacaoService}.
 */
@Service
@Transactional
public class RegistroService {

    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoDetalheRepository eventoDetalheRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final MovimentoFactory movimentoFactory;
    private final EventoFinanceiroFactory eventoFinanceiroFactory;
    private final RecorrenciaFactory recorrenciaFactory;
    private final InstituicaoService instituicaoService;

    public RegistroService(EventoFinanceiroRepository eventoFinanceiroRepository,
                           EventoInstituicaoRepository eventoInstituicaoRepository,
                           EventoDetalheRepository eventoDetalheRepository,
                           CategoriaUsuarioRepository categoriaUsuarioRepository,
                           UsuarioRepository usuarioRepository,
                           InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                           MovimentoFactory movimentoFactory,
                           EventoFinanceiroFactory eventoFinanceiroFactory,
                           RecorrenciaFactory recorrenciaFactory,
                           InstituicaoService instituicaoService) {
        this.eventoFinanceiroRepository  = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoDetalheRepository     = eventoDetalheRepository;
        this.categoriaUsuarioRepository  = categoriaUsuarioRepository;
        this.usuarioRepository           = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.movimentoFactory            = movimentoFactory;
        this.eventoFinanceiroFactory     = eventoFinanceiroFactory;
        this.recorrenciaFactory          = recorrenciaFactory;
        this.instituicaoService          = instituicaoService;
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    /**
     * Retorna todos os eventos financeiros do usuário, ordenados por data.
     */
    @Transactional(readOnly = true)
    public List<EventoFinanceiro> getEventosFinanceirosByUser(UUID userId) {
        if (!usuarioRepository.existsById(userId)) {
            throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado.".formatted(userId));
        }
        return eventoFinanceiroRepository.getEventoFinanceirosByUsuario_id(userId);
    }

    /**
     * Para cada evento da lista, retorna suas instituições vinculadas (mesma ordem).
     */
    @Transactional(readOnly = true)
    public List<List<EventoInstituicao>> getEventosInstituicoesByEventoFinanceiro(List<EventoFinanceiro> eventos) {
        return eventos.stream()
                .map(ev -> eventoInstituicaoRepository.findEventoInstituicaoByEventoFinanceiro_Id(ev.getId()))
                .toList();
    }

    /**
     * Para cada evento da lista, retorna seu detalhe (título + categorias) (mesma ordem).
     */
    @Transactional(readOnly = true)
    public List<EventoDetalhe> getGastosDetalhesByEventoFinanceiro(List<EventoFinanceiro> eventos) {
        return eventos.stream()
                .map(ev -> {
                    if (!eventoFinanceiroRepository.existsById(ev.getId())) {
                        throw new EntidadeNaoEncontradaException(
                                "Evento financeiro de id: %s não encontrado.".formatted(ev.getId()));
                    }
                    return eventoDetalheRepository.findGastoDetalheByEventoFinanceiro(ev);
                })
                .toList();
    }

    /**
     * Retorna o saldo acumulado na poupança do usuário.
     */
    @Transactional(readOnly = true)
    public Double getSaldoPoupanca(UUID userId) {
        buscarUsuarioOuErro(userId); // valida existência
        // TODO: implementar cálculo real com base nos eventos de poupança
        return 0.0;
    }

    /**
     * Busca registros aplicando filtros dinâmicos. Todos os parâmetros são opcionais.
     */
    @Transactional(readOnly = true)
    public List<RegistroResponseDto> getByFilter(UUID userId,
                                                  Double valor,
                                                  List<TipoMovimento> tiposMovimento,
                                                  List<Tipo> tipos,
                                                  LocalDate dataEvento,
                                                  List<InstituicaoUsuario> instituicoes,
                                                  List<CategoriaUsuario> categorias,
                                                  String descricao,
                                                  String titulo) {
        Usuario usuario = buscarUsuarioOuErro(userId);

        Specification<EventoFinanceiro> filtroUsuario =
                (root, query, cb) -> cb.equal(root.get("usuario"), usuario);

        Specification<EventoFinanceiro> filtros = EventoFinanceiroSpecifications.porFiltros(
                valor, tipos, dataEvento, descricao, tiposMovimento, instituicoes, categorias, titulo);

        return eventoFinanceiroRepository.findAll(filtroUsuario.and(filtros)).stream()
                .map(ev -> RegistrosMapper.toResponse(ev, ev.getEventoInstituicoes(), ev.getGastoDetalhe()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // CRIAR
    // =========================================================================

    /**
     * Cria um registro financeiro completo (evento + meios de pagamento + detalhe).
     *
     * <p>As regras de cada tipo são aplicadas via Strategy:
     * <ul>
     *   <li><b>Gasto</b> → debita da(s) instituição(ões); valida saldo.</li>
     *   <li><b>Recebimento</b> → credita na(s) instituição(ões).</li>
     *   <li><b>Transferência</b> → gera evento de saída + evento de recebimento interno.</li>
     *   <li><b>Poupança</b> → debita e registra rendimento projetado.</li>
     *   <li><b>Empréstimo</b> → credita com taxa de juros registrada.</li>
     * </ul>
     *
     * <p>Um evento pode ter <b>múltiplas instituições</b> (ex.: compra paga metade no débito,
     * metade no Pix) e <b>múltiplas categorias</b> (ex.: iFood → "Aplicativo" + "Alimentação").
     */
    public Registro createEventoFinanceiro(EventoFinanceiro financeiro,
                                           List<EventoInstituicao> instituicoes,
                                           EventoDetalhe detalhe) {
        // Valida e vincula o usuário real ao evento
        Usuario usuario = buscarUsuarioOuErro(financeiro.getUsuario().getId());
        financeiro.setUsuario(usuario);

        // Aplica as regras de negócio do tipo de evento via Strategy
        EventoFinanceiroStrategy strategy = eventoFinanceiroFactory.getStrategy(financeiro.getTipo());
        Registro registroProcessado = strategy.processar(financeiro, instituicoes, detalhe);

        // Persiste todos os eventos gerados pela strategy (pode ser 1 ou mais)
        return persistirRegistro(registroProcessado);
    }

    /**
     * Cria uma série de eventos recorrentes a partir de uma regra de recorrência.
     *
     * <p>Cada ocorrência recebe as mesmas instituições e o mesmo detalhe da requisição original.
     * Exemplo de uso: "Todo dia 15 recebo meu salário" ou "Todo dia compro almoço".
     *
     * @param recorrencia regra com periodicidade, datas e dados do evento.
     * @param instituicoes meios de pagamento (já mapeados do DTO).
     * @param detalhe título e categorias (já mapeados do DTO).
     */
    public List<RegistroResponseDto> createEventosRecorrentes(RecorrenciaFinanceira recorrencia,
                                                               List<EventoInstituicao> instituicoes,
                                                               EventoDetalhe detalhe) {
        Usuario usuario = buscarUsuarioOuErro(recorrencia.getUsuario().getId());
        recorrencia.setUsuario(usuario);

        RecorrenciaStrategy strategy = recorrenciaFactory.getStrategy(recorrencia.getPeriodicidade());
        List<EventoFinanceiro> eventosGerados = strategy.gerarEventos(recorrencia, recorrencia.getDataFim());

        return eventosGerados.stream()
                .map(evento -> {
                    evento.setDataRegistro(LocalDateTime.now());
                    EventoFinanceiro salvo         = eventoFinanceiroRepository.save(evento);
                    List<EventoInstituicao> insts  = createEventoInstituicao(instituicoes, salvo);
                    EventoDetalhe detalheSalvo     = createGastoDetalhe(detalhe, salvo);
                    return RegistrosMapper.toResponse(salvo, insts, detalheSalvo);
                })
                .toList();
    }

    /**
     * Valida e persiste os meios de pagamento de um evento.
     *
     * <p>Regras aplicadas por tipo de movimento:
     * <ul>
     *   <li><b>Débito / Pix / Dinheiro</b> → pagamento à vista (1 parcela).</li>
     *   <li><b>Voucher</b> → pagamento à vista; apenas instituições aceitas pelo tipo.</li>
     *   <li><b>Crédito / Boleto</b> → parcelado; cria um {@link EventoInstituicao} por parcela.</li>
     * </ul>
     * Para Gasto e Transferência, verifica se há saldo suficiente na instituição.
     */
    public List<EventoInstituicao> createEventoInstituicao(List<EventoInstituicao> instituicoes,
                                                            EventoFinanceiro evento) {
        if (!eventoFinanceiroRepository.existsById(evento.getId())) {
            throw new EntidadeNaoEncontradaException(
                    "Evento Financeiro de id: %s não encontrado.".formatted(evento.getId()));
        }

        // Cada instituição pode gerar 1 ou N registros (parcelamento)
        return instituicoes.stream()
                .flatMap(inst -> processarPagamento(inst, evento).stream())
                .toList();
    }

    /**
     * Valida e persiste o detalhe de um evento (título + categorias).
     *
     * <p>Um evento pode ter <b>múltiplas categorias</b> — por exemplo, um pedido no iFood
     * pode ser categorizado como "Aplicativo" e "Alimentação" ao mesmo tempo.
     */
    public EventoDetalhe createGastoDetalhe(EventoDetalhe detalhe, EventoFinanceiro evento) {
        if (!eventoFinanceiroRepository.existsById(evento.getId())) {
            throw new EntidadeNaoEncontradaException(
                    "Evento Financeiro de id: %s não encontrado.".formatted(evento.getId()));
        }

        // Verifica se todas as categorias existem antes de salvar
        List<CategoriaUsuario> categorias = detalhe.getCategoriaUsuario().stream()
                .map(cu -> buscarCategoriaOuErro(cu.getId()))
                .toList();

        detalhe.setEventoFinanceiro(evento);
        detalhe.setCategoriaUsuario(categorias);
        return eventoDetalheRepository.save(detalhe);
    }

    // =========================================================================
    // EDITAR
    // =========================================================================

    /**
     * Atualiza os campos básicos de um evento: tipo, data e descrição.
     */
    public EventoFinanceiro editEventoFinanceiro(UUID eventoId, EventoFinanceiro novosDados) {
        EventoFinanceiro existente = buscarEventoOuErro(eventoId);

        if (!novosDados.getDataEvento().equals(existente.getDataEvento())) {
            existente.setDataEvento(novosDados.getDataEvento());
        }
        if (!Objects.equals(novosDados.getDescricao(), existente.getDescricao())) {
            existente.setDescricao(novosDados.getDescricao());
        }
        if (novosDados.getTipo() != existente.getTipo()) {
            existente.setTipo(novosDados.getTipo());
        }
        return eventoFinanceiroRepository.save(existente);
    }

    /**
     * Sincroniza os meios de pagamento de um evento com a nova lista recebida.
     * Remove os que saíram, atualiza os que ficaram e cria os novos.
     */
    public List<EventoInstituicao> editEventoInstituicao(UUID eventoId, List<EventoInstituicao> novas) {
        EventoFinanceiro evento = buscarEventoOuErro(eventoId);

        List<EventoInstituicao> existentes = eventoInstituicaoRepository
                .findEventoInstituicaoByEventoFinanceiro_Id(eventoId);

        // IDs que vieram na nova lista
        Set<Integer> novosIds = novas.stream()
                .map(e -> e.getInstituicaoUsuario().getId())
                .collect(Collectors.toSet());

        // Remove as que não estão mais na lista
        existentes.stream()
                .filter(ex -> !novosIds.contains(ex.getInstituicaoUsuario().getId()))
                .forEach(eventoInstituicaoRepository::delete);

        // Atualiza ou insere cada item da nova lista
        return novas.stream().map(nova -> {
            EventoInstituicao alvo = existentes.stream()
                    .filter(ex -> Objects.equals(ex.getInstituicaoUsuario().getId(),
                            nova.getInstituicaoUsuario().getId()))
                    .findFirst()
                    .orElse(new EventoInstituicao());

            InstituicaoUsuario instUsuario = buscarInstituicaoAtivaOuErro(nova.getInstituicaoUsuario().getId());
            alvo.setInstituicaoUsuario(instUsuario);
            alvo.setValor(nova.getValor());
            alvo.setParcelas(nova.getParcelas());
            alvo.setTipoMovimento(nova.getTipoMovimento());
            alvo.setEventoFinanceiro(evento);
            return eventoInstituicaoRepository.save(alvo);
        }).toList();
    }

    /**
     * Atualiza título e categorias do detalhe de um evento.
     */
    public EventoDetalhe editGastoDetalhe(UUID eventoId, EventoDetalhe novosDados) {
        EventoFinanceiro evento = buscarEventoOuErro(eventoId);
        EventoDetalhe detalhe  = eventoDetalheRepository.findGastoDetalheByEventoFinanceiro_Id(eventoId);

        if (!Objects.equals(novosDados.getTituloGasto(), detalhe.getTituloGasto())) {
            detalhe.setTituloGasto(novosDados.getTituloGasto());
        }

        // Sincroniza categorias: remove as que saíram, adiciona as novas
        Set<Integer> novosIds = novosDados.getCategoriaUsuario().stream()
                .map(CategoriaUsuario::getId)
                .collect(Collectors.toSet());

        List<CategoriaUsuario> categoriasAtuais = new ArrayList<>(detalhe.getCategoriaUsuario());
        categoriasAtuais.removeIf(c -> !novosIds.contains(c.getId()));

        novosDados.getCategoriaUsuario().forEach(nova -> {
            boolean jaExiste = categoriasAtuais.stream().anyMatch(c -> Objects.equals(c.getId(), nova.getId()));
            if (!jaExiste) {
                categoriasAtuais.add(buscarCategoriaOuErro(nova.getId()));
            }
        });

        detalhe.setCategoriaUsuario(categoriasAtuais);
        detalhe.setEventoFinanceiro(evento);
        return eventoDetalheRepository.save(detalhe);
    }

    // =========================================================================
    // DELETAR
    // =========================================================================

    /**
     * Remove um registro completo. As sub-entidades (instituições e detalhe) são removidas
     * automaticamente pelo JPA via {@code CascadeType.ALL + orphanRemoval = true}.
     */
    public void deleteRegistroByEventoFinanceiro_Id(UUID eventoId) {
        EventoFinanceiro evento = buscarEventoOuErro(eventoId);
        eventoFinanceiroRepository.delete(evento);
    }

    // =========================================================================
    // PRIVADOS — persistência
    // =========================================================================

    /**
     * Percorre todos os eventos gerados pela Strategy, salva cada um e vincula
     * as respectivas instituições e detalhes.
     */
    private Registro persistirRegistro(Registro registroProcessado) {
        List<EventoFinanceiro> eventosSalvos             = new ArrayList<>();
        Map<EventoFinanceiro, List<EventoInstituicao>> instituicoesMap = new HashMap<>();
        Map<EventoFinanceiro, EventoDetalhe> detalheMap  = new HashMap<>();

        for (EventoFinanceiro ev : registroProcessado.getEventosFinanceiros()) {
            ev.setDataRegistro(LocalDateTime.now());
            EventoFinanceiro evSalvo = eventoFinanceiroRepository.save(ev);
            eventosSalvos.add(evSalvo);

            // Vincula meios de pagamento (pode ser mais de um)
            List<EventoInstituicao> insts = registroProcessado.getInstituicoesPorEvento().get(ev);
            if (insts != null) {
                instituicoesMap.put(evSalvo, createEventoInstituicao(insts, evSalvo));
            }

            // Vincula detalhe (título + categorias)
            EventoDetalhe det = registroProcessado.getDetalhePorEvento().get(ev);
            if (det != null) {
                detalheMap.put(evSalvo, createGastoDetalhe(det, evSalvo));
            }
        }

        return new Registro(eventosSalvos, instituicoesMap, detalheMap);
    }

    /**
     * Valida uma instituição, aplica as regras do tipo de movimento (Strategy),
     * confere o saldo quando necessário e persiste as parcelas.
     */
    private List<EventoInstituicao> processarPagamento(EventoInstituicao pagamento,
                                                        EventoFinanceiro evento) {
        InstituicaoUsuario instUsuario = buscarInstituicaoAtivaOuErro(pagamento.getInstituicaoUsuario().getId());

        Map<String, Object> params = Map.of("parcelas", pagamento.getParcelas());
        MovimentoStrategy movimentoStrategy = movimentoFactory.getStrategy(pagamento.getTipoMovimento(), params);
        movimentoStrategy.validar(instUsuario);
        MovimentoResultado resultado = movimentoStrategy.processar(pagamento);

        // Gastos e transferências requerem saldo suficiente na instituição
        if (tipoRequerValidacaoSaldo(evento.getTipo())) {
            BigDecimal saldoDisponivel = instituicaoService.getSaldoByInstituicao(instUsuario.getId());
            if (BigDecimal.valueOf(resultado.getValorParcela()).compareTo(saldoDisponivel) > 0) {
                throw new SaldoInsuficienteException(
                        "Saldo insuficiente na instituição %s para realizar a operação."
                                .formatted(instUsuario.getInstituicao().getNome()));
            }
        }

        return salvarParcelas(resultado, pagamento, instUsuario, evento);
    }

    /**
     * Salva os registros de parcelas de um pagamento.
     *
     * <ul>
     *   <li><b>À vista (1 parcela)</b>: salva um único {@link EventoInstituicao}.</li>
     *   <li><b>Parcelado (N parcelas)</b>: salva um registro por parcela com o valor fracionado.
     *       Ex.: R$ 300 em 3x → três registros de R$ 100.</li>
     * </ul>
     */
    private List<EventoInstituicao> salvarParcelas(MovimentoResultado resultado,
                                                    EventoInstituicao pagamento,
                                                    InstituicaoUsuario instUsuario,
                                                    EventoFinanceiro evento) {
        if (resultado.getParcelas() == 1) {
            pagamento.setInstituicaoUsuario(instUsuario);
            pagamento.setEventoFinanceiro(evento);
            pagamento.setParcelas(1);
            return List.of(eventoInstituicaoRepository.save(pagamento));
        }

        // Parcelado: cria um registro por número de parcela
        return IntStream.rangeClosed(1, resultado.getParcelas())
                .mapToObj(numeroParcela -> {
                    EventoInstituicao parcela = new EventoInstituicao();
                    parcela.setParcelas(numeroParcela);
                    parcela.setEventoFinanceiro(evento);
                    parcela.setInstituicaoUsuario(instUsuario);
                    parcela.setTipoMovimento(pagamento.getTipoMovimento());
                    parcela.setValor(resultado.getValorParcela());
                    return eventoInstituicaoRepository.save(parcela);
                })
                .toList();
    }

    // =========================================================================
    // PRIVADOS — validação e busca
    // =========================================================================

    /**
     * Tipos que exigem verificação de saldo antes de processar o pagamento.
     * Recebimento, Poupança e Empréstimo são entradas — não consomem saldo.
     */
    private boolean tipoRequerValidacaoSaldo(Tipo tipo) {
        return tipo == Tipo.Gasto || tipo == Tipo.Transferencia;
    }

    private Usuario buscarUsuarioOuErro(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Usuário de id: %s não encontrado.".formatted(id)));
    }

    private EventoFinanceiro buscarEventoOuErro(UUID eventoId) {
        return eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Evento financeiro de id: %s não encontrado.".formatted(eventoId)));
    }

    /** Busca e valida que a instituição existe e está ativa. */
    private InstituicaoUsuario buscarInstituicaoAtivaOuErro(Integer id) {
        InstituicaoUsuario inst = instituicaoUsuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Instituição associada ao usuário não encontrada."));
        if (!inst.getIsAtivo()) {
            throw new InstituicaoInativaException(
                    "Instituição '%s' está inativa e não pode ser utilizada."
                            .formatted(inst.getInstituicao().getNome()));
        }
        return inst;
    }

    private CategoriaUsuario buscarCategoriaOuErro(Integer id) {
        return categoriaUsuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Categoria Usuário de id: %d não encontrada.".formatted(id)));
    }
}
