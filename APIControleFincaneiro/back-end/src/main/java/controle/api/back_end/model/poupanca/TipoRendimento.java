package controle.api.back_end.model.poupanca;

/**
 * Tipo de indexador/rendimento de uma caixinha de poupança.
 *
 * <ul>
 *   <li>{@link #CDI}         – Certificado de Depósito Interbancário (ex.: 100 % do CDI, 110 % do CDI).</li>
 *   <li>{@link #SELIC}       – Taxa básica de juros da economia.</li>
 *   <li>{@link #POUPANCA}    – Rendimento tradicional da caderneta de poupança (70 % da SELIC + TR).</li>
 *   <li>{@link #PREFIXADO}   – Taxa fixa anual definida pelo usuário (ex.: 12 % a.a.).</li>
 *   <li>{@link #PERSONALIZADO} – Taxa personalizada inserida manualmente pelo usuário.</li>
 * </ul>
 */
public enum TipoRendimento {
    CDI,
    SELIC,
    POUPANCA,
    PREFIXADO,
    PERSONALIZADO
}

