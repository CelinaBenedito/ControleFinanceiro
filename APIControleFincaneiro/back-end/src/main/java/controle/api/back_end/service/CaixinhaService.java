package controle.api.back_end.service;

import controle.api.back_end.dto.poupanca.in.CaixinhaCreateDTO;
import controle.api.back_end.dto.poupanca.out.CaixinhaResponseDTO;
import controle.api.back_end.dto.poupanca.out.GraficoProgressoConsolidadoCaixinhaDto;
import controle.api.back_end.dto.poupanca.out.KpiProgressoGeralCaixinhaDto;
import controle.api.back_end.dto.poupanca.out.KpiRendimentoEstimadoMesCaixinhaDto;
import controle.api.back_end.dto.poupanca.out.KpiStatusCaixinhasDto;
import controle.api.back_end.dto.poupanca.out.KpiTotalAcumuladoCaixinhaDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.poupanca.Caixinha;
import controle.api.back_end.model.poupanca.CaixinhaInstituicao;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.poupanca.CaixinhaInstituicaoRepository;
import controle.api.back_end.repository.poupanca.CaixinhaRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import controle.api.back_end.model.configuracoes.TipoAlertaEmail;
import controle.api.back_end.repository.configuracoes.ConfiguracoesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pelo ciclo de vida das caixinhas de poupança e
 * todos os cálculos financeiros associados.
 *
 * <h3>Cálculos implementados</h3>
 * <ul>
 *   <li><b>Taxa mensal efetiva</b>: convertida da taxa anual para composto mensal.</li>
 *   <li><b>Montante projetado sem aportes</b>: VF = VP × (1 + r)^n</li>
 *   <li><b>Aporte mensal sugerido (PMT)</b>:
 *       PMT = [VF − VP×(1+r)^n] × r / [(1+r)^n − 1]
 *       <br>Se r = 0: PMT = (VF − VP) / n</li>
 *   <li><b>Montante projetado com aportes</b>:
 *       VF = VP×(1+r)^n + PMT × [(1+r)^n − 1] / r</li>
 * </ul>
 */
@Service
@Transactional
public class CaixinhaService {

    private static final Logger log = LoggerFactory.getLogger(CaixinhaService.class);

    private final CaixinhaRepository caixinhaRepository;
    private final CaixinhaInstituicaoRepository caixinhaInstituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final ConfiguracoesRepository configuracoesRepository;
    private final EmailService emailService;

    public CaixinhaService(CaixinhaRepository caixinhaRepository,
                           CaixinhaInstituicaoRepository caixinhaInstituicaoRepository,
                           UsuarioRepository usuarioRepository,
                           InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                           EventoFinanceiroRepository eventoFinanceiroRepository,
                           ConfiguracoesRepository configuracoesRepository,
                           EmailService emailService) {
        this.caixinhaRepository              = caixinhaRepository;
        this.caixinhaInstituicaoRepository   = caixinhaInstituicaoRepository;
        this.usuarioRepository               = usuarioRepository;
        this.instituicaoUsuarioRepository    = instituicaoUsuarioRepository;
        this.eventoFinanceiroRepository      = eventoFinanceiroRepository;
        this.configuracoesRepository         = configuracoesRepository;
        this.emailService                    = emailService;
    }

    // =========================================================================
    // CRIAR
    // =========================================================================

    /** Cria uma nova caixinha e vincula as instituições informadas. */
    public CaixinhaResponseDTO criar(CaixinhaCreateDTO dto) {
        usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Usuário de id: %s não encontrado.".formatted(dto.getUsuarioId())));

        Caixinha caixinha = new Caixinha();
        caixinha.setUsuario(usuarioRepository.getReferenceById(dto.getUsuarioId()));
        caixinha.setNome(dto.getNome());
        caixinha.setDescricao(dto.getDescricao());
        caixinha.setValorMeta(dto.getValorMeta());
        caixinha.setDataPrazo(dto.getDataPrazo());
        caixinha.setTipoRendimento(dto.getTipoRendimento());
        caixinha.setPercentualRendimento(dto.getPercentualRendimento());
        caixinha.setTaxaAnualPersonalizada(dto.getTaxaAnualPersonalizada());
        caixinha.setTaxaReferenciaAtual(dto.getTaxaReferenciaAtual());
        caixinha.setIsCompartilhada(dto.getIsCompartilhada() != null && dto.getIsCompartilhada());
        caixinha.setIsAtiva(true);
        caixinha.setDataCriacao(LocalDate.now());

        Caixinha salva = caixinhaRepository.save(caixinha);

        // Vincula as instituicoes
        for (Integer instId : dto.getInstituicaoUsuarioIds()) {
            InstituicaoUsuario inst = instituicaoUsuarioRepository.findById(instId)
                    .orElseThrow(() -> new EntidadeNaoEncontradaException(
                            "InstituicaoUsuario de id: %d nao encontrada.".formatted(instId)));
            CaixinhaInstituicao vinculo = new CaixinhaInstituicao();
            vinculo.setCaixinha(salva);
            vinculo.setInstituicaoUsuario(inst);
            caixinhaInstituicaoRepository.save(vinculo);
        }

        CaixinhaResponseDTO resultado = calcularEMontar(salva);
        verificarEEnviarAlertaMeta(salva, resultado);
        return resultado;
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    /** Retorna todas as caixinhas (ativas e inativas) do usuário com cálculos. */
    @Transactional(readOnly = true)
    public List<CaixinhaResponseDTO> listarPorUsuario(UUID usuarioId) {
        validarUsuario(usuarioId);
        return caixinhaRepository.findAllByUsuario_Id(usuarioId).stream()
                .map(this::calcularEMontar)
                .toList();
    }

    /** Retorna apenas as caixinhas ativas do usuário. */
    @Transactional(readOnly = true)
    public List<CaixinhaResponseDTO> listarAtivasPorUsuario(UUID usuarioId) {
        validarUsuario(usuarioId);
        return caixinhaRepository.findAllByUsuario_IdAndIsAtivaTrue(usuarioId).stream()
                .map(this::calcularEMontar)
                .toList();
    }

    /** Retorna uma caixinha específica com todos os cálculos. */
    @Transactional(readOnly = true)
    public CaixinhaResponseDTO buscarPorId(UUID caixinhaId) {
        Caixinha caixinha = caixinhaRepository.findById(caixinhaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Caixinha de id: %s não encontrada.".formatted(caixinhaId)));
        return calcularEMontar(caixinha);
    }

    /** Resumo total de poupança do usuário (soma de todas as caixinhas ativas). */
    @Transactional(readOnly = true)
    public BigDecimal resumoTotalPoupanca(UUID usuarioId) {
        validarUsuario(usuarioId);
        return caixinhaRepository.findAllByUsuario_IdAndIsAtivaTrue(usuarioId).stream()
                .map(c -> eventoFinanceiroRepository.sumValorByCaixinha(c.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public KpiTotalAcumuladoCaixinhaDto obterKpiTotalAcumulado(UUID usuarioId) {
        List<Caixinha> caixinhasAtivas = buscarCaixinhasAtivasPorUsuario(usuarioId);
        BigDecimal totalAcumulado = resumoTotalPoupanca(usuarioId);
        return new KpiTotalAcumuladoCaixinhaDto(totalAcumulado, caixinhasAtivas.size());
    }

    @Transactional(readOnly = true)
    public KpiProgressoGeralCaixinhaDto obterKpiProgressoGeral(UUID usuarioId) {
        List<Caixinha> caixinhasAtivas = buscarCaixinhasAtivasPorUsuario(usuarioId);
        BigDecimal totalAcumulado = resumoTotalPoupanca(usuarioId);
        BigDecimal totalMetas = somarMetasDasCaixinhas(caixinhasAtivas);

        int percentualProgressoGeral = 0;
        if (totalMetas.compareTo(BigDecimal.ZERO) > 0) {
            percentualProgressoGeral = totalAcumulado
                    .divide(totalMetas, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            percentualProgressoGeral = Math.min(percentualProgressoGeral, 100);
        }

        return new KpiProgressoGeralCaixinhaDto(totalAcumulado, totalMetas, percentualProgressoGeral);
    }

    @Transactional(readOnly = true)
    public KpiRendimentoEstimadoMesCaixinhaDto obterKpiRendimentoEstimadoMes(UUID usuarioId) {
        List<Caixinha> caixinhasAtivas = buscarCaixinhasAtivasPorUsuario(usuarioId);

        BigDecimal rendimentoEstimadoMes = caixinhasAtivas.stream()
                .map(caixinha -> {
                    BigDecimal valorAtual = eventoFinanceiroRepository.sumValorByCaixinha(caixinha.getId());
                    BigDecimal taxaMensalEfetiva = BigDecimal.valueOf(calcularTaxaMensal(caixinha));
                    return valorAtual.multiply(taxaMensalEfetiva);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        String mesReferencia = YearMonth.now().format(DateTimeFormatter.ofPattern("MM/yyyy"));
        return new KpiRendimentoEstimadoMesCaixinhaDto(rendimentoEstimadoMes, mesReferencia);
    }

    @Transactional(readOnly = true)
    public KpiStatusCaixinhasDto obterKpiStatusCaixinhas(UUID usuarioId) {
        validarUsuario(usuarioId);
        List<Caixinha> todasCaixinhas = caixinhaRepository.findAllByUsuario_Id(usuarioId);

        int quantidadeAtivas = (int) todasCaixinhas.stream()
                .filter(caixinha -> Boolean.TRUE.equals(caixinha.getIsAtiva()))
                .count();
        int quantidadeEncerradas = (int) todasCaixinhas.stream()
                .filter(caixinha -> !Boolean.TRUE.equals(caixinha.getIsAtiva()))
                .count();

        return new KpiStatusCaixinhasDto(quantidadeAtivas, quantidadeEncerradas);
    }

    @Transactional(readOnly = true)
    public GraficoProgressoConsolidadoCaixinhaDto obterGraficoProgressoConsolidado(UUID usuarioId) {
        KpiProgressoGeralCaixinhaDto kpiProgresso = obterKpiProgressoGeral(usuarioId);
        return new GraficoProgressoConsolidadoCaixinhaDto(
                kpiProgresso.percentualProgressoGeral(),
                kpiProgresso.totalAcumulado(),
                kpiProgresso.totalMetas());
    }

    // =========================================================================
    // EDITAR
    // =========================================================================

    /** Edita os dados configuráveis de uma caixinha (não afeta aportes existentes). */
    public CaixinhaResponseDTO editar(UUID caixinhaId, CaixinhaCreateDTO dto) {
        Caixinha caixinha = caixinhaRepository.findById(caixinhaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Caixinha de id: %s não encontrada.".formatted(caixinhaId)));

        if (dto.getNome() != null) caixinha.setNome(dto.getNome());
        if (dto.getDescricao() != null) caixinha.setDescricao(dto.getDescricao());
        if (dto.getValorMeta() != null) caixinha.setValorMeta(dto.getValorMeta());
        if (dto.getDataPrazo() != null) caixinha.setDataPrazo(dto.getDataPrazo());
        if (dto.getTipoRendimento() != null) caixinha.setTipoRendimento(dto.getTipoRendimento());
        if (dto.getPercentualRendimento() != null) caixinha.setPercentualRendimento(dto.getPercentualRendimento());
        if (dto.getTaxaAnualPersonalizada() != null) caixinha.setTaxaAnualPersonalizada(dto.getTaxaAnualPersonalizada());
        if (dto.getTaxaReferenciaAtual() != null) caixinha.setTaxaReferenciaAtual(dto.getTaxaReferenciaAtual());

        return calcularEMontar(caixinhaRepository.save(caixinha));
    }

    /** Encerra uma caixinha (marca como inativa). */
    public CaixinhaResponseDTO encerrar(UUID caixinhaId) {
        Caixinha caixinha = caixinhaRepository.findById(caixinhaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Caixinha de id: %s não encontrada.".formatted(caixinhaId)));
        caixinha.setIsAtiva(false);
        caixinha.setDataEncerramento(LocalDate.now());
        return calcularEMontar(caixinhaRepository.save(caixinha));
    }

    /** Vincula uma nova instituição a uma caixinha compartilhada. */
    public CaixinhaResponseDTO adicionarInstituicao(UUID caixinhaId, Integer instId) {
        Caixinha caixinha = caixinhaRepository.findById(caixinhaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Caixinha de id: %s não encontrada.".formatted(caixinhaId)));
        if (caixinhaInstituicaoRepository.existsByCaixinha_IdAndInstituicaoUsuario_Id(caixinhaId, instId)) {
            throw new IllegalArgumentException("Instituição já vinculada a esta caixinha.");
        }
        InstituicaoUsuario inst = instituicaoUsuarioRepository.findById(instId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "InstituicaoUsuario de id: %d não encontrada.".formatted(instId)));
        CaixinhaInstituicao vinculo = new CaixinhaInstituicao();
        vinculo.setCaixinha(caixinha);
        vinculo.setInstituicaoUsuario(inst);
        caixinhaInstituicaoRepository.save(vinculo);
        caixinha.setIsCompartilhada(true);
        return calcularEMontar(caixinhaRepository.save(caixinha));
    }

    // =========================================================================
    // DELETAR
    // =========================================================================

    public void deletar(UUID caixinhaId) {
        if (!caixinhaRepository.existsById(caixinhaId)) {
            throw new EntidadeNaoEncontradaException(
                    "Caixinha de id: %s não encontrada.".formatted(caixinhaId));
        }
        caixinhaRepository.deleteById(caixinhaId);
    }

    // =========================================================================
    // CÁLCULOS FINANCEIROS (acesso package para uso no EmailService)
    // =========================================================================

    /**
     * Monta o DTO de resposta completo com todos os cálculos aplicados.
     */
    CaixinhaResponseDTO calcularEMontar(Caixinha c) {
        CaixinhaResponseDTO dto = new CaixinhaResponseDTO();

        // Dados básicos
        dto.setId(c.getId());
        dto.setNome(c.getNome());
        dto.setDescricao(c.getDescricao());
        dto.setIsAtiva(c.getIsAtiva());
        dto.setIsCompartilhada(c.getIsCompartilhada());
        dto.setDataCriacao(c.getDataCriacao());
        dto.setDataEncerramento(c.getDataEncerramento());
        dto.setValorMeta(c.getValorMeta());
        dto.setDataPrazo(c.getDataPrazo());
        dto.setTipoRendimento(c.getTipoRendimento());
        dto.setPercentualRendimento(c.getPercentualRendimento());
        dto.setTaxaAnualPersonalizada(c.getTaxaAnualPersonalizada());
        dto.setTaxaReferenciaAtual(c.getTaxaReferenciaAtual());

        // Valor atual = soma dos aportes registrados
        BigDecimal valorAtual = eventoFinanceiroRepository.sumValorByCaixinha(c.getId());
        dto.setValorAtual(valorAtual);

        // Progresso em relação à meta
        if (c.getValorMeta() != null && c.getValorMeta().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal falta = c.getValorMeta().subtract(valorAtual).max(BigDecimal.ZERO);
            dto.setFaltaParaMeta(falta);
            double pct = valorAtual.divide(c.getValorMeta(), 6, RoundingMode.HALF_UP)
                                   .multiply(BigDecimal.valueOf(100))
                                   .doubleValue();
            dto.setPercentualAtingido(Math.min(pct, 100.0));
        }

        // Taxa mensal efetiva
        double taxaMensal = calcularTaxaMensal(c);
        dto.setTaxaMensalEfetiva(taxaMensal);

        // Projeções com prazo
        if (c.getDataPrazo() != null && c.getDataPrazo().isAfter(LocalDate.now())) {
            int meses = (int) Period.between(LocalDate.now(), c.getDataPrazo()).toTotalMonths();
            dto.setMesesRestantes(meses);

            if (meses > 0) {
                double vp = valorAtual.doubleValue();
                double fatorN = Math.pow(1 + taxaMensal, meses);

                // Montante sem novos aportes: VP × (1+r)^n
                double montanteSemAportes = vp * fatorN;
                dto.setMontanteProjetadoSemAportes(
                        BigDecimal.valueOf(montanteSemAportes).setScale(2, RoundingMode.HALF_UP));

                // Aporte mensal sugerido (PMT)
                if (c.getValorMeta() != null) {
                    double vf = c.getValorMeta().doubleValue();
                    double pmt;
                    if (taxaMensal < 1e-10) {
                        // Taxa zero: divisão simples
                        pmt = Math.max(0, (vf - vp)) / meses;
                    } else {
                        pmt = Math.max(0, (vf - vp * fatorN) * taxaMensal / (fatorN - 1));
                    }
                    dto.setAporteMensalSugerido(
                            BigDecimal.valueOf(pmt).setScale(2, RoundingMode.HALF_UP));

                    // Montante com aportes: VP×(1+r)^n + PMT×[(1+r)^n − 1]/r
                    double montanteComAportes;
                    if (taxaMensal < 1e-10) {
                        montanteComAportes = vp + pmt * meses;
                    } else {
                        montanteComAportes = vp * fatorN + pmt * (fatorN - 1) / taxaMensal;
                    }
                    dto.setMontanteProjetadoComAportes(
                            BigDecimal.valueOf(montanteComAportes).setScale(2, RoundingMode.HALF_UP));

                    dto.setMetaAlcancavel(montanteComAportes >= vf - 0.01);
                }
            }
        }

        // Instituições com valor aportado por cada uma
        List<CaixinhaResponseDTO.InstituicaoDTO> instDtos = new ArrayList<>();
        List<CaixinhaInstituicao> vinculos = caixinhaInstituicaoRepository.findAllByCaixinha_Id(c.getId());
        for (CaixinhaInstituicao vi : vinculos) {
            List<EventoFinanceiro> eventosDaInst = eventoFinanceiroRepository
                    .findAllByCaixinha_Id(c.getId()).stream()
                    .filter(e -> e.getEventoInstituicoes() != null &&
                                 e.getEventoInstituicoes().stream()
                                   .anyMatch(ei -> ei.getInstituicaoUsuario().getId()
                                                     .equals(vi.getInstituicaoUsuario().getId())))
                    .toList();
            BigDecimal aportadoNaInst = eventosDaInst.stream()
                    .map(e -> BigDecimal.valueOf(e.getValor()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            CaixinhaResponseDTO.InstituicaoDTO iDto = new CaixinhaResponseDTO.InstituicaoDTO();
            iDto.setId(vi.getInstituicaoUsuario().getId());
            iDto.setNome(vi.getInstituicaoUsuario().getInstituicao().getNome());
            iDto.setValorAportado(aportadoNaInst);
            instDtos.add(iDto);
        }
        dto.setInstituicoes(instDtos);

        return dto;
    }

    /**
     * Calcula a taxa mensal efetiva a partir da configuração da caixinha.
     *
     * <ul>
     *   <li>CDI: taxaReferenciaAtual × (percentualRendimento / 100) / 12 (simplificado)</li>
     *   <li>SELIC: igual ao CDI</li>
     *   <li>POUPANÇA: 70% da SELIC + 0% TR simplificado</li>
     *   <li>PREFIXADO / PERSONALIZADO: taxa anual → mensal composto</li>
     * </ul>
     */
    private double calcularTaxaMensal(Caixinha c) {
        if (c.getTipoRendimento() == null) {
            return 0.0;
        }

        double taxaAnualPercent;

        switch (c.getTipoRendimento()) {
            case CDI, SELIC -> {
                if (c.getTaxaReferenciaAtual() == null) {
                    taxaAnualPercent = 0.0;
                    break;
                }
                double referencia = c.getTaxaReferenciaAtual(); // % a.a.
                double percentual = c.getPercentualRendimento() != null
                        ? c.getPercentualRendimento() / 100.0 : 1.0;
                taxaAnualPercent = referencia * percentual;
            }
            case POUPANCA -> {
                if (c.getTaxaReferenciaAtual() == null) {
                    taxaAnualPercent = 0.0;
                    break;
                }
                // Regra atual da poupança: 70% da SELIC quando SELIC > 8,5% a.a.
                taxaAnualPercent = c.getTaxaReferenciaAtual() * 0.70;
            }
            case PREFIXADO, PERSONALIZADO -> {
                taxaAnualPercent = c.getTaxaAnualPersonalizada() != null
                        ? c.getTaxaAnualPersonalizada() : 0.0;
            }
            default -> taxaAnualPercent = 0.0;
        }

        // Converte % a.a. para decimal mensal via juros compostos: (1 + i_a)^(1/12) - 1
        return Math.pow(1 + taxaAnualPercent / 100.0, 1.0 / 12.0) - 1;
    }

    // =========================================================================
    // PRIVADO
    // =========================================================================

    // =========================================================================
    // PRIVADO
    // =========================================================================

    /**
     * Verifica se a caixinha atingiu o percentual de meta configurado pelo usuario
     * e envia o alerta de e-mail (uma vez por mes).
     */
    private void verificarEEnviarAlertaMeta(Caixinha caixinha, CaixinhaResponseDTO dto) {
        try {
            if (dto.getPercentualAtingido() == null || caixinha.getValorMeta() == null) return;

            configuracoesRepository.findConfiguracoesByUsuario_Id(caixinha.getUsuario().getId())
                    .ifPresent(config -> {
                        if (!config.getAlertasEmailAtivos().contains(TipoAlertaEmail.ALERTA_META_POUPANCA)) return;

                        int threshold = config.getPercentualAlertaMeta() != null
                                ? config.getPercentualAlertaMeta() : 90;

                        if (dto.getPercentualAtingido() < threshold) return;

                        // Ja enviou este mes?
                        LocalDate hoje = java.time.LocalDate.now();
                        if (config.getUltimoAlertaMetaEnviado() != null
                                && config.getUltimoAlertaMetaEnviado().getMonth() == hoje.getMonth()
                                && config.getUltimoAlertaMetaEnviado().getYear() == hoje.getYear()) {
                            return;
                        }

                        emailService.enviarAlertaMetaPoupanca(
                                caixinha.getUsuario(),
                                caixinha.getNome(),
                                dto.getValorAtual(),
                                caixinha.getValorMeta(),
                                dto.getPercentualAtingido());

                        config.setUltimoAlertaMetaEnviado(hoje);
                        configuracoesRepository.save(config);
                    });
        } catch (Exception e) {
            log.warn("[CaixinhaService] Erro ao verificar alerta de meta: {}", e.getMessage());
        }
    }

    private void validarUsuario(UUID usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new EntidadeNaoEncontradaException(
                    "Usuário de id: %s não encontrado.".formatted(usuarioId));
        }
    }

    private List<Caixinha> buscarCaixinhasAtivasPorUsuario(UUID usuarioId) {
        validarUsuario(usuarioId);
        return caixinhaRepository.findAllByUsuario_IdAndIsAtivaTrue(usuarioId);
    }

    private BigDecimal somarMetasDasCaixinhas(List<Caixinha> caixinhas) {
        return caixinhas.stream()
                .map(Caixinha::getValorMeta)
                .filter(valorMeta -> valorMeta != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}






