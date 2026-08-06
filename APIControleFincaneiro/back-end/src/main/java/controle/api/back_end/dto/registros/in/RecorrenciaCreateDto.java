package controle.api.back_end.dto.registros.in;

import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.Periodicidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Dados para criação/edição de uma recorrência financeira")
public class RecorrenciaCreateDto {

    @NotNull
    @Schema(description = "Valor da recorrência", example = "150.0")
    private Double valor;

    @Schema(description = "Descrição da recorrência", example = "Salário mensal")
    private String descricao;

    @NotNull
    @Schema(description = "Periodicidade da recorrência")
    private Periodicidade periodicidade;

    @Schema(description = "Data final da recorrência")
    private LocalDate dataFim;

    @Schema(description = "Intervalo entre ocorrências (ex: a cada 2 semanas)", example = "1")
    private Integer intervalo;

    @Schema(description = "Dia do mês para recorrências mensais/anuais", example = "15")
    private Integer dia;

    @Schema(description = "Dias da semana para recorrências semanais")
    private List<DayOfWeek> diasDaSemana;

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public void setPeriodicidade(Periodicidade periodicidade) {
        this.periodicidade = periodicidade;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Integer getIntervalo() {
        return intervalo;
    }

    public void setIntervalo(Integer intervalo) {
        this.intervalo = intervalo;
    }

    public Integer getDia() {
        return dia;
    }

    public void setDia(Integer dia) {
        this.dia = dia;
    }

    public List<DayOfWeek> getDiasDaSemana() {
        return diasDaSemana;
    }

    public void setDiasDaSemana(List<DayOfWeek> diasDaSemana) {
        this.diasDaSemana = diasDaSemana;
    }
}

