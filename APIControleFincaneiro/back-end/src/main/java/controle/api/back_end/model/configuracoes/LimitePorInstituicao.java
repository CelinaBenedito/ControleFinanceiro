package controle.api.back_end.model.configuracoes;

import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class LimitePorInstituicao {
    @Id
    @ManyToOne(optional = false)
    @JoinColumn( nullable = false)
    private InstituicaoUsuario institucaoUsuario;
    private Double limiteDesejado;

    public InstituicaoUsuario getInstitucaoUsuario() {
        return institucaoUsuario;
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
}
