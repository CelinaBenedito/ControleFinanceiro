package controle.api.back_end.model.eventoFinanceiro;

import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
public class EventoFinanceiro {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @NotNull
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Tipo tipo;

    @NotNull
    @PositiveOrZero
    private Double valor;

    @Size(max = 500)
    private String descricao;

    @NotNull
    private LocalDate dataEvento;

    @OneToMany(mappedBy = "eventoFinanceiro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoInstituicao> eventoInstituicao;

    @OneToOne(mappedBy = "eventoFinanceiro", cascade = CascadeType.ALL, orphanRemoval = true)
    private GastoDetalhe gastoDetalhe;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDate dataRegistro;

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario fkUsuario) {
        this.usuario = fkUsuario;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public LocalDate getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDate dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public List<EventoInstituicao> getEventoInstituicao() {
        return eventoInstituicao;
    }

    public void setEventoInstituicao(List<EventoInstituicao> eventoInstituicao) {
        this.eventoInstituicao = eventoInstituicao;
    }

    public GastoDetalhe getGastoDetalhe() {
        return gastoDetalhe;
    }

    public void setGastoDetalhe(GastoDetalhe gastoDetalhe) {
        this.gastoDetalhe = gastoDetalhe;
    }
}
