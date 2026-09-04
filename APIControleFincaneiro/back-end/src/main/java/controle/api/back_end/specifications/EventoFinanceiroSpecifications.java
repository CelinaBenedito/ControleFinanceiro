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
            Double valor, Double valorMin, Double valorMax, List<Tipo> tipo, LocalDate dataEvento, String descricao,
            List<TipoMovimento> tipoMovimento, List<InstituicaoUsuario> instituicao,
            List<CategoriaUsuario> categoria, String titulo) {

        return (root, query, cb) -> {
            if (query != null) query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (valor != null) predicates.add(cb.equal(root.get("valor"), valor));
            if (valorMin != null) predicates.add(cb.greaterThanOrEqualTo(root.get("valor"), valorMin));
            if (valorMax != null) predicates.add(cb.lessThanOrEqualTo(root.get("valor"), valorMax));
            if (tipo != null && !tipo.isEmpty()) {
                predicates.add(root.get("tipo").in(tipo));
            }
            if (dataEvento != null) predicates.add(cb.equal(root.get("dataEvento"), dataEvento));
            if (descricao != null && !descricao.isEmpty()) predicates.add(cb.like(cb.lower(root.get("descricao")), "%" + descricao.toLowerCase() + "%"));

            if (tipoMovimento != null && !tipoMovimento.isEmpty()) {
                Join<EventoFinanceiro, EventoInstituicao> joinInstituicao = root.join("eventoInstituicoes", JoinType.LEFT);
                predicates.add(joinInstituicao.get("tipoMovimento").in(tipoMovimento));
            }
            if (instituicao != null && !instituicao.isEmpty()) {
                Join<EventoFinanceiro, EventoInstituicao> joinInst2 = root.join("eventoInstituicoes", JoinType.LEFT);
                predicates.add(joinInst2.get("instituicaoUsuario").in(instituicao));
            }

            // "eventoDetalhe" é o nome real do atributo mapeado na entidade
            // (o getter chama-se getGastoDetalhe(), mas o campo/atributo JPA é "eventoDetalhe").
            if ((titulo != null && !titulo.isEmpty()) || (categoria != null && !categoria.isEmpty())) {
                Join<EventoFinanceiro, EventoDetalhe> joinDetalhe = root.join("eventoDetalhe", JoinType.LEFT);

                if (titulo != null && !titulo.isEmpty()) {
                    predicates.add(cb.like(cb.lower(joinDetalhe.get("tituloGasto")), "%" + titulo.toLowerCase() + "%"));
                }
                if (categoria != null && !categoria.isEmpty()) {
                    Join<EventoDetalhe, CategoriaUsuario> joinCategoria = joinDetalhe.join("categoriaUsuario", JoinType.LEFT);
                    predicates.add(joinCategoria.in(categoria));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}