package controle.api.back_end.dto.registros.in;

import controle.api.back_end.model.eventoFinanceiro.Tipo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public class EventoFinanceiroCreateDto {
    @Schema(example = "21eb5d2f-3fd8-439e-b647-5cc1f753ae58", description = "Representa o id do usuário responsável por criar o evento")
    @NotNull
    private UUID usuario_id;
    @Schema(example = "1", description = "Respresenta o tipo do evento financeiro")
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
}
