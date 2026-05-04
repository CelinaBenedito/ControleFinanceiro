package controle.api.back_end.model.eventoFinanceiro;

import controle.api.back_end.model.categoria.CategoriaUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Entity
public class EventoDetalhe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "fkEvento", nullable = false)
    private EventoFinanceiro eventoFinanceiro;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "evento_detalhe_categoria",
            joinColumns = @JoinColumn(name = "evento_detalhe_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_usuario_id")
    )
    private List<CategoriaUsuario> categoriaUsuario;

    @Size(max = 50)
    @NotBlank
    private String tituloGasto;

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

    public List<CategoriaUsuario> getCategoriaUsuario() {
        return categoriaUsuario;
    }

    public void setCategoriaUsuario(List<CategoriaUsuario> categoriaUsuario) {
        this.categoriaUsuario = categoriaUsuario;
    }

    public String getTituloGasto() {
        return tituloGasto;
    }

    public void setTituloGasto(String tituloGasto) {
        this.tituloGasto = tituloGasto;
    }
}