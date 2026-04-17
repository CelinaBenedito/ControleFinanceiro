package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.GastoDetalhe;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RegistroService {
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final GastoDetalheRepository gastoDetalheRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;

    public RegistroService(EventoFinanceiroRepository eventoFinanceiroRepository,
                           EventoInstituicaoRepository eventoInstituicaoRepository,
                           GastoDetalheRepository gastoDetalheRepository,
                           CategoriaUsuarioRepository categoriaUsuarioRepository,
                           UsuarioRepository usuarioRepository,
                           InstituicaoUsuarioRepository instituicaoUsuarioRepository) {
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.gastoDetalheRepository = gastoDetalheRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
    }

    public EventoFinanceiro createEventoFinanceiro(EventoFinanceiro entity) {

        Usuario user = usuarioRepository
                .findById(entity.getFkUsuario().getId())
                        .orElseThrow(() ->
                                new EntidadeNaoEncontradaException(
                                        "Usuário de id: %s não encontrado."
                                        .formatted(entity.getFkUsuario().getId()
                                        )
                                )
                        );

        entity.setFkUsuario(user);
        entity.setDataRegistro(LocalDate.now());
        return eventoFinanceiroRepository.save(entity);
    }

    public EventoInstituicao createEventoInstituicao(EventoInstituicao entity,
                                                     EventoFinanceiro eventoFinanceiro){
        if (!eventoFinanceiroRepository.existsById(eventoFinanceiro.getId())){
            throw new EntidadeNaoEncontradaException(
                    "Evento Financeiro de id: %s não encontrado"
                            .formatted(eventoFinanceiro.getId())
            );
        }
       InstituicaoUsuario instituicaoUsuario = instituicaoUsuarioRepository
               .findById(entity.getInstituicaoUsuario().getId())
               .orElseThrow(()->
                       new EntidadeNaoEncontradaException(
                               "Instituição associada ao Usuário não encontrada."
                       )
               );

       entity.setInstituicaoUsuario(instituicaoUsuario);
       entity.setEvento(eventoFinanceiro);
       return eventoInstituicaoRepository.save(entity);
    }

    public GastoDetalhe createGastoDetalhe(GastoDetalhe entity,EventoFinanceiro eventoFinanceiro){
       if (!eventoFinanceiroRepository.existsById(eventoFinanceiro.getId())){
           throw new EntidadeNaoEncontradaException(
                   "Evento Financeiro de id: %s não encontrado"
                           .formatted(eventoFinanceiro.getId())
           );
       }
        CategoriaUsuario categoriaUsuario = categoriaUsuarioRepository
                .findById(entity.getCategoriaUsuario().getId())
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException("Categoria Usuário de id: %d não encontrada."
                                .formatted(entity.getCategoriaUsuario().getId()))
                );
       entity.setEventoFinanceiro(eventoFinanceiro);
       entity.setCategoriaUsuario(categoriaUsuario);

       return gastoDetalheRepository.save(entity);
    }
}
