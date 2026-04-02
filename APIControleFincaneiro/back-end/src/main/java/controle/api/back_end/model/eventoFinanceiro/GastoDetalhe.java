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
    private EventoFincaceiro eventoFincaceiro;

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

    public EventoFincaceiro getEventoFincaceiro() {
        return eventoFincaceiro;
    }

    public void setEventoFincaceiro(EventoFincaceiro eventoFincaceiro) {
        this.eventoFincaceiro = eventoFincaceiro;
    }


}

