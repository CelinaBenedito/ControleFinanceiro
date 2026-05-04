package controle.api.back_end.service;

import controle.api.back_end.dto.dashboard.CategoriaEPorcentagens;
import controle.api.back_end.dto.dashboard.MaiorGastoDoMes;
import controle.api.back_end.dto.dashboard.GastoTotalDoMes;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.repository.*;
import controle.api.back_end.utils.MesFiscalUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final RegistroService registroService;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoDetalheRepository eventoDetalheRepository;
    private final InstituicaoService instituicaoService;
    private final UsuarioService usuarioService;
    private final ConfiguracoesService configuracoesService;
    private final ConfiguracoesRepository configuracoesRepository;

    public DashboardService(UsuarioRepository usuarioRepository,
                            RegistroService registroService,
                            EventoFinanceiroRepository eventoFinanceiroRepository,
                            EventoInstituicaoRepository eventoInstituicaoRepository,
                            EventoDetalheRepository eventoDetalheRepository,
                            InstituicaoService instituicaoService,
                            UsuarioService usuarioService,
                            ConfiguracoesService configuracoesService,
                            ConfiguracoesRepository configuracoesRepository) {
        this.usuarioRepository = usuarioRepository;
        this.registroService = registroService;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoDetalheRepository = eventoDetalheRepository;
        this.instituicaoService = instituicaoService;
        this.usuarioService = usuarioService;
        this.configuracoesService = configuracoesService;
        this.configuracoesRepository = configuracoesRepository;
    }

    public GastoTotalDoMes getGastoTotalDoMes(LocalDate data, UUID userId) {
        if (!usuarioRepository.existsById(userId)) {
            throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado"
                    .formatted(userId));
        }
        Configuracoes configuracoes = configuracoesService.getConfiguracaoByUserId(userId);
        Integer diaInicioMesFiscal = configuracoes.getInicioMesFiscal();
        MesFiscalUtils.PeriodoFiscal periodo = MesFiscalUtils.calcularPeriodoFiscal(data, diaInicioMesFiscal);

        LocalDate inicioMesFiscal=periodo.getInicio();
        LocalDate fimMesFiscal = periodo.getFim();

        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(userId);
        BigDecimal saldo = BigDecimal.ZERO;

        for (EventoFinanceiro evento : eventosFinanceiros){
            if (evento.getDataEvento().getMonth().equals(data.getMonth()) &&
                    evento.getDataEvento().getYear() == (data.getYear())
            ){
                saldo = InstituicaoService.getSaldo(saldo, evento);
            }
        }
        BigDecimal gastosMes = BigDecimal.ZERO;
        BigDecimal gastosMesAnterior = BigDecimal.ZERO;

        for (EventoFinanceiro evento : eventosFinanceiros) {
            // GASTOS do mês atual
            if (!evento.getDataEvento().isBefore(inicioMesFiscal) &&
                    !evento.getDataEvento().isAfter(fimMesFiscal) &&
                    evento.getTipo().equals(Tipo.Gasto) || evento.getTipo().equals(Tipo.Transferencia)) {
                gastosMes = gastosMes.add(BigDecimal.valueOf(evento.getValor()));
            }

            // GASTOS do mês anterior
            LocalDate mesAnterior = data.minusMonths(1);
            if (!evento.getDataEvento().isBefore(inicioMesFiscal) &&
                    !evento.getDataEvento().isAfter(fimMesFiscal) &&
                    evento.getTipo().equals(Tipo.Gasto) || evento.getTipo().equals(Tipo.Transferencia)) {
                gastosMesAnterior = gastosMesAnterior.add(BigDecimal.valueOf(evento.getValor()));
            }
        }

        BigDecimal diferenca = gastosMes.subtract(gastosMesAnterior);
        BigDecimal percentual = BigDecimal.ZERO;

        if (gastosMesAnterior.compareTo(BigDecimal.ZERO) != 0) {
            percentual = diferenca
                    .divide(gastosMesAnterior, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new GastoTotalDoMes(saldo, percentual.intValue());
    }

    public MaiorGastoDoMes getMaiorGastoDoMes(LocalDate data, UUID userId) {
            if (!usuarioRepository.existsById(userId)) {
                throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado"
                        .formatted(userId));
            }

        Configuracoes configuracoes = configuracoesService.getConfiguracaoByUserId(userId);

        Integer diaInicioMesFiscal = configuracoes.getInicioMesFiscal();
        MesFiscalUtils.PeriodoFiscal periodo = MesFiscalUtils.calcularPeriodoFiscal(data, diaInicioMesFiscal);

        LocalDate inicioMesFiscal=periodo.getInicio();
        LocalDate fimMesFiscal = periodo.getFim();

        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(userId);

            BigDecimal maiorValor = BigDecimal.ZERO;
            EventoFinanceiro maiorEvento = null;
            BigDecimal saldo = BigDecimal.ZERO;

            for (EventoFinanceiro evento : eventosFinanceiros) {
                if (!evento.getDataEvento().isBefore(inicioMesFiscal) && !evento.getDataEvento().isAfter(fimMesFiscal)) {

                    if (evento.getTipo().equals(Tipo.Recebimento)) {
                        saldo = saldo.add(BigDecimal.valueOf(evento.getValor()));
                    }

                    if (evento.getTipo().equals(Tipo.Gasto)) {
                        if (maiorValor.compareTo(BigDecimal.valueOf(evento.getValor())) < 0) {
                            maiorValor = BigDecimal.valueOf(evento.getValor());
                            maiorEvento = evento;
                        }
                    }
                }
            }

            if (maiorEvento == null) {
                return new MaiorGastoDoMes("Sem evento", 0.0, 0);
            }

            if (saldo.compareTo(BigDecimal.ZERO) == 0) {
                return new MaiorGastoDoMes(
                        maiorEvento.getGastoDetalhe().getCategoriaUsuario().getFirst().getCategoria().getTitulo(),
                        maiorEvento.getValor(),
                        0
                );
            }

            BigDecimal percentual = maiorValor
                    .divide(saldo, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            return new MaiorGastoDoMes(
                    maiorEvento.getGastoDetalhe().getCategoriaUsuario().getFirst().getCategoria().getTitulo(),
                    maiorEvento.getValor(),
                    percentual.intValue()
            );
        }

    public CategoriaEPorcentagens getCategoriasEPorcentagens(LocalDate data, UUID userId) {
        usuarioService.getUsuario(userId);

        Configuracoes configuracoes = configuracoesService.getConfiguracaoByUserId(userId);
        Integer diaInicioMesFiscal = configuracoes.getInicioMesFiscal();
        MesFiscalUtils.PeriodoFiscal periodo = MesFiscalUtils.calcularPeriodoFiscal(data, diaInicioMesFiscal);

        LocalDate inicioMesFiscal = periodo.getInicio();
        LocalDate fimMesFiscal = periodo.getFim();

        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(userId);

        // Mapa para acumular valores por categoria
        Map<String, BigDecimal> valoresPorCategoria = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (EventoFinanceiro evento : eventosFinanceiros) {
            if (!evento.getDataEvento().isBefore(inicioMesFiscal) &&
                    !evento.getDataEvento().isAfter(fimMesFiscal) &&
                    (evento.getTipo().equals(Tipo.Gasto) || evento.getTipo().equals(Tipo.Transferencia))) {

                EventoDetalhe detalhe = evento.getGastoDetalhe();
                if (detalhe != null && detalhe.getCategoriaUsuario() != null) {
                    for (CategoriaUsuario categoriaUsuario : detalhe.getCategoriaUsuario()) {
                        String nomeCategoria = categoriaUsuario.getCategoria().getTitulo();
                        valoresPorCategoria.put(nomeCategoria,
                                valoresPorCategoria.getOrDefault(nomeCategoria, BigDecimal.ZERO)
                                        .add(BigDecimal.valueOf(evento.getValor())));
                    }
                }
                total = total.add(BigDecimal.valueOf(evento.getValor()));
            }
        }

        List<String> categorias = new ArrayList<>();
        List<Integer> porcentagens = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : valoresPorCategoria.entrySet()) {
            categorias.add(entry.getKey());
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentual = entry.getValue()
                        .divide(total, 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                porcentagens.add(percentual.intValue());
            } else {
                porcentagens.add(0);
            }
        }

        CategoriaEPorcentagens resultado = new CategoriaEPorcentagens();
        resultado.setCategorias(categorias);
        resultado.setPorcentagens(porcentagens);
        return resultado;
    }

    public List<RegistroResponseDto> getGastosPorPeriodoDeTempo(LocalDate data, UUID userId){
        usuarioService.getUsuario(userId);
        Configuracoes configuracoes = configuracoesService.getConfiguracaoByUserId(userId);
        Integer diaInicioMesFiscal = configuracoes.getInicioMesFiscal();
        MesFiscalUtils.PeriodoFiscal periodo = MesFiscalUtils.calcularPeriodoFiscal(data, diaInicioMesFiscal);

        LocalDate inicioMesFiscal = periodo.getInicio();
        LocalDate fimMesFiscal = periodo.getFim();

        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findByUsuarioAndPeriodoFiscal(userId, inicioMesFiscal, fimMesFiscal);

        List<List<EventoInstituicao>> eventosInstituicoes = new ArrayList<>();
        List<EventoDetalhe> eventoDetalhes = new ArrayList<>();

        for (EventoFinanceiro evento : eventosFinanceiros){

                List<EventoInstituicao> eventoInstituicao = eventoInstituicaoRepository.findEventoInstituicaoByEventoFinanceiro_Id(evento.getId());
                eventosInstituicoes.add(eventoInstituicao);
                eventoDetalhes.add(eventoDetalheRepository.findGastoDetalheByEventoFinanceiro(evento));

        }

        return RegistrosMapper.toResponse(eventosFinanceiros,eventosInstituicoes,eventoDetalhes);
    }
    }
