package controle.api.back_end.dto.dashboard;

public class MaiorGastoDoMes {
    private String nomeCategoria;
    private Double valor;
    private int porcentagem;

    public MaiorGastoDoMes() {
    }

    public MaiorGastoDoMes(String nomeCategoria, Double valor, int porcentagem) {
        this.nomeCategoria = nomeCategoria;
        this.valor = valor;
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

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }
}
