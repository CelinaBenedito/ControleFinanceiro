package controle.api.back_end.model;

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
}
