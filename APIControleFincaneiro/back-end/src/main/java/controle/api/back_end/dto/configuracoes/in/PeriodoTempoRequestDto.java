package controle.api.back_end.dto.configuracoes.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class PeriodoTempoRequestDto {
    @PastOrPresent
    @NotNull
    LocalDate dataInical;

    @PastOrPresent
    @NotNull
    LocalDate dataFinal;

    public LocalDate getDataInical() {
        return dataInical;
    }

    public void setDataInical(LocalDate dataInical) {
        this.dataInical = dataInical;
    }

    public LocalDate getDataFinal() {
        return dataFinal;
    }

    public void setDataFinal(LocalDate dataFinal) {
        this.dataFinal = dataFinal;
    }
}
