package controle.api.back_end.service;

import controle.api.back_end.dto.emprestimo.in.EmprestimoCreateDTO;
import controle.api.back_end.dto.emprestimo.in.EmprestimoPagamentoParcialDTO;
import controle.api.back_end.dto.emprestimo.out.EmprestimoResponseDTO;
import controle.api.back_end.dto.emprestimo.out.EmprestimoResumoDTO;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.emprestimo.Emprestimo;
import controle.api.back_end.model.emprestimo.StatusEmprestimo;
import controle.api.back_end.model.emprestimo.TipoEmprestimo;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.EmprestimoRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmprestimoService {

    private final EmprestimoRepository emprestimoRepository;
    private final UsuarioRepository usuarioRepository;

    public EmprestimoService(EmprestimoRepository emprestimoRepository,
                             UsuarioRepository usuarioRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // =========================================================================
    // CRIAR
    // =========================================================================

    @Transactional
    public EmprestimoResponseDTO criar(EmprestimoCreateDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário não encontrado"));

        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setUsuario(usuario);
        emprestimo.setTipo(dto.tipo());
        emprestimo.setPessoaOuGrupo(dto.pessoaOuGrupo());
        emprestimo.setValorTotal(dto.valorTotal());
        emprestimo.setValorPago(BigDecimal.ZERO);
        emprestimo.setStatus(StatusEmprestimo.PENDENTE);
        emprestimo.setDataPrevisao(dto.dataPrevisao());
        emprestimo.setObservacoes(dto.observacoes());
        emprestimo.setDataCriacao(LocalDateTime.now());

        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        return toResponseDTO(salvo);
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarPorUsuario(UUID usuarioId) {
        validarUsuarioExiste(usuarioId);
        return emprestimoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarNaoQuitados(UUID usuarioId) {
        validarUsuarioExiste(usuarioId);
        return emprestimoRepository.findNaoQuitadosByUsuarioId(usuarioId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarPorTipo(UUID usuarioId, TipoEmprestimo tipo) {
        validarUsuarioExiste(usuarioId);
        return emprestimoRepository.findByUsuarioIdAndTipoOrderByDataCriacaoDesc(usuarioId, tipo)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmprestimoResponseDTO buscarPorId(UUID emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empréstimo não encontrado"));
        return toResponseDTO(emprestimo);
    }

    @Transactional(readOnly = true)
    public EmprestimoResumoDTO obterResumo(UUID usuarioId) {
        validarUsuarioExiste(usuarioId);

        Long quantidadePessoasDevem = emprestimoRepository.countPessoasDevemAoUsuario(usuarioId);
        BigDecimal valorAReceber = emprestimoRepository.somaValorAReceber(usuarioId);

        Long quantidadePessoasDevo = emprestimoRepository.countPessoasUsuarioDeve(usuarioId);
        BigDecimal valorAPagar = emprestimoRepository.somaValorAPagar(usuarioId);

        return new EmprestimoResumoDTO(
                quantidadePessoasDevem != null ? quantidadePessoasDevem : 0L,
                valorAReceber != null ? valorAReceber : BigDecimal.ZERO,
                quantidadePessoasDevo != null ? quantidadePessoasDevo : 0L,
                valorAPagar != null ? valorAPagar : BigDecimal.ZERO
        );
    }

    // =========================================================================
    // ATUALIZAR
    // =========================================================================

    @Transactional
    public EmprestimoResponseDTO editar(UUID emprestimoId, EmprestimoCreateDTO dto) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empréstimo não encontrado"));

        emprestimo.setTipo(dto.tipo());
        emprestimo.setPessoaOuGrupo(dto.pessoaOuGrupo());
        emprestimo.setValorTotal(dto.valorTotal());
        emprestimo.setDataPrevisao(dto.dataPrevisao());
        emprestimo.setObservacoes(dto.observacoes());

        // Recalcula o status baseado no valor pago
        atualizarStatus(emprestimo);

        Emprestimo atualizado = emprestimoRepository.save(emprestimo);
        return toResponseDTO(atualizado);
    }

    @Transactional
    public EmprestimoResponseDTO registrarPagamentoParcial(UUID emprestimoId,
                                                           EmprestimoPagamentoParcialDTO dto) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empréstimo não encontrado"));

        BigDecimal novoValorPago = emprestimo.getValorPago().add(dto.valorPago());

        if (novoValorPago.compareTo(emprestimo.getValorTotal()) > 0) {
            throw new IllegalArgumentException("Valor pago não pode ser maior que o valor total");
        }

        emprestimo.setValorPago(novoValorPago);
        atualizarStatus(emprestimo);

        Emprestimo atualizado = emprestimoRepository.save(emprestimo);
        return toResponseDTO(atualizado);
    }

    @Transactional
    public EmprestimoResponseDTO marcarComoQuitado(UUID emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empréstimo não encontrado"));

        emprestimo.setValorPago(emprestimo.getValorTotal());
        emprestimo.setStatus(StatusEmprestimo.QUITADO);
        emprestimo.setDataQuitacao(LocalDateTime.now());

        Emprestimo atualizado = emprestimoRepository.save(emprestimo);
        return toResponseDTO(atualizado);
    }

    @Transactional
    public EmprestimoResponseDTO reabrir(UUID emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empréstimo não encontrado"));

        emprestimo.setStatus(StatusEmprestimo.PENDENTE);
        emprestimo.setDataQuitacao(null);
        emprestimo.setValorPago(BigDecimal.ZERO);

        Emprestimo atualizado = emprestimoRepository.save(emprestimo);
        return toResponseDTO(atualizado);
    }

    // =========================================================================
    // DELETAR
    // =========================================================================

    @Transactional
    public void deletar(UUID emprestimoId) {
        if (!emprestimoRepository.existsById(emprestimoId)) {
            throw new EntidadeNaoEncontradaException("Empréstimo não encontrado");
        }
        emprestimoRepository.deleteById(emprestimoId);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private void validarUsuarioExiste(UUID usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new EntidadeNaoEncontradaException("Usuário não encontrado");
        }
    }

    private void atualizarStatus(Emprestimo emprestimo) {
        BigDecimal valorRestante = emprestimo.getValorTotal().subtract(emprestimo.getValorPago());

        if (valorRestante.compareTo(BigDecimal.ZERO) <= 0) {
            emprestimo.setStatus(StatusEmprestimo.QUITADO);
            emprestimo.setDataQuitacao(LocalDateTime.now());
        } else if (emprestimo.getValorPago().compareTo(BigDecimal.ZERO) > 0) {
            emprestimo.setStatus(StatusEmprestimo.PAGO_PARCIAL);
            emprestimo.setDataQuitacao(null);
        } else {
            emprestimo.setStatus(StatusEmprestimo.PENDENTE);
            emprestimo.setDataQuitacao(null);
        }
    }

    private EmprestimoResponseDTO toResponseDTO(Emprestimo emprestimo) {
        BigDecimal valorRestante = emprestimo.getValorTotal().subtract(emprestimo.getValorPago());

        Double percentualPago = 0.0;
        if (emprestimo.getValorTotal().compareTo(BigDecimal.ZERO) > 0) {
            percentualPago = emprestimo.getValorPago()
                    .divide(emprestimo.getValorTotal(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
        }

        Boolean atrasado = false;
        if (emprestimo.getDataPrevisao() != null
            && emprestimo.getStatus() != StatusEmprestimo.QUITADO
            && emprestimo.getDataPrevisao().isBefore(LocalDate.now())) {
            atrasado = true;
        }

        return new EmprestimoResponseDTO(
                emprestimo.getId(),
                emprestimo.getUsuario().getId(),
                emprestimo.getTipo(),
                emprestimo.getStatus(),
                emprestimo.getPessoaOuGrupo(),
                emprestimo.getValorTotal(),
                emprestimo.getValorPago(),
                valorRestante,
                percentualPago,
                emprestimo.getDataPrevisao(),
                emprestimo.getDataCriacao(),
                emprestimo.getDataQuitacao(),
                emprestimo.getObservacoes(),
                atrasado
        );
    }
}
