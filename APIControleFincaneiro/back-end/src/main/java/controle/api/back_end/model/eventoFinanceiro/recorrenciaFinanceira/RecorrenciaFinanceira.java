package controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira;

import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
public class RecorrenciaFinanceira {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    @NotNull
    private Double valor;

    private String descricao;

    @Enumerated(EnumType.STRING)
    private Periodicidade periodicidade;

    private LocalDate dataInicio;
    private LocalDate dataFim;

    private Integer intervalo;
    private Integer dia;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<DayOfWeek> diasDaSemana;

    @OneToMany(mappedBy = "recorrenciaFinanceira", cascade = CascadeType.ALL, orphanRemoval = true)
    List<EventoInstituicao> eventoInstituicaos;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public void setPeriodicidade(Periodicidade periodicidade) {
        this.periodicidade = periodicidade;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Integer getIntervalo() {
        return intervalo;
    }

    public void setIntervalo(Integer intervalo) {
        this.intervalo = intervalo;
    }

    public Integer getDia() {
        return dia;
    }

    public void setDia(Integer dia) {
        this.dia = dia;
    }

    public List<DayOfWeek> getDiasDaSemana() {
        return diasDaSemana;
    }

    public void setDiasDaSemana(List<DayOfWeek> diasDaSemana) {
        this.diasDaSemana = diasDaSemana;
    }

    public List<EventoInstituicao> getEventoInstituicaos() {
        return eventoInstituicaos;
    }

    public void setEventoInstituicaos(List<EventoInstituicao> eventoInstituicaos) {
        this.eventoInstituicaos = eventoInstituicaos;
    }
}

