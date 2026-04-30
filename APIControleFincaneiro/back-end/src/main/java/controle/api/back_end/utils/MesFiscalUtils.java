package controle.api.back_end.utils;

import java.time.LocalDate;

public class MesFiscalUtils {

    /**
     * Retorna o intervalo do mês fiscal (início e fim) com base na data e no dia de início do mês fiscal.
     *
     * @param data              Data de referência
     * @param diaInicioMesFiscal Dia do mês em que começa o mês fiscal (ex: 15)
     * @return um objeto contendo inicio e fim do mês fiscal
     */
    public static PeriodoFiscal calcularPeriodoFiscal(LocalDate data, int diaInicioMesFiscal) {
        LocalDate inicioMesFiscal;
        LocalDate fimMesFiscal;

        if (data.getDayOfMonth() >= diaInicioMesFiscal) {
            inicioMesFiscal = LocalDate.of(data.getYear(), data.getMonth(), diaInicioMesFiscal);
            fimMesFiscal = inicioMesFiscal.plusMonths(1).minusDays(1);
        } else {
            inicioMesFiscal = LocalDate.of(data.minusMonths(1).getYear(), data.minusMonths(1).getMonth(), diaInicioMesFiscal);
            fimMesFiscal = inicioMesFiscal.plusMonths(1).minusDays(1);
        }

        return new PeriodoFiscal(inicioMesFiscal, fimMesFiscal);
    }

    /**
     * Classe auxiliar para representar o intervalo do mês fiscal.
     */
    public static class PeriodoFiscal {
        private final LocalDate inicio;
        private final LocalDate fim;

        public PeriodoFiscal(LocalDate inicio, LocalDate fim) {
            this.inicio = inicio;
            this.fim = fim;
        }

        public LocalDate getInicio() {
            return inicio;
        }

        public LocalDate getFim() {
            return fim;
        }
    }
}
