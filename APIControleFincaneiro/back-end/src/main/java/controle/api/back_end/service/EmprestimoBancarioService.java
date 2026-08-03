package controle.api.back_end.service;
import controle.api.back_end.dto.emprestimo.bancario.EmprestimoBancarioCreateDTO;
import controle.api.back_end.dto.emprestimo.bancario.EmprestimoBancarioPagamentoDTO;
import controle.api.back_end.dto.emprestimo.bancario.EmprestimoBancarioResponseDTO;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.emprestimo.EmprestimoBancario;
import controle.api.back_end.model.emprestimo.StatusEmprestimoBancario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.EmprestimoBancarioRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
public class EmprestimoBancarioService {
    private final EmprestimoBancarioRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final RegistroService registroService;
    public EmprestimoBancarioService(EmprestimoBancarioRepository repository,
                                     UsuarioRepository usuarioRepository,
                                     InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                                     @Lazy RegistroService registroService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.registroService = registroService;
    }
    /**
     * Calcula a parcela pelo sistema Price (tabela francesa).
     * PMT = PV * [i*(1+i)^n] / [(1+i)^n - 1]
     * Se a taxa for zero, retorna PV/n.
     */
    public static BigDecimal calcularParcelaPrice(BigDecimal valorPrincipal,
                                                   BigDecimal taxaJurosMensal,
                                                   int parcelas) {
        if (taxaJurosMensal.compareTo(BigDecimal.ZERO) == 0) {
            return valorPrincipal.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);
        }
        BigDecimal i = taxaJurosMensal.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal umMaisI = BigDecimal.ONE.add(i);
        BigDecimal fator = umMaisI.pow(parcelas, new MathContext(15, RoundingMode.HALF_UP));
        BigDecimal numerador = valorPrincipal.multiply(i).multiply(fator);
        BigDecimal denominador = fator.subtract(BigDecimal.ONE);
        return numerador.divide(denominador, 2, RoundingMode.HALF_UP);
    }
    @Transactional
    public EmprestimoBancarioResponseDTO criar(EmprestimoBancarioCreateDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.usuarioId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usu\u00e1rio n\u00e3o encontrado"));
        BigDecimal valorParcela = calcularParcelaPrice(dto.valorPrincipal(), dto.taxaJurosMensal(), dto.totalParcelas());
        EmprestimoBancario eb = new EmprestimoBancario();
        eb.setUsuario(usuario);
        eb.setBancoNome(dto.bancoNome());
        eb.setModalidade(dto.modalidade());
        eb.setValorPrincipal(dto.valorPrincipal());
        eb.setTaxaJurosMensal(dto.taxaJurosMensal());
        eb.setTotalParcelas(dto.totalParcelas());
        eb.setParcelasPagas(0);
        eb.setValorParcela(valorParcela);
        eb.setDataContratacao(dto.dataContratacao() != null ? dto.dataContratacao() : LocalDate.now());
        eb.setDataPrimeiraParcela(dto.dataPrimeiraParcela());
        eb.setInstituicaoUsuarioId(dto.instituicaoUsuarioId());
        eb.setObservacoes(dto.observacoes());
        eb.setStatus(StatusEmprestimoBancario.ATIVO);
        eb.setDataCriacao(LocalDateTime.now());
        // Evento: dinheiro do banco entrou na conta
        if (dto.instituicaoUsuarioId() != null) {
            String titulo = "Empr\u00e9stimo banc\u00e1rio " + dto.bancoNome();
            criarEvento(usuario, dto.instituicaoUsuarioId(), Tipo.Recebimento,
                    dto.valorPrincipal().doubleValue(), titulo,
                    eb.getDataContratacao());
        }
        return toDTO(repository.save(eb));
    }
    @Transactional(readOnly = true)
    public List<EmprestimoBancarioResponseDTO> listarPorUsuario(UUID usuarioId) {
        if (!usuarioRepository.existsById(usuarioId))
            throw new EntidadeNaoEncontradaException("Usu\u00e1rio n\u00e3o encontrado");
        return repository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }
    @Transactional
    public EmprestimoBancarioResponseDTO pagarParcela(UUID id, EmprestimoBancarioPagamentoDTO dto) {
        EmprestimoBancario eb = buscarEntidade(id);
        if (eb.getStatus() != StatusEmprestimoBancario.ATIVO)
            throw new IllegalStateException("Empr\u00e9stimo n\u00e3o est\u00e1 ativo");
        if (eb.getParcelasPagas() >= eb.getTotalParcelas())
            throw new IllegalStateException("Todas as parcelas j\u00e1 foram pagas");
        eb.setParcelasPagas(eb.getParcelasPagas() + 1);
        if (eb.getParcelasPagas().equals(eb.getTotalParcelas())) {
            eb.setStatus(StatusEmprestimoBancario.QUITADO);
            eb.setDataQuitacao(LocalDateTime.now());
        }
        Integer instId = dto.instituicaoUsuarioId() != null
                ? dto.instituicaoUsuarioId()
                : eb.getInstituicaoUsuarioId();
        if (instId != null) {
            LocalDate dataPag = dto.dataPagamento() != null ? dto.dataPagamento() : LocalDate.now();
            String titulo = "Parcela " + eb.getParcelasPagas() + "/" + eb.getTotalParcelas()
                    + " - " + eb.getBancoNome();
            criarEvento(eb.getUsuario(), instId, Tipo.Gasto,
                    eb.getValorParcela().doubleValue(), titulo, dataPag);
        }
        return toDTO(repository.save(eb));
    }
    @Transactional
    public EmprestimoBancarioResponseDTO quitar(UUID id, EmprestimoBancarioPagamentoDTO dto) {
        EmprestimoBancario eb = buscarEntidade(id);
        if (eb.getStatus() != StatusEmprestimoBancario.ATIVO)
            throw new IllegalStateException("Empr\u00e9stimo n\u00e3o est\u00e1 ativo");
        int parcelasRestantes = eb.getTotalParcelas() - eb.getParcelasPagas();
        eb.setParcelasPagas(eb.getTotalParcelas());
        eb.setStatus(StatusEmprestimoBancario.QUITADO);
        eb.setDataQuitacao(LocalDateTime.now());
        Integer instId = dto != null && dto.instituicaoUsuarioId() != null
                ? dto.instituicaoUsuarioId()
                : eb.getInstituicaoUsuarioId();
        if (instId != null && parcelasRestantes > 0) {
            BigDecimal valorTotal = eb.getValorParcela().multiply(BigDecimal.valueOf(parcelasRestantes));
            String titulo = "Quita\u00e7\u00e3o antecipada - " + eb.getBancoNome();
            LocalDate data = dto != null && dto.dataPagamento() != null ? dto.dataPagamento() : LocalDate.now();
            criarEvento(eb.getUsuario(), instId, Tipo.Gasto, valorTotal.doubleValue(), titulo, data);
        }
        return toDTO(repository.save(eb));
    }
    @Transactional
    public void deletar(UUID id) {
        if (!repository.existsById(id))
            throw new EntidadeNaoEncontradaException("Empr\u00e9stimo banc\u00e1rio n\u00e3o encontrado");
        repository.deleteById(id);
    }
    private EmprestimoBancario buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empr\u00e9stimo banc\u00e1rio n\u00e3o encontrado"));
    }
    private void criarEvento(Usuario usuario, Integer instituicaoUsuarioId,
                              Tipo tipo, double valor, String titulo, LocalDate data) {
        InstituicaoUsuario inst = instituicaoUsuarioRepository.findById(instituicaoUsuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Institui\u00e7\u00e3o n\u00e3o encontrada: " + instituicaoUsuarioId));
        EventoFinanceiro ef = new EventoFinanceiro();
        ef.setUsuario(usuario);
        ef.setTipo(tipo);
        ef.setValor(valor);
        ef.setDescricao(titulo);
        ef.setDataEvento(data);
        ef.setDataRegistro(LocalDateTime.now());
        EventoInstituicao ei = new EventoInstituicao();
        ei.setInstituicaoUsuario(inst);
        ei.setTipoMovimento(TipoMovimento.Dinheiro);
        ei.setValor(valor);
        ei.setParcelas(1);
        EventoDetalhe detalhe = new EventoDetalhe();
        detalhe.setTituloGasto(titulo);
        detalhe.setCategoriaUsuario(new ArrayList<>());
        registroService.createEventoFinanceiro(ef, List.of(ei), detalhe);
    }
    private EmprestimoBancarioResponseDTO toDTO(EmprestimoBancario eb) {
        int parcelasRestantes = eb.getTotalParcelas() - eb.getParcelasPagas();
        BigDecimal valorTotalComJuros = eb.getValorParcela()
                .multiply(BigDecimal.valueOf(eb.getTotalParcelas()));
        BigDecimal valorTotalPago = eb.getValorParcela()
                .multiply(BigDecimal.valueOf(eb.getParcelasPagas()));
        BigDecimal saldoDevedor = eb.getValorParcela()
                .multiply(BigDecimal.valueOf(parcelasRestantes));
        double percentualPago = eb.getTotalParcelas() > 0
                ? (double) eb.getParcelasPagas() / eb.getTotalParcelas() * 100
                : 0.0;
        LocalDate proximaParcela = null;
        boolean atrasado = false;
        if (eb.getDataPrimeiraParcela() != null && eb.getStatus() == StatusEmprestimoBancario.ATIVO) {
            proximaParcela = eb.getDataPrimeiraParcela().plusMonths(eb.getParcelasPagas());
            atrasado = proximaParcela.isBefore(LocalDate.now());
        }
        return new EmprestimoBancarioResponseDTO(
                eb.getId(), eb.getUsuario().getId(),
                eb.getBancoNome(), eb.getModalidade(),
                eb.getValorPrincipal(), eb.getTaxaJurosMensal(),
                eb.getTotalParcelas(), eb.getParcelasPagas(),
                parcelasRestantes, eb.getValorParcela(),
                valorTotalComJuros, valorTotalPago, saldoDevedor,
                percentualPago,
                eb.getDataContratacao(), eb.getDataPrimeiraParcela(),
                proximaParcela,
                eb.getDataCriacao(), eb.getDataQuitacao(),
                eb.getStatus(), eb.getObservacoes(), atrasado
        );
    }
}