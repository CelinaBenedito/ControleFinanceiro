package controle.api.back_end.dto.emprestimo.in;

import jakarta.validation.constraints.NotNull;

public record EmprestimoQuitarDTO(
        @NotNull(message = "Instituição é obrigatória")
        Integer instituicaoUsuarioId
) {
}

