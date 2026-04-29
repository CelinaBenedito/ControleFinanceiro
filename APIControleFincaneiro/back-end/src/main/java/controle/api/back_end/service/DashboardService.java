package controle.api.back_end.service;

import controle.api.back_end.dto.MaiorGastoDoMes;
import controle.api.back_end.dto.dashboard.GastoTotalDoMes;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.repository.ConfiguracoesRepository;
import controle.api.back_end.repository.EventoFinanceiroRepository;
import controle.api.back_end.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final RegistroService registroService;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final InstituicaoService instituicaoService;
    private final UsuarioService usuarioService;
    private final ConfiguracoesService configuracoesService;
    private final ConfiguracoesRepository configuracoesRepository;

    public DashboardService(UsuarioRepository usuarioRepository,
                            RegistroService registroService,
                            EventoFinanceiroRepository eventoFinanceiroRepository,
                            InstituicaoService instituicaoService,
                            UsuarioService usuarioService,
                            ConfiguracoesService configuracoesService,
                            ConfiguracoesRepository configuracoesRepository) {
        this.usuarioRepository = usuarioRepository;
        this.registroService = registroService;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.instituicaoService = instituicaoService;
        this.usuarioService = usuarioService;
        this.configuracoesService = configuracoesService;
        this.configuracoesRepository = configuracoesRepository;
    }


    public GastoTotalDoMes getGastoTotaldoMesAtual(LocalDate data, UUID userId){
        if(!usuarioRepository.existsById(userId)){
            throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado"
                    .formatted(userId));
        }
        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(userId);
        BigDecimal saldo = BigDecimal.ZERO;
        BigDecimal saldoPassado = BigDecimal.ZERO;
        for (EventoFinanceiro evento: eventosFinanceiros){
            if (evento.getDataEvento().getMonth().equals(data.getMonth())){
                saldo = InstituicaoService.getSaldo(saldo, evento);
            }
            if (evento.getDataEvento().getMonth().equals(data.getMonth().minus(1))){
                saldoPassado = InstituicaoService.getSaldo(saldoPassado, evento);
            }
        }


        BigDecimal diferenca = saldo.subtract(saldoPassado);
        BigDecimal percentual = BigDecimal.ZERO;

        if (saldoPassado.compareTo(BigDecimal.ZERO) != 0) {
            percentual = diferenca
                    .divide(saldoPassado, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new GastoTotalDoMes(saldo, percentual.intValue());
    }
    //POR A DATA
    public MaiorGastoDoMes getMaiorGastoDoMes(LocalDate data, UUID userId){
        if(!usuarioRepository.existsById(userId)){
            throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado"
                    .formatted(userId));
        }
        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository.findEventoFinanceiroByUsuario_Id(userId);

        BigDecimal maiorValor = BigDecimal.valueOf(eventosFinanceiros.getFirst().getValor());
        EventoFinanceiro maiorEvento = eventosFinanceiros.getFirst();

        BigDecimal saldo = BigDecimal.ZERO;

        for (EventoFinanceiro evento: eventosFinanceiros){
            if (evento.getDataEvento().getMonth().equals(data.getMonth())){
                if (evento.getTipo().equals(Tipo.Recebimento)){
                    saldo = saldo.add(BigDecimal.valueOf(evento.getValor()));

                }
                if (maiorValor.compareTo(BigDecimal.valueOf(evento.getValor()))<0){
                    maiorValor = (BigDecimal.valueOf(evento.getValor()));
                    maiorEvento = evento;
                }
            }
        }
        if (saldo.compareTo(BigDecimal.ZERO) == 0) {
            return new MaiorGastoDoMes(
                    maiorEvento != null ? maiorEvento.getGastoDetalhe().getCategoriaUsuario().getFirst().getCategoria().getTitulo() : "Sem evento",
                    0
            );
        }

        BigDecimal percentual = maiorValor
                .divide(saldo, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));


        return new MaiorGastoDoMes(maiorEvento.getGastoDetalhe().getCategoriaUsuario().getFirst().getCategoria().getTitulo(), percentual.intValue());

    }
}
