package controle.api.back_end.dto.dashboard.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/** KPI 1 — Saldo acumulado do usuário até o final do período. */
public record SaldoTotalDto(
        BigDecimal saldo,
        LocalDate dataReferencia,  // min(fimPeriodo, hoje)
        String labelPeriodo
) {}

