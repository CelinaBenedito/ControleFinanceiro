package controle.api.back_end.utils;

import controle.api.back_end.model.dashboard.TipoPeriodo;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Utilitário para cálculo de períodos temporais do dashboard,
 * respeitando o dia de início do mês fiscal configurado pelo usuário.
 */
public class PeriodoTemporalUtils {

    public record Periodo(LocalDate inicio, LocalDate fim, String label) {}

    // ── Período atual ──────────────────────────────────────────────────────

    public static Periodo calcular(TipoPeriodo tipo,
                                   int ano,
                                   Integer mes,
                                   Integer trimestre,
                                   Integer semestre,
                                   int diaInicioMesFiscal) {
        return switch (tipo) {
            case MENSAL      -> calcularMensal(ano, mes, diaInicioMesFiscal);
            case TRIMESTRAL  -> calcularTrimestral(ano, trimestre, diaInicioMesFiscal);
            case SEMESTRAL   -> calcularSemestral(ano, semestre, diaInicioMesFiscal);
            case ANUAL       -> calcularAnual(ano, diaInicioMesFiscal);
        };
    }

    // ── Período anterior (para cálculo de variação percentual) ────────────

    public static Periodo calcularAnterior(TipoPeriodo tipo,
                                           int ano,
                                           Integer mes,
                                           Integer trimestre,
                                           Integer semestre,
                                           int diaInicioMesFiscal) {
        return switch (tipo) {
            case MENSAL -> {
                int mesAnt = (mes > 1) ? mes - 1 : 12;
                int anoAnt = (mes > 1) ? ano : ano - 1;
                yield calcularMensal(anoAnt, mesAnt, diaInicioMesFiscal);
            }
            case TRIMESTRAL -> {
                int trimAnt = (trimestre > 1) ? trimestre - 1 : 4;
                int anoAnt  = (trimestre > 1) ? ano : ano - 1;
                yield calcularTrimestral(anoAnt, trimAnt, diaInicioMesFiscal);
            }
            case SEMESTRAL -> {
                int semAnt = (semestre > 1) ? semestre - 1 : 2;
                int anoAnt = (semestre > 1) ? ano : ano - 1;
                yield calcularSemestral(anoAnt, semAnt, diaInicioMesFiscal);
            }
            case ANUAL -> calcularAnual(ano - 1, diaInicioMesFiscal);
        };
    }

    // ── Validação dos parâmetros ───────────────────────────────────────────

    /**
     * Lança {@link IllegalArgumentException} se os parâmetros do período forem inválidos.
     */
    public static void validar(TipoPeriodo tipo, Integer ano, Integer mes, Integer trimestre, Integer semestre) {
        if (ano == null) throw new IllegalArgumentException("O parâmetro 'ano' é obrigatório.");

        switch (tipo) {
            case MENSAL -> {
                if (mes == null || mes < 1 || mes > 12)
                    throw new IllegalArgumentException(
                            "Para periodo=MENSAL o parâmetro 'mes' é obrigatório e deve estar entre 1 e 12.");
            }
            case TRIMESTRAL -> {
                if (trimestre == null || trimestre < 1 || trimestre > 4)
                    throw new IllegalArgumentException(
                            "Para periodo=TRIMESTRAL o parâmetro 'trimestre' é obrigatório e deve estar entre 1 e 4.");
            }
            case SEMESTRAL -> {
                if (semestre == null || semestre < 1 || semestre > 2)
                    throw new IllegalArgumentException(
                            "Para periodo=SEMESTRAL o parâmetro 'semestre' é obrigatório e deve estar entre 1 e 2.");
            }
            case ANUAL -> { /* apenas 'ano' é necessário */ }
        }
    }

    // ── Implementações internas ────────────────────────────────────────────

    private static Periodo calcularMensal(int ano, int mes, int diaInicio) {
        LocalDate inicio = LocalDate.of(ano, mes, normalizarDia(ano, mes, diaInicio));
        LocalDate fim    = inicio.plusMonths(1).minusDays(1);
        String label = Month.of(mes).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"))
                .substring(0, 1).toUpperCase()
                + Month.of(mes).getDisplayName(TextStyle.FULL, new Locale("pt", "BR")).substring(1)
                + " " + ano;
        return new Periodo(inicio, fim, label);
    }

    private static Periodo calcularTrimestral(int ano, int trimestre, int diaInicio) {
        int mesInicio = (trimestre - 1) * 3 + 1;
        LocalDate inicio = LocalDate.of(ano, mesInicio, normalizarDia(ano, mesInicio, diaInicio));
        LocalDate fim    = inicio.plusMonths(3).minusDays(1);
        String label = "T" + trimestre + " " + ano;
        return new Periodo(inicio, fim, label);
    }

    private static Periodo calcularSemestral(int ano, int semestre, int diaInicio) {
        int mesInicio = (semestre - 1) * 6 + 1;
        LocalDate inicio = LocalDate.of(ano, mesInicio, normalizarDia(ano, mesInicio, diaInicio));
        LocalDate fim    = inicio.plusMonths(6).minusDays(1);
        String label = semestre + "º Semestre " + ano;
        return new Periodo(inicio, fim, label);
    }

    private static Periodo calcularAnual(int ano, int diaInicio) {
        LocalDate inicio = LocalDate.of(ano, 1, normalizarDia(ano, 1, diaInicio));
        LocalDate fim    = inicio.plusYears(1).minusDays(1);
        return new Periodo(inicio, fim, String.valueOf(ano));
    }

    /**
     * Garante que o dia de início não ultrapasse o último dia do mês (ex: dia 31 em fevereiro).
     */
    private static int normalizarDia(int ano, int mes, int dia) {
        int ultimoDia = LocalDate.of(ano, mes, 1).lengthOfMonth();
        return Math.min(dia, ultimoDia);
    }
}

