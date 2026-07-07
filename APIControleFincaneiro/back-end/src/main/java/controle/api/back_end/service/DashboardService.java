package controle.api.back_end.service;

import controle.api.back_end.dto.dashboard.out.*;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.dashboard.TipoPeriodo;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.repository.configuracoes.ConfiguracoesRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import controle.api.back_end.utils.PeriodoTemporalUtils;
import controle.api.back_end.utils.PeriodoTemporalUtils.Periodo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoDetalheRepository eventoDetalheRepository;
    private final UsuarioService usuarioService;
    private final ConfiguracoesService configuracoesService;
    private final ConfiguracoesRepository configuracoesRepository;
    private final InstituicaoService instituicaoService;
    private final RegistroService registroService;

    public DashboardService(UsuarioRepository usuarioRepository,
                            RegistroService registroService,
                            EventoFinanceiroRepository eventoFinanceiroRepository,
                            EventoInstituicaoRepository eventoInstituicaoRepository,
                            EventoDetalheRepository eventoDetalheRepository,
                            InstituicaoService instituicaoService,
                            UsuarioService usuarioService,
                            ConfiguracoesService configuracoesService,
                            ConfiguracoesRepository configuracoesRepository) {
        this.usuarioRepository        = usuarioRepository;
        this.registroService          = registroService;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoDetalheRepository  = eventoDetalheRepository;
        this.instituicaoService       = instituicaoService;
        this.usuarioService           = usuarioService;
        this.configuracoesService     = configuracoesService;
        this.configuracoesRepository  = configuracoesRepository;
    }

    // =========================================================================
    //  KPI 1 — SALDO TOTAL DO PERÍODO
    //  Saldo acumulado de TODA a história do usuário até o final do período
    //  (ou até hoje, se o período ainda está em aberto).
    // =========================================================================

    public SaldoTotalDto getSaldoTotal(TipoPeriodo tipo, int ano, Integer mes,
                                       Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);
        LocalDate corte = dataCorte(periodo);

        List<EventoFinanceiro> todos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);
        BigDecimal saldo = BigDecimal.ZERO;
        for (EventoFinanceiro e : todos) {
            if (!e.getDataEvento().isAfter(corte)) {
                saldo = saldoContribuicao(saldo, e);
            }
        }

        return new SaldoTotalDto(saldo, corte, periodo.label());
    }

    // =========================================================================
    //  KPI 2 — GASTO TOTAL DO PERÍODO + variação vs. período anterior
    // =========================================================================

    public GastoTotalDto getGastoTotal(TipoPeriodo tipo, int ano, Integer mes,
                                       Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        int diaFiscal    = getDiaFiscal(userId);
        Periodo atual    = PeriodoTemporalUtils.calcular(tipo, ano, mes, trimestre, semestre, diaFiscal);
        Periodo anterior = PeriodoTemporalUtils.calcularAnterior(tipo, ano, mes, trimestre, semestre, diaFiscal);

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        BigDecimal gastosAtual    = somarGastos(eventos, atual.inicio(), atual.fim());
        BigDecimal gastosAnterior = somarGastos(eventos, anterior.inicio(), anterior.fim());

        int variacao = calcularVariacaoPercentual(gastosAtual, gastosAnterior);

        return new GastoTotalDto(gastosAtual, gastosAnterior, variacao, atual.label(), anterior.label());
    }

    // =========================================================================
    //  KPI 3 — MAIOR GASTO DO PERÍODO
    // =========================================================================

    public MaiorGastoDto getMaiorGasto(TipoPeriodo tipo, int ano, Integer mes,
                                       Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        BigDecimal maiorValor  = BigDecimal.ZERO;
        BigDecimal totalGastos = BigDecimal.ZERO;
        EventoFinanceiro maiorEvento = null;

        for (EventoFinanceiro e : eventos) {
            if (!emPeriodo(e, periodo) || e.getTipo() != Tipo.Gasto) continue;

            BigDecimal v = BigDecimal.valueOf(e.getValor());
            totalGastos = totalGastos.add(v);
            if (v.compareTo(maiorValor) > 0) {
                maiorValor = v;
                maiorEvento = e;
            }
        }

        if (maiorEvento == null) {
            return new MaiorGastoDto("Sem gastos", "N/A", BigDecimal.ZERO, 0, null);
        }

        int pct = totalGastos.compareTo(BigDecimal.ZERO) > 0
                ? maiorValor.divide(totalGastos, 4, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100)).intValue()
                : 0;

        return new MaiorGastoDto(
                tituloEvento(maiorEvento),
                categoriaEvento(maiorEvento),
                BigDecimal.valueOf(maiorEvento.getValor()),
                pct,
                maiorEvento.getDataEvento()
        );
    }

    // =========================================================================
    //  KPI 4 — CATEGORIA QUE MAIS IMPACTOU
    // =========================================================================

    public CategoriaImpactoDto getCategoriaImpacto(TipoPeriodo tipo, int ano, Integer mes,
                                                    Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        int diaFiscal    = getDiaFiscal(userId);
        Periodo atual    = PeriodoTemporalUtils.calcular(tipo, ano, mes, trimestre, semestre, diaFiscal);
        Periodo anterior = PeriodoTemporalUtils.calcularAnterior(tipo, ano, mes, trimestre, semestre, diaFiscal);

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        Map<String, BigDecimal> atualPorCat    = somarGastosPorCategoria(eventos, atual.inicio(), atual.fim());
        Map<String, BigDecimal> anteriorPorCat = somarGastosPorCategoria(eventos, anterior.inicio(), anterior.fim());

        if (atualPorCat.isEmpty()) {
            return new CategoriaImpactoDto("Sem gastos", BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        String categoriaTopo = atualPorCat.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Sem categoria");

        BigDecimal valorAtual    = atualPorCat.getOrDefault(categoriaTopo, BigDecimal.ZERO);
        BigDecimal valorAnterior = anteriorPorCat.getOrDefault(categoriaTopo, BigDecimal.ZERO);
        int variacao = calcularVariacaoPercentual(valorAtual, valorAnterior);

        return new CategoriaImpactoDto(categoriaTopo, valorAtual, valorAnterior, variacao);
    }

    // =========================================================================
    //  GRÁFICO 1 — EVOLUÇÃO DOS GASTOS (line chart)
    //  MENSAL / TRIMESTRAL → granularidade diária
    //  SEMESTRAL           → granularidade semanal
    //  ANUAL               → granularidade mensal
    // =========================================================================

    public EvolucaoGastosDto getEvolucaoGastos(TipoPeriodo tipo, int ano, Integer mes,
                                                Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        // daily map of gastos
        Map<LocalDate, BigDecimal> porDia = new TreeMap<>();
        for (EventoFinanceiro e : eventos) {
            if (!emPeriodo(e, periodo)) continue;
            if (e.getTipo() != Tipo.Gasto && e.getTipo() != Tipo.Transferencia) continue;
            porDia.merge(e.getDataEvento(), BigDecimal.valueOf(e.getValor()), BigDecimal::add);
        }

        String granularidade;
        List<EvolucaoGastosDto.Ponto> dados;

        switch (tipo) {
            case MENSAL, TRIMESTRAL -> {
                granularidade = "DIARIO";
                dados = seriesDiaria(porDia, periodo.inicio(), periodo.fim(), "dd/MM");
            }
            case SEMESTRAL -> {
                granularidade = "SEMANAL";
                dados = seriesSemanais(porDia, periodo.inicio(), periodo.fim());
            }
            default -> { // ANUAL
                granularidade = "MENSAL";
                dados = seriesMensais(porDia, periodo.inicio(), periodo.fim());
            }
        }

        return new EvolucaoGastosDto(periodo.label(), granularidade, dados);
    }

    // =========================================================================
    //  GRÁFICO 2 — CATEGORIAS (stacked bar chart)
    // =========================================================================

    public CategoriasGraficoDto getCategoriasGrafico(TipoPeriodo tipo, int ano, Integer mes,
                                                      Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        Map<String, BigDecimal> totais     = new LinkedHashMap<>();
        Map<String, Integer>    ocorrencias = new LinkedHashMap<>();
        BigDecimal totalGeral = BigDecimal.ZERO;

        for (EventoFinanceiro e : eventos) {
            if (!emPeriodo(e, periodo)) continue;
            if (e.getTipo() != Tipo.Gasto && e.getTipo() != Tipo.Transferencia) continue;

            EventoDetalhe det = e.getGastoDetalhe();
            if (det == null || det.getCategoriaUsuario() == null) continue;

            for (CategoriaUsuario cu : det.getCategoriaUsuario()) {
                String nome = cu.getCategoria().getTitulo();
                totais.merge(nome, BigDecimal.valueOf(e.getValor()), BigDecimal::add);
                ocorrencias.merge(nome, 1, Integer::sum);
            }
            totalGeral = totalGeral.add(BigDecimal.valueOf(e.getValor()));
        }

        final BigDecimal tf = totalGeral;
        List<CategoriasGraficoDto.CategoriaData> lista = totais.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(entry -> {
                    int pct = tf.compareTo(BigDecimal.ZERO) > 0
                            ? entry.getValue().divide(tf, 4, RoundingMode.HALF_UP)
                                              .multiply(BigDecimal.valueOf(100)).intValue()
                            : 0;
                    return new CategoriasGraficoDto.CategoriaData(
                            entry.getKey(), entry.getValue(), pct,
                            ocorrencias.getOrDefault(entry.getKey(), 0));
                })
                .toList();

        return new CategoriasGraficoDto(periodo.label(), lista);
    }

    // =========================================================================
    //  GRÁFICO 3 — COMPARAÇÃO ENTRE PERÍODOS (multiple line chart)
    //  Ambas as séries são normalizadas por posição (Dia 1, Dia 2, … Dia N).
    // =========================================================================

    public ComparacaoPeriodoDto getComparacaoPeriodo(TipoPeriodo tipo, int ano, Integer mes,
                                                      Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        int diaFiscal    = getDiaFiscal(userId);
        Periodo atual    = PeriodoTemporalUtils.calcular(tipo, ano, mes, trimestre, semestre, diaFiscal);
        Periodo anterior = PeriodoTemporalUtils.calcularAnterior(tipo, ano, mes, trimestre, semestre, diaFiscal);

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        Map<LocalDate, BigDecimal> porDiaAtual    = gastosPorDia(eventos, atual.inicio(), atual.fim());
        Map<LocalDate, BigDecimal> porDiaAnterior = gastosPorDia(eventos, anterior.inicio(), anterior.fim());

        long duracaoAtual    = ChronoUnit.DAYS.between(atual.inicio(), atual.fim()) + 1;
        long duracaoAnterior = ChronoUnit.DAYS.between(anterior.inicio(), anterior.fim()) + 1;
        long numPontos = Math.min(duracaoAtual, duracaoAnterior);

        String labelDia = tipo == TipoPeriodo.ANUAL ? "Mês" : "Dia";

        List<ComparacaoPeriodoDto.PontoComparacao> pontos = new ArrayList<>();
        for (int i = 0; i < numPontos; i++) {
            LocalDate dAtual    = atual.inicio().plusDays(i);
            LocalDate dAnterior = anterior.inicio().plusDays(i);

            pontos.add(new ComparacaoPeriodoDto.PontoComparacao(
                    labelDia + " " + (i + 1),
                    porDiaAtual.getOrDefault(dAtual, BigDecimal.ZERO),
                    porDiaAnterior.getOrDefault(dAnterior, BigDecimal.ZERO)
            ));
        }

        return new ComparacaoPeriodoDto(atual.label(), anterior.label(), pontos);
    }

    // =========================================================================
    //  GRÁFICO 4 — GASTOS POR DIA DA SEMANA (heat map)
    // =========================================================================

    public HeatMapDiaSemanaDto getHeatMapDiaSemana(TipoPeriodo tipo, int ano, Integer mes,
                                                    Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        Map<DayOfWeek, BigDecimal> porDia = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) porDia.put(d, BigDecimal.ZERO);

        for (EventoFinanceiro e : eventos) {
            if (!emPeriodo(e, periodo)) continue;
            if (e.getTipo() != Tipo.Gasto && e.getTipo() != Tipo.Transferencia) continue;
            porDia.merge(e.getDataEvento().getDayOfWeek(), BigDecimal.valueOf(e.getValor()), BigDecimal::add);
        }

        BigDecimal max = porDia.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        Locale ptBr = new Locale("pt", "BR");

        DayOfWeek[] ordem = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };

        List<HeatMapDiaSemanaDto.DiaDado> dias = Arrays.stream(ordem)
                .map(d -> {
                    BigDecimal v = porDia.get(d);
                    double norm = max.compareTo(BigDecimal.ZERO) > 0
                            ? v.divide(max, 4, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    String nome = d.getDisplayName(TextStyle.FULL, ptBr);
                    nome = Character.toUpperCase(nome.charAt(0)) + nome.substring(1);
                    return new HeatMapDiaSemanaDto.DiaDado(nome, v, norm);
                })
                .toList();

        return new HeatMapDiaSemanaDto(periodo.label(), dias);
    }

    // =========================================================================
    //  GRÁFICO 5 — FLUXO FINANCEIRO (Sankey)
    //  Fluxo: Entrada → Instituição → Categoria/Transferência/Poupança
    // =========================================================================

    public FluxoFinanceiroDto getFluxoFinanceiro(TipoPeriodo tipo, int ano, Integer mes,
                                                  Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        // --- acumuladores ---
        Map<String, String>     instLabels    = new LinkedHashMap<>(); // id → label
        Map<String, BigDecimal> instEntrada   = new LinkedHashMap<>();
        Map<String, BigDecimal> instSaida     = new LinkedHashMap<>();
        Map<String, String>     catLabels     = new LinkedHashMap<>(); // id → label
        Map<String, BigDecimal> catTotal      = new LinkedHashMap<>();
        Map<String, BigDecimal> saidaSpecial  = new LinkedHashMap<>(); // "Transferência","Poupança"
        // links: source → target → valor
        Map<String, Map<String, BigDecimal>> flowMap = new LinkedHashMap<>();

        for (EventoFinanceiro e : eventos) {
            if (!emPeriodo(e, periodo)) continue;

            List<EventoInstituicao> insts = eventoInstituicaoRepository
                    .findEventoInstituicaoByEventoFinanceiro_Id(e.getId());
            String instNome = insts.isEmpty() ? "Sem Instituição"
                    : insts.get(0).getInstituicaoUsuario().getInstituicao().getNome();
            String instId = "inst_" + normalizeId(instNome);
            instLabels.put(instId, instNome);

            BigDecimal valor = BigDecimal.valueOf(e.getValor());

            switch (e.getTipo()) {
                case Recebimento, Emprestimo -> {
                    instEntrada.merge(instId, valor, BigDecimal::add);
                    // link: "entrada" → instituição
                    String entradaId = "entrada";
                    flowMap.computeIfAbsent(entradaId, k -> new LinkedHashMap<>())
                           .merge(instId, valor, BigDecimal::add);
                }
                case Gasto -> {
                    instSaida.merge(instId, valor, BigDecimal::add);
                    EventoDetalhe det = e.getGastoDetalhe();
                    if (det != null && det.getCategoriaUsuario() != null
                            && !det.getCategoriaUsuario().isEmpty()) {
                        int n = det.getCategoriaUsuario().size();
                        BigDecimal parcel = valor.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
                        for (CategoriaUsuario cu : det.getCategoriaUsuario()) {
                            String catNome = cu.getCategoria().getTitulo();
                            String catId   = "cat_" + normalizeId(catNome);
                            catLabels.put(catId, catNome);
                            catTotal.merge(catId, parcel, BigDecimal::add);
                            // link: instituição → categoria
                            flowMap.computeIfAbsent(instId, k -> new LinkedHashMap<>())
                                   .merge(catId, parcel, BigDecimal::add);
                        }
                    } else {
                        // sem categoria → nó "Outros"
                        String catId = "cat_outros";
                        catLabels.put(catId, "Outros");
                        catTotal.merge(catId, valor, BigDecimal::add);
                        flowMap.computeIfAbsent(instId, k -> new LinkedHashMap<>())
                               .merge(catId, valor, BigDecimal::add);
                    }
                }
                case Transferencia -> {
                    instSaida.merge(instId, valor, BigDecimal::add);
                    saidaSpecial.merge("saida_transferencia", valor, BigDecimal::add);
                    flowMap.computeIfAbsent(instId, k -> new LinkedHashMap<>())
                           .merge("saida_transferencia", valor, BigDecimal::add);
                }
                case Poupanca -> {
                    instSaida.merge(instId, valor, BigDecimal::add);
                    saidaSpecial.merge("saida_poupanca", valor, BigDecimal::add);
                    flowMap.computeIfAbsent(instId, k -> new LinkedHashMap<>())
                           .merge("saida_poupanca", valor, BigDecimal::add);
                }
                default -> { /* Emprestimo já tratado acima */ }
            }
        }

        // --- montar lista de nós ---
        List<FluxoFinanceiroDto.No> nos = new ArrayList<>();

        // nó virtual "Entrada"
        BigDecimal totalEntrada = instEntrada.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalEntrada.compareTo(BigDecimal.ZERO) > 0) {
            nos.add(new FluxoFinanceiroDto.No("entrada", "Entrada", "ENTRADA",
                    totalEntrada, BigDecimal.ZERO));
        }

        // nós de instituição
        Set<String> todasInst = new LinkedHashSet<>();
        todasInst.addAll(instEntrada.keySet());
        todasInst.addAll(instSaida.keySet());
        todasInst.forEach(id -> nos.add(new FluxoFinanceiroDto.No(
                id, instLabels.getOrDefault(id, id), "INSTITUICAO",
                instEntrada.getOrDefault(id, BigDecimal.ZERO),
                instSaida.getOrDefault(id, BigDecimal.ZERO))));

        // nós de categoria
        catLabels.forEach((id, label) -> nos.add(new FluxoFinanceiroDto.No(
                id, label, "CATEGORIA", BigDecimal.ZERO,
                catTotal.getOrDefault(id, BigDecimal.ZERO))));

        // nós especiais (Transferência / Poupança)
        if (saidaSpecial.containsKey("saida_transferencia")) {
            nos.add(new FluxoFinanceiroDto.No("saida_transferencia", "Transferência", "SAIDA",
                    BigDecimal.ZERO, saidaSpecial.get("saida_transferencia")));
        }
        if (saidaSpecial.containsKey("saida_poupanca")) {
            nos.add(new FluxoFinanceiroDto.No("saida_poupanca", "Poupança", "SAIDA",
                    BigDecimal.ZERO, saidaSpecial.get("saida_poupanca")));
        }

        // --- montar lista de links ---
        List<FluxoFinanceiroDto.Link> links = new ArrayList<>();
        flowMap.forEach((src, targets) ->
                targets.forEach((tgt, v) ->
                        links.add(new FluxoFinanceiroDto.Link(src, tgt, v))));

        return new FluxoFinanceiroDto(periodo.label(), nos, links);
    }

    // =========================================================================
    //  REGISTROS DO PERÍODO (lista completa — mantido)
    // =========================================================================

    public List<RegistroResponseDto> getRegistrosPorPeriodo(TipoPeriodo tipo, int ano, Integer mes,
                                                             Integer trimestre, Integer semestre, UUID userId) {
        usuarioService.getUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);

        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository
                .findByUsuarioAndPeriodoFiscal(userId, periodo.inicio(), periodo.fim());

        List<List<EventoInstituicao>> institList = new ArrayList<>();
        List<EventoDetalhe>           detalheList = new ArrayList<>();

        for (EventoFinanceiro e : eventos) {
            institList.add(eventoInstituicaoRepository
                    .findEventoInstituicaoByEventoFinanceiro_Id(e.getId()));
            detalheList.add(eventoDetalheRepository.findGastoDetalheByEventoFinanceiro(e));
        }

        return RegistrosMapper.toResponse(eventos, institList, detalheList);
    }

    // =========================================================================
    //  HELPERS PRIVADOS
    // =========================================================================

    private Periodo resolverPeriodo(UUID userId, TipoPeriodo tipo, int ano,
                                    Integer mes, Integer trimestre, Integer semestre) {
        return PeriodoTemporalUtils.calcular(tipo, ano, mes, trimestre, semestre, getDiaFiscal(userId));
    }

    /** Para KPI 1: corta o cálculo em hoje se o período ainda não terminou. */
    private LocalDate dataCorte(Periodo p) {
        LocalDate hoje = LocalDate.now();
        return p.fim().isAfter(hoje) ? hoje : p.fim();
    }

    private int getDiaFiscal(UUID userId) {
        Configuracoes c = configuracoesService.getConfiguracaoByUserId(userId);
        return c.getInicioMesFiscal() != null ? c.getInicioMesFiscal() : 1;
    }

    private void validarUsuario(UUID userId) {
        if (!usuarioRepository.existsById(userId)) {
            throw new EntidadeNaoEncontradaException(
                    "Usuário de id: %s não encontrado".formatted(userId));
        }
    }

    private boolean emPeriodo(EventoFinanceiro e, Periodo p) {
        LocalDate d = e.getDataEvento();
        return !d.isBefore(p.inicio()) && !d.isAfter(p.fim());
    }

    /**
     * Calcula a contribuição ao saldo total de acordo com as regras de negócio:
     * <ul>
     *   <li>Recebimento / Empréstimo → +valor</li>
     *   <li>Gasto / Transferência / Poupança → -valor</li>
     * </ul>
     */
    private BigDecimal saldoContribuicao(BigDecimal acc, EventoFinanceiro e) {
        BigDecimal v = BigDecimal.valueOf(e.getValor());
        return switch (e.getTipo()) {
            case Recebimento, Emprestimo   -> acc.add(v);
            case Gasto, Transferencia, Poupanca -> acc.subtract(v);
        };
    }

    /** Soma apenas Gasto + Transferência dentro do intervalo. */
    private BigDecimal somarGastos(List<EventoFinanceiro> eventos,
                                   LocalDate inicio, LocalDate fim) {
        BigDecimal total = BigDecimal.ZERO;
        for (EventoFinanceiro e : eventos) {
            LocalDate d = e.getDataEvento();
            if (!d.isBefore(inicio) && !d.isAfter(fim)
                    && (e.getTipo() == Tipo.Gasto || e.getTipo() == Tipo.Transferencia)) {
                total = total.add(BigDecimal.valueOf(e.getValor()));
            }
        }
        return total;
    }

    /** Agrupa gastos por nome de categoria. */
    private Map<String, BigDecimal> somarGastosPorCategoria(List<EventoFinanceiro> eventos,
                                                             LocalDate inicio, LocalDate fim) {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (EventoFinanceiro e : eventos) {
            LocalDate d = e.getDataEvento();
            if (!d.isBefore(inicio) && !d.isAfter(fim)
                    && (e.getTipo() == Tipo.Gasto || e.getTipo() == Tipo.Transferencia)) {
                EventoDetalhe det = e.getGastoDetalhe();
                if (det == null || det.getCategoriaUsuario() == null) continue;
                for (CategoriaUsuario cu : det.getCategoriaUsuario()) {
                    mapa.merge(cu.getCategoria().getTitulo(),
                               BigDecimal.valueOf(e.getValor()), BigDecimal::add);
                }
            }
        }
        return mapa;
    }

    /** Variação percentual entre valor atual e anterior (+ = piorou / - = melhorou nos gastos). */
    private int calcularVariacaoPercentual(BigDecimal atual, BigDecimal anterior) {
        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            return atual.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        return atual.subtract(anterior)
                .divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValue();
    }

    // ── séries temporais para o gráfico de evolução ──────────────────────────

    private Map<LocalDate, BigDecimal> gastosPorDia(List<EventoFinanceiro> eventos,
                                                     LocalDate inicio, LocalDate fim) {
        Map<LocalDate, BigDecimal> mapa = new TreeMap<>();
        for (EventoFinanceiro e : eventos) {
            LocalDate d = e.getDataEvento();
            if (!d.isBefore(inicio) && !d.isAfter(fim)
                    && (e.getTipo() == Tipo.Gasto || e.getTipo() == Tipo.Transferencia)) {
                mapa.merge(d, BigDecimal.valueOf(e.getValor()), BigDecimal::add);
            }
        }
        return mapa;
    }

    private List<EvolucaoGastosDto.Ponto> seriesDiaria(Map<LocalDate, BigDecimal> por,
                                                         LocalDate inicio, LocalDate fim,
                                                         String pattern) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
        List<EvolucaoGastosDto.Ponto> lista = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            lista.add(new EvolucaoGastosDto.Ponto(d.format(fmt),
                    por.getOrDefault(d, BigDecimal.ZERO)));
        }
        return lista;
    }

    private List<EvolucaoGastosDto.Ponto> seriesSemanais(Map<LocalDate, BigDecimal> por,
                                                          LocalDate inicio, LocalDate fim) {
        List<EvolucaoGastosDto.Ponto> lista = new ArrayList<>();
        int semana = 1;
        LocalDate semInicio = inicio;
        while (!semInicio.isAfter(fim)) {
            LocalDate semFim = semInicio.plusDays(6).isAfter(fim) ? fim : semInicio.plusDays(6);
            BigDecimal total = BigDecimal.ZERO;
            for (LocalDate d = semInicio; !d.isAfter(semFim); d = d.plusDays(1)) {
                total = total.add(por.getOrDefault(d, BigDecimal.ZERO));
            }
            lista.add(new EvolucaoGastosDto.Ponto("Sem " + semana, total));
            semana++;
            semInicio = semInicio.plusDays(7);
        }
        return lista;
    }

    private List<EvolucaoGastosDto.Ponto> seriesMensais(Map<LocalDate, BigDecimal> por,
                                                         LocalDate inicio, LocalDate fim) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM/yy",
                new Locale("pt", "BR"));
        List<EvolucaoGastosDto.Ponto> lista = new ArrayList<>();
        LocalDate mesInicio = inicio.withDayOfMonth(1);
        while (!mesInicio.isAfter(fim)) {
            LocalDate mesFim = mesInicio.withDayOfMonth(mesInicio.lengthOfMonth());
            BigDecimal total = BigDecimal.ZERO;
            for (LocalDate d = mesInicio; !d.isAfter(mesFim) && !d.isAfter(fim); d = d.plusDays(1)) {
                total = total.add(por.getOrDefault(d, BigDecimal.ZERO));
            }
            lista.add(new EvolucaoGastosDto.Ponto(mesInicio.format(fmt), total));
            mesInicio = mesInicio.plusMonths(1);
        }
        return lista;
    }

    // ── helpers de string ────────────────────────────────────────────────────

    private String tituloEvento(EventoFinanceiro e) {
        try { return e.getGastoDetalhe().getTituloGasto(); }
        catch (Exception ex) { return "Sem título"; }
    }

    private String categoriaEvento(EventoFinanceiro e) {
        try {
            return e.getGastoDetalhe().getCategoriaUsuario().getFirst().getCategoria().getTitulo();
        } catch (Exception ex) { return "Sem categoria"; }
    }

    private String normalizeId(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "_");
    }
}
