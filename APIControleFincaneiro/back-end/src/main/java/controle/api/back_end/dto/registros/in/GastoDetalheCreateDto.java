package controle.api.back_end.dto.registros.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.OnOpen;

import java.util.List;

public class GastoDetalheCreateDto {
    @Schema(example = "[1, 2]", description = "Representa as categorias da movimentação")
    @NotNull
    private List<Integer> categoriaUsuario_id;
    @Schema(example = "Champagne", description = "Titulo que aparecera em conjunto ao valor da movimentação")
    @NotBlank
    private String tituloGasto;

    public List<Integer> getCategoriaUsuario_id() {
        return categoriaUsuario_id;
    }

    public void setCategoriaUsuario_id(List<Integer> categoriaUsuario_id) {
        this.categoriaUsuario_id = categoriaUsuario_id;
    }

    public String getTituloGasto() {
        return tituloGasto;
    }

    public void setTituloGasto(String tituloGasto) {
        this.tituloGasto = tituloGasto;
    }
}
