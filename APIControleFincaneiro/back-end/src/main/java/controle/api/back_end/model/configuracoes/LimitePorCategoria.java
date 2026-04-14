package controle.api.back_end.model.configuracoes;

import controle.api.back_end.model.categoria.CategoriaUsuario;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class LimitePorCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private CategoriaUsuario categoriaUsuario;

    private Double limiteDesejado;

    public CategoriaUsuario getCategoriaUsuario() {
        return categoriaUsuario;
    }

    public void setCategoriaUsuario(CategoriaUsuario categoriaUsuario) {
        this.categoriaUsuario = categoriaUsuario;
    }

    public Double getLimiteDesejado() {
        return limiteDesejado;
    }

    public void setLimiteDesejado(Double limiteDesejado) {
        this.limiteDesejado = limiteDesejado;
    }
}
