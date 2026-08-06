package controle.api.back_end.service;
import controle.api.back_end.dto.emprestimo.in.EmprestimoCreateDTO;
import controle.api.back_end.dto.emprestimo.in.EmprestimoPagamentoParcialDTO;
import controle.api.back_end.dto.emprestimo.in.EmprestimoQuitarDTO;
import controle.api.back_end.dto.emprestimo.out.EmprestimoResponseDTO;
import controle.api.back_end.dto.emprestimo.out.EmprestimoResumoDTO;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.emprestimo.Emprestimo;
import controle.api.back_end.model.emprestimo.StatusEmprestimo;
import controle.api.back_end.model.emprestimo.TipoEmprestimo;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.EmprestimoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
public class EmprestimoService {
    private final EmprestimoRepository emprestimoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final RegistroService registroService;
    public EmprestimoService(EmprestimoRepository emprestimoRepository,
                             UsuarioRepository usuarioRepository,
                             InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                             @Lazy RegistroService registroService) {
        this.emprestimoRepository = emprestimoRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.registroService = registroService;
    }
    @Transactional
    public EmprestimoResponseDTO criar(EmprestimoCreateDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usu\u00e1rio n\u00e3o encontrado"));
        LocalDate dataEvento = dto.dataEmprestimo() != null ? dto.dataEmprestimo() : LocalDate.now();
        Emprestimo emprestimo = new Emprestimo();
        emprestimo.setUsuario(usuario);
        emprestimo.setTipo(dto.tipo());
        emprestimo.setPessoaOuGrupo(dto.pessoaOuGrupo());
        emprestimo.setValorTotal(dto.valorTotal());
        emprestimo.setValorPago(BigDecimal.ZERO);
        emprestimo.setStatus(StatusEmprestimo.PENDENTE);
        emprestimo.setDataPrevisao(dto.dataPrevisao());
        emprestimo.setDataEmprestimo(dataEvento);
        emprestimo.setObservacoes(dto.observacoes());
        emprestimo.setDataCriacao(LocalDateTime.now());
        emprestimo.setInstituicaoUsuarioId(dto.instituicaoUsuarioId());
        Tipo tipoEvento = dto.tipo() == TipoEmprestimo.EMPRESTEI ? Tipo.Gasto : Tipo.Recebimento;
        String titulo = dto.tipo() == TipoEmprestimo.EMPRESTEI
                ? "Empr\u00e9stimo pessoal p/ " + dto.pessoaOuGrupo()
                : "Empr\u00e9stimo recebido de " + dto.pessoaOuGrupo();
        criarEventoEmprestimo(usuario, dto.instituicaoUsuarioId(), tipoEvento,
                dto.valorTotal().doubleValue(), titulo, dataEvento);
        return toResponseDTO(emprestimoRepository.save(emprestimo));
    }
    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarPorUsuario(UUID usuarioId) {
        validarUsuarioExiste(usuarioId);
        return emprestimoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarNaoQuitados(UUID usuarioId) {
        validarUsuarioExiste(usuarioId);
        return emprestimoRepository.findNaoQuitadosByUsuarioId(usuarioId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarPorTipo(UUID usuarioId, TipoEmprestimo tipo) {
        validarUsuarioExiste(usuarioId);
        return emprestimoRepository.findByUsuarioIdAndTipoOrderByDataCriacaoDesc(usuarioId, tipo)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public EmprestimoResponseDTO buscarPorId(UUID emprestimoId) {
        return toResponseDTO(emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empr\u00e9stimo n\u00e3o encontrado")));
    }
    @Transactional(readOnly = true)
    public EmprestimoResumoDTO obterResumo(UUID usuarioId) {
        validarUsuarioExiste(usuarioId);
        Long qtdDevem  = emprestimoRepository.countPessoasDevemAoUsuario(usuarioId);
        BigDecimal aReceber = emprestimoRepository.somaValorAReceber(usuarioId);
        Long qtdDevo   = emprestimoRepository.countPessoasUsuarioDeve(usuarioId);
        BigDecimal aPagar   = emprestimoRepository.somaValorAPagar(usuarioId);
        return new EmprestimoResumoDTO(
                qtdDevem   != null ? qtdDevem   : 0L,
                aReceber   != null ? aReceber   : BigDecimal.ZERO,
                qtdDevo    != null ? qtdDevo    : 0L,
                aPagar     != null ? aPagar     : BigDecimal.ZERO
        );
    }
    @Transactional
    public EmprestimoResponseDTO editar(UUID emprestimoId, EmprestimoCreateDTO dto) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empr\u00e9stimo n\u00e3o encontrado"));
        emprestimo.setTipo(dto.tipo());
        emprestimo.setPessoaOuGrupo(dto.pessoaOuGrupo());
        emprestimo.setValorTotal(dto.valorTotal());
        emprestimo.setDataPrevisao(dto.dataPrevisao());
        if (dto.dataEmprestimo() != null) emprestimo.setDataEmprestimo(dto.dataEmprestimo());
        emprestimo.setObservacoes(dto.observacoes());
        atualizarStatus(emprestimo);
        return toResponseDTO(emprestimoRepository.save(emprestimo));
    }
    @Transactional
    public EmprestimoResponseDTO registrarPagamentoParcial(UUID emprestimoId,
                                                           EmprestimoPagamentoParcialDTO dto) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empr\u00e9stimo n\u00e3o encontrado"));
        BigDecimal novoValorPago = emprestimo.getValorPago().add(dto.valorPago());
        if (novoValorPago.compareTo(emprestimo.getValorTotal()) > 0) {
            throw new IllegalArgumentException("Valor pago n\u00e3o pode ser maior que o valor total");
        }
        emprestimo.setValorPago(novoValorPago);
        atualizarStatus(emprestimo);
        Emprestimo atualizado = emprestimoRepository.save(emprestimo);
        Tipo tipoEvento = emprestimo.getTipo() == TipoEmprestimo.EMPRESTEI ? Tipo.Recebimento : Tipo.Gasto;
        String titulo = emprestimo.getTipo() == TipoEmprestimo.EMPRESTEI
                ? "Retorno de empr\u00e9stimo de " + emprestimo.getPessoaOuGrupo()
                : "Pagamento de empr\u00e9stimo p/ " + emprestimo.getPessoaOuGrupo();
        LocalDate dataPag = dto.dataPagamento() != null ? dto.dataPagamento() : LocalDate.now();
        criarEventoEmprestimo(emprestimo.getUsuario(), dto.instituicaoUsuarioId(), tipoEvento,
                dto.valorPago().doubleValue(), titulo, dataPag);
        return toResponseDTO(atualizado);
    }
    @Transactional
    public EmprestimoResponseDTO marcarComoQuitado(UUID emprestimoId, EmprestimoQuitarDTO dto) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empr\u00e9stimo n\u00e3o encontrado"));
        BigDecimal valorRestante = emprestimo.getValorTotal().subtract(emprestimo.getValorPago());
        emprestimo.setValorPago(emprestimo.getValorTotal());
        emprestimo.setStatus(StatusEmprestimo.QUITADO);
        emprestimo.setDataQuitacao(LocalDateTime.now());
        Emprestimo atualizado = emprestimoRepository.save(emprestimo);
        if (valorRestante.compareTo(BigDecimal.ZERO) > 0) {
            Integer instId = dto != null && dto.instituicaoUsuarioId() != null
                    ? dto.instituicaoUsuarioId()
                    : emprestimo.getInstituicaoUsuarioId();
            if (instId != null) {
                Tipo tipoEvento = emprestimo.getTipo() == TipoEmprestimo.EMPRESTEI ? Tipo.Recebimento : Tipo.Gasto;
                String titulo = emprestimo.getTipo() == TipoEmprestimo.EMPRESTEI
                        ? "Quita\u00e7\u00e3o de empr\u00e9stimo de " + emprestimo.getPessoaOuGrupo()
                        : "Quita\u00e7\u00e3o de d\u00edvida p/ " + emprestimo.getPessoaOuGrupo();
                criarEventoEmprestimo(emprestimo.getUsuario(), instId, tipoEvento,
                        valorRestante.doubleValue(), titulo, LocalDate.now());
            }
        }
        return toResponseDTO(atualizado);
    }
    @Transactional
    public EmprestimoResponseDTO reabrir(UUID emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empr\u00e9stimo n\u00e3o encontrado"));
        emprestimo.setStatus(StatusEmprestimo.PENDENTE);
        emprestimo.setDataQuitacao(null);
        emprestimo.setValorPago(BigDecimal.ZERO);
        return toResponseDTO(emprestimoRepository.save(emprestimo));
    }
    @Transactional
    public void deletar(UUID emprestimoId) {
        if (!emprestimoRepository.existsById(emprestimoId)) {
            throw new EntidadeNaoEncontradaException("Empr\u00e9stimo n\u00e3o encontrado");
        }
        emprestimoRepository.deleteById(emprestimoId);
    }
    private void validarUsuarioExiste(UUID usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new EntidadeNaoEncontradaException("Usu\u00e1rio n\u00e3o encontrado");
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
    private void criarEventoEmprestimo(Usuario usuario, Integer instituicaoUsuarioId,
                                       Tipo tipo, Double valor, String titulo,
                                       LocalDate dataEvento) {
        InstituicaoUsuario inst = instituicaoUsuarioRepository.findById(instituicaoUsuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Institui\u00e7\u00e3o n\u00e3o encontrada: " + instituicaoUsuarioId));
        EventoFinanceiro ef = new EventoFinanceiro();
        ef.setUsuario(usuario);
        ef.setTipo(tipo);
        ef.setValor(valor);
        ef.setDescricao(titulo);
        ef.setDataEvento(dataEvento);
        ef.setDataRegistro(LocalDateTime.now());
        EventoInstituicao ei = new EventoInstituicao();
        ei.setInstituicaoUsuario(inst);
        ei.setTipoMovimento(TipoMovimento.Debito);
        ei.setValor(valor);
        ei.setParcelas(1);
        EventoDetalhe detalhe = new EventoDetalhe();
        detalhe.setTituloGasto(titulo);
        detalhe.setCategoriaUsuario(new ArrayList<>());
        registroService.createEventoFinanceiro(ef, List.of(ei), detalhe);
    }
    private EmprestimoResponseDTO toResponseDTO(Emprestimo emprestimo) {
        BigDecimal valorRestante = emprestimo.getValorTotal().subtract(emprestimo.getValorPago());
        double percentualPago = 0.0;
        if (emprestimo.getValorTotal().compareTo(BigDecimal.ZERO) > 0) {
            percentualPago = emprestimo.getValorPago()
                    .divide(emprestimo.getValorTotal(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
        }
        boolean atrasado = emprestimo.getDataPrevisao() != null
                && emprestimo.getStatus() != StatusEmprestimo.QUITADO
                && emprestimo.getDataPrevisao().isBefore(LocalDate.now());
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
                emprestimo.getDataEmprestimo(),
                emprestimo.getDataPrevisao(),
                emprestimo.getDataCriacao(),
                emprestimo.getDataQuitacao(),
                emprestimo.getObservacoes(),
                atrasado
        );
    }
}