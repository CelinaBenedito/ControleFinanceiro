package controle.api.back_end.dto.registros.in;

import controle.api.back_end.model.eventoFinanceiro.Tipo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class EventoFinanceiroCreateDto {
    @Schema(example = "21eb5d2f-3fd8-439e-b647-5cc1f753ae58", description = "Representa o id do usuário responsável por criar o evento")
    @NotNull
    private UUID usuario_id;

    @Schema(example = "1", description = "Representa o tipo do evento financeiro")
    @NotNull
    private Tipo tipo;

    @Schema(example = "100.0", description = "Representa o valor do evento financeiro.")
    @NotNull
    @Positive
    private Double valor;

    @Schema(example = "Champagne de Ano novo")
    private String descricao;

    @Schema(example = "2026-01-01")
    @NotNull
    private LocalDate dataEvento;

    @Schema(example = "true",
            description = "Indica se o evento é recorrente (apenas Gasto e Recebimento)")
    private Boolean recorrente;

    @Schema(example = "SEMANAL",
            description = "Periodicidade da recorrência: DIARIO, SEMANAL, MENSAL ou ANUAL")
    private String periodicidade;

    @Schema(example = "2",
            description = "Intervalo da recorrência (ex.: a cada 2 dias, 2 semanas, 2 meses)")
    private Integer intervalo;

    @Schema(example = "[\"MONDAY\", \"WEDNESDAY\"]",
            description = "Dias da semana em que o evento ocorre (apenas para recorrência semanal)")
    private List<DayOfWeek> diasDaSemana;

    @Schema(example = "15",
            description = "Dia específico do mês ou ano em que o evento ocorre (apenas para mensal/anual)")
    private Integer dia;

    @Schema(example = "2026-12-31",
            description = "Data final da recorrência")
    private LocalDate dataFim;

    private Double taxaRendimento;

    private Integer tempoAplicacao;

    private Integer tempoProjecao;

    public UUID getUsuario_id() {
        return usuario_id;
    }

    public void setUsuario_id(UUID usuario_id) {
        this.usuario_id = usuario_id;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

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

    public LocalDate getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(LocalDate dataEvento) {
        this.dataEvento = dataEvento;
    }

    public Boolean getRecorrente() {
        return recorrente;
    }

    public void setRecorrente(Boolean recorrente) {
        this.recorrente = recorrente;
    }

    public String getPeriodicidade() {
        return periodicidade;
    }

    public void setPeriodicidade(String periodicidade) {
        this.periodicidade = periodicidade;
    }

    public Integer getIntervalo() {
        return intervalo;
    }

    public void setIntervalo(Integer intervalo) {
        this.intervalo = intervalo;
    }

    public List<DayOfWeek> getDiasDaSemana() {
        return diasDaSemana;
    }

    public void setDiasDaSemana(List<DayOfWeek> diasDaSemana) {
        this.diasDaSemana = diasDaSemana;
    }

    public Integer getDia() {
        return dia;
    }

    public void setDia(Integer dia) {
        this.dia = dia;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Double getTaxaRendimento() {
        return taxaRendimento;
    }

    public void setTaxaRendimento(Double taxaRendimento) {
        this.taxaRendimento = taxaRendimento;
    }

    public Integer getTempoAplicacao() {
        return tempoAplicacao;
    }

    public void setTempoAplicacao(Integer tempoAplicacao) {
        this.tempoAplicacao = tempoAplicacao;
    }

    public Integer getTempoProjecao() {
        return tempoProjecao;
    }

    public void setTempoProjecao(Integer tempoProjecao) {
        this.tempoProjecao = tempoProjecao;
    }
}
