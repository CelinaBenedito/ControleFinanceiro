package controle.api.back_end.model.recorrenciaFinanceira;

import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;
import java.util.UUID;

@Entity
public class RecorrenciaFinanceira {
    @Id
    private UUID id;

    @ManyToOne
    private Usuario usuario;

    private Tipo tipo;
    private Double valor;
    private String descricao;
    private LocalDate diaRecorrencia;
    private Periodicidade periodicidade;

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

    public LocalDate getDiaRecorrencia() {
        return diaRecorrencia;
    }

    public void setDiaRecorrencia(LocalDate diaRecorrencia) {
        this.diaRecorrencia = diaRecorrencia;
    }

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public void setPeriodicidade(Periodicidade periodicidade) {
        this.periodicidade = periodicidade;
    }
}

