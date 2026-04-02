package controle.api.back_end.model.eventoFinanceiro;

import controle.api.back_end.model.categoria.Categoria;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class GastoDetalhe {
    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "fkEvento", nullable = false)
    @NotNull
    private EventoFinanceiro eventoFinanceiro;

    @ManyToOne
    @JoinColumn(name = "fkCategoria")
    @NotNull
    private Categoria categoria;

    @Size(max = 50)
    @NotBlank
    private String tituloGasto;

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public EventoFinanceiro getEventoFincaceiro() {
        return eventoFinanceiro;
    }

    public void setEventoFincaceiro(EventoFinanceiro eventoFinanceiro) {
        this.eventoFinanceiro = eventoFinanceiro;
    }


}

