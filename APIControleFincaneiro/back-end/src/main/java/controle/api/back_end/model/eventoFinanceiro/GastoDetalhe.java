package controle.api.back_end.model.eventoFinanceiro;

import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventoFinanceiro getEventoFinanceiro() {
        return eventoFinanceiro;
    }

    public void setEventoFinanceiro(EventoFinanceiro eventoFinanceiro) {
        this.eventoFinanceiro = eventoFinanceiro;
    }

    public CategoriaUsuario getCategoriaUsuario() {
        return categoriaUsuario;
    }

    public void setCategoriaUsuario(CategoriaUsuario categoriaUsuario) {
        this.categoriaUsuario = categoriaUsuario;
    }

    public String getTituloGasto() {
        return tituloGasto;
    }

    public void setTituloGasto(String tituloGasto) {
        this.tituloGasto = tituloGasto;
    }
}

