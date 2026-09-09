package controle.api.back_end.dto.instituicao.out;

public class InstituicaoResponseDTO {
    private Integer id;
    private String nome;
    private Boolean isVoucher;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Boolean getIsVoucher() {
        return isVoucher;
    }

    public void setIsVoucher(Boolean isVoucher) {
        this.isVoucher = isVoucher;
    }

}
