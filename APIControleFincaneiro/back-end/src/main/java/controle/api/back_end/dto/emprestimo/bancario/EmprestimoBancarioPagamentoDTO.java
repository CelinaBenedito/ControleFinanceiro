package controle.api.back_end.dto.emprestimo.bancario;
import java.time.LocalDate;
public record EmprestimoBancarioPagamentoDTO(
    Integer instituicaoUsuarioId,
    LocalDate dataPagamento
) {}