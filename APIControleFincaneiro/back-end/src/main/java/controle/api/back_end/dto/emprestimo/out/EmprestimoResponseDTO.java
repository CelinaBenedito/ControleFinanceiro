package controle.api.back_end.dto.emprestimo.out;

import controle.api.back_end.model.emprestimo.StatusEmprestimo;
import controle.api.back_end.model.emprestimo.TipoEmprestimo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta com informações completas do empréstimo.
 */
public record EmprestimoResponseDTO(
        UUID id,
        UUID usuarioId,
        TipoEmprestimo tipo,
        StatusEmprestimo status,
        String pessoaOuGrupo,
        BigDecimal valorTotal,
        BigDecimal valorPago,
        BigDecimal valorRestante,
        Double percentualPago,
        LocalDate dataEmprestimo,
        LocalDate dataPrevisao,
        LocalDateTime dataCriacao,
        LocalDateTime dataQuitacao,
        String observacoes,
        Boolean atrasado
) {
}

