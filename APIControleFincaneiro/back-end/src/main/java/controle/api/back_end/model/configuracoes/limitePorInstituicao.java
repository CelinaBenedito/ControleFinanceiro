package controle.api.back_end.model.configuracoes;

import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class limitePorInstituicao {
    @Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "fkInstituicaoUsuario", nullable = false)
    private InstituicaoUsuario institucaousuario;
    private Double limiteDesejado;

    public InstituicaoUsuario getInstitucaousuario() {
        return institucaousuario;
    }

    public void setInstitucaousuario(InstituicaoUsuario institucaousuario) {
        this.institucaousuario = institucaousuario;
    }

    public Double getLimiteDesejado() {
        return limiteDesejado;
    }

    public void setLimiteDesejado(Double limiteDesejado) {
        this.limiteDesejado = limiteDesejado;
    }
}
