package controle.api.back_end.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
public class EventoInstituicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "fkEvento")
    private EventoFincaceiro evento;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "fkInstituicaoUsuario")
    private InstituicaoUsuario instituicaoUsuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoMovimento tipoMovimento;

    @PositiveOrZero
    @NotNull
    private Double valor;

    public InstituicaoUsuario getInstituicaoUsuario() {
        return instituicaoUsuario;
    }

    public void setInstituicaoUsuario(InstituicaoUsuario instituicaoUsuario) {
        this.instituicaoUsuario = instituicaoUsuario;
    }

    public EventoFincaceiro getEvento() {
        return evento;
    }

    public void setEvento(EventoFincaceiro evento) {
        this.evento = evento;
    }

}
