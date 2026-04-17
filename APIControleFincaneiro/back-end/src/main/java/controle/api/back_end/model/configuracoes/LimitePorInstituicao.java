package controle.api.back_end.model.configuracoes;

import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class LimitePorInstituicao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private InstituicaoUsuario institucaoUsuario;

    @Column(nullable = false)
    private Double limiteDesejado;
    public InstituicaoUsuario getInstitucaoUsuario() {
        return institucaoUsuario;
    }

    @ManyToOne
    private Configuracoes configuracoes;

    public Configuracoes getConfiguracoes() {
        return configuracoes;
    }

    public void setConfiguracoes(Configuracoes configuracoes) {
        this.configuracoes = configuracoes;
    }


    public void setInstitucaoUsuario(InstituicaoUsuario institucaousuario) {
        this.institucaoUsuario = institucaousuario;
    }

    public Double getLimiteDesejado() {
        return limiteDesejado;
    }

    public void setLimiteDesejado(Double limiteDesejado) {
        this.limiteDesejado = limiteDesejado;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
