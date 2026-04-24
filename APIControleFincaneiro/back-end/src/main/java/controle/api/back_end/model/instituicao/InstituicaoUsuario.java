package controle.api.back_end.model.instituicao;

import controle.api.back_end.model.usuario.Usuario;
import jakarta.persistence.*;

@Entity
public class InstituicaoUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private Usuario usuario;

    @ManyToOne
    private Instituicao instituicao;

    @Column(name = "is_ativo", nullable = false)
    private Boolean isAtivo = Boolean.TRUE;

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario fkUsuario) {
        this.usuario = fkUsuario;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Instituicao getInstituicao() {
        return instituicao;
    }

    public void setInstituicao(Instituicao fkInstituicao) {
        this.instituicao = fkInstituicao;
    }

    public Boolean getIsAtivo() {
        return isAtivo;
    }

    public void setIsAtivo(Boolean ativo) {
        isAtivo = ativo;
    }
}
