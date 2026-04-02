package controle.api.back_end.model.instituicao;

import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;

@Entity
public class InstituicaoUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "fkUsuario")
    private Usuario fkUsuario;

    @ManyToOne
    @JoinColumn(name = "fkInstituicao")
    private Instituicao fkInstituicao;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isAtivo;

    public Usuario getFkUsuario() {
        return fkUsuario;
    }

    public void setFkUsuario(Usuario fkUsuario) {
        this.fkUsuario = fkUsuario;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Instituicao getFkInstituicao() {
        return fkInstituicao;
    }

    public void setFkInstituicao(Instituicao fkInstituicao) {
        this.fkInstituicao = fkInstituicao;
    }

    public Boolean getAtivo() {
        return isAtivo;
    }

    public void setAtivo(Boolean ativo) {
        isAtivo = ativo;
    }
}
