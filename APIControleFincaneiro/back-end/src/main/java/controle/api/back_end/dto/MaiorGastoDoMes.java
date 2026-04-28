package controle.api.back_end.dto;

public class MaiorGastoDoMes {
    private String nomeCategoria;
    private int porcentagem;

    public MaiorGastoDoMes() {
    }

    public MaiorGastoDoMes(String nomeCategoria, int porcentagem) {
        this.nomeCategoria = nomeCategoria;
        this.porcentagem = porcentagem;
    }

    public String getNomeCategoria() {
        return nomeCategoria;
    }

    public void setNomeCategoria(String nomeCategoria) {
        this.nomeCategoria = nomeCategoria;
    }

    public int getPorcentagem() {
        return porcentagem;
    }

    public void setPorcentagem(int porcentagem) {
        this.porcentagem = porcentagem;
    }
}
