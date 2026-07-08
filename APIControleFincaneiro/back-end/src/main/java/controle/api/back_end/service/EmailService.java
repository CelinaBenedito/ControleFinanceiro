package controle.api.back_end.service;

import controle.api.back_end.dto.poupanca.out.CaixinhaResponseDTO;
import controle.api.back_end.model.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servico responsavel pelo envio de e-mails de alertas, relatorios e
 * mensagens especiais (aniversario) para os usuarios.
 *
 * <h3>Para ativar o envio real de e-mails</h3>
 * <ol>
 *   <li>Configure as propriedades {@code spring.mail.*} no {@code application.properties}.</li>
 *   <li>Mude {@code app.email.habilitado=true}.</li>
 *   <li>No Gmail: gere uma "Senha de app" em Minha Conta {@literal >} Seguranca {@literal >} Senhas de app.</li>
 * </ol>
 *
 * <p>Enquanto desabilitado ({@code app.email.habilitado=false}), todos os envios sao
 * apenas logados — nenhum e-mail e enviado de fato.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FMT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SEP = "================================================";

    /** Injecao opcional: se spring-boot-starter-mail nao estiver configurado, sera null. */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.email.habilitado:false}")
    private boolean habilitado;

    @Value("${app.email.remetente:noreply@myfinance.app}")
    private String remetente;

    // =========================================================================
    // ALERTA: LIMITE MENSAL DE GASTOS
    // =========================================================================

    /**
     * Enviado quando o usuario atinge {@code percentualAtingido}% do limite mensal.
     *
     * @param usuario          destinatario
     * @param gastoAtual       total gasto no mes ate agora
     * @param limiteMensal     limite configurado pelo usuario
     * @param percentualConfig percentual que disparou o alerta (ex.: 80)
     * @param percentualReal   percentual real atual (pode ser maior que percentualConfig)
     */
    public void enviarAlertaLimiteMensal(Usuario usuario,
                                         double gastoAtual,
                                         double limiteMensal,
                                         int percentualConfig,
                                         double percentualReal) {
        String assunto = String.format("[MyFinance] Alerta: %d%% do limite mensal atingido", percentualConfig);

        StringBuilder sb = new StringBuilder();
        sb.append("Ola, ").append(usuario.getNome()).append("!\n\n");
        sb.append(SEP).append("\n");
        sb.append("  ALERTA DE LIMITE MENSAL\n");
        sb.append(SEP).append("\n\n");

        sb.append("Voce ja usou ").append(String.format("%.1f%%", percentualReal))
          .append(" do seu limite de gastos mensais.\n\n");

        sb.append("  Gasto atual:     R$ ").append(String.format("%.2f", gastoAtual)).append("\n");
        sb.append("  Limite mensal:   R$ ").append(String.format("%.2f", limiteMensal)).append("\n");
        sb.append("  Restante:        R$ ").append(String.format("%.2f", limiteMensal - gastoAtual)).append("\n\n");

        if (percentualReal >= 100) {
            sb.append("Atencao: voce ultrapassou o limite que definiu para este mes.\n");
            sb.append("Considere revisar seus gastos ou ajustar o limite nas configuracoes.\n\n");
        } else {
            sb.append("Fique de olho nos seus gastos para nao ultrapassar o limite!\n\n");
        }

        sb.append("Acesse o app para ver o detalhamento completo.\n\n");
        sb.append("-- Equipe MyFinance");

        enviar(usuario.getEmail(), assunto, sb.toString());
    }

    // =========================================================================
    // ALERTA: META DE POUPANCA
    // =========================================================================

    /**
     * Enviado quando uma caixinha atinge o percentual configurado da meta.
     *
     * @param usuario        destinatario
     * @param nomeCaixinha   nome da caixinha (ex.: "Viagem para o Japao")
     * @param valorAtual     saldo atual da caixinha
     * @param meta           valor da meta
     * @param percentualReal percentual real atingido
     */
    public void enviarAlertaMetaPoupanca(Usuario usuario,
                                          String nomeCaixinha,
                                          BigDecimal valorAtual,
                                          BigDecimal meta,
                                          double percentualReal) {
        String emoji = percentualReal >= 100 ? "PARABENS" : "QUASE LA";
        String assunto = String.format("[MyFinance] %s! Meta '%s' %.0f%% concluida",
                emoji, nomeCaixinha, percentualReal);

        StringBuilder sb = new StringBuilder();
        sb.append("Ola, ").append(usuario.getNome()).append("!\n\n");
        sb.append(SEP).append("\n");
        sb.append("  ALERTA DE META DE POUPANCA\n");
        sb.append(SEP).append("\n\n");

        if (percentualReal >= 100) {
            sb.append("Incrivel! Voce atingiu 100% da sua meta \"").append(nomeCaixinha).append("\"!\n\n");
            sb.append("Esse e o resultado de disciplina e foco. Parabens!\n\n");
        } else {
            sb.append("Voce esta chegando la! A caixinha \"").append(nomeCaixinha)
              .append("\" atingiu ").append(String.format("%.1f%%", percentualReal)).append(" da meta.\n\n");
        }

        sb.append("  Caixinha:     ").append(nomeCaixinha).append("\n");
        sb.append("  Saldo atual:  R$ ").append(valorAtual).append("\n");
        sb.append("  Meta:         R$ ").append(meta).append("\n");
        sb.append("  Progresso:    ").append(String.format("%.1f%%", percentualReal)).append("\n");

        if (percentualReal < 100 && meta != null) {
            BigDecimal falta = meta.subtract(valorAtual).max(BigDecimal.ZERO);
            sb.append("  Falta:        R$ ").append(falta).append("\n");
        }
        sb.append("\n");

        sb.append("Continue assim, voce esta quase la!\n\n");
        sb.append("-- Equipe MyFinance");

        enviar(usuario.getEmail(), assunto, sb.toString());
    }

    // =========================================================================
    // FELIZ ANIVERSARIO
    // =========================================================================

    /** Enviado no dia do aniversario do usuario. */
    public void enviarFelizAniversario(Usuario usuario) {
        String assunto = "[MyFinance] Feliz aniversario, " + usuario.getNome() + "!";

        StringBuilder sb = new StringBuilder();
        sb.append("Feliz aniversario, ").append(usuario.getNome()).append("!\n\n");
        sb.append(SEP).append("\n");
        sb.append("  PARABENS PELO SEU DIA!\n");
        sb.append(SEP).append("\n\n");

        sb.append("Hoje e um dia especial e queremos te desejar tudo de bom!\n\n");
        sb.append("Que este novo ano de vida seja cheio de realizacoes,\n");
        sb.append("saude, felicidade... e muito planejamento financeiro!\n\n");
        sb.append("Aproveite o dia. Voce merece!\n\n");
        sb.append("Com carinho,\n");
        sb.append("-- Equipe MyFinance");

        enviar(usuario.getEmail(), assunto, sb.toString());
    }

    // =========================================================================
    // RELATORIO MENSAL
    // =========================================================================

    /**
     * Relatorio completo enviado no dia 1 de cada mes com resumo de gastos e poupancas.
     *
     * @param usuario        destinatario
     * @param caixinhas      lista de caixinhas ativas com calculos
     * @param gastoMes       total gasto no mes anterior
     * @param limiteMensal   limite configurado (0 = nao configurado)
     */
    public void enviarRelatorioMensal(Usuario usuario,
                                       List<CaixinhaResponseDTO> caixinhas,
                                       double gastoMes,
                                       double limiteMensal) {
        String mesRef = LocalDate.now().minusMonths(1)
                .format(DateTimeFormatter.ofPattern("MM/yyyy"));
        String assunto = "[MyFinance] Seu relatorio financeiro de " + mesRef;

        StringBuilder sb = new StringBuilder();
        sb.append("Ola, ").append(usuario.getNome()).append("!\n\n");
        sb.append(SEP).append("\n");
        sb.append("  RELATORIO MENSAL - ").append(mesRef).append("\n");
        sb.append(SEP).append("\n\n");

        // Resumo de gastos
        sb.append("GASTOS DO MES\n");
        sb.append("--------------\n");
        sb.append("  Total gasto:  R$ ").append(String.format("%.2f", gastoMes)).append("\n");
        if (limiteMensal > 0) {
            double pct = (gastoMes / limiteMensal) * 100;
            sb.append("  Limite:       R$ ").append(String.format("%.2f", limiteMensal)).append("\n");
            sb.append("  Utilizacao:   ").append(String.format("%.1f%%", pct)).append("\n");
        }
        sb.append("\n");

        // Resumo de poupancas
        if (!caixinhas.isEmpty()) {
            sb.append("CAIXINHAS DE POUPANCA\n");
            sb.append("----------------------\n");
            BigDecimal totalPoupado = BigDecimal.ZERO;
            for (CaixinhaResponseDTO c : caixinhas) {
                sb.append("  ").append(c.getNome()).append("\n");
                sb.append("    Saldo:    R$ ").append(c.getValorAtual()).append("\n");
                if (c.getValorMeta() != null) {
                    sb.append("    Meta:     R$ ").append(c.getValorMeta())
                      .append(" (").append(String.format("%.1f%%", c.getPercentualAtingido())).append(")\n");
                }
                if (c.getAporteMensalSugerido() != null) {
                    sb.append("    Aporte/mes sugerido: R$ ").append(c.getAporteMensalSugerido()).append("\n");
                }
                sb.append("\n");
                totalPoupado = totalPoupado.add(c.getValorAtual());
            }
            sb.append("  Total poupado: R$ ").append(totalPoupado).append("\n\n");
        }

        sb.append(SEP).append("\n");
        sb.append("Acesse o app para ver o detalhamento completo.\n\n");
        sb.append("-- Equipe MyFinance");

        enviar(usuario.getEmail(), assunto, sb.toString());
    }

    // =========================================================================
    // LEMBRETE DE APORTE (SEMANAL)
    // =========================================================================

    /** Lembrete semanal para realizar aportes nas caixinhas com meta pendente. */
    public void enviarLembreteAporte(Usuario usuario, List<CaixinhaResponseDTO> caixinhas) {
        String assunto = "[MyFinance] Lembrete: seus aportes da semana";

        StringBuilder sb = new StringBuilder();
        sb.append("Ola, ").append(usuario.getNome()).append("!\n\n");
        sb.append("Aqui esta o lembrete semanal das suas caixinhas de poupanca:\n\n");
        sb.append(SEP).append("\n");

        boolean temAporte = false;
        for (CaixinhaResponseDTO c : caixinhas) {
            if (c.getAporteMensalSugerido() != null
                    && c.getAporteMensalSugerido().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("  ").append(c.getNome()).append("\n");
                sb.append("    Saldo atual: R$ ").append(c.getValorAtual()).append("\n");
                sb.append("    Progresso:   ").append(String.format("%.1f%%", c.getPercentualAtingido()))
                  .append(" da meta\n");
                sb.append("    Aporte/mes:  R$ ").append(c.getAporteMensalSugerido()).append("\n\n");
                temAporte = true;
            }
        }

        if (!temAporte) {
            sb.append("  Todas as suas metas estao em dia!\n\n");
        }

        sb.append(SEP).append("\n");
        sb.append("Acesse o app para registrar seus aportes. Cada real conta!\n\n");
        sb.append("-- Equipe MyFinance");

        enviar(usuario.getEmail(), assunto, sb.toString());
    }

    // =========================================================================
    // ENVIO CENTRAL
    // =========================================================================

    private void enviar(String destinatario, String assunto, String corpo) {
        if (!habilitado) {
            log.info("[EmailService] E-mail desabilitado. Para: '{}' | Assunto: '{}'",
                    destinatario, assunto);
            return;
        }

        if (mailSender == null) {
            log.error("[EmailService] JavaMailSender nao esta configurado. " +
                      "Configure spring.mail.* no application.properties.");
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(remetente);
            msg.setTo(destinatario);
            msg.setSubject(assunto);
            msg.setText(corpo);
            mailSender.send(msg);
            log.info("[EmailService] E-mail enviado com sucesso para: {}", destinatario);
        } catch (Exception e) {
            log.error("[EmailService] Falha ao enviar para {}: {}", destinatario, e.getMessage());
        }
    }
}

