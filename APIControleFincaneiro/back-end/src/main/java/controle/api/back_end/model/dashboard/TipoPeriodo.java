package controle.api.back_end.model.dashboard;

/**
 * Representa o tipo de período temporal para as consultas do dashboard.
 * <ul>
 *   <li>MENSAL   → requer {@code ano} + {@code mes} (1-12)</li>
 *   <li>TRIMESTRAL → requer {@code ano} + {@code trimestre} (1-4)</li>
 *   <li>SEMESTRAL → requer {@code ano} + {@code semestre} (1-2)</li>
 *   <li>ANUAL    → requer apenas {@code ano}</li>
 * </ul>
 */
public enum TipoPeriodo {
    MENSAL,
    TRIMESTRAL,
    SEMESTRAL,
    ANUAL
}

