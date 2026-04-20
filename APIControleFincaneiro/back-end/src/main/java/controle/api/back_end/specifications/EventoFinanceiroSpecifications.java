package controle.api.back_end.specifications;

import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventoFinanceiroSpecifications {

    public static Specification<EventoFinanceiro> porFiltros(
            Double valor, Tipo tipo, LocalDate dataEvento, String descricao,
            TipoMovimento tipoMovimento, InstituicaoUsuario instituicao,
            CategoriaUsuario categoria, String titulo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (valor != null) predicates.add(cb.equal(root.get("valor"), valor));
            if (tipo != null) predicates.add(cb.equal(root.get("tipo"), tipo));
            if (dataEvento != null) predicates.add(cb.equal(root.get("dataEvento"), dataEvento));
            if (descricao != null) predicates.add(cb.like(cb.lower(root.get("descricao")), "%" + descricao.toLowerCase() + "%"));

            if (tipoMovimento != null || instituicao != null) {
                Join<EventoFinanceiro, EventoInstituicao> joinInstituicao = root.join("eventoInstituicao", JoinType.LEFT);
                if (tipoMovimento != null) predicates.add(cb.equal(joinInstituicao.get("tipoMovimento"), tipoMovimento));
                if (instituicao != null) predicates.add(cb.equal(joinInstituicao.get("instituicaoUsuario"), instituicao));
            }

            if (categoria != null || titulo != null) {
                Join<EventoFinanceiro, GastoDetalhe> joinGasto = root.join("gastoDetalhe", JoinType.LEFT);
                if (categoria != null) predicates.add(cb.equal(joinGasto.get("categoriaUsuario"), categoria));
                if (titulo != null) predicates.add(cb.like(cb.lower(joinGasto.get("tituloGasto")), "%" + titulo.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}