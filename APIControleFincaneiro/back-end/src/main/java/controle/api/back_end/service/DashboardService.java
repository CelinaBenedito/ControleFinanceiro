package controle.api.back_end.service;

import controle.api.back_end.dto.dashboard.out.*;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.dashboard.NivelSaudeFinanceira;
import controle.api.back_end.model.dashboard.TipoPeriodo;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.poupanca.Caixinha;
import controle.api.back_end.repository.configuracoes.ConfiguracoesRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.poupanca.CaixinhaRepository;
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
    private final CaixinhaRepository caixinhaRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;

    public DashboardService(UsuarioRepository usuarioRepository,
                            RegistroService registroService,
                            EventoFinanceiroRepository eventoFinanceiroRepository,
                            EventoInstituicaoRepository eventoInstituicaoRepository,
                            EventoDetalheRepository eventoDetalheRepository,
                            InstituicaoService instituicaoService,
                            UsuarioService usuarioService,
                            ConfiguracoesService configuracoesService,
                            ConfiguracoesRepository configuracoesRepository,
                            CaixinhaRepository caixinhaRepository,
                            InstituicaoUsuarioRepository instituicaoUsuarioRepository) {
        this.usuarioRepository        = usuarioRepository;
        this.registroService          = registroService;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoDetalheRepository  = eventoDetalheRepository;
        this.instituicaoService       = instituicaoService;
        this.usuarioService           = usuarioService;
        this.configuracoesService     = configuracoesService;
        this.configuracoesRepository  = configuracoesRepository;
        this.caixinhaRepository       = caixinhaRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
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

        // daily map de gastos e de recebimentos
        Map<LocalDate, BigDecimal> porDia    = new TreeMap<>();
        Map<LocalDate, BigDecimal> porDiaRec = new TreeMap<>();
        for (EventoFinanceiro e : eventos) {
            if (!emPeriodo(e, periodo)) continue;
            if (e.getTipo() == Tipo.Gasto) {
                porDia.merge(e.getDataEvento(), BigDecimal.valueOf(e.getValor()), BigDecimal::add);
            } else if (e.getTipo() == Tipo.Recebimento || e.getTipo() == Tipo.Emprestimo) {
                porDiaRec.merge(e.getDataEvento(), BigDecimal.valueOf(e.getValor()), BigDecimal::add);
            }
        }

        String granularidade;
        List<EvolucaoGastosDto.Ponto> dados;
        List<EvolucaoGastosDto.Ponto> dadosRec;

        switch (tipo) {
            case MENSAL, TRIMESTRAL -> {
                granularidade = "DIARIO";
                dados    = seriesDiaria(porDia,    periodo.inicio(), periodo.fim(), "dd/MM");
                dadosRec = seriesDiaria(porDiaRec, periodo.inicio(), periodo.fim(), "dd/MM");
            }
            case SEMESTRAL -> {
                granularidade = "SEMANAL";
                dados    = seriesSemanais(porDia,    periodo.inicio(), periodo.fim());
                dadosRec = seriesSemanais(porDiaRec, periodo.inicio(), periodo.fim());
            }
            default -> { // ANUAL
                granularidade = "MENSAL";
                dados    = seriesMensais(porDia,    periodo.inicio(), periodo.fim());
                dadosRec = seriesMensais(porDiaRec, periodo.inicio(), periodo.fim());
            }
        }

        return new EvolucaoGastosDto(periodo.label(), granularidade, dados, dadosRec);
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
            if (e.getTipo() != Tipo.Gasto) continue;

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

        // Para ANUAL: granularidade mensal (12 pontos com nome do mês)
        if (tipo == TipoPeriodo.ANUAL) {
            DateTimeFormatter fmtMes = DateTimeFormatter.ofPattern("MMM", new Locale("pt", "BR"));
            List<ComparacaoPeriodoDto.PontoComparacao> pontos = new ArrayList<>();
            for (int m = 0; m < 12; m++) {
                LocalDate inicioAtual    = atual.inicio().withDayOfMonth(1).plusMonths(m);
                LocalDate fimAtual       = inicioAtual.withDayOfMonth(inicioAtual.lengthOfMonth());
                LocalDate inicioAnterior = anterior.inicio().withDayOfMonth(1).plusMonths(m);
                LocalDate fimAnterior    = inicioAnterior.withDayOfMonth(inicioAnterior.lengthOfMonth());
                if (inicioAtual.isAfter(atual.fim())) break;
                String nomeMes = inicioAtual.format(fmtMes);
                nomeMes = Character.toUpperCase(nomeMes.charAt(0)) + nomeMes.substring(1);
                pontos.add(new ComparacaoPeriodoDto.PontoComparacao(
                        nomeMes,
                        somarRange(porDiaAtual,    inicioAtual,    fimAtual.isAfter(atual.fim())         ? atual.fim()     : fimAtual),
                        somarRange(porDiaAnterior, inicioAnterior, fimAnterior.isAfter(anterior.fim())   ? anterior.fim()  : fimAnterior)
                ));
            }
            return new ComparacaoPeriodoDto(atual.label(), anterior.label(), pontos);
        }

        long duracaoAtual    = ChronoUnit.DAYS.between(atual.inicio(), atual.fim()) + 1;
        long duracaoAnterior = ChronoUnit.DAYS.between(anterior.inicio(), anterior.fim()) + 1;
        long numPontos = Math.min(duracaoAtual, duracaoAnterior);

        List<ComparacaoPeriodoDto.PontoComparacao> pontos = new ArrayList<>();
        for (int i = 0; i < numPontos; i++) {
            LocalDate dAtual    = atual.inicio().plusDays(i);
            LocalDate dAnterior = anterior.inicio().plusDays(i);

            pontos.add(new ComparacaoPeriodoDto.PontoComparacao(
                    "Dia " + (i + 1),
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
            if (e.getTipo() != Tipo.Gasto) continue;
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
        try {
            Configuracoes c = configuracoesService.getConfiguracaoByUserId(userId);
            return c.getInicioMesFiscal() != null ? c.getInicioMesFiscal() : 1;
        } catch (Exception e) {
            // Usuário sem configuração → usa o dia 1 como padrão
            return 1;
        }
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

    /** Soma apenas Gastos (sem transferências) dentro do intervalo. */
    private BigDecimal somarGastos(List<EventoFinanceiro> eventos,
                                   LocalDate inicio, LocalDate fim) {
        BigDecimal total = BigDecimal.ZERO;
        for (EventoFinanceiro e : eventos) {
            LocalDate d = e.getDataEvento();
            if (!d.isBefore(inicio) && !d.isAfter(fim) && e.getTipo() == Tipo.Gasto) {
                total = total.add(BigDecimal.valueOf(e.getValor()));
            }
        }
        return total;
    }

    /** Agrupa apenas gastos (sem transferências) por nome de categoria. */
    private Map<String, BigDecimal> somarGastosPorCategoria(List<EventoFinanceiro> eventos,
                                                             LocalDate inicio, LocalDate fim) {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (EventoFinanceiro e : eventos) {
            LocalDate d = e.getDataEvento();
            if (!d.isBefore(inicio) && !d.isAfter(fim) && e.getTipo() == Tipo.Gasto) {
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

    /** Soma valores de um map Date→BigDecimal num intervalo fechado [inicio, fim]. */
    private BigDecimal somarRange(Map<LocalDate, BigDecimal> mapa, LocalDate inicio, LocalDate fim) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            total = total.add(mapa.getOrDefault(d, BigDecimal.ZERO));
        }
        return total;
    }

    private Map<LocalDate, BigDecimal> gastosPorDia(List<EventoFinanceiro> eventos,
                                                     LocalDate inicio, LocalDate fim) {
        Map<LocalDate, BigDecimal> mapa = new TreeMap<>();
        for (EventoFinanceiro e : eventos) {
            LocalDate d = e.getDataEvento();
            if (!d.isBefore(inicio) && !d.isAfter(fim) && e.getTipo() == Tipo.Gasto) {
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

    /**
     * Retorna os anos distintos em que o usuário possui registros, mais recente primeiro.
     * Garante que o ano corrente sempre conste na lista, mesmo sem registros.
     */
    public List<Integer> getAnosDisponiveis(UUID userId) {
        List<Integer> anos = new ArrayList<>(eventoFinanceiroRepository.findDistinctAnosByUserId(userId));
        int anoAtual = LocalDate.now().getYear();
        if (!anos.contains(anoAtual)) {
            anos.add(0, anoAtual);
        }
        anos.sort(Comparator.reverseOrder());
        return anos;
    }

    // =========================================================================
    //  KPI — POUPANÇA (agregado de todas as caixinhas ativas)
    // =========================================================================
    public KpiPoupancaDto getKpiPoupanca(UUID userId) {
        validarUsuario(userId);
        List<Caixinha> caixinhas = caixinhaRepository.findAllByUsuario_IdAndIsAtivaTrue(userId);
        if (caixinhas.isEmpty()) {
            return new KpiPoupancaDto("Sem poupança ativa", BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, "Nenhuma caixinha ativa encontrada");
        }
        BigDecimal valorGuardado = BigDecimal.ZERO;
        BigDecimal valorMeta = BigDecimal.ZERO;
        for (Caixinha c : caixinhas) {
            BigDecimal aportado = eventoFinanceiroRepository.sumValorByCaixinha(c.getId());
            valorGuardado = valorGuardado.add(aportado);
            if (c.getValorMeta() != null) valorMeta = valorMeta.add(c.getValorMeta());
        }
        int pct = valorMeta.compareTo(BigDecimal.ZERO) > 0
                ? valorGuardado.divide(valorMeta, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue()
                : 0;
        BigDecimal faltante = valorMeta.subtract(valorGuardado).max(BigDecimal.ZERO);
        String nome = caixinhas.size() == 1 ? caixinhas.get(0).getNome() : "Poupança Total";
        String descricao = nome + " — " + pct + "% da meta atingida";
        return new KpiPoupancaDto(nome, valorGuardado, valorMeta, pct, faltante, descricao);
    }

    // =========================================================================
    //  KPI — EMPRÉSTIMO ATIVO (mais recente)
    // =========================================================================
    public KpiEmprestimoDto getKpiEmprestimo(UUID userId) {
        validarUsuario(userId);
        List<EventoFinanceiro> todos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);
        EventoFinanceiro emprestimo = todos.stream()
                .filter(e -> e.getTipo() == Tipo.Emprestimo)
                .max(Comparator.comparing(EventoFinanceiro::getDataEvento))
                .orElse(null);
        if (emprestimo == null) {
            return new KpiEmprestimoDto(false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0, "N/A");
        }
        List<EventoInstituicao> insts = eventoInstituicaoRepository.findEventoInstituicaoByEventoFinanceiro_Id(emprestimo.getId());
        int parcelasTotal = insts.isEmpty() ? 1 : insts.get(0).getParcelas();
        String nomeInst = insts.isEmpty() ? "N/A" : insts.get(0).getInstituicaoUsuario().getInstituicao().getNome();
        Double taxaJuros = insts.isEmpty() ? null : insts.get(0).getInstituicaoUsuario().getTaxaJuros();

        long mesesDecorridos = java.time.temporal.ChronoUnit.MONTHS.between(emprestimo.getDataEvento(), LocalDate.now());
        int parcelasPagas = (int) Math.min(mesesDecorridos, parcelasTotal);
        int parcelasFaltantes = parcelasTotal - parcelasPagas;

        BigDecimal valorTotal = BigDecimal.valueOf(emprestimo.getValor());

        double r = (taxaJuros != null && taxaJuros > 0) ? taxaJuros / 100.0 : 0.0;
        double pmt;
        if (r < 1e-10) {
            pmt = valorTotal.doubleValue() / parcelasTotal;
        } else {
            double fator = Math.pow(1 + r, parcelasTotal);
            pmt = valorTotal.doubleValue() * r * fator / (fator - 1);
        }
        double totalPago = pmt * parcelasPagas;

        double saldoDevedor;
        if (r < 1e-10) {
            saldoDevedor = valorTotal.doubleValue() - (valorTotal.doubleValue() / parcelasTotal) * parcelasPagas;
        } else {
            double fatorK = Math.pow(1 + r, parcelasPagas);
            saldoDevedor = valorTotal.doubleValue() * fatorK - pmt * (fatorK - 1) / r;
        }
        double principalPago = valorTotal.doubleValue() - Math.max(saldoDevedor, 0);
        double jurosPagosVal = totalPago - principalPago;

        int pct = parcelasTotal > 0 ? (parcelasPagas * 100 / parcelasTotal) : 0;

        return new KpiEmprestimoDto(
                true,
                valorTotal,
                BigDecimal.valueOf(Math.max(totalPago, 0)).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(Math.max(saldoDevedor, 0)).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(Math.max(jurosPagosVal, 0)).setScale(2, RoundingMode.HALF_UP),
                parcelasTotal,
                parcelasPagas,
                parcelasFaltantes,
                pct,
                nomeInst
        );
    }

    // =========================================================================
    //  KPI — SAÚDE FINANCEIRA
    // =========================================================================
    public KpiSaudeFinanceiraDto getKpiSaudeFinanceira(TipoPeriodo tipo, int ano, Integer mes, Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);
        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        BigDecimal receita = BigDecimal.ZERO;
        BigDecimal gastos = BigDecimal.ZERO;
        BigDecimal transferencias = BigDecimal.ZERO;
        for (EventoFinanceiro e : eventos) {
            if (!emPeriodo(e, periodo)) continue;
            BigDecimal v = BigDecimal.valueOf(e.getValor());
            if (e.getTipo() == Tipo.Recebimento || e.getTipo() == Tipo.Emprestimo) receita = receita.add(v);
            else if (e.getTipo() == Tipo.Gasto) gastos = gastos.add(v);
            else if (e.getTipo() == Tipo.Transferencia) transferencias = transferencias.add(v);
        }
        BigDecimal totalSaidas = gastos.add(transferencias);

        int pontos = 0;
        String motivoPrincipal = "";

        // A) Relação gastos/receita (40 pts)
        if (receita.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = totalSaidas.divide(receita, 4, RoundingMode.HALF_UP).doubleValue();
            if (ratio <= 0.5) { pontos += 40; }
            else if (ratio <= 0.7) { pontos += 30; }
            else if (ratio <= 0.9) { pontos += 20; motivoPrincipal = "Seus gastos estão elevados. Reduza despesas variáveis para melhorar."; }
            else if (ratio <= 1.0) { pontos += 10; motivoPrincipal = "Seus gastos estão muito próximos da sua receita. Atenção ao orçamento!"; }
            else { motivoPrincipal = "Você está gastando mais do que ganha. Revise seus gastos urgentemente!"; }
        } else {
            motivoPrincipal = "Nenhuma receita registrada no período.";
        }

        // B) Poupança ativa (30 pts)
        List<Caixinha> caixinhas = caixinhaRepository.findAllByUsuario_IdAndIsAtivaTrue(userId);
        if (!caixinhas.isEmpty()) {
            BigDecimal totalPoupado = BigDecimal.ZERO;
            for (Caixinha c : caixinhas) totalPoupado = totalPoupado.add(eventoFinanceiroRepository.sumValorByCaixinha(c.getId()));
            if (receita.compareTo(BigDecimal.ZERO) > 0) {
                double taxaPoupanca = totalPoupado.divide(receita, 4, RoundingMode.HALF_UP).doubleValue();
                if (taxaPoupanca >= 0.2) pontos += 30;
                else if (taxaPoupanca >= 0.1) pontos += 20;
                else if (taxaPoupanca > 0) pontos += 10;
            }
        }

        // C) Sem empréstimos ativos (20 pts)
        boolean temEmprestimo = eventos.stream().anyMatch(e -> e.getTipo() == Tipo.Emprestimo);
        if (!temEmprestimo) pontos += 20;
        else pontos += 5;

        // D) Utilização de crédito (10 pts)
        List<InstituicaoUsuario> instList = instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
        BigDecimal totalLimite = BigDecimal.ZERO;
        BigDecimal totalCreditoUsado = BigDecimal.ZERO;
        for (InstituicaoUsuario iu : instList) {
            if (iu.getLimiteCredito() != null && iu.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0) {
                totalLimite = totalLimite.add(iu.getLimiteCredito());
            }
            for (EventoInstituicao ei : eventoInstituicaoRepository.findByInstituicaoUsuario_Id(iu.getId())) {
                if (ei.getTipoMovimento() == TipoMovimento.Credito && emPeriodo(ei.getEventoFinanceiro(), periodo)) {
                    totalCreditoUsado = totalCreditoUsado.add(BigDecimal.valueOf(ei.getValor()));
                }
            }
        }
        if (totalLimite.compareTo(BigDecimal.ZERO) > 0) {
            double pctCredito = totalCreditoUsado.divide(totalLimite, 4, RoundingMode.HALF_UP).doubleValue();
            if (pctCredito < 0.3) pontos += 10;
            else if (pctCredito < 0.6) pontos += 5;
        } else {
            pontos += 10;
        }

        pontos = Math.min(100, Math.max(0, pontos));

        NivelSaudeFinanceira nivel;
        if (pontos >= 80) nivel = NivelSaudeFinanceira.NORMAL;
        else if (pontos >= 60) nivel = NivelSaudeFinanceira.ATENCAO;
        else if (pontos >= 40) nivel = NivelSaudeFinanceira.BAIXO;
        else nivel = NivelSaudeFinanceira.CRITICO;

        if (motivoPrincipal.isEmpty()) motivoPrincipal = "Suas finanças estão sob controle. Continue assim!";

        return new KpiSaudeFinanceiraDto(pontos, nivel, motivoPrincipal, periodo.label());
    }

    // =========================================================================
    //  HISTÓRIA FINANCEIRA
    // =========================================================================
    public HistoriaFinanceiraDto getHistoriaFinanceira(TipoPeriodo tipo, int ano, Integer mes, Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);
        int diaFiscal = getDiaFiscal(userId);
        Periodo atual = PeriodoTemporalUtils.calcular(tipo, ano, mes, trimestre, semestre, diaFiscal);
        Periodo anterior = PeriodoTemporalUtils.calcularAnterior(tipo, ano, mes, trimestre, semestre, diaFiscal);
        List<EventoFinanceiro> todos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);

        BigDecimal gastosAtual = somarGastos(todos, atual.inicio(), atual.fim());
        BigDecimal gastosAnterior = somarGastos(todos, anterior.inicio(), anterior.fim());
        BigDecimal difGastos = gastosAtual.subtract(gastosAnterior).abs();

        Map<DayOfWeek, BigDecimal> porDiaSemana = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) porDiaSemana.put(d, BigDecimal.ZERO);
        for (EventoFinanceiro e : todos) {
            if (!emPeriodo(e, atual) || e.getTipo() != Tipo.Gasto) continue;
            porDiaSemana.merge(e.getDataEvento().getDayOfWeek(), BigDecimal.valueOf(e.getValor()), BigDecimal::add);
        }
        DayOfWeek diaPico = porDiaSemana.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        Map<String, BigDecimal> porCategoria = somarGastosPorCategoria(todos, atual.inicio(), atual.fim());
        String catTopo = null; BigDecimal catTopVal = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : porCategoria.entrySet()) {
            if (entry.getValue().compareTo(catTopVal) > 0) { catTopVal = entry.getValue(); catTopo = entry.getKey(); }
        }

        String nomePeriodo = atual.label();
        String titulo = "Sua história financeira de " + nomePeriodo.toLowerCase(Locale.ROOT);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Você gastou R$ %s", gastosAtual.setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", ",")));
        if (tipo == TipoPeriodo.MENSAL) {
            sb.append(" este mês");
        } else {
            sb.append(" neste período");
        }

        if (gastosAnterior.compareTo(BigDecimal.ZERO) > 0) {
            boolean maisQueAntes = gastosAtual.compareTo(gastosAnterior) > 0;
            String periodoAnteriorLabel = anterior.label().toLowerCase(Locale.ROOT);
            sb.append(String.format(" — R$ %s %s que em %s.",
                    difGastos.setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", ","),
                    maisQueAntes ? "a mais" : "a menos",
                    periodoAnteriorLabel));
        } else {
            sb.append(".");
        }

        Locale ptBr = new Locale("pt", "BR");
        if (diaPico != null && porDiaSemana.get(diaPico).compareTo(BigDecimal.ZERO) > 0) {
            String nomeDia = diaPico.getDisplayName(TextStyle.FULL, ptBr);
            nomeDia = Character.toUpperCase(nomeDia.charAt(0)) + nomeDia.substring(1) + "s";
            sb.append(" Suas ").append(nomeDia).append(" são responsáveis pelo pico de gastos.");
        }

        if (catTopo != null && gastosAtual.compareTo(BigDecimal.ZERO) > 0) {
            int pctCat = catTopVal.divide(gastosAtual, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue();
            sb.append(String.format(" %s sozinho consome %d%% de tudo que você gastou.", catTopo, pctCat));
        }

        return new HistoriaFinanceiraDto(titulo, sb.toString(), atual.label());
    }

    // =========================================================================
    //  KPI — INSTITUIÇÃO MAIS UTILIZADA
    // =========================================================================
    public KpiInstituicaoMaisUtilizadaDto getKpiInstituicaoMaisUtilizada(TipoPeriodo tipo, int ano, Integer mes, Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);
        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);

        List<InstituicaoUsuario> instList = instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
        Map<String, Integer> transacoesPorInst = new LinkedHashMap<>();

        for (InstituicaoUsuario iu : instList) {
            List<EventoInstituicao> eis = eventoInstituicaoRepository.findByInstituicaoUsuario_Id(iu.getId());
            int count = (int) eis.stream()
                    .filter(ei -> {
                        EventoFinanceiro ef = ei.getEventoFinanceiro();
                        if (ef == null) return false;
                        return emPeriodo(ef, periodo) && (ef.getTipo() == Tipo.Gasto || ef.getTipo() == Tipo.Transferencia);
                    }).count();
            if (count > 0) transacoesPorInst.put(iu.getInstituicao().getNome(), count);
        }

        if (transacoesPorInst.isEmpty()) {
            return new KpiInstituicaoMaisUtilizadaDto("N/A", 0, 0.0, periodo.label());
        }

        String maisUsada = transacoesPorInst.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("N/A");
        int transMaisUsada = transacoesPorInst.get(maisUsada);

        int somaOutras = transacoesPorInst.entrySet().stream().filter(e -> !e.getKey().equals(maisUsada)).mapToInt(Map.Entry::getValue).sum();
        double pctVantagem = somaOutras > 0 ? ((double)(transMaisUsada - somaOutras / Math.max(1, transacoesPorInst.size() - 1)) / somaOutras * 100.0) : 100.0;

        return new KpiInstituicaoMaisUtilizadaDto(maisUsada, transMaisUsada, Math.max(0, pctVantagem), periodo.label());
    }

    // =========================================================================
    //  KPI — PARCELAMENTOS ATIVOS
    // =========================================================================
    public KpiParcelamentosAtivosDto getKpiParcelamentosAtivos(UUID userId) {
        validarUsuario(userId);
        List<InstituicaoUsuario> instList = instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
        LocalDate hoje = LocalDate.now();
        int totalAtivos = 0;
        for (InstituicaoUsuario iu : instList) {
            List<EventoInstituicao> eis = eventoInstituicaoRepository.findByInstituicaoUsuario_Id(iu.getId());
            for (EventoInstituicao ei : eis) {
                if (ei.getParcelas() != null && ei.getParcelas() > 1 && ei.getEventoFinanceiro() != null) {
                    LocalDate fimParcelamento = ei.getEventoFinanceiro().getDataEvento().plusMonths(ei.getParcelas());
                    if (!fimParcelamento.isBefore(hoje)) totalAtivos++;
                }
            }
        }
        return new KpiParcelamentosAtivosDto(totalAtivos);
    }

    // =========================================================================
    //  KPI — MAIOR GASTO MÉDIO POR TRANSAÇÃO (por instituição)
    // =========================================================================
    public KpiMaiorGastoMedioInstituicaoDto getKpiMaiorGastoMedioInstituicao(TipoPeriodo tipo, int ano, Integer mes, Integer trimestre, Integer semestre, UUID userId) {
        validarUsuario(userId);
        PeriodoTemporalUtils.validar(tipo, ano, mes, trimestre, semestre);
        Periodo periodo = resolverPeriodo(userId, tipo, ano, mes, trimestre, semestre);

        List<InstituicaoUsuario> instList = instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
        String melhorInst = "N/A";
        BigDecimal melhorMedia = BigDecimal.ZERO;
        int melhorCount = 0;

        for (InstituicaoUsuario iu : instList) {
            List<EventoInstituicao> eis = eventoInstituicaoRepository.findByInstituicaoUsuario_Id(iu.getId());
            BigDecimal total = BigDecimal.ZERO;
            int count = 0;
            for (EventoInstituicao ei : eis) {
                EventoFinanceiro ef = ei.getEventoFinanceiro();
                if (ef == null || !emPeriodo(ef, periodo)) continue;
                if (ef.getTipo() == Tipo.Gasto || ef.getTipo() == Tipo.Transferencia) {
                    total = total.add(BigDecimal.valueOf(ei.getValor()));
                    count++;
                }
            }
            if (count > 0) {
                BigDecimal media = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
                if (media.compareTo(melhorMedia) > 0) {
                    melhorMedia = media;
                    melhorInst = iu.getInstituicao().getNome();
                    melhorCount = count;
                }
            }
        }

        return new KpiMaiorGastoMedioInstituicaoDto(melhorInst, melhorMedia, melhorCount, periodo.label());
    }
}
