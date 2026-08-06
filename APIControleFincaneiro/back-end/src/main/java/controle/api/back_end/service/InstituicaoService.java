package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.dto.instituicao.in.AtualizarInstituicaoUsuarioDto;
import controle.api.back_end.dto.instituicao.out.DetalheInstituicaoDto;
import controle.api.back_end.dto.instituicao.out.ResumoInstituicaoDto;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InstituicaoService {
    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoDetalheRepository eventoDetalheRepository;
    private final UsuarioService usuarioService;


    public InstituicaoService(InstituicaoRepository instituicaoRepository,
                              UsuarioRepository usuarioRepository,
                              InstituicaoUsuarioRepository instituicaoUsuarioRepository, EventoInstituicaoRepository eventoInstituicaoRepository, EventoFinanceiroRepository eventoFinanceiroRepository, EventoDetalheRepository eventoDetalheRepository, UsuarioService usuarioService) {
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoDetalheRepository = eventoDetalheRepository;
        this.usuarioService = usuarioService;
    }

    public Page<Instituicao> getInstituicoes(Pageable pageable) {
        return instituicaoRepository.findAll(pageable);
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
        instituicaoUsuario.setUltimaModificacao(LocalDateTime.now());
        instituicaoUsuario.setIsAtivo(true);
        return instituicaoUsuarioRepository.save(instituicaoUsuario);
    }

    public Page<InstituicaoUsuario> getInstituicoesByUserId(UUID idUser, Pageable pageable) {
        if (!usuarioRepository.existsById(idUser)) {
            throw new EntidadeNaoEncontradaException("Usuario de id: %s não encontrado".formatted(idUser));
        }
        return instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(idUser, pageable);
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
        instituicaoUsuario.setUltimaModificacao(LocalDateTime.now());
        return instituicaoUsuarioRepository.save(instituicaoUsuario);
    }

    /**
     * Retorna o crédito disponível do cartão de crédito: limite - totalCreditoUsado.
     * Para instituições sem limite configurado, retorna o saldo de débito (fluxo de caixa).
     * Use este método apenas para EXIBIÇÃO do saldo do cartão de crédito.
     */
    public BigDecimal getSaldoByInstituicao(Integer instituicaoUsuarioId) {
        InstituicaoUsuario iu = instituicaoUsuarioRepository.findById(instituicaoUsuarioId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Associação de instituição e usuário de id: %d não encontrada."
                                        .formatted(instituicaoUsuarioId)
                        )
                );
        List<EventoInstituicao> eventosInstituicao =
                eventoInstituicaoRepository.findByInstituicaoUsuario_Id(instituicaoUsuarioId);

        BigDecimal saldo = BigDecimal.ZERO;
        BigDecimal totalCreditoUsado = BigDecimal.ZERO;

        for (EventoInstituicao ei : eventosInstituicao) {
            EventoFinanceiro eventoFinanceiro = ei.getEventoFinanceiro();
            if (eventoFinanceiro == null) continue;

            saldo = getSaldoPorMovimento(saldo, eventoFinanceiro, ei);

            if (ei.getTipoMovimento() == TipoMovimento.Credito) {
                BigDecimal v = BigDecimal.valueOf(ei.getValor());
                String descricao = eventoFinanceiro.getDescricao() != null ? eventoFinanceiro.getDescricao() : "";
                if (descricao.contains("Pagamento da fatura")) {
                    totalCreditoUsado = totalCreditoUsado.subtract(v);
                } else if (eventoFinanceiro.getTipo() == Tipo.Gasto || eventoFinanceiro.getTipo() == Tipo.Transferencia) {
                    totalCreditoUsado = totalCreditoUsado.add(v);
                }
            }
        }

        // Para cartão de crédito: retorna crédito disponível (limite - crédito utilizado)
        BigDecimal limite = iu.getLimiteCredito();
        if (limite != null && limite.compareTo(BigDecimal.ZERO) > 0) {
            return limite.subtract(totalCreditoUsado).max(BigDecimal.ZERO);
        }

        return saldo;
    }

    /**
     * Retorna o saldo de débito (fluxo de caixa) da instituição, ignorando o limite de crédito.
     * Use este método para validar transações no débito e para exibição em contexto de débito.
     * O limite de crédito do cartão NÃO é considerado aqui.
     */
    public BigDecimal getSaldoDebitoByInstituicao(Integer instituicaoUsuarioId) {
        if (!instituicaoUsuarioRepository.existsById(instituicaoUsuarioId)) {
            throw new EntidadeNaoEncontradaException(
                    "Associação de instituição e usuário de id: %d não encontrada."
                            .formatted(instituicaoUsuarioId)
            );
        }
        List<EventoInstituicao> eventosInstituicao =
                eventoInstituicaoRepository.findByInstituicaoUsuario_Id(instituicaoUsuarioId);

        BigDecimal saldo = BigDecimal.ZERO;
        for (EventoInstituicao ei : eventosInstituicao) {
            EventoFinanceiro ef = ei.getEventoFinanceiro();
            if (ef == null) continue;
            saldo = getSaldoPorMovimento(saldo, ef, ei);
        }
        return saldo;
    }

    static BigDecimal getSaldo(BigDecimal saldo, EventoFinanceiro eventoFinanceiro) {
        BigDecimal valor = BigDecimal.valueOf(eventoFinanceiro.getValor());

        if (eventoFinanceiro.getTipo() == Tipo.Gasto || eventoFinanceiro.getTipo() == Tipo.Transferencia || eventoFinanceiro.getTipo() == Tipo.Poupanca) {
            saldo = saldo.subtract(valor);
        } else if (eventoFinanceiro.getTipo() == Tipo.Recebimento || eventoFinanceiro.getTipo() == Tipo.Emprestimo) {
            saldo = saldo.add(valor);
        }
        return saldo;
    }

    static BigDecimal getSaldoPorMovimento(BigDecimal saldo,
                                           EventoFinanceiro eventoFinanceiro,
                                           EventoInstituicao eventoInstituicao) {
        BigDecimal valor = (eventoInstituicao != null && eventoInstituicao.getValor() != null)
                ? BigDecimal.valueOf(eventoInstituicao.getValor())
                : BigDecimal.valueOf(eventoFinanceiro.getValor());

        TipoMovimento tipoMovimento = eventoInstituicao != null ? eventoInstituicao.getTipoMovimento() : null;
        String descricao = eventoFinanceiro.getDescricao() != null ? eventoFinanceiro.getDescricao() : "";

        // Pagamento de fatura: debita do saldo mesmo sendo Credito
        if (tipoMovimento == TipoMovimento.Credito && descricao.contains("Pagamento da fatura")) {
            return saldo.subtract(valor);
        }

        // Compra no cartão (Credito sem ser pagamento) não altera caixa
        if (tipoMovimento == TipoMovimento.Credito) {
            return saldo;
        }

        if (eventoFinanceiro.getTipo() == Tipo.Gasto || eventoFinanceiro.getTipo() == Tipo.Transferencia || eventoFinanceiro.getTipo() == Tipo.Poupanca) {
            return saldo.subtract(valor);
        }
        if (eventoFinanceiro.getTipo() == Tipo.Recebimento || eventoFinanceiro.getTipo() == Tipo.Emprestimo) {
            return saldo.add(valor);
        }
        return saldo;
    }

    public void detachAllIntituicoes(UUID userId) {
        usuarioService.getUsuario(userId);

        List<InstituicaoUsuario> instituicoesUsuarios = instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);

        for (InstituicaoUsuario instituicao : instituicoesUsuarios){
            instituicao.setIsAtivo(false);
            instituicaoUsuarioRepository.save(instituicao);
        }
    }

    // =========================================================================
    //  RESUMO DAS INSTITUIÇÕES DO USUÁRIO
    // =========================================================================
    public List<ResumoInstituicaoDto> getResumoInstituicoes(UUID userId) {
        return getResumoInstituicoes(userId, null, null);
    }

    public List<ResumoInstituicaoDto> getResumoInstituicoes(UUID userId, LocalDate dataInicio, LocalDate dataFim) {
        if (!usuarioRepository.existsById(userId)) {
            throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado".formatted(userId));
        }
        List<InstituicaoUsuario> instList = instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
        List<ResumoInstituicaoDto> resultado = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        for (InstituicaoUsuario iu : instList) {
            List<EventoInstituicao> eis = eventoInstituicaoRepository.findByInstituicaoUsuario_Id(iu.getId());

            int transacoes = 0;
            BigDecimal totalCredito = BigDecimal.ZERO;
            BigDecimal totalDebito = BigDecimal.ZERO;
            BigDecimal saldo = BigDecimal.ZERO;
            BigDecimal totalCreditoAcumulado = BigDecimal.ZERO;
            int parcelamentosAtivos = 0;

            for (EventoInstituicao ei : eis) {
                EventoFinanceiro ef = ei.getEventoFinanceiro();
                if (ef == null) continue;

                LocalDate dataEvento = ef.getDataEvento();
                if (dataEvento == null) continue;

                // Saldo acumulado: calcula TODOS os eventos até o final do período selecionado
                // Se dataFim é null, calcula all-time
                boolean dentroDoAcumulado = (dataFim == null) || (!dataEvento.isAfter(dataFim));

                if (dentroDoAcumulado) {
                    saldo = getSaldoPorMovimento(saldo, ef, ei);

                    // Rastreia crédito utilizado acumulado até dataFim
                    if (ei.getTipoMovimento() == TipoMovimento.Credito) {
                        BigDecimal vAt = BigDecimal.valueOf(ei.getValor());
                        String descAt = ef.getDescricao() != null ? ef.getDescricao() : "";
                        if (descAt.contains("Pagamento da fatura")) {
                            totalCreditoAcumulado = totalCreditoAcumulado.subtract(vAt);
                        } else if (ef.getTipo() == Tipo.Gasto || ef.getTipo() == Tipo.Transferencia) {
                            totalCreditoAcumulado = totalCreditoAcumulado.add(vAt);
                        }
                    }
                }

                // Parcelamentos ativos: sobreposição com período (se fornecido) ou ainda não vencido
                if (ei.getParcelas() != null && ei.getParcelas() > 1) {
                    LocalDate inicioParc = dataEvento;
                    LocalDate fimParc = inicioParc.plusMonths(ei.getParcelas());
                    if (dataInicio != null && dataFim != null) {
                        if (!fimParc.isBefore(dataInicio) && !inicioParc.isAfter(dataFim)) {
                            parcelamentosAtivos++;
                        }
                    } else {
                        if (!fimParc.isBefore(hoje)) parcelamentosAtivos++;
                    }
                }

                // Totais de crédito/débito e transações: apenas DENTRO do período selecionado
                boolean dentroDoPeriodo = (dataInicio == null || dataFim == null) ||
                                         (!dataEvento.isBefore(dataInicio) && !dataEvento.isAfter(dataFim));

                if (!dentroDoPeriodo) continue;

                if (ef.getTipo() == Tipo.Gasto || ef.getTipo() == Tipo.Transferencia) transacoes++;

                if (ei.getTipoMovimento() == TipoMovimento.Credito) {
                    BigDecimal v = BigDecimal.valueOf(ei.getValor());
                    String descricao = ef.getDescricao() != null ? ef.getDescricao() : "";

                    if (descricao.contains("Pagamento da fatura")) {
                        totalCredito = totalCredito.subtract(v);
                    } else if (ef.getTipo() == Tipo.Gasto || ef.getTipo() == Tipo.Transferencia) {
                        totalCredito = totalCredito.add(v);
                    }
                } else if (ei.getTipoMovimento() == TipoMovimento.Debito || ei.getTipoMovimento() == TipoMovimento.Pix) {
                    // Soma apenas gastos no débito/pix (não recebimentos)
                    if (ef.getTipo() == Tipo.Gasto || ef.getTipo() == Tipo.Transferencia) {
                        totalDebito = totalDebito.add(BigDecimal.valueOf(ei.getValor()));
                    }
                }
            }

            BigDecimal limite = iu.getLimiteCredito() != null ? iu.getLimiteCredito() : BigDecimal.ZERO;
            int pctCredito = limite.compareTo(BigDecimal.ZERO) > 0
                    ? totalCreditoAcumulado.divide(limite, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue()
                    : 0;

            // Saldo disponível: sempre mostra o fluxo de caixa (débito/pix)
            // O limite de crédito é mostrado separadamente na UI
            BigDecimal saldoDisponivel = saldo;

            boolean temCredito = calcularTemCredito(iu.getInstituicao().getNome());

            resultado.add(new ResumoInstituicaoDto(
                    iu.getId(),
                    iu.getInstituicao().getNome(),
                    transacoes,
                    saldoDisponivel,
                    totalCredito,
                    totalDebito,
                    limite,
                    Math.min(pctCredito, 100),
                    parcelamentosAtivos,
                    iu.getTaxaJuros(),
                    temCredito
            ));
        }
        return resultado;
    }

    /** Retorna false para instituições de benefício/alimentação que não possuem limite de crédito rotativo. */
    private boolean calcularTemCredito(String nomeInstituicao) {
        if (nomeInstituicao == null) return true;
        String nome = nomeInstituicao.toLowerCase()
                .replace("ã", "a").replace("á", "a").replace("â", "a")
                .replace("é", "e").replace("ê", "e").replace("í", "i")
                .replace("ó", "o").replace("ô", "o").replace("ú", "u")
                .replace("ç", "c");
        return !(nome.contains("alelo") || nome.contains("aelo") ||
                nome.contains("vale") || nome.contains("ticket") ||
                nome.contains("pluxee") || nome.contains("sodexo") ||
                nome.contains("multibene") || nome.contains("beneficio") ||
                nome.contains("beneficios"));
    }

    // =========================================================================
    //  DETALHE DA INSTITUIÇÃO COM DISTRIBUIÇÃO POR MOVIMENTO
    // =========================================================================
    public DetalheInstituicaoDto getDetalheInstituicao(Integer instUsuarioId) {
        return getDetalheInstituicao(instUsuarioId, null, null);
    }

    public DetalheInstituicaoDto getDetalheInstituicao(Integer instUsuarioId, LocalDate dataInicio, LocalDate dataFim) {
        InstituicaoUsuario iu = instituicaoUsuarioRepository.findById(instUsuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("InstituicaoUsuario de id: %d não encontrada.".formatted(instUsuarioId)));

        List<EventoInstituicao> eis = eventoInstituicaoRepository.findByInstituicaoUsuario_Id(instUsuarioId);
        Map<String, BigDecimal> porMovimento = new LinkedHashMap<>();
        for (TipoMovimento tm : TipoMovimento.values()) {
            // Pular Pix pois será agrupado com Débito
            if (tm != TipoMovimento.Pix) {
                porMovimento.put(tm.name(), BigDecimal.ZERO);
            }
        }

        for (EventoInstituicao ei : eis) {
            if (ei.getTipoMovimento() != null) {
                EventoFinanceiro ef = ei.getEventoFinanceiro();
                if (ef == null) continue;

                // Filtrar por período se fornecido
                if (dataInicio != null && dataFim != null) {
                    LocalDate dataEvento = ef.getDataEvento();
                    if (dataEvento == null || dataEvento.isBefore(dataInicio) || dataEvento.isAfter(dataFim)) {
                        continue;
                    }
                }

                String descricao = ef.getDescricao() != null ? ef.getDescricao() : "";

                // Não incluir pagamento de fatura na distribuição (ele já quita o crédito usado)
                if (descricao.contains("Pagamento da fatura")) continue;

                // Somar apenas gastos/transferências (não recebimentos)
                if (ef.getTipo() != Tipo.Gasto && ef.getTipo() != Tipo.Transferencia) continue;

                // Agrupar Pix com Débito
                String chave = ei.getTipoMovimento() == TipoMovimento.Pix
                    ? TipoMovimento.Debito.name()
                    : ei.getTipoMovimento().name();
                porMovimento.merge(chave, BigDecimal.valueOf(ei.getValor()), BigDecimal::add);
            }
        }

        List<DetalheInstituicaoDto.DistribuicaoMovimentoDto> distribuicao = porMovimento.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(e -> new DetalheInstituicaoDto.DistribuicaoMovimentoDto(e.getKey(), e.getValue()))
                .toList();

        return new DetalheInstituicaoDto(
                iu.getId(),
                iu.getInstituicao().getNome(),
                iu.getLimiteCredito() != null ? iu.getLimiteCredito() : BigDecimal.ZERO,
                iu.getTaxaJuros(),
                distribuicao
        );
    }

    // =========================================================================
    //  ATUALIZAR LIMITE DE CRÉDITO E TAXA DE JUROS DA INSTITUIÇÃO
    // =========================================================================
    public InstituicaoUsuario atualizarInstituicaoUsuario(Integer instUsuarioId, AtualizarInstituicaoUsuarioDto dto) {
        InstituicaoUsuario iu = instituicaoUsuarioRepository.findById(instUsuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("InstituicaoUsuario de id: %d não encontrada.".formatted(instUsuarioId)));
        if (dto.getLimiteCredito() != null) iu.setLimiteCredito(dto.getLimiteCredito());
        if (dto.getTaxaJuros() != null) iu.setTaxaJuros(dto.getTaxaJuros());
        iu.setUltimaModificacao(LocalDateTime.now());
        return instituicaoUsuarioRepository.save(iu);
    }

    // =========================================================================
    //  PAGAMENTO DE FATURA (Cartão de Crédito)
    // =========================================================================
    /**
     * Registra o pagamento de fatura do cartão de crédito.
     * Cria um EventoFinanceiro de Recebimento vinculado à instituição,
     * com tipoMovimento=Debito (pagamento saindo da conta corrente/dinheiro).
     *
     * @param instUsuarioId  instituição com crédito a ser paga
     * @param valorPagamento valor a ser pago (pode ser total ou parcial)
     * @return EventoInstituicao do pagamento registrado
     */
    public EventoInstituicao pagarFatura(Integer instUsuarioId, BigDecimal valorPagamento) {
        InstituicaoUsuario iu = instituicaoUsuarioRepository.findById(instUsuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "InstituicaoUsuario de id: %d não encontrada.".formatted(instUsuarioId)));

        if (valorPagamento == null || valorPagamento.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do pagamento deve ser maior que zero.");
        }

        // Verifica se o usuário tem saldo suficiente para pagar a fatura
        BigDecimal saldoTotal = usuarioService.getSaldoByUsuario(iu.getUsuario().getId());

        if (saldoTotal.compareTo(valorPagamento) < 0) {
            throw new controle.api.back_end.exception.SaldoInsuficienteException(
                    "Saldo insuficiente para pagar a fatura.");
        }

        // Cria o EventoFinanceiro de Gasto (pagamento saindo do saldo)
        EventoFinanceiro pagamento = new EventoFinanceiro();
        pagamento.setUsuario(iu.getUsuario());
        pagamento.setTipo(Tipo.Gasto);
        pagamento.setValor(valorPagamento.doubleValue());
        pagamento.setDescricao("Pagamento da fatura " + iu.getInstituicao().getNome());
        pagamento.setDataEvento(java.time.LocalDate.now());
        pagamento.setDataRegistro(java.time.LocalDateTime.now());
        EventoFinanceiro eventoSalvo = eventoFinanceiroRepository.save(pagamento);

        // Cria EventoDetalhe para que apareça nos registros
        EventoDetalhe detalhe = new EventoDetalhe();
        detalhe.setEventoFinanceiro(eventoSalvo);
        detalhe.setTituloGasto("Pagamento da fatura " + iu.getInstituicao().getNome());
        detalhe.setCategoriaUsuario(new java.util.ArrayList<>());
        eventoDetalheRepository.save(detalhe);

        // Vincula o evento apenas com Credito (quita o crédito usado)
        // O débito do saldo é calculado automaticamente pelo getSaldoPorMovimento
        EventoInstituicao ei = new EventoInstituicao();
        ei.setEventoFinanceiro(eventoSalvo);
        ei.setInstituicaoUsuario(iu);
        ei.setTipoMovimento(TipoMovimento.Credito);
        ei.setValor(valorPagamento.doubleValue());
        ei.setParcelas(1);
        return eventoInstituicaoRepository.save(ei);
    }
}
