package controle.api.back_end.dto.emprestimo.in;

import controle.api.back_end.model.emprestimo.TipoEmprestimo;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para criação e edição de empréstimos.
 */
public record EmprestimoCreateDTO(
        @NotNull(message = "ID do usuário é obrigatório")
        UUID usuarioId,

        @NotNull(message = "Tipo do empréstimo é obrigatório")
        TipoEmprestimo tipo,

        @NotBlank(message = "Nome da pessoa ou grupo é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String pessoaOuGrupo,

        @NotNull(message = "Valor total é obrigatório")
        @Positive(message = "Valor total deve ser positivo")
        BigDecimal valorTotal,

        LocalDate dataPrevisao,

        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres")
        String observacoes
) {
}

