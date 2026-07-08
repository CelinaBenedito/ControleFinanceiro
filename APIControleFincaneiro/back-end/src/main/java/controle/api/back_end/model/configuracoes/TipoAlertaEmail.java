package controle.api.back_end.model.configuracoes;

/**
 * Tipos de alerta/notificacao por e-mail que o usuario pode ativar
 * individualmente nas configuracoes.
 */
public enum TipoAlertaEmail {

    /** Email no dia do aniversario do usuario. */
    ANIVERSARIO,

    /**
     * Alerta quando o total gasto no mes fiscal atingir o percentual
     * configurado em {@code Configuracoes.percentualAlertaGasto} (ex.: 80%).
     */
    ALERTA_LIMITE_MENSAL,

    /**
     * Alerta quando uma caixinha de poupanca atingir o percentual
     * configurado em {@code Configuracoes.percentualAlertaMeta} (ex.: 90%).
     */
    ALERTA_META_POUPANCA,

    /** Resumo mensal de gastos e poupancas, enviado no dia 1 de cada mes. */
    RELATORIO_MENSAL,

    /** Lembrete semanal (toda segunda-feira) para realizar os aportes do mes. */
    LEMBRETE_APORTE
}

