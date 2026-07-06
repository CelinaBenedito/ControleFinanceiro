package controle.api.back_end.model.poupanca;

import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import jakarta.persistence.*;

/**
 * Vincula uma {@link Caixinha} a uma {@link InstituicaoUsuario}.
 *
 * <p>Uma caixinha simples tem exatamente uma instituição.
 * Uma caixinha compartilhada ({@code isCompartilhada = true}) pode ter várias,
 * permitindo que o usuário divida a meta entre múltiplas contas/bancos.
 */
@Entity
public class CaixinhaInstituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    private Caixinha caixinha;

    @ManyToOne(optional = false)
    private InstituicaoUsuario instituicaoUsuario;

    // ── Getters e Setters ────────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Caixinha getCaixinha() { return caixinha; }
    public void setCaixinha(Caixinha caixinha) { this.caixinha = caixinha; }

    public InstituicaoUsuario getInstituicaoUsuario() { return instituicaoUsuario; }
    public void setInstituicaoUsuario(InstituicaoUsuario instituicaoUsuario) { this.instituicaoUsuario = instituicaoUsuario; }
}

