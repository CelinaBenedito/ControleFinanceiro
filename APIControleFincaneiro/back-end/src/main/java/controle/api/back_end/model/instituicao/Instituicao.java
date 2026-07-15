package controle.api.back_end.model.instituicao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Instituicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(max = 50)
    @NotBlank
    @Column(unique = true)
    private String nome;

    /** Taxa de juros padrão (% a.m.) pré-setada pela instituição. Nullable. */
    private Double taxaJurosPadrao;

    /** Indica se é instituição financeira (não benefício). Default true. */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean isInstituicaoFinanceira = Boolean.TRUE;

    public Instituicao() {
    }

    public Instituicao(Integer id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Double getTaxaJurosPadrao() { return taxaJurosPadrao; }
    public void setTaxaJurosPadrao(Double taxaJurosPadrao) { this.taxaJurosPadrao = taxaJurosPadrao; }

    public Boolean getIsInstituicaoFinanceira() { return isInstituicaoFinanceira; }
    public void setIsInstituicaoFinanceira(Boolean isInstituicaoFinanceira) { this.isInstituicaoFinanceira = isInstituicaoFinanceira; }
}
