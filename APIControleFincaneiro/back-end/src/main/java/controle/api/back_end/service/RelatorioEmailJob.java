package controle.api.back_end.service;

import controle.api.back_end.dto.poupanca.out.CaixinhaResponseDTO;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.TipoAlertaEmail;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.configuracoes.ConfiguracoesRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.poupanca.CaixinhaRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Jobs agendados que disparam os e-mails automaticamente para os usuarios.
 *
 * <ul>
 *   <li>Aniversario        — diariamente as 08:00, verifica quem faz aniversario hoje.</li>
 *   <li>Relatorio mensal   — dia 1 de cada mes as 08:00.</li>
 *   <li>Lembrete de aporte — toda segunda-feira as 08:00.</li>
 * </ul>
 *
 * Os e-mails so sao enviados se o usuario tiver o tipo correspondente em
 * {@code Configuracoes.alertasEmailAtivos} e se {@code app.email.habilitado=true}.
 */
@Component
public class RelatorioEmailJob {

    private static final Logger log = LoggerFactory.getLogger(RelatorioEmailJob.class);

    private final ConfiguracoesRepository configuracoesRepository;
    private final CaixinhaRepository caixinhaRepository;
    private final CaixinhaService caixinhaService;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepository;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;

    public RelatorioEmailJob(ConfiguracoesRepository configuracoesRepository,
                             CaixinhaRepository caixinhaRepository,
                             CaixinhaService caixinhaService,
                             EmailService emailService,
                             UsuarioRepository usuarioRepository,
                             EventoFinanceiroRepository eventoFinanceiroRepository) {
        this.configuracoesRepository    = configuracoesRepository;
        this.caixinhaRepository         = caixinhaRepository;
        this.caixinhaService            = caixinhaService;
        this.emailService               = emailService;
        this.usuarioRepository          = usuarioRepository;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
    }

    // =========================================================================
    // ANIVERSARIO — todos os dias as 08:00
    // =========================================================================

    @Scheduled(cron = "0 0 8 * * *")
    public void dispararAniversario() {
        LocalDate hoje = LocalDate.now();
        log.info("[RelatorioEmailJob] Verificando aniversariantes de hoje ({}/{})...",
                hoje.getDayOfMonth(), hoje.getMonthValue());

        usuarioRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsAtivo()))
                .filter(u -> u.getDataNascimento() != null
                          && u.getDataNascimento().getDayOfMonth() == hoje.getDayOfMonth()
                          && u.getDataNascimento().getMonthValue() == hoje.getMonthValue())
                .forEach(usuario -> {
                    // Verifica se o usuario ativou o alerta de aniversario
                    configuracoesRepository.findConfiguracoesByUsuario_Id(usuario.getId())
                            .ifPresent(config -> {
                                if (config.getAlertasEmailAtivos().contains(TipoAlertaEmail.ANIVERSARIO)) {
                                    emailService.enviarFelizAniversario(usuario);
                                    log.info("[RelatorioEmailJob] Feliz aniversario enviado para: {}",
                                            usuario.getEmail());
                                }
                            });
                });
    }

    // =========================================================================
    // RELATORIO MENSAL — dia 1 de cada mes as 08:00
    // =========================================================================

    @Scheduled(cron = "0 0 8 1 * *")
    public void dispararRelatorioMensal() {
        log.info("[RelatorioEmailJob] Disparando relatorios mensais...");

        configuracoesRepository.findAll().stream()
                .filter(c -> c.getAlertasEmailAtivos().contains(TipoAlertaEmail.RELATORIO_MENSAL))
                .forEach(config -> {
                    try {
                        Usuario usuario = config.getUsuario();

                        // Gastos do mes anterior
                        LocalDate inicioMesAnterior = LocalDate.now().minusMonths(1).withDayOfMonth(1);
                        LocalDate fimMesAnterior     = inicioMesAnterior.withDayOfMonth(
                                inicioMesAnterior.lengthOfMonth());

                        double gastoMes = eventoFinanceiroRepository
                                .findEventoFinanceiroByUsuario_IdAndDataEventoBetween(
                                        usuario.getId(), inicioMesAnterior, fimMesAnterior)
                                .stream()
                                .filter(e -> e.getTipo() == Tipo.Gasto)
                                .mapToDouble(EventoFinanceiro::getValor)
                                .sum();

                        List<CaixinhaResponseDTO> caixinhas = caixinhaRepository
                                .findAllByUsuario_IdAndIsAtivaTrue(usuario.getId())
                                .stream().map(caixinhaService::calcularEMontar).toList();

                        double limite = config.getLimiteDesejadoMensal() != null
                                ? config.getLimiteDesejadoMensal() : 0;

                        emailService.enviarRelatorioMensal(usuario, caixinhas, gastoMes, limite);
                    } catch (Exception e) {
                        log.error("[RelatorioEmailJob] Erro no relatorio mensal para usuario {}: {}",
                                config.getUsuario().getId(), e.getMessage());
                    }
                });
    }

    // =========================================================================
    // LEMBRETE DE APORTE — toda segunda-feira as 08:00
    // =========================================================================

    @Scheduled(cron = "0 0 8 * * MON")
    public void dispararLembreteSemanal() {
        log.info("[RelatorioEmailJob] Disparando lembretes semanais de aporte...");

        configuracoesRepository.findAll().stream()
                .filter(c -> c.getAlertasEmailAtivos().contains(TipoAlertaEmail.LEMBRETE_APORTE))
                .forEach(config -> {
                    try {
                        Usuario usuario = config.getUsuario();
                        List<CaixinhaResponseDTO> caixinhas = caixinhaRepository
                                .findAllByUsuario_IdAndIsAtivaTrue(usuario.getId())
                                .stream().map(caixinhaService::calcularEMontar).toList();

                        if (!caixinhas.isEmpty()) {
                            emailService.enviarLembreteAporte(usuario, caixinhas);
                        }
                    } catch (Exception e) {
                        log.error("[RelatorioEmailJob] Erro no lembrete semanal para usuario {}: {}",
                                config.getUsuario().getId(), e.getMessage());
                    }
                });
    }
}
