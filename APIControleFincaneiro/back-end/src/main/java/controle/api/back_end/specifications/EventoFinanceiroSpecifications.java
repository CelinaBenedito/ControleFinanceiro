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
            Double valor, List<Tipo> tipo, LocalDate dataEvento, String descricao,
            List<TipoMovimento> tipoMovimento, List<InstituicaoUsuario> instituicao,
            List<CategoriaUsuario> categoria, String titulo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (valor != null) predicates.add(cb.equal(root.get("valor"), valor));
            if (tipo != null && !tipo.isEmpty()) {
                predicates.add(root.get("tipo").in(tipo));
            }
            if (titulo != null && !titulo.isEmpty()){
                predicates.add(root.get("titulo").in(titulo));
            }
            if (dataEvento != null) predicates.add(cb.equal(root.get("dataEvento"), dataEvento));
            if (descricao != null) predicates.add(cb.like(cb.lower(root.get("descricao")), "%" + descricao.toLowerCase() + "%"));

            if (tipoMovimento != null && !tipoMovimento.isEmpty()) {
                Join<EventoFinanceiro, EventoInstituicao> joinInstituicao = root.join("eventoInstituicao", JoinType.LEFT);
                predicates.add(joinInstituicao.get("tipoMovimento").in(tipoMovimento));
            }
            if (instituicao != null && !instituicao.isEmpty()) {
                Join<EventoFinanceiro, EventoInstituicao> joinInstituicao = root.join("eventoInstituicao", JoinType.LEFT);
                predicates.add(joinInstituicao.get("instituicaoUsuario").in(instituicao));
            }


            if (categoria != null && !categoria.isEmpty()) {
                Join<EventoFinanceiro, EventoDetalhe> joinGasto = root.join("gastoDetalhe", JoinType.LEFT);
                predicates.add(joinGasto.get("categoriaUsuario").in(categoria));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}