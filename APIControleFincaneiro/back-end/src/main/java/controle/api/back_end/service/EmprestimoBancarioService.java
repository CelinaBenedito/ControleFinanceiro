package controle.api.back_end.service;
import controle.api.back_end.dto.emprestimo.bancario.EmprestimoBancarioCreateDTO;
import controle.api.back_end.dto.emprestimo.bancario.EmprestimoBancarioPagamentoDTO;
import controle.api.back_end.dto.emprestimo.bancario.EmprestimoBancarioResponseDTO;
import controle.api.back_end.dto.emprestimo.bancario.ParcelaEmprestimoBancarioDTO;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.emprestimo.EmprestimoBancario;
import controle.api.back_end.model.emprestimo.StatusEmprestimoBancario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.EmprestimoBancarioRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import controle.api.back_end.strategy.eventoFinanceiro.EmprestimoEvento;
import controle.api.back_end.strategy.eventoFinanceiro.Registro;
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
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoDetalheRepository eventoDetalheRepository;
    private final EmprestimoEvento emprestimoEvento;
    private final InstituicaoService instituicaoService;

    public EmprestimoBancarioService(EmprestimoBancarioRepository repository,
                                     UsuarioRepository usuarioRepository,
                                     InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                                     EventoFinanceiroRepository eventoFinanceiroRepository,
                                     EventoInstituicaoRepository eventoInstituicaoRepository,
                                     EventoDetalheRepository eventoDetalheRepository,
                                     EmprestimoEvento emprestimoEvento,
                                     InstituicaoService instituicaoService) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoDetalheRepository = eventoDetalheRepository;
        this.emprestimoEvento = emprestimoEvento;
        this.instituicaoService = instituicaoService;
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

        // Salva o empréstimo bancário primeiro para ter o ID
        eb = repository.save(eb);

        // Usa o strategy para gerar eventos: 1 recebimento + N parcelas de gasto futuras
        if (dto.instituicaoUsuarioId() != null) {
            InstituicaoUsuario inst = instituicaoUsuarioRepository.findById(dto.instituicaoUsuarioId())
                    .orElseThrow(() -> new EntidadeNaoEncontradaException("Institui\u00e7\u00e3o n\u00e3o encontrada"));

            // Cria o evento base para processar pelo strategy
            EventoFinanceiro eventoBase = new EventoFinanceiro();
            eventoBase.setUsuario(usuario);
            eventoBase.setTipo(Tipo.Emprestimo);
            eventoBase.setValor(dto.valorPrincipal().doubleValue());
            eventoBase.setDescricao("Empr\u00e9stimo banc\u00e1rio " + dto.bancoNome());
            eventoBase.setDataEvento(eb.getDataContratacao());
            eventoBase.setDataRegistro(LocalDateTime.now());

            // Calcula taxa total em percentual para o strategy
            BigDecimal valorTotalComJuros = valorParcela.multiply(BigDecimal.valueOf(dto.totalParcelas()));
            BigDecimal taxaTotalPercentual = valorTotalComJuros.subtract(dto.valorPrincipal())
                    .divide(dto.valorPrincipal(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            eventoBase.setTaxaRendimento(taxaTotalPercentual.doubleValue());

            // Prepara instituição para o strategy
            EventoInstituicao ei = new EventoInstituicao();
            ei.setInstituicaoUsuario(inst);
            ei.setTipoMovimento(TipoMovimento.Debito);
            ei.setValor(dto.valorPrincipal().doubleValue());
            ei.setParcelas(dto.totalParcelas());

            // Detalhe com título
            EventoDetalhe detalhe = new EventoDetalhe();
            detalhe.setTituloGasto("Empr\u00e9stimo banc\u00e1rio " + dto.bancoNome());
            detalhe.setCategoriaUsuario(new ArrayList<>());

            // Processa via strategy
            Registro registro = emprestimoEvento.processar(eventoBase, List.of(ei), detalhe);

            // Persiste todos os eventos gerados e vincula ao empréstimo bancário
            EmprestimoBancario ebFinal = eb;
            for (EventoFinanceiro ef : registro.getEventosFinanceiros()) {
                ef.setEmprestimoBancario(ebFinal);

                // Ajusta datas das parcelas com base na data da primeira parcela
                if (ef.getTipo() == Tipo.Gasto) {
                    // Calcula qual parcela é baseado na descrição
                    int numeroParcela = extrairNumeroParcela(ef.getDescricao());
                    if (numeroParcela > 0 && dto.dataPrimeiraParcela() != null) {
                        ef.setDataEvento(dto.dataPrimeiraParcela().plusMonths(numeroParcela - 1));
                    }
                }

                EventoFinanceiro efSalvo = eventoFinanceiroRepository.save(ef);

                // Salva instituições
                List<EventoInstituicao> instituicoes = registro.getInstituicoesPorEvento().get(ef);
                if (instituicoes != null) {
                    for (EventoInstituicao instEvento : instituicoes) {
                        instEvento.setEventoFinanceiro(efSalvo);
                        eventoInstituicaoRepository.save(instEvento);
                    }
                }

                // Salva detalhe
                EventoDetalhe det = registro.getDetalhePorEvento().get(ef);
                if (det != null) {
                    det.setEventoFinanceiro(efSalvo);
                    eventoDetalheRepository.save(det);
                }
            }
        }

        return toDTO(eb);
    }

    private int extrairNumeroParcela(String descricao) {
        if (descricao != null && descricao.contains("Parcela ")) {
            try {
                String[] parts = descricao.split("Parcela ")[1].split("/");
                return Integer.parseInt(parts[0]);
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
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

        // Determinar instituição para débito
        Integer instituicaoId = dto.instituicaoUsuarioId() != null
            ? dto.instituicaoUsuarioId()
            : eb.getInstituicaoUsuarioId();

        // Verificar saldo disponível
        if (instituicaoId != null) {
            BigDecimal saldoDisponivel = instituicaoService.getSaldoByInstituicao(instituicaoId);
            if (saldoDisponivel.compareTo(eb.getValorParcela()) < 0) {
                throw new IllegalStateException(
                    "Saldo insuficiente na conta. Saldo dispon\u00edvel: R$ " + saldoDisponivel +
                    ", Valor da parcela: R$ " + eb.getValorParcela()
                );
            }
        }

        // Busca o próximo evento de gasto futuro vinculado a este empréstimo
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository
                .findAllByEmprestimoBancario_Id(eb.getId())
                .stream()
                .filter(e -> e.getTipo() == Tipo.Gasto)
                .sorted((a, b) -> a.getDataEvento().compareTo(b.getDataEvento()))
                .toList();

        if (!eventos.isEmpty() && eb.getParcelasPagas() < eventos.size()) {
            EventoFinanceiro eventoFuturo = eventos.get(eb.getParcelasPagas());

            // Atualiza a data do evento futuro para a data real de pagamento
            LocalDate dataPagamento = dto.dataPagamento() != null ? dto.dataPagamento() : LocalDate.now();
            eventoFuturo.setDataEvento(dataPagamento);

            // Atualiza instituição se fornecida
            if (dto.instituicaoUsuarioId() != null) {
                InstituicaoUsuario inst = instituicaoUsuarioRepository.findById(dto.instituicaoUsuarioId())
                        .orElseThrow(() -> new EntidadeNaoEncontradaException("Institui\u00e7\u00e3o n\u00e3o encontrada"));

                List<EventoInstituicao> instituicoes = eventoInstituicaoRepository
                        .findEventoInstituicaoByEventoFinanceiro_Id(eventoFuturo.getId());

                if (!instituicoes.isEmpty()) {
                    EventoInstituicao ei = instituicoes.get(0);
                    ei.setInstituicaoUsuario(inst);
                    eventoInstituicaoRepository.save(ei);
                }
            }

            eventoFinanceiroRepository.save(eventoFuturo);
        }

        eb.setParcelasPagas(eb.getParcelasPagas() + 1);
        if (eb.getParcelasPagas().equals(eb.getTotalParcelas())) {
            eb.setStatus(StatusEmprestimoBancario.QUITADO);
            eb.setDataQuitacao(LocalDateTime.now());
        }

        return toDTO(repository.save(eb));
    }
    @Transactional
    public EmprestimoBancarioResponseDTO quitar(UUID id, EmprestimoBancarioPagamentoDTO dto) {
        EmprestimoBancario eb = buscarEntidade(id);
        if (eb.getStatus() != StatusEmprestimoBancario.ATIVO)
            throw new IllegalStateException("Empr\u00e9stimo n\u00e3o est\u00e1 ativo");

        // Calcular valor total a quitar (parcelas restantes)
        int parcelasRestantes = eb.getTotalParcelas() - eb.getParcelasPagas();
        BigDecimal valorTotalQuitar = eb.getValorParcela().multiply(BigDecimal.valueOf(parcelasRestantes));

        // Determinar instituição para débito
        Integer instituicaoId = (dto != null && dto.instituicaoUsuarioId() != null)
            ? dto.instituicaoUsuarioId()
            : eb.getInstituicaoUsuarioId();

        // Verificar saldo disponível
        if (instituicaoId != null) {
            BigDecimal saldoDisponivel = instituicaoService.getSaldoByInstituicao(instituicaoId);
            if (saldoDisponivel.compareTo(valorTotalQuitar) < 0) {
                throw new IllegalStateException(
                    "Saldo insuficiente para quitar o empr\u00e9stimo. Saldo dispon\u00edvel: R$ " + saldoDisponivel +
                    ", Valor total a quitar (" + parcelasRestantes + " parcelas): R$ " + valorTotalQuitar
                );
            }
        }

        // Busca todos os eventos de gasto futuros
        List<EventoFinanceiro> eventosFuturos = eventoFinanceiroRepository
                .findAllByEmprestimoBancario_Id(eb.getId())
                .stream()
                .filter(e -> e.getTipo() == Tipo.Gasto)
                .sorted((a, b) -> a.getDataEvento().compareTo(b.getDataEvento()))
                .skip(eb.getParcelasPagas())
                .toList();

        LocalDate dataQuitacao = dto != null && dto.dataPagamento() != null ? dto.dataPagamento() : LocalDate.now();

        // Atualiza todas as parcelas restantes para a data de quitação
        for (EventoFinanceiro ef : eventosFuturos) {
            ef.setDataEvento(dataQuitacao);

            // Atualiza instituição se fornecida
            if (dto != null && dto.instituicaoUsuarioId() != null) {
                InstituicaoUsuario inst = instituicaoUsuarioRepository.findById(dto.instituicaoUsuarioId())
                        .orElseThrow(() -> new EntidadeNaoEncontradaException("Institui\u00e7\u00e3o n\u00e3o encontrada"));

                List<EventoInstituicao> instituicoes = eventoInstituicaoRepository
                        .findEventoInstituicaoByEventoFinanceiro_Id(ef.getId());

                if (!instituicoes.isEmpty()) {
                    EventoInstituicao ei = instituicoes.get(0);
                    ei.setInstituicaoUsuario(inst);
                    eventoInstituicaoRepository.save(ei);
                }
            }

            eventoFinanceiroRepository.save(ef);
        }

        eb.setParcelasPagas(eb.getTotalParcelas());
        eb.setStatus(StatusEmprestimoBancario.QUITADO);
        eb.setDataQuitacao(LocalDateTime.now());

        return toDTO(repository.save(eb));
    }
    @Transactional
    public void deletar(UUID id) {
        if (!repository.existsById(id))
            throw new EntidadeNaoEncontradaException("Empr\u00e9stimo banc\u00e1rio n\u00e3o encontrado");

        // Deleta eventos vinculados
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByEmprestimoBancario_Id(id);
        for (EventoFinanceiro ef : eventos) {
            eventoFinanceiroRepository.delete(ef);
        }

        repository.deleteById(id);
    }
    private EmprestimoBancario buscarEntidade(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Empr\u00e9stimo banc\u00e1rio n\u00e3o encontrado"));
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

    @Transactional(readOnly = true)
    public List<ParcelaEmprestimoBancarioDTO> listarParcelas(UUID emprestimoId) {
        EmprestimoBancario eb = buscarEntidade(emprestimoId);

        // Busca todos os eventos de gasto vinculados a este empréstimo
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository
                .findAllByEmprestimoBancario_Id(eb.getId())
                .stream()
                .filter(e -> e.getTipo() == Tipo.Gasto)
                .sorted((a, b) -> a.getDataEvento().compareTo(b.getDataEvento()))
                .toList();

        LocalDate hoje = LocalDate.now();
        List<ParcelaEmprestimoBancarioDTO> parcelas = new ArrayList<>();

        for (int i = 0; i < eventos.size(); i++) {
            EventoFinanceiro evento = eventos.get(i);
            int numeroParcela = i + 1;

            // Determina o status da parcela
            String status;
            boolean podeSerPaga;

            if (numeroParcela <= eb.getParcelasPagas()) {
                status = "PAGA";
                podeSerPaga = false;
            } else {
                // Parcelas não pagas
                if (evento.getDataEvento().isBefore(hoje)) {
                    status = "ATRASADA";
                } else {
                    status = "A_VENCER";
                }
                podeSerPaga = eb.getStatus() == StatusEmprestimoBancario.ATIVO;
            }

            parcelas.add(new ParcelaEmprestimoBancarioDTO(
                    evento.getId(),
                    numeroParcela,
                    eb.getTotalParcelas(),
                    eb.getValorParcela(),
                    evento.getDataEvento(),
                    status,
                    evento.getDescricao(),
                    podeSerPaga
            ));
        }

        return parcelas;
    }
}