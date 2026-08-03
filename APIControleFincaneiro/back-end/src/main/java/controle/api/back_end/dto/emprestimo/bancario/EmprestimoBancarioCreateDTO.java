package controle.api.back_end.dto.emprestimo.bancario;
import controle.api.back_end.model.emprestimo.ModalidadeEmprestimoBancario;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record EmprestimoBancarioCreateDTO(
    @NotNull UUID usuarioId,
    @NotBlank @Size(max = 150) String bancoNome,
    @NotNull ModalidadeEmprestimoBancario modalidade,
    @NotNull @Positive BigDecimal valorPrincipal,
    @NotNull @PositiveOrZero BigDecimal taxaJurosMensal,
    @NotNull @Min(1) Integer totalParcelas,
    LocalDate dataContratacao,
    LocalDate dataPrimeiraParcela,
    Integer instituicaoUsuarioId,
    String observacoes
) {}