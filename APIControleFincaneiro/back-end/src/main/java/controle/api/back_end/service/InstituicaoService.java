package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.dto.instituicao.in.AtualizarInstituicaoUsuarioDto;
import controle.api.back_end.dto.instituicao.out.DetalheInstituicaoDto;
import controle.api.back_end.dto.instituicao.out.ResumoInstituicaoDto;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
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
    private final UsuarioService usuarioService;


    public InstituicaoService(InstituicaoRepository instituicaoRepository,
                              UsuarioRepository usuarioRepository,
                              InstituicaoUsuarioRepository instituicaoUsuarioRepository, EventoInstituicaoRepository eventoInstituicaoRepository, EventoFinanceiroRepository eventoFinanceiroRepository, UsuarioService usuarioService) {
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
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
            int parcelamentosAtivos = 0;

            for (EventoInstituicao ei : eis) {
                EventoFinanceiro ef = ei.getEventoFinanceiro();
                if (ef == null) continue;

                // Saldo acumulado é sempre all-time (saldo atual da conta)
                saldo = getSaldo(saldo, ef);

                // Parcelamentos ativos: sobreposição com período (se fornecido) ou ainda não vencido
                if (ei.getParcelas() != null && ei.getParcelas() > 1) {
                    LocalDate inicioParc = ef.getDataEvento();
                    LocalDate fimParc = inicioParc.plusMonths(ei.getParcelas());
                    if (dataInicio != null && dataFim != null) {
                        if (!fimParc.isBefore(dataInicio) && !inicioParc.isAfter(dataFim)) {
                            parcelamentosAtivos++;
                        }
                    } else {
                        if (!fimParc.isBefore(hoje)) parcelamentosAtivos++;
                    }
                }

                // Totais de crédito/débito e transações: filtrados pelo período
                if (dataInicio != null && dataFim != null) {
                    LocalDate dataEvento = ef.getDataEvento();
                    if (dataEvento == null || dataEvento.isBefore(dataInicio) || dataEvento.isAfter(dataFim)) continue;
                }

                if (ef.getTipo() == Tipo.Gasto || ef.getTipo() == Tipo.Transferencia) transacoes++;

                if (ei.getTipoMovimento() == TipoMovimento.Credito)
                    totalCredito = totalCredito.add(BigDecimal.valueOf(ei.getValor()));
                else if (ei.getTipoMovimento() == TipoMovimento.Debito)
                    totalDebito = totalDebito.add(BigDecimal.valueOf(ei.getValor()));
            }

            BigDecimal limite = iu.getLimiteCredito() != null ? iu.getLimiteCredito() : BigDecimal.ZERO;
            int pctCredito = limite.compareTo(BigDecimal.ZERO) > 0
                    ? totalCredito.divide(limite, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue()
                    : 0;

            boolean temCredito = calcularTemCredito(iu.getInstituicao().getNome());

            resultado.add(new ResumoInstituicaoDto(
                    iu.getId(),
                    iu.getInstituicao().getNome(),
                    transacoes,
                    saldo,
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
        InstituicaoUsuario iu = instituicaoUsuarioRepository.findById(instUsuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("InstituicaoUsuario de id: %d não encontrada.".formatted(instUsuarioId)));

        List<EventoInstituicao> eis = eventoInstituicaoRepository.findByInstituicaoUsuario_Id(instUsuarioId);
        Map<String, BigDecimal> porMovimento = new LinkedHashMap<>();
        for (TipoMovimento tm : TipoMovimento.values()) porMovimento.put(tm.name(), BigDecimal.ZERO);

        for (EventoInstituicao ei : eis) {
            if (ei.getTipoMovimento() != null) {
                porMovimento.merge(ei.getTipoMovimento().name(), BigDecimal.valueOf(ei.getValor()), BigDecimal::add);
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
}
