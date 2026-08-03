package controle.api.back_end.repository;
import controle.api.back_end.model.emprestimo.EmprestimoBancario;
import controle.api.back_end.model.emprestimo.StatusEmprestimoBancario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@Repository
public interface EmprestimoBancarioRepository extends JpaRepository<EmprestimoBancario, UUID> {
    List<EmprestimoBancario> findByUsuarioIdOrderByDataCriacaoDesc(UUID usuarioId);
    List<EmprestimoBancario> findByUsuarioIdAndStatusOrderByDataCriacaoDesc(
            UUID usuarioId, StatusEmprestimoBancario status);
    @Query("SELECT COALESCE(SUM(eb.valorParcela * (eb.totalParcelas - eb.parcelasPagas)), 0) " +
           "FROM EmprestimoBancario eb WHERE eb.usuario.id = :uid AND eb.status = 'ATIVO'")
    BigDecimal somaDebitosPendentes(@Param("uid") UUID uid);
}