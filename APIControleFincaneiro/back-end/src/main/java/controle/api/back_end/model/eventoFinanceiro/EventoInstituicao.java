package controle.api.back_end.model.eventoFinanceiro;

import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

@Entity
public class EventoInstituicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "fkEvento", nullable = false)
    private EventoFinanceiro eventoFinanceiro;

    @NotNull
    @ManyToOne
    private InstituicaoUsuario instituicaoUsuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoMovimento tipoMovimento;

    @PositiveOrZero
    @NotNull
    private Double valor;

    @Positive
    @NotNull
    private Integer parcelas = 1;

    public InstituicaoUsuario getInstituicaoUsuario() {
        return instituicaoUsuario;
    }

    public void setInstituicaoUsuario(InstituicaoUsuario instituicaoUsuario) {
        this.instituicaoUsuario = instituicaoUsuario;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TipoMovimento getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimento tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public EventoFinanceiro getEventoFinanceiro() {
        return eventoFinanceiro;
    }

    public void setEventoFinanceiro(EventoFinanceiro evento) {
        this.eventoFinanceiro = evento;
    }

    public Integer getParcelas() {
        return parcelas;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }
}
