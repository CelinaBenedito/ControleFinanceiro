package controle.api.back_end.repository;

import controle.api.back_end.model.emprestimo.Emprestimo;
import controle.api.back_end.model.emprestimo.StatusEmprestimo;
import controle.api.back_end.model.emprestimo.TipoEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmprestimoRepository extends JpaRepository<Emprestimo, UUID> {

    /**
     * Lista todos os empréstimos de um usuário.
     */
    List<Emprestimo> findByUsuarioIdOrderByDataCriacaoDesc(UUID usuarioId);

    /**
     * Lista empréstimos por tipo e status.
     */
    List<Emprestimo> findByUsuarioIdAndTipoAndStatusOrderByDataCriacaoDesc(
            UUID usuarioId, TipoEmprestimo tipo, StatusEmprestimo status);

    /**
     * Lista empréstimos por tipo.
     */
    List<Emprestimo> findByUsuarioIdAndTipoOrderByDataCriacaoDesc(UUID usuarioId, TipoEmprestimo tipo);

    /**
     * Lista empréstimos pendentes e parciais (não quitados).
     */
    @Query("SELECT e FROM Emprestimo e WHERE e.usuario.id = :usuarioId " +
           "AND e.status IN ('PENDENTE', 'PAGO_PARCIAL') ORDER BY e.dataCriacao DESC")
    List<Emprestimo> findNaoQuitadosByUsuarioId(@Param("usuarioId") UUID usuarioId);

    /**
     * Conta quantas pessoas devem ao usuário (EMPRESTEI não quitado).
     */
    @Query("SELECT COUNT(DISTINCT e.pessoaOuGrupo) FROM Emprestimo e " +
           "WHERE e.usuario.id = :usuarioId AND e.tipo = 'EMPRESTEI' " +
           "AND e.status IN ('PENDENTE', 'PAGO_PARCIAL')")
    Long countPessoasDevemAoUsuario(@Param("usuarioId") UUID usuarioId);

    /**
     * Soma o valor total que o usuário emprestou e ainda não recebeu.
     */
    @Query("SELECT COALESCE(SUM(e.valorTotal - e.valorPago), 0) FROM Emprestimo e " +
           "WHERE e.usuario.id = :usuarioId AND e.tipo = 'EMPRESTEI' " +
           "AND e.status IN ('PENDENTE', 'PAGO_PARCIAL')")
    BigDecimal somaValorAReceber(@Param("usuarioId") UUID usuarioId);

    /**
     * Conta quantas pessoas o usuário deve (PEDI_EMPRESTADO não quitado).
     */
    @Query("SELECT COUNT(DISTINCT e.pessoaOuGrupo) FROM Emprestimo e " +
           "WHERE e.usuario.id = :usuarioId AND e.tipo = 'PEDI_EMPRESTADO' " +
           "AND e.status IN ('PENDENTE', 'PAGO_PARCIAL')")
    Long countPessoasUsuarioDeve(@Param("usuarioId") UUID usuarioId);

    /**
     * Soma o valor total que o usuário deve e ainda não pagou.
     */
    @Query("SELECT COALESCE(SUM(e.valorTotal - e.valorPago), 0) FROM Emprestimo e " +
           "WHERE e.usuario.id = :usuarioId AND e.tipo = 'PEDI_EMPRESTADO' " +
           "AND e.status IN ('PENDENTE', 'PAGO_PARCIAL')")
    BigDecimal somaValorAPagar(@Param("usuarioId") UUID usuarioId);
}

