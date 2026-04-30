package controle.api.back_end.dto.dashboard;

import java.util.List;

public class CategoriaEPorcentagens {
    List<String> categorias;
    List<Integer> porcentagens;

    public List<String> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<String> categorias) {
        this.categorias = categorias;
    }

    public List<Integer> getPorcentagens() {
        return porcentagens;
    }

    public void setPorcentagens(List<Integer> porcentagens) {
        this.porcentagens = porcentagens;
    }
}
