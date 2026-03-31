package controle.api.back_end.repository;

import controle.api.back_end.model.Categoria;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria,Integer> {
    List<Categoria> findAllByUsuarioId(UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Categoria c WHERE c.id = :id")
    void deleteCategoriaById(Integer id);
}
