package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InstituicaoService {
    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;


    public InstituicaoService(InstituicaoRepository instituicaoRepository,
                              UsuarioRepository usuarioRepository,
                              InstituicaoUsuarioRepository instituicaoUsuarioRepository, EventoInstituicaoRepository eventoInstituicaoRepository, EventoFinanceiroRepository eventoFinanceiroRepository) {
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
    }

    public List<Instituicao> getInstituicoes() {
        return instituicaoRepository.findAll();
    }

    public Instituicao getInstituicaoById(Integer id) {
        return instituicaoRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                                "Instituicao de id: %d não encontrada".formatted(id)
                        )
                );
    }


    public Instituicao createInstituicao(Instituicao entity) {
        Instituicao instituicaoByNomeContainingIgnoreCase = instituicaoRepository.findInstituicaoByNomeContainingIgnoreCase(entity.getNome());
        if (instituicaoByNomeContainingIgnoreCase != null) {
            throw new EntidadeJaExisteException("Já existe uma instituição com o nome %s no banco de dados".formatted(entity.getNome()));
        }
        return instituicaoRepository.save(entity);
    }

    public void deleteInstituicao(Integer id) {
        if (instituicaoRepository.existsById(id)) {
            instituicaoRepository.deleteInstituicaoById(id);

        } else {
            throw new EntidadeNaoEncontradaException("Instituição de id: %d não encontrada.".formatted(id));
        }
    }

    public InstituicaoUsuario createInstituicaoForUsuario(Integer instituicao_id, UUID user_id) {
        if (!usuarioRepository.existsById(user_id)) {
            throw new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado"
                    .formatted(user_id));
        }
        if (!instituicaoRepository.existsById(instituicao_id)) {
            throw new EntidadeNaoEncontradaException("Instituição de id: %s não encontrado".formatted(instituicao_id));
        }

        Optional<Usuario> user = usuarioRepository.findById(user_id);
        Optional<Instituicao> instituicao = instituicaoRepository.findById(instituicao_id);

        if (instituicaoUsuarioRepository.existsByUsuarioAndInstituicao(user.get(), instituicao.get())) {
            throw new IllegalArgumentException("Usuário já vinculado a esta instituição.");
        }
        InstituicaoUsuario instituicaoUsuario = new InstituicaoUsuario();
        instituicaoUsuario.setInstituicao(instituicao.get());
        instituicaoUsuario.setUsuario(user.get());
        instituicaoUsuario.setIsAtivo(true);
        return instituicaoUsuarioRepository.save(instituicaoUsuario);
    }

    public List<InstituicaoUsuario> getInstituicoesByUserId(UUID idUser) {
        if (!usuarioRepository.existsById(idUser)) {
            throw new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado".formatted(idUser));
        }
        return instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(idUser);
    }

    public InstituicaoUsuario detachUserFromInstituicao(Integer instituicaoId, UUID userId) {
        if (!usuarioRepository.existsById(userId)) {
            throw new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado".formatted(userId));
        }
        if (!instituicaoRepository.existsById(instituicaoId)) {
            throw new EntidadeNaoEncontradaException("Instituicao de id: %d não encontrado".formatted(instituicaoId));
        }
        InstituicaoUsuario instituicaoUsuario = instituicaoUsuarioRepository.findByUsuario_IdAndInstituicao_Id(userId, instituicaoId);
        instituicaoUsuario.setIsAtivo(false);
        return instituicaoUsuarioRepository.save(instituicaoUsuario);
    }

    public BigDecimal getSaldoByInstituicao(Integer instituicaoUsuarioId) {
        instituicaoUsuarioRepository.findById(instituicaoUsuarioId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Associação de instituição e usuário de id: %d não encontrada."
                                        .formatted(instituicaoUsuarioId)
                        )
                );
        List<EventoInstituicao> eventosInstituicao =
                eventoInstituicaoRepository.findByInstituicaoUsuario_Id(instituicaoUsuarioId);

        BigDecimal saldo = BigDecimal.ZERO;

        for (EventoInstituicao eventoInstituicao : eventosInstituicao) {
            EventoFinanceiro eventoFinanceiro = eventoInstituicao.getEventoFinanceiro();

            if (eventoFinanceiro == null) {
                continue;
            }

            saldo = getSaldo(saldo, eventoFinanceiro);
        }

        return saldo;
    }

    static BigDecimal getSaldo(BigDecimal saldo, EventoFinanceiro eventoFinanceiro) {
        BigDecimal valor = BigDecimal.valueOf(eventoFinanceiro.getValor());

        if (eventoFinanceiro.getTipo() == Tipo.Gasto || eventoFinanceiro.getTipo() == Tipo.Transferencia) {
            saldo = saldo.subtract(valor);
        } else if (eventoFinanceiro.getTipo() == Tipo.Recebimento) {
            saldo = saldo.add(valor);
        }
        return saldo;
    }
}
