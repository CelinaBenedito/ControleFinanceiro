package controle.api.back_end.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.dto.upload.ImportResultDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.emprestimo.Emprestimo;
import controle.api.back_end.model.emprestimo.EmprestimoBancario;
import controle.api.back_end.model.emprestimo.ModalidadeEmprestimoBancario;
import controle.api.back_end.model.emprestimo.StatusEmprestimo;
import controle.api.back_end.model.emprestimo.StatusEmprestimoBancario;
import controle.api.back_end.model.emprestimo.TipoEmprestimo;
import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.Periodicidade;
import controle.api.back_end.model.eventoFinanceiro.recorrenciaFinanceira.RecorrenciaFinanceira;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.poupanca.Caixinha;
import controle.api.back_end.model.poupanca.CaixinhaInstituicao;
import controle.api.back_end.model.poupanca.TipoRendimento;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.EmprestimoBancarioRepository;
import controle.api.back_end.repository.EmprestimoRepository;
import controle.api.back_end.repository.categoria.CategoriaRepository;
import controle.api.back_end.repository.categoria.CategoriaUsuarioRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.eventoFinanceiro.RecorrenciaFinanceiraRepository;
import controle.api.back_end.repository.instituicao.InstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.poupanca.CaixinhaInstituicaoRepository;
import controle.api.back_end.repository.poupanca.CaixinhaRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import controle.api.back_end.strategy.eventoFinanceiro.Registro;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UploadService {

    private final RegistroService registroService;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final RecorrenciaFinanceiraRepository recorrenciaFinanceiraRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EmprestimoRepository emprestimoRepository;
    private final EmprestimoBancarioRepository emprestimoBancarioRepository;
    private final CaixinhaRepository caixinhaRepository;
    private final CaixinhaInstituicaoRepository caixinhaInstituicaoRepository;

    // Portuguese month name → month number
    private static final Map<String, Integer> MESES_PT = new HashMap<>();

    static {
        MESES_PT.put("janeiro", 1);
        MESES_PT.put("fevereiro", 2);
        MESES_PT.put("março", 3);
        MESES_PT.put("marco", 3);
        MESES_PT.put("abril", 4);
        MESES_PT.put("maio", 5);
        MESES_PT.put("junho", 6);
        MESES_PT.put("julho", 7);
        MESES_PT.put("agosto", 8);
        MESES_PT.put("setembro", 9);
        MESES_PT.put("outubro", 10);
        MESES_PT.put("novembro", 11);
        MESES_PT.put("dezembro", 12);
    }

    public UploadService(RegistroService registroService,
                         UsuarioRepository usuarioRepository,
                         InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                         CategoriaUsuarioRepository categoriaUsuarioRepository,
                         InstituicaoRepository instituicaoRepository,
                         CategoriaRepository categoriaRepository,
                         RecorrenciaFinanceiraRepository recorrenciaFinanceiraRepository,
                         EventoInstituicaoRepository eventoInstituicaoRepository,
                         EmprestimoRepository emprestimoRepository,
                         EmprestimoBancarioRepository emprestimoBancarioRepository,
                         CaixinhaRepository caixinhaRepository,
                         CaixinhaInstituicaoRepository caixinhaInstituicaoRepository) {
        this.registroService = registroService;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.recorrenciaFinanceiraRepository = recorrenciaFinanceiraRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.emprestimoRepository = emprestimoRepository;
        this.emprestimoBancarioRepository = emprestimoBancarioRepository;
        this.caixinhaRepository = caixinhaRepository;
        this.caixinhaInstituicaoRepository = caixinhaInstituicaoRepository;
    }

    // =====================================================================
    // RESOLUÇÃO / CRIAÇÃO DE INSTITUIÇÕES, CATEGORIAS E RECORRÊNCIAS
    // (usado na importação para vincular corretamente ao usuário que está
    //  importando, mesmo que ele não possua ainda a instituição/categoria
    //  referenciada no arquivo — ex.: arquivo tem "Inter" mas o usuário só
    //  tem "Itaú" cadastrado: cria-se o vínculo automaticamente)
    // =====================================================================

    /** Resolve (ou cria) a InstituicaoUsuario do usuário importador pelo NOME da instituição. */
    private InstituicaoUsuario resolveOuCriarInstituicaoUsuario(UUID userId, String nomeInstituicao) {
        if (nomeInstituicao == null || nomeInstituicao.isBlank()) return null;
        String nome = nomeInstituicao.trim();

        return instituicaoUsuarioRepository.findByUsuario_IdAndInstituicao_Nome(userId, nome)
                .orElseGet(() -> {
                    Instituicao instituicao = instituicaoRepository.findByNomeIgnoreCase(nome);
                    if (instituicao == null) {
                        instituicao = instituicaoRepository.findInstituicaoByNomeContainingIgnoreCase(nome);
                    }
                    if (instituicao == null) {
                        Instituicao nova = new Instituicao();
                        nova.setNome(nome);
                        instituicao = instituicaoRepository.save(nova);
                    }
                    InstituicaoUsuario iu = new InstituicaoUsuario();
                    iu.setUsuario(getUsuario(userId));
                    iu.setInstituicao(instituicao);
                    iu.setIsAtivo(true);
                    iu.setUltimaModificacao(LocalDateTime.now());
                    return instituicaoUsuarioRepository.save(iu);
                });
    }

    /** Resolve (ou cria) a CategoriaUsuario do usuário importador pelo TÍTULO da categoria. */
    private CategoriaUsuario resolveOuCriarCategoriaUsuario(UUID userId, String titulo) {
        if (titulo == null || titulo.isBlank()) return null;
        String tit = titulo.trim();

        return categoriaUsuarioRepository.findByUsuario_IdAndCategoria_Titulo(userId, tit)
                .orElseGet(() -> {
                    Categoria categoria = categoriaRepository.findByTituloIgnoreCase(tit)
                            .orElseGet(() -> {
                                Categoria nova = new Categoria();
                                nova.setTitulo(tit);
                                return categoriaRepository.save(nova);
                            });
                    CategoriaUsuario cu = new CategoriaUsuario();
                    cu.setUsuario(getUsuario(userId));
                    cu.setCategoria(categoria);
                    cu.setAtivo(true);
                    cu.setUltimaAtualizacao(LocalDateTime.now());
                    return categoriaUsuarioRepository.save(cu);
                });
    }

    /**
     * Resolve (memorizando por chave original) ou cria uma nova RecorrenciaFinanceira
     * para o usuário importador, a partir dos dados de recorrência do arquivo original.
     */
    private RecorrenciaFinanceira resolveOuCriarRecorrencia(UUID userId, String origKey,
                                                             Map<String, RecorrenciaFinanceira> cache,
                                                             Map<String, Map<String, Object>> recorrenciasOrig) {
        if (origKey == null) return null;
        return cache.computeIfAbsent(origKey, k -> {
            Map<String, Object> dados = recorrenciasOrig.get(origKey);
            RecorrenciaFinanceira nova = new RecorrenciaFinanceira();
            nova.setUsuario(getUsuario(userId));
            if (dados != null) {
                if (dados.get("tipo") != null) nova.setTipo(Tipo.valueOf((String) dados.get("tipo")));
                if (dados.get("valor") != null) nova.setValor(((Number) dados.get("valor")).doubleValue());
                nova.setDescricao((String) dados.get("descricao"));
                if (dados.get("periodicidade") != null) {
                    nova.setPeriodicidade(Periodicidade.valueOf((String) dados.get("periodicidade")));
                }
                if (dados.get("data_inicio") != null) nova.setDataInicio(LocalDate.parse((String) dados.get("data_inicio")));
                if (dados.get("data_fim") != null) nova.setDataFim(LocalDate.parse((String) dados.get("data_fim")));
                if (dados.get("intervalo") != null) nova.setIntervalo(((Number) dados.get("intervalo")).intValue());
                if (dados.get("dia") != null) nova.setDia(((Number) dados.get("dia")).intValue());
                @SuppressWarnings("unchecked")
                List<String> dias = (List<String>) dados.get("dias_da_semana");
                if (dias != null && !dias.isEmpty()) {
                    nova.setDiasDaSemana(dias.stream().map(java.time.DayOfWeek::valueOf).toList());
                }
            }
            return recorrenciaFinanceiraRepository.save(nova);
        });
    }

    // =====================================================================
    // EMPRÉSTIMOS, EMPRÉSTIMOS BANCÁRIOS E CAIXINHAS — IMPORTAÇÃO
    // (lógica compartilhada entre importFromJson e importFromSql)
    // =====================================================================

    /**
     * Resolve a InstituicaoUsuario a vincular, priorizando o NOME (portátil entre
     * contas/usuários diferentes) e caindo para o ID legado apenas quando o nome
     * não estiver disponível (exportações antigas).
     */
    private InstituicaoUsuario resolverInstituicaoUsuario(UUID userId, String instituicaoNome, Integer idLegado) {
        if (instituicaoNome != null && !instituicaoNome.isBlank()) {
            InstituicaoUsuario iu = resolveOuCriarInstituicaoUsuario(userId, instituicaoNome);
            if (iu != null) return iu;
        }
        if (idLegado != null) {
            return instituicaoUsuarioRepository.findById(idLegado).orElseGet(() -> {
                InstituicaoUsuario stub = new InstituicaoUsuario();
                stub.setId(idLegado);
                return stub;
            });
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object valor) {
        if (valor == null) return null;
        if (valor instanceof BigDecimal bd) return bd;
        if (valor instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(valor.toString());
    }

    private void importarEmprestimo(UUID userId, String tipoStr, String statusStr, String pessoa,
                                     BigDecimal valorTotal, BigDecimal valorPago,
                                     LocalDate dataEmprestimo, LocalDate dataPrevisao,
                                     LocalDateTime dataCriacao, LocalDateTime dataQuitacao,
                                     String observacoes, String instituicaoNome, Integer instituicaoIdLegado) {
        Emprestimo emp = new Emprestimo();
        emp.setUsuario(getUsuario(userId));
        emp.setTipo(TipoEmprestimo.valueOf(tipoStr));
        emp.setStatus(statusStr != null ? StatusEmprestimo.valueOf(statusStr) : StatusEmprestimo.PENDENTE);
        emp.setPessoaOuGrupo(pessoa);
        emp.setValorTotal(valorTotal);
        emp.setValorPago(valorPago != null ? valorPago : BigDecimal.ZERO);
        emp.setDataEmprestimo(dataEmprestimo);
        emp.setDataPrevisao(dataPrevisao);
        emp.setDataCriacao(dataCriacao != null ? dataCriacao : LocalDateTime.now());
        emp.setDataQuitacao(dataQuitacao);
        emp.setObservacoes(observacoes);
        InstituicaoUsuario iu = resolverInstituicaoUsuario(userId, instituicaoNome, instituicaoIdLegado);
        emp.setInstituicaoUsuarioId(iu != null ? iu.getId() : null);
        emprestimoRepository.save(emp);
    }

    private void importarEmprestimoBancario(UUID userId, String bancoNome, String modalidadeStr,
                                             BigDecimal valorPrincipal, BigDecimal taxaJurosMensal,
                                             Integer totalParcelas, Integer parcelasPagas, BigDecimal valorParcela,
                                             LocalDate dataContratacao, LocalDate dataPrimeiraParcela,
                                             String observacoes, String statusStr,
                                             LocalDateTime dataCriacao, LocalDateTime dataQuitacao,
                                             String instituicaoNome, Integer instituicaoIdLegado) {
        EmprestimoBancario eb = new EmprestimoBancario();
        eb.setUsuario(getUsuario(userId));
        eb.setBancoNome(bancoNome);
        eb.setModalidade(modalidadeStr != null ? ModalidadeEmprestimoBancario.valueOf(modalidadeStr) : null);
        eb.setValorPrincipal(valorPrincipal);
        eb.setTaxaJurosMensal(taxaJurosMensal);
        eb.setTotalParcelas(totalParcelas);
        eb.setParcelasPagas(parcelasPagas != null ? parcelasPagas : 0);
        eb.setValorParcela(valorParcela);
        eb.setDataContratacao(dataContratacao);
        eb.setDataPrimeiraParcela(dataPrimeiraParcela);
        InstituicaoUsuario iu = resolverInstituicaoUsuario(userId, instituicaoNome, instituicaoIdLegado);
        eb.setInstituicaoUsuarioId(iu != null ? iu.getId() : null);
        eb.setObservacoes(observacoes);
        eb.setStatus(statusStr != null ? StatusEmprestimoBancario.valueOf(statusStr) : StatusEmprestimoBancario.ATIVO);
        eb.setDataCriacao(dataCriacao != null ? dataCriacao : LocalDateTime.now());
        eb.setDataQuitacao(dataQuitacao);
        emprestimoBancarioRepository.save(eb);
    }

    private Caixinha importarCaixinhaBase(UUID userId, String nome, String descricao, BigDecimal valorMeta,
                                           LocalDate dataPrazo, String tipoRendimentoStr,
                                           Double percentualRendimento, Double taxaAnualPersonalizada,
                                           Double taxaReferenciaAtual, Boolean isCompartilhada, Boolean isAtiva,
                                           LocalDate dataCriacao, LocalDate dataEncerramento) {
        Caixinha cx = new Caixinha();
        cx.setUsuario(getUsuario(userId));
        cx.setNome(nome);
        cx.setDescricao(descricao);
        cx.setValorMeta(valorMeta);
        cx.setDataPrazo(dataPrazo);
        cx.setTipoRendimento(tipoRendimentoStr != null ? TipoRendimento.valueOf(tipoRendimentoStr) : TipoRendimento.POUPANCA);
        cx.setPercentualRendimento(percentualRendimento);
        cx.setTaxaAnualPersonalizada(taxaAnualPersonalizada);
        cx.setTaxaReferenciaAtual(taxaReferenciaAtual);
        cx.setIsCompartilhada(isCompartilhada != null ? isCompartilhada : false);
        cx.setIsAtiva(isAtiva != null ? isAtiva : true);
        cx.setDataCriacao(dataCriacao != null ? dataCriacao : LocalDate.now());
        cx.setDataEncerramento(dataEncerramento);
        return caixinhaRepository.save(cx);
    }

    private void importarCaixinhaInstituicao(UUID userId, Caixinha caixinha,
                                              String instituicaoNome, Integer instituicaoIdLegado) {
        InstituicaoUsuario iu = resolverInstituicaoUsuario(userId, instituicaoNome, instituicaoIdLegado);
        if (iu == null) return;
        CaixinhaInstituicao ci = new CaixinhaInstituicao();
        ci.setCaixinha(caixinha);
        ci.setInstituicaoUsuario(iu);
        caixinhaInstituicaoRepository.save(ci);
    }

    /**
     * Importa a lista de empréstimos (formato do campo "emprestimos" do JSON exportado).
     * Retorna a quantidade importada com sucesso.
     */
    @SuppressWarnings("unchecked")
    private int importarEmprestimosJson(UUID userId, List<Map<String, Object>> lista, List<String> erros) {
        if (lista == null) return 0;
        int count = 0;
        for (Map<String, Object> m : lista) {
            try {
                importarEmprestimo(userId,
                        (String) m.get("tipo"),
                        (String) m.get("status"),
                        (String) m.get("pessoa_ou_grupo"),
                        toBigDecimal(m.get("valor_total")),
                        toBigDecimal(m.get("valor_pago")),
                        m.get("data_emprestimo") != null ? LocalDate.parse((String) m.get("data_emprestimo")) : null,
                        m.get("data_previsao") != null ? LocalDate.parse((String) m.get("data_previsao")) : null,
                        m.get("data_criacao") != null ? LocalDateTime.parse((String) m.get("data_criacao")) : null,
                        m.get("data_quitacao") != null ? LocalDateTime.parse((String) m.get("data_quitacao")) : null,
                        (String) m.get("observacoes"),
                        (String) m.get("instituicao_nome"),
                        m.get("instituicao_usuario_id") != null ? ((Number) m.get("instituicao_usuario_id")).intValue() : null);
                count++;
            } catch (Exception e) {
                erros.add("Empréstimo '" + m.get("pessoa_ou_grupo") + "': " + e.getMessage());
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private int importarEmprestimosBancariosJson(UUID userId, List<Map<String, Object>> lista, List<String> erros) {
        if (lista == null) return 0;
        int count = 0;
        for (Map<String, Object> m : lista) {
            try {
                importarEmprestimoBancario(userId,
                        (String) m.get("banco_nome"),
                        (String) m.get("modalidade"),
                        toBigDecimal(m.get("valor_principal")),
                        toBigDecimal(m.get("taxa_juros_mensal")),
                        m.get("total_parcelas") != null ? ((Number) m.get("total_parcelas")).intValue() : null,
                        m.get("parcelas_pagas") != null ? ((Number) m.get("parcelas_pagas")).intValue() : null,
                        toBigDecimal(m.get("valor_parcela")),
                        m.get("data_contratacao") != null ? LocalDate.parse((String) m.get("data_contratacao")) : null,
                        m.get("data_primeira_parcela") != null ? LocalDate.parse((String) m.get("data_primeira_parcela")) : null,
                        (String) m.get("observacoes"),
                        (String) m.get("status"),
                        m.get("data_criacao") != null ? LocalDateTime.parse((String) m.get("data_criacao")) : null,
                        m.get("data_quitacao") != null ? LocalDateTime.parse((String) m.get("data_quitacao")) : null,
                        (String) m.get("instituicao_nome"),
                        m.get("instituicao_usuario_id") != null ? ((Number) m.get("instituicao_usuario_id")).intValue() : null);
                count++;
            } catch (Exception e) {
                erros.add("Empréstimo bancário '" + m.get("banco_nome") + "': " + e.getMessage());
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private int importarCaixinhasJson(UUID userId, List<Map<String, Object>> lista, List<String> erros) {
        if (lista == null) return 0;
        int count = 0;
        for (Map<String, Object> m : lista) {
            try {
                Caixinha cx = importarCaixinhaBase(userId,
                        (String) m.get("nome"),
                        (String) m.get("descricao"),
                        toBigDecimal(m.get("valor_meta")),
                        m.get("data_prazo") != null ? LocalDate.parse((String) m.get("data_prazo")) : null,
                        (String) m.get("tipo_rendimento"),
                        m.get("percentual_rendimento") != null ? ((Number) m.get("percentual_rendimento")).doubleValue() : null,
                        m.get("taxa_anual_personalizada") != null ? ((Number) m.get("taxa_anual_personalizada")).doubleValue() : null,
                        m.get("taxa_referencia_atual") != null ? ((Number) m.get("taxa_referencia_atual")).doubleValue() : null,
                        (Boolean) m.get("is_compartilhada"),
                        (Boolean) m.get("is_ativa"),
                        m.get("data_criacao") != null ? LocalDate.parse((String) m.get("data_criacao")) : null,
                        m.get("data_encerramento") != null ? LocalDate.parse((String) m.get("data_encerramento")) : null);

                List<Map<String, Object>> instList = (List<Map<String, Object>>) m.get("instituicoes");
                if (instList != null) {
                    for (Map<String, Object> inst : instList) {
                        importarCaixinhaInstituicao(userId, cx,
                                (String) inst.get("instituicao_nome"),
                                inst.get("instituicao_usuario_id") != null
                                        ? ((Number) inst.get("instituicao_usuario_id")).intValue() : null);
                    }
                }
                count++;
            } catch (Exception e) {
                erros.add("Caixinha '" + m.get("nome") + "': " + e.getMessage());
            }
        }
        return count;
    }

    /**
     * Extrai os valores (na ordem, sem aspas) de uma linha "INSERT INTO tabela (...) VALUES (...);"
     * gerada em uma única linha por {@code RegistroExportacaoService.exportarSql()}.
     */
    private List<String> extrairValoresInsertSql(String line, String tableName) {
        String prefix = "INSERT INTO " + tableName + " (";
        if (!line.regionMatches(true, 0, prefix, 0, Math.min(prefix.length(), line.length()))) return null;
        int valuesStart = line.indexOf(") VALUES (");
        if (valuesStart < 0) return null;
        String valuesPart = line.substring(valuesStart + ") VALUES (".length()).trim();
        if (valuesPart.endsWith(");")) valuesPart = valuesPart.substring(0, valuesPart.length() - 2);
        else if (valuesPart.endsWith(")")) valuesPart = valuesPart.substring(0, valuesPart.length() - 1);
        return splitTopLevelCsv(valuesPart);
    }

    /** Divide uma lista de valores separados por vírgula, respeitando aspas simples (e '' escapado). */
    private List<String> splitTopLevelCsv(String s) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                if (inQuotes && i + 1 < s.length() && s.charAt(i + 1) == '\'') {
                    cur.append("''");
                    i++;
                    continue;
                }
                inQuotes = !inQuotes;
                cur.append(c);
            } else if (c == ',' && !inQuotes) {
                parts.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString().trim());
        return parts;
    }

    /** Remove aspas simples de um valor SQL extraído e converte o literal null (sem aspas) em {@code null}. */
    private String valorSql(List<String> valores, int idx) {
        if (valores == null || idx >= valores.size()) return null;
        String raw = valores.get(idx).trim();
        if (raw.equalsIgnoreCase("null")) return null;
        if (raw.length() >= 2 && raw.startsWith("'") && raw.endsWith("'")) {
            return raw.substring(1, raw.length() - 1).replace("''", "'");
        }
        return raw;
    }

    // =====================================================================
    // JSON IMPORT
    // =====================================================================

    public ImportResultDto importFromJson(UUID userId, byte[] content) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        int extrasImportados = 0;

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> json = mapper.readValue(content, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> eventos =
                    (List<Map<String, Object>>) json.get("eventos_financeiros");

            if (eventos == null) {
                erros.add("Arquivo JSON inválido: campo 'eventos_financeiros' não encontrado.");
                return new ImportResultDto(0, importados, erros);
            }

            // Recorrências declaradas no arquivo (id original → dados), para recriação sob demanda
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recorrenciasJson =
                    (List<Map<String, Object>>) json.get("recorrencias");
            Map<String, Map<String, Object>> recorrenciasOrig = new HashMap<>();
            if (recorrenciasJson != null) {
                for (Map<String, Object> rec : recorrenciasJson) {
                    Object id = rec.get("id");
                    if (id != null) recorrenciasOrig.put(id.toString(), rec);
                }
            }
            Map<String, RecorrenciaFinanceira> recorrenciaCache = new HashMap<>();

            for (Map<String, Object> evento : eventos) {
                String eventoId = (String) evento.get("id");
                try {
                    EventoFinanceiro financeiro = buildEventoFinanceiro(userId, evento);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> instList =
                            (List<Map<String, Object>>) evento.get("instituicoes");
                    List<EventoInstituicao> instituicoes = new ArrayList<>();
                    List<RecorrenciaFinanceira> recorrenciasPorInst = new ArrayList<>();
                    if (instList != null) {
                        for (Map<String, Object> inst : instList) {
                            String nomeInst = (String) inst.get("instituicao_nome");
                            InstituicaoUsuario iu = resolveOuCriarInstituicaoUsuario(userId, nomeInst);
                            if (iu == null) {
                                // Compatibilidade com exportações antigas sem "instituicao_nome"
                                iu = new InstituicaoUsuario();
                                Object idAntigo = inst.get("instituicao_usuario_id");
                                if (idAntigo != null) iu.setId(((Number) idAntigo).intValue());
                            }
                            EventoInstituicao ei = new EventoInstituicao();
                            ei.setInstituicaoUsuario(iu);
                            ei.setTipoMovimento(TipoMovimento.valueOf((String) inst.get("tipo_movimento")));
                            ei.setValor(((Number) inst.get("valor")).doubleValue());
                            ei.setParcelas(((Number) inst.get("parcelas")).intValue());
                            instituicoes.add(ei);

                            Object recId = inst.get("recorrencia_id");
                            recorrenciasPorInst.add(recId != null
                                    ? resolveOuCriarRecorrencia(userId, recId.toString(), recorrenciaCache, recorrenciasOrig)
                                    : null);
                        }
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> gastoMap =
                            (Map<String, Object>) evento.get("gasto_detalhe");
                    EventoDetalhe detalhe = buildDetalheFromJson(userId, gastoMap);

                    RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe, recorrenciasPorInst);
                    if (dto != null) importados.add(dto);

                } catch (Exception e) {
                    erros.add("Evento '" + eventoId + "': " + e.getMessage());
                }
            }

            // ── Empréstimos, Empréstimos Bancários e Caixinhas ────────────────
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> emprestimosJson = (List<Map<String, Object>>) json.get("emprestimos");
            extrasImportados += importarEmprestimosJson(userId, emprestimosJson, erros);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> emprestimosBancariosJson =
                    (List<Map<String, Object>>) json.get("emprestimos_bancarios");
            extrasImportados += importarEmprestimosBancariosJson(userId, emprestimosBancariosJson, erros);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> caixinhasJson = (List<Map<String, Object>>) json.get("caixinhas");
            extrasImportados += importarCaixinhasJson(userId, caixinhasJson, erros);

        } catch (Exception e) {
            erros.add("Erro ao processar arquivo JSON: " + e.getMessage());
        }

        return new ImportResultDto(importados.size() + extrasImportados, importados, erros);
    }

    // =====================================================================
    // SQL IMPORT
    // =====================================================================

    public ImportResultDto importFromSql(UUID userId, byte[] content) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        int extrasImportados = 0;

        String sqlContent = new String(content, StandardCharsets.UTF_8);
        String[] lines = sqlContent.split("\n");

        // Ordered map to preserve insertion order (event id → fields)
        Map<String, Map<String, Object>> eventos = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> instituicoesMap = new HashMap<>();
        Map<String, Map<String, Object>> detalhesMap = new HashMap<>();   // eventoId → detalhe
        Map<String, List<String>> categoriaDetalheMap = new HashMap<>();  // gastoId  → categoria titulos
        Map<String, Map<String, Object>> recorrenciasOrig = new HashMap<>(); // recorrenciaId original → dados

        // Empréstimos, Empréstimos Bancários e Caixinhas (linhas geradas em formato "posicional")
        List<List<String>> emprestimosSql = new ArrayList<>();
        List<List<String>> emprestimosBancariosSql = new ArrayList<>();
        Map<String, List<String>> caixinhasSqlPorId = new LinkedHashMap<>();       // caixinhaId original → valores
        Map<String, List<List<String>>> caixinhaInstSqlPorCaixinhaId = new HashMap<>();

        // Patterns based on the SQL generated por RegistroExportacaoService.exportarSql()
        Pattern eventoPattern = Pattern.compile(
                "INSERT INTO evento_financeiro \\([^)]+\\) VALUES \\('([0-9a-f\\-]+)',\\s*'[0-9a-f\\-]+',\\s*'([^']+)',\\s*([\\d.]+),\\s*'((?:[^']|'')*)',\\s*'(\\d{4}-\\d{2}-\\d{2})',",
                Pattern.CASE_INSENSITIVE
        );
        Pattern instPattern = Pattern.compile(
                "INSERT INTO evento_instituicao \\([^)]+\\) VALUES \\(\\d+,\\s*'([0-9a-f\\-]+)',\\s*\\d+,\\s*'([^']+)',\\s*([\\d.]+),\\s*(\\d+),\\s*'((?:[^']|'')*)',\\s*(?:'([0-9a-f\\-]+)'|null)\\)",
                Pattern.CASE_INSENSITIVE
        );
        // Padrão legado (sem instituicao_nome/fk_recorrencia) — compatibilidade com exportações antigas
        Pattern instPatternLegado = Pattern.compile(
                "INSERT INTO evento_instituicao \\([^)]+\\) VALUES \\(\\d+,\\s*'([0-9a-f\\-]+)',\\s*(\\d+),\\s*'([^']+)',\\s*([\\d.]+),\\s*(\\d+)\\)",
                Pattern.CASE_INSENSITIVE
        );
        Pattern gastoPattern = Pattern.compile(
                "INSERT INTO gasto_detalhe \\([^)]+\\) VALUES \\('(\\d+)',\\s*'([0-9a-f\\-]+)',\\s*'((?:[^']|'')*)'\\)",
                Pattern.CASE_INSENSITIVE
        );
        Pattern catPattern = Pattern.compile(
                "INSERT INTO gasto_detalhe_categoria \\([^)]+\\) VALUES \\('(\\d+)',\\s*\\d+,\\s*'((?:[^']|'')*)'\\)",
                Pattern.CASE_INSENSITIVE
        );
        // Padrão legado (sem categoria_titulo)
        Pattern catPatternLegado = Pattern.compile(
                "INSERT INTO gasto_detalhe_categoria \\([^)]+\\) VALUES \\('(\\d+)',\\s*(\\d+)\\)",
                Pattern.CASE_INSENSITIVE
        );
        Pattern recPattern = Pattern.compile(
                "INSERT INTO recorrencia_financeira \\([^)]+\\) VALUES \\('([0-9a-f\\-]+)',\\s*'[0-9a-f\\-]+',\\s*'([^']+)',\\s*([\\d.]+),\\s*(?:'((?:[^']|'')*)'|null),\\s*'([^']+)',\\s*(?:'(\\d{4}-\\d{2}-\\d{2})'|null),\\s*(?:'(\\d{4}-\\d{2}-\\d{2})'|null),\\s*(?:(\\d+)|null),\\s*(?:(\\d+)|null),\\s*'([^']*)'\\)",
                Pattern.CASE_INSENSITIVE
        );

        for (String line : lines) {
            line = line.trim();

            Matcher m = eventoPattern.matcher(line);
            if (m.find()) {
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("tipo", m.group(2));
                ev.put("valor", Double.parseDouble(m.group(3)));
                ev.put("descricao", m.group(4).replace("''", "'"));
                ev.put("data_evento", m.group(5));
                eventos.put(m.group(1), ev);
                continue;
            }

            Matcher mr = recPattern.matcher(line);
            if (mr.find()) {
                Map<String, Object> rec = new LinkedHashMap<>();
                rec.put("tipo", mr.group(2));
                rec.put("valor", Double.parseDouble(mr.group(3)));
                rec.put("descricao", mr.group(4) != null ? mr.group(4).replace("''", "'") : null);
                rec.put("periodicidade", mr.group(5));
                rec.put("data_inicio", mr.group(6));
                rec.put("data_fim", mr.group(7));
                rec.put("intervalo", mr.group(8) != null ? Integer.parseInt(mr.group(8)) : null);
                rec.put("dia", mr.group(9) != null ? Integer.parseInt(mr.group(9)) : null);
                String diasStr = mr.group(10);
                if (diasStr != null && !diasStr.isBlank()) {
                    rec.put("dias_da_semana", Arrays.asList(diasStr.split(",")));
                }
                recorrenciasOrig.put(mr.group(1), rec);
                continue;
            }

            Matcher mi = instPattern.matcher(line);
            if (mi.find()) {
                String eventoId = mi.group(1);
                Map<String, Object> inst = new LinkedHashMap<>();
                inst.put("tipo_movimento", mi.group(2));
                inst.put("valor", Double.parseDouble(mi.group(3)));
                inst.put("parcelas", Integer.parseInt(mi.group(4)));
                inst.put("instituicao_nome", mi.group(5).replace("''", "'"));
                inst.put("recorrencia_id", mi.group(6));
                instituicoesMap.computeIfAbsent(eventoId, k -> new ArrayList<>()).add(inst);
                continue;
            }

            Matcher miLeg = instPatternLegado.matcher(line);
            if (miLeg.find()) {
                String eventoId = miLeg.group(1);
                Map<String, Object> inst = new LinkedHashMap<>();
                inst.put("instituicao_usuario_id_legado", Integer.parseInt(miLeg.group(2)));
                inst.put("tipo_movimento", miLeg.group(3));
                inst.put("valor", Double.parseDouble(miLeg.group(4)));
                inst.put("parcelas", Integer.parseInt(miLeg.group(5)));
                instituicoesMap.computeIfAbsent(eventoId, k -> new ArrayList<>()).add(inst);
                continue;
            }

            Matcher mg = gastoPattern.matcher(line);
            if (mg.find()) {
                String gastoId = mg.group(1);
                String eventoId = mg.group(2);
                Map<String, Object> gasto = new LinkedHashMap<>();
                gasto.put("id", gastoId);
                gasto.put("titulo_gasto", mg.group(3).replace("''", "'"));
                detalhesMap.put(eventoId, gasto);
                continue;
            }

            Matcher mc = catPattern.matcher(line);
            if (mc.find()) {
                String gastoId = mc.group(1);
                String catTitulo = mc.group(2).replace("''", "'");
                categoriaDetalheMap.computeIfAbsent(gastoId, k -> new ArrayList<>()).add(catTitulo);
                continue;
            }

            Matcher mcLeg = catPatternLegado.matcher(line);
            if (mcLeg.find()) {
                // Padrão legado: sem título de categoria disponível, ignora (não há como resolver por nome)
                continue;
            }

            // ── Empréstimos, Empréstimos Bancários e Caixinhas ────────────────
            List<String> valsEmp = extrairValoresInsertSql(line, "emprestimo");
            if (valsEmp != null) {
                emprestimosSql.add(valsEmp);
                continue;
            }

            List<String> valsEb = extrairValoresInsertSql(line, "emprestimo_bancario");
            if (valsEb != null) {
                emprestimosBancariosSql.add(valsEb);
                continue;
            }

            List<String> valsCx = extrairValoresInsertSql(line, "caixinha_instituicao");
            if (valsCx != null) {
                String caixinhaId = valorSql(valsCx, 1);
                caixinhaInstSqlPorCaixinhaId.computeIfAbsent(caixinhaId, k -> new ArrayList<>()).add(valsCx);
                continue;
            }

            // Precisa vir depois de "caixinha_instituicao" pois ambas começam com "INSERT INTO caixinha"
            List<String> valsCaixinha = extrairValoresInsertSql(line, "caixinha");
            if (valsCaixinha != null) {
                String caixinhaId = valorSql(valsCaixinha, 0);
                caixinhasSqlPorId.put(caixinhaId, valsCaixinha);
                continue;
            }
        }

        Map<String, RecorrenciaFinanceira> recorrenciaCache = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : eventos.entrySet()) {
            String eventoId = entry.getKey();
            Map<String, Object> ev = entry.getValue();

            try {
                EventoFinanceiro financeiro = buildEventoFinanceiro(userId, ev);

                List<EventoInstituicao> instituicoes = new ArrayList<>();
                List<RecorrenciaFinanceira> recorrenciasPorInst = new ArrayList<>();
                for (Map<String, Object> inst : instituicoesMap.getOrDefault(eventoId, List.of())) {
                    EventoInstituicao ei = new EventoInstituicao();
                    String nomeInst = (String) inst.get("instituicao_nome");
                    InstituicaoUsuario iu = resolveOuCriarInstituicaoUsuario(userId, nomeInst);
                    if (iu == null) {
                        // Compatibilidade com exportações antigas (apenas ID, sem nome)
                        iu = new InstituicaoUsuario();
                        Object idLegado = inst.get("instituicao_usuario_id_legado");
                        if (idLegado != null) iu.setId((Integer) idLegado);
                    }
                    ei.setInstituicaoUsuario(iu);
                    ei.setTipoMovimento(TipoMovimento.valueOf((String) inst.get("tipo_movimento")));
                    ei.setValor((Double) inst.get("valor"));
                    ei.setParcelas((Integer) inst.get("parcelas"));
                    instituicoes.add(ei);

                    String recId = (String) inst.get("recorrencia_id");
                    recorrenciasPorInst.add(recId != null
                            ? resolveOuCriarRecorrencia(userId, recId, recorrenciaCache, recorrenciasOrig)
                            : null);
                }

                Map<String, Object> gastoMap = detalhesMap.get(eventoId);
                EventoDetalhe detalhe = new EventoDetalhe();
                if (gastoMap != null) {
                    String gastoId = (String) gastoMap.get("id");
                    detalhe.setTituloGasto((String) gastoMap.get("titulo_gasto"));
                    List<CategoriaUsuario> categorias = new ArrayList<>();
                    for (String catTitulo : categoriaDetalheMap.getOrDefault(gastoId, List.of())) {
                        CategoriaUsuario cu = resolveOuCriarCategoriaUsuario(userId, catTitulo);
                        if (cu != null) categorias.add(cu);
                    }
                    detalhe.setCategoriaUsuario(categorias);
                } else {
                    detalhe.setTituloGasto("Registro Importado");
                    detalhe.setCategoriaUsuario(new ArrayList<>());
                }

                RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe, recorrenciasPorInst);
                if (dto != null) importados.add(dto);

            } catch (Exception e) {
                erros.add("Evento '" + eventoId + "': " + e.getMessage());
            }
        }

        // ── Empréstimos ────────────────────────────────────────────────────────
        for (List<String> v : emprestimosSql) {
            String pessoa = valorSql(v, 4);
            try {
                importarEmprestimo(userId,
                        valorSql(v, 2),
                        valorSql(v, 3),
                        pessoa,
                        toBigDecimal(valorSql(v, 5)),
                        toBigDecimal(valorSql(v, 6)),
                        valorSql(v, 7) != null ? LocalDate.parse(valorSql(v, 7)) : null,
                        valorSql(v, 8) != null ? LocalDate.parse(valorSql(v, 8)) : null,
                        valorSql(v, 9) != null ? LocalDateTime.parse(valorSql(v, 9)) : null,
                        valorSql(v, 10) != null ? LocalDateTime.parse(valorSql(v, 10)) : null,
                        valorSql(v, 11),
                        valorSql(v, 13),
                        valorSql(v, 12) != null ? Integer.parseInt(valorSql(v, 12)) : null);
                extrasImportados++;
            } catch (Exception e) {
                erros.add("Empréstimo '" + pessoa + "': " + e.getMessage());
            }
        }

        // ── Empréstimos Bancários ────────────────────────────────────────────────
        for (List<String> v : emprestimosBancariosSql) {
            String banco = valorSql(v, 2);
            try {
                importarEmprestimoBancario(userId,
                        banco,
                        valorSql(v, 3),
                        toBigDecimal(valorSql(v, 4)),
                        toBigDecimal(valorSql(v, 5)),
                        valorSql(v, 6) != null ? Integer.parseInt(valorSql(v, 6)) : null,
                        valorSql(v, 7) != null ? Integer.parseInt(valorSql(v, 7)) : null,
                        toBigDecimal(valorSql(v, 8)),
                        valorSql(v, 9) != null ? LocalDate.parse(valorSql(v, 9)) : null,
                        valorSql(v, 10) != null ? LocalDate.parse(valorSql(v, 10)) : null,
                        valorSql(v, 12),
                        valorSql(v, 13),
                        valorSql(v, 14) != null ? LocalDateTime.parse(valorSql(v, 14)) : null,
                        valorSql(v, 15) != null ? LocalDateTime.parse(valorSql(v, 15)) : null,
                        valorSql(v, 16),
                        valorSql(v, 11) != null ? Integer.parseInt(valorSql(v, 11)) : null);
                extrasImportados++;
            } catch (Exception e) {
                erros.add("Empréstimo bancário '" + banco + "': " + e.getMessage());
            }
        }

        // ── Caixinhas + Instituições vinculadas ──────────────────────────────────
        for (Map.Entry<String, List<String>> entry : caixinhasSqlPorId.entrySet()) {
            String caixinhaIdOriginal = entry.getKey();
            List<String> v = entry.getValue();
            String nome = valorSql(v, 2);
            try {
                Caixinha cx = importarCaixinhaBase(userId,
                        nome,
                        valorSql(v, 3),
                        toBigDecimal(valorSql(v, 4)),
                        valorSql(v, 5) != null ? LocalDate.parse(valorSql(v, 5)) : null,
                        valorSql(v, 6),
                        valorSql(v, 7) != null ? Double.parseDouble(valorSql(v, 7)) : null,
                        valorSql(v, 8) != null ? Double.parseDouble(valorSql(v, 8)) : null,
                        valorSql(v, 9) != null ? Double.parseDouble(valorSql(v, 9)) : null,
                        valorSql(v, 10) != null ? Boolean.parseBoolean(valorSql(v, 10)) : null,
                        valorSql(v, 11) != null ? Boolean.parseBoolean(valorSql(v, 11)) : null,
                        valorSql(v, 12) != null ? LocalDate.parse(valorSql(v, 12)) : null,
                        valorSql(v, 13) != null ? LocalDate.parse(valorSql(v, 13)) : null);

                for (List<String> ciVals : caixinhaInstSqlPorCaixinhaId.getOrDefault(caixinhaIdOriginal, List.of())) {
                    importarCaixinhaInstituicao(userId, cx,
                            valorSql(ciVals, 3),
                            valorSql(ciVals, 2) != null ? Integer.parseInt(valorSql(ciVals, 2)) : null);
                }
                extrasImportados++;
            } catch (Exception e) {
                erros.add("Caixinha '" + nome + "': " + e.getMessage());
            }
        }

        return new ImportResultDto(importados.size() + extrasImportados, importados, erros);
    }

    // =====================================================================
    // EXCEL IMPORT
    // =====================================================================

    public ImportResultDto importFromExcel(UUID userId, byte[] content) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            // Sheet 0 = "Informações Básicas" → skip; import from monthly sheets
            for (int sheetIndex = 1; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                boolean isFirstRow = true;

                for (Row row : sheet) {
                    // Skip header row
                    if (isFirstRow) {
                        isFirstRow = false;
                        continue;
                    }

                    String firstValue = getCellValue(row.getCell(0)).trim();

                    // Empty row signals the start of the summary section → stop
                    if (firstValue.isEmpty()) break;

                    // Only process rows whose first cell is a date "yyyy-MM-dd"
                    if (!firstValue.matches("\\d{4}-\\d{2}-\\d{2}")) continue;

                    try {
                        String titulo        = getCellValue(row.getCell(1)).trim();
                        String valorStr      = getCellValue(row.getCell(2)).trim();
                        String tipoStr       = getCellValue(row.getCell(3)).trim();
                        String descricao     = getCellValue(row.getCell(4)).trim();
                        String instNome      = getCellValue(row.getCell(5)).trim();
                        String movimentacao  = getCellValue(row.getCell(6)).trim();
                        String parcelasStr   = getCellValue(row.getCell(7)).trim();
                        String catTitulo     = getCellValue(row.getCell(8)).trim();

                        EventoFinanceiro financeiro = new EventoFinanceiro();
                        financeiro.setUsuario(getUsuario(userId));
                        financeiro.setTipo(Tipo.valueOf(tipoStr));
                        financeiro.setValor(Double.parseDouble(valorStr));
                        financeiro.setDescricao(descricao);
                        financeiro.setDataEvento(LocalDate.parse(firstValue));

                        // Resolve institution by name
                        List<EventoInstituicao> instituicoes = new ArrayList<>();
                        if (!instNome.isEmpty() && !"-".equals(instNome)) {
                            InstituicaoUsuario iu = instituicaoUsuarioRepository
                                    .findByUsuario_IdAndInstituicao_Nome(userId, instNome)
                                    .orElseThrow(() -> new EntidadeNaoEncontradaException(
                                            "Instituição '" + instNome + "' não encontrada para o usuário."));
                            EventoInstituicao ei = new EventoInstituicao();
                            ei.setInstituicaoUsuario(iu);
                            ei.setTipoMovimento(TipoMovimento.valueOf(movimentacao));
                            ei.setValor(Double.parseDouble(valorStr));
                            ei.setParcelas("-".equals(parcelasStr) || parcelasStr.isEmpty()
                                    ? 1 : Integer.parseInt(parcelasStr));
                            instituicoes.add(ei);
                        }

                        // Resolve category by title
                        EventoDetalhe detalhe = new EventoDetalhe();
                        detalhe.setTituloGasto(titulo.isEmpty() ? "Registro Importado" : titulo);
                        List<CategoriaUsuario> categorias = new ArrayList<>();
                        if (!catTitulo.isEmpty() && !"-".equals(catTitulo)) {
                            categoriaUsuarioRepository
                                    .findByUsuario_IdAndCategoria_Titulo(userId, catTitulo)
                                    .ifPresent(categorias::add);
                        }
                        detalhe.setCategoriaUsuario(categorias);

                        RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe);
                        if (dto != null) importados.add(dto);

                    } catch (Exception e) {
                        erros.add("Aba '" + sheet.getSheetName() + "' linha " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            erros.add("Erro ao processar arquivo Excel: " + e.getMessage());
        }

        return new ImportResultDto(importados.size(), importados, erros);
    }

    // =====================================================================
    // PDF IMPORT — compatível com o formato gerado pelo MyFinance
    // =====================================================================

    /**
     * Importa registros a partir de um PDF gerado pelo próprio sistema MyFinance.
     *
     * <p>Formato esperado nas linhas de tabela:
     * {@code dd/MM  Título  R$ X.XXX,XX  Tipo  Descrição  Instituição  Movimento  (À vista|Nx)  Categorias}
     *
     * <p>O parser detecta:
     * <ul>
     *   <li>Ano (4 dígitos sozinhos na linha) → contexto de ano</li>
     *   <li>Cabeçalho de seção de mês (ex.: "Janeiro 2025") → contexto de mês/ano</li>
     *   <li>Cabeçalho de tabela ("Data" + "Título" + "Valor") → início da tabela</li>
     *   <li>Início da seção analítica ("ANÁLISE FINANCEIRA") → fim dos registros</li>
     *   <li>Caixas de resumo ("Receitas", "Gastos", "Poupança" juntos) → pula resumo</li>
     * </ul>
     */
    public ImportResultDto importFromPdf(UUID userId, byte[] content) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        try {
            List<InstituicaoUsuario> userInstituicoes =
                    instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
            List<CategoriaUsuario> userCategorias =
                    categoriaUsuarioRepository.findAllByUsuario_Id(userId);

            List<String> instNomes = userInstituicoes.stream()
                    .map(i -> i.getInstituicao().getNome())
                    .toList();

            String tipoRegex = "Gasto|Recebimento|Transferencia|Transferência|Poupanca|Poupança|Emprestimo|Empréstimo";
            String movRegex  = "Debito|Débito|Credito|Crédito|Dinheiro|Pix|Boleto|Voucher";

            // Padrão novo: dd/MM  Título  R$ X.XXX,XX  Tipo  ...  Movimento  (À vista|Nx)  Categorias
            Pattern rowPatternNovo = Pattern.compile(
                "^(\\d{2}/\\d{2})\\s+(.+?)\\s+R\\$\\s*([\\d.]+,[\\d]{2})\\s+(" + tipoRegex + ")\\s+(.*?)\\s+(" + movRegex + ")\\s+(À vista|\\d+x)\\s+(.*)$"
            );

            // Padrão legado: <dia> <...> <valor> <Tipo> <...> <Movimento> <parcelas> <...>
            Pattern rowPatternLegado = Pattern.compile(
                "^(\\d{1,2})\\s+(.+?)\\s+([\\d]+[.,]?[\\d]*)\\s+(" + tipoRegex + ")\\s+(.*?)\\s+(" + movRegex + ")\\s+(\\d+)\\s+(.*)$"
            );

            int currentYear  = LocalDate.now().getYear();
            int currentMonth = 1;
            boolean inTable  = false;
            boolean pastSummary = false;
            boolean analiseEncontrada = false;

            try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(content)))) {

                for (int page = 1; page <= pdfDoc.getNumberOfPages(); page++) {
                    if (analiseEncontrada) break;

                    String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page));
                    String[] lines = pageText.split("\n");

                    for (String rawLine : lines) {
                        String line = rawLine.trim();
                        if (line.isEmpty()) continue;

                        // Detecta seção de análise financeira → para de importar registros
                        if (line.toUpperCase().contains("ANÁLISE FINANCEIRA") ||
                            line.toUpperCase().contains("ANALISE FINANCEIRA")) {
                            analiseEncontrada = true;
                            inTable = false;
                            break;
                        }

                        // Capa / sumário — pula até encontrar registros reais
                        if (line.equalsIgnoreCase("Sumário") || line.equalsIgnoreCase("Sumario") ||
                            line.startsWith("MyFinance") || line.startsWith("Relatório Financeiro")) {
                            pastSummary = false;
                            inTable = false;
                            continue;
                        }

                        // Detecta ano isolado na linha (ex.: "2025")
                        if (line.matches("^\\d{4}$")) {
                            currentYear = Integer.parseInt(line);
                            pastSummary = true;
                            inTable = false;
                            continue;
                        }

                        // Detecta cabeçalho de mês no novo formato: "Janeiro 2025"
                        // Formato novo: "NomeMes AAAA" (ex.: "Janeiro 2025")
                        boolean detectedMonth = false;
                        String[] partesMes = line.split("\\s+");
                        if (partesMes.length == 2 && partesMes[1].matches("\\d{4}")) {
                            Integer mesNum = MESES_PT.get(partesMes[0].toLowerCase());
                            if (mesNum != null) {
                                currentMonth = mesNum;
                                currentYear  = Integer.parseInt(partesMes[1]);
                                pastSummary  = true;
                                inTable      = false;
                                detectedMonth = true;
                            }
                        }
                        // Formato legado: nome do mês sozinho
                        if (!detectedMonth) {
                            Integer mesNum = MESES_PT.get(line.toLowerCase());
                            if (mesNum != null) {
                                currentMonth = mesNum;
                                inTable = false;
                                continue;
                            }
                        } else {
                            continue;
                        }

                        // Detecta cabeçalho de tabela
                        if (line.contains("Título") && line.contains("Valor") && line.contains("Tipo")) {
                            inTable = pastSummary;
                            continue;
                        }

                        // Detecta fim da tabela (resumo mensal / anual)
                        if (line.startsWith("Resumo") || line.startsWith("Receitas")
                                || line.startsWith("Ganhos") || line.startsWith("Saldo")
                                || line.startsWith("Pontuação")) {
                            inTable = false;
                            continue;
                        }

                        if (!inTable) continue;

                        // ── Tenta padrão novo (dd/MM, R$ formatado) ──────────────────
                        Matcher mNovo = rowPatternNovo.matcher(line);
                        if (mNovo.find()) {
                            try {
                                String dataDDMM = mNovo.group(1);      // dd/MM
                                String titulo   = mNovo.group(2).trim();
                                String valorStr = mNovo.group(3);       // X.XXX,XX
                                String tipoStr  = normalizarTipo(mNovo.group(4));
                                String afterTipo = mNovo.group(5).trim();
                                String movStr   = normalizarMovimento(mNovo.group(6));
                                String parcStr  = mNovo.group(7);       // "À vista" ou "Nx"
                                String catStr   = mNovo.group(8).trim();

                                String[] dmParts = dataDDMM.split("/");
                                int dia = Integer.parseInt(dmParts[0]);
                                int mes = Integer.parseInt(dmParts[1]);
                                LocalDate data = LocalDate.of(currentYear, mes, dia);

                                double valor = Double.parseDouble(
                                        valorStr.replaceAll("\\.", "").replace(",", "."));
                                Tipo tipo = Tipo.valueOf(tipoStr);
                                TipoMovimento mv = TipoMovimento.valueOf(movStr);
                                int parcelas = "À vista".equalsIgnoreCase(parcStr) ? 1
                                        : Integer.parseInt(parcStr.replace("x", "").trim());

                                // Separa descrição da instituição (afterTipo pode conter ambos)
                                String descricao = afterTipo;
                                String instNomeMatch = null;
                                for (String nome : instNomes) {
                                    if (afterTipo.contains(nome)) {
                                        instNomeMatch = nome;
                                        descricao = afterTipo.replace(nome, "").trim();
                                        break;
                                    }
                                }

                                RegistroResponseDto dto = montarEPersistir(
                                        userId, titulo, valor, tipo, descricao, data,
                                        instNomeMatch, mv, parcelas, catStr,
                                        userInstituicoes, userCategorias);
                                if (dto != null) importados.add(dto);

                            } catch (Exception e) {
                                erros.add("PDF p." + page + " (novo formato) — '"
                                        + truncarLog(line) + "': " + e.getMessage());
                            }
                            continue;
                        }

                        // ── Tenta padrão legado (dia inteiro, valor sem R$) ───────────
                        Matcher mLeg = rowPatternLegado.matcher(line);
                        if (mLeg.find()) {
                            try {
                                int dia      = Integer.parseInt(mLeg.group(1));
                                String titulo = mLeg.group(2).trim();
                                double valor  = Double.parseDouble(mLeg.group(3).replace(",", "."));
                                Tipo tipo     = Tipo.valueOf(normalizarTipo(mLeg.group(4)));
                                String afterTipo = mLeg.group(5).trim();
                                TipoMovimento mv = TipoMovimento.valueOf(normalizarMovimento(mLeg.group(6)));
                                int parcelas  = Integer.parseInt(mLeg.group(7));
                                String catStr = mLeg.group(8).trim();

                                String descricao = afterTipo;
                                String instNomeMatch = null;
                                for (String nome : instNomes) {
                                    if (afterTipo.contains(nome)) {
                                        instNomeMatch = nome;
                                        descricao = afterTipo.replace(nome, "").trim();
                                        break;
                                    }
                                }

                                LocalDate data = LocalDate.of(currentYear, currentMonth, dia);

                                RegistroResponseDto dto = montarEPersistir(
                                        userId, titulo, valor, tipo, descricao, data,
                                        instNomeMatch, mv, parcelas, catStr,
                                        userInstituicoes, userCategorias);
                                if (dto != null) importados.add(dto);

                            } catch (Exception e) {
                                erros.add("PDF p." + page + " (formato legado) — '"
                                        + truncarLog(line) + "': " + e.getMessage());
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            erros.add("Erro ao processar arquivo PDF: " + e.getMessage());
        }

        if (importados.isEmpty() && erros.isEmpty()) {
            erros.add("Nenhum registro importado do PDF. O PDF pode não conter tabelas de dados reconhecíveis. " +
                    "Para importação confiável, prefira os formatos JSON ou Excel.");
        }

        return new ImportResultDto(importados.size(), importados, erros);
    }

    /**
     * Constrói o EventoFinanceiro, EventoInstituicao e EventoDetalhe e persiste via RegistroService.
     */
    private RegistroResponseDto montarEPersistir(UUID userId, String titulo, double valor,
                                                   Tipo tipo, String descricao, LocalDate data,
                                                   String instNome, TipoMovimento tipoMovimento, int parcelas,
                                                   String catStr,
                                                   List<InstituicaoUsuario> userInstituicoes,
                                                   List<CategoriaUsuario> userCategorias) {
        EventoFinanceiro financeiro = new EventoFinanceiro();
        financeiro.setUsuario(getUsuario(userId));
        financeiro.setTipo(tipo);
        financeiro.setValor(valor);
        financeiro.setDescricao(descricao.length() > 500 ? descricao.substring(0, 500) : descricao);
        financeiro.setDataEvento(data);

        List<EventoInstituicao> instituicoes = new ArrayList<>();
        if (instNome != null) {
            final String finalNome = instNome;
            userInstituicoes.stream()
                    .filter(i -> i.getInstituicao().getNome().equals(finalNome))
                    .findFirst()
                    .ifPresent(iu -> {
                        EventoInstituicao ei = new EventoInstituicao();
                        ei.setInstituicaoUsuario(iu);
                        ei.setTipoMovimento(tipoMovimento);
                        ei.setValor(valor);
                        ei.setParcelas(parcelas);
                        instituicoes.add(ei);
                    });
        }

        EventoDetalhe detalhe = new EventoDetalhe();
        detalhe.setTituloGasto(titulo == null || titulo.isBlank() ? "Registro Importado" : titulo);
        List<CategoriaUsuario> categorias = new ArrayList<>();
        if (catStr != null && !catStr.isBlank() && !"-".equals(catStr)) {
            // Suporta múltiplas categorias separadas por ", " (novo formato)
            for (String catTitulo : catStr.split(",\\s*|\\s*/\\s*")) {
                String t = catTitulo.trim();
                userCategorias.stream()
                        .filter(c -> c.getCategoria().getTitulo().equalsIgnoreCase(t))
                        .findFirst()
                        .ifPresent(categorias::add);
            }
        }
        detalhe.setCategoriaUsuario(categorias);

        return persistirRegistro(financeiro, instituicoes, detalhe);
    }

    /** Normaliza variações acentuadas/sem acento dos tipos de evento para o enum exato. */
    private String normalizarTipo(String raw) {
        return switch (raw.trim()) {
            case "Transferência" -> "Transferencia";
            case "Poupança"      -> "Poupanca";
            case "Empréstimo"    -> "Emprestimo";
            default              -> raw.trim();
        };
    }

    /** Normaliza variações acentuadas/sem acento dos tipos de movimento para o enum exato. */
    private String normalizarMovimento(String raw) {
        return switch (raw.trim()) {
            case "Débito"  -> "Debito";
            case "Crédito" -> "Credito";
            default        -> raw.trim();
        };
    }

    private String truncarLog(String linha) {
        return linha.length() > 80 ? linha.substring(0, 80) + "…" : linha;
    }

    // =====================================================================
    // OFX IMPORT
    // =====================================================================

    /**
     * Importa transações a partir de um arquivo OFX (Open Financial Exchange).
     * Suporta OFX 1.x (SGML) e OFX 2.x (XML).
     * <p>
     * Mapeamento de campos OFX → modelo:
     * <ul>
     *   <li>TRNTYPE + sinal do valor  → {@link Tipo} (Gasto / Recebimento)</li>
     *   <li>TRNAMT (valor absoluto)   → EventoFinanceiro.valor</li>
     *   <li>DTPOSTED (YYYYMMDD...)    → EventoFinanceiro.dataEvento</li>
     *   <li>NAME                      → EventoDetalhe.tituloGasto</li>
     *   <li>MEMO                      → EventoFinanceiro.descricao</li>
     *   <li>ORG (banco emissor)       → InstituicaoUsuario (por correspondência de nome)</li>
     * </ul>
     */
    public ImportResultDto importFromOfx(UUID userId, byte[] content) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        try {
            // Detect charset from OFX header (default UTF-8 for OFX 2.x, ISO-8859-1 common for 1.x)
            String rawHeader = new String(content, 0, Math.min(content.length, 512), StandardCharsets.ISO_8859_1).toUpperCase();
            java.nio.charset.Charset charset = rawHeader.contains("CHARSET:1252") || rawHeader.contains("CHARSET:ISO")
                    ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8;

            String ofxContent = new String(content, charset);

            // Locate the start of the OFX body (skip SGML header lines)
            int ofxStart = -1;
            for (String tag : new String[]{"<OFX>", "<ofx>"}) {
                ofxStart = ofxContent.indexOf(tag);
                if (ofxStart >= 0) break;
            }
            if (ofxStart < 0) {
                erros.add("Arquivo OFX inválido: tag <OFX> não encontrada.");
                return new ImportResultDto(0, importados, erros);
            }
            String body = ofxContent.substring(ofxStart);

            // Try to match institution from the <ORG> tag in the OFX header
            List<InstituicaoUsuario> userInstituicoes =
                    instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);

            String orgName = ofxExtractTagValue(body, "ORG");
            InstituicaoUsuario matchedInstituicao = null;
            if (orgName != null && !orgName.isBlank()) {
                final String orgLower = orgName.toLowerCase();
                matchedInstituicao = userInstituicoes.stream()
                        .filter(iu -> {
                            String nome = iu.getInstituicao().getNome().toLowerCase();
                            return nome.contains(orgLower) || orgLower.contains(nome);
                        })
                        .findFirst()
                        .orElse(null);
            }

            // Extract all <STMTTRN>...</STMTTRN> blocks
            Pattern trnPattern = Pattern.compile(
                    "<STMTTRN>(.+?)</STMTTRN>",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );
            Matcher trnMatcher = trnPattern.matcher(body);

            int transacaoIndex = 0;
            while (trnMatcher.find()) {
                transacaoIndex++;
                String trn = trnMatcher.group(1);
                String fitId = ofxExtractTagValue(trn, "FITID");
                String identificador = fitId != null ? fitId : "transacao-" + transacaoIndex;

                try {
                    String trnType  = ofxExtractTagValue(trn, "TRNTYPE");
                    String dtPosted = ofxExtractTagValue(trn, "DTPOSTED");
                    String amtStr   = ofxExtractTagValue(trn, "TRNAMT");
                    String name     = ofxExtractTagValue(trn, "NAME");
                    String memo     = ofxExtractTagValue(trn, "MEMO");

                    if (amtStr == null || dtPosted == null) {
                        erros.add("Transação '" + identificador + "': campos TRNAMT ou DTPOSTED ausentes.");
                        continue;
                    }

                    double amount    = Double.parseDouble(amtStr.replace(",", "."));
                    double absAmount = Math.abs(amount);
                    LocalDate data   = ofxParseDate(dtPosted);

                    // Analisa MEMO para determinar tipo, movimento e título de forma inteligente
                    String memoText = memo != null && !memo.isBlank() ? memo
                            : (name != null ? name : "");
                    MemoParseResult parsed = parseMemoOFX(memoText, trnType, amount);

                    // EventoFinanceiro
                    EventoFinanceiro financeiro = new EventoFinanceiro();
                    financeiro.setUsuario(getUsuario(userId));
                    financeiro.setTipo(parsed.tipo());
                    financeiro.setValor(absAmount);
                    String descricao = memoText.isBlank() ? "Importado via OFX" : memoText;
                    financeiro.setDescricao(descricao.length() > 500 ? descricao.substring(0, 500) : descricao);
                    financeiro.setDataEvento(data);

                    // EventoInstituicao (somente se encontrou correspondência)
                    List<EventoInstituicao> instituicoes = new ArrayList<>();
                    if (matchedInstituicao != null) {
                        EventoInstituicao ei = new EventoInstituicao();
                        ei.setInstituicaoUsuario(matchedInstituicao);
                        ei.setTipoMovimento(parsed.tipoMovimento());
                        ei.setValor(absAmount);
                        ei.setParcelas(1);
                        instituicoes.add(ei);
                    }

                    // EventoDetalhe
                    EventoDetalhe detalhe = new EventoDetalhe();
                    // Se houver um NAME explícito e o título inferido for genérico, prefere o NAME
                    String tituloFinal = parsed.titulo();
                    if ((tituloFinal == null || tituloFinal.equals("Registro OFX"))
                            && name != null && !name.isBlank()) {
                        tituloFinal = sanitizeMerchant(name);
                    }
                    detalhe.setTituloGasto(tituloFinal != null ? tituloFinal : "Registro OFX");
                    detalhe.setCategoriaUsuario(new ArrayList<>());

                    RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe);
                    if (dto != null) importados.add(dto);

                } catch (Exception e) {
                    erros.add("Transação '" + identificador + "': " + e.getMessage());
                }
            }

            if (transacaoIndex == 0) {
                erros.add("Nenhuma transação (<STMTTRN>) encontrada no arquivo OFX.");
            }

        } catch (Exception e) {
            erros.add("Erro ao processar arquivo OFX: " + e.getMessage());
        }

        return new ImportResultDto(importados.size(), importados, erros);
    }

    /**
     * Extrai o valor de uma tag OFX, suportando tanto OFX 1.x (sem closing tag)
     * quanto OFX 2.x (com closing tag).
     */
    private String ofxExtractTagValue(String content, String tagName) {
        // OFX 2.x: <TAG>value</TAG>
        Pattern withClose = Pattern.compile(
                "<" + tagName + ">([^<]+)</" + tagName + ">",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = withClose.matcher(content);
        if (m.find()) return m.group(1).trim();

        // OFX 1.x: <TAG>value  (sem closing tag — valor termina na próxima tag ou EOL)
        Pattern withoutClose = Pattern.compile(
                "<" + tagName + ">([^\r\n<]+)",
                Pattern.CASE_INSENSITIVE
        );
        m = withoutClose.matcher(content);
        if (m.find()) return m.group(1).trim();

        return null;
    }

    /**
     * Converte a string de data no formato OFX (YYYYMMDDHHMMSS[offset:TZ] ou YYYYMMDD) para LocalDate.
     */
    private LocalDate ofxParseDate(String dtPosted) {
        // Mantém apenas os dígitos iniciais
        String digits = dtPosted.replaceAll("[^0-9].*$", "").replaceAll("[^0-9]", "");
        if (digits.length() < 8) {
            throw new IllegalArgumentException("Data OFX inválida: " + dtPosted);
        }
        int year  = Integer.parseInt(digits.substring(0, 4));
        int month = Integer.parseInt(digits.substring(4, 6));
        int day   = Integer.parseInt(digits.substring(6, 8));
        return LocalDate.of(year, month, day);
    }

    // =====================================================================
    // MEMO PARSER — interpretação semântica do campo MEMO do OFX
    // Suporta Nubank, Inter, C6, Bradesco, Itaú e outros bancos brasileiros
    // =====================================================================

    /** Resultado da análise semântica do campo MEMO. */
    private record MemoParseResult(Tipo tipo, TipoMovimento tipoMovimento, String titulo) {}

    /**
     * Analisa o campo MEMO do OFX para inferir {@link Tipo}, {@link TipoMovimento} e
     * título de forma inteligente, eliminando a dependência exclusiva do campo TRNTYPE.
     *
     * <p>Padrões reconhecidos (Nubank e bancos BR em geral):
     * <ul>
     *   <li>"Compra no débito/crédito - NOME"           → Gasto / Debito ou Credito</li>
     *   <li>"Pagamento de boleto efetuado - NOME"        → Gasto / Boleto</li>
     *   <li>"Pagamento de fatura"                        → Gasto / Boleto</li>
     *   <li>"Transferência enviada pelo Pix - NOME - ..."→ Transferencia / Pix</li>
     *   <li>"Transferência recebida pelo Pix - NOME - ..."→ Recebimento / Pix</li>
     *   <li>"Aplicação RDB/CDB/Poupança"                → Poupanca / Debito</li>
     *   <li>"Resgate RDB/CDB"                            → Recebimento / Credito</li>
     *   <li>"Resgate/Pagamento de empréstimo"            → Gasto / Debito</li>
     *   <li>"Saque"                                      → Gasto / Dinheiro</li>
     *   <li>"Estorno / Devolução"                        → Recebimento / Credito</li>
     *   <li>"TED/DOC enviado"                            → Transferencia / Debito</li>
     *   <li>"TED/DOC recebido"                           → Recebimento / Credito</li>
     * </ul>
     */
    private MemoParseResult parseMemoOFX(String memo, String trnType, double amount) {
        if (memo == null || memo.isBlank()) {
            return new MemoParseResult(
                    ofxMapTrnType(trnType, amount),
                    amount < 0 ? TipoMovimento.Debito : TipoMovimento.Credito,
                    "Registro OFX");
        }

        String lower = memo.toLowerCase();

        // ── Pix enviado → Transferência ────────────────────────────────────────
        if (lower.contains("enviada pelo pix") || lower.contains("enviado pelo pix")
                || lower.contains("transferência enviada") || lower.contains("transferencia enviada")) {
            return new MemoParseResult(Tipo.Transferencia, TipoMovimento.Pix,
                    sanitizeMerchant(extrairNomeAposDash(memo)));
        }

        // ── Pix recebido → Recebimento ─────────────────────────────────────────
        if (lower.contains("recebida pelo pix") || lower.contains("recebido pelo pix")
                || lower.contains("transferência recebida") || lower.contains("transferencia recebida")) {
            return new MemoParseResult(Tipo.Recebimento, TipoMovimento.Pix,
                    sanitizeMerchant(extrairNomeAposDash(memo)));
        }

        // ── Compra no débito ────────────────────────────────────────────────────
        if (lower.startsWith("compra no déb") || lower.startsWith("compra no deb")) {
            return new MemoParseResult(Tipo.Gasto, TipoMovimento.Debito,
                    sanitizeMerchant(extrairNomeAposDash(memo)));
        }

        // ── Compra no crédito ────────────────────────────────────────────────────
        if (lower.startsWith("compra no cré") || lower.startsWith("compra no cre")) {
            return new MemoParseResult(Tipo.Gasto, TipoMovimento.Credito,
                    sanitizeMerchant(extrairNomeAposDash(memo)));
        }

        // ── Compra (sem especificar débito/crédito) ─────────────────────────────
        if (lower.startsWith("compra ") || lower.startsWith("compra-")) {
            TipoMovimento mv = detectTipoMovimento(lower, amount);
            return new MemoParseResult(Tipo.Gasto, mv,
                    sanitizeMerchant(extrairNomeAposDash(memo)));
        }

        // ── Pagamento de boleto ─────────────────────────────────────────────────
        if (lower.startsWith("pagamento de boleto")) {
            return new MemoParseResult(Tipo.Gasto, TipoMovimento.Boleto,
                    sanitizeMerchant(extrairNomeAposDash(memo)));
        }

        // ── Pagamento de fatura ─────────────────────────────────────────────────
        if (lower.startsWith("pagamento de fatura")) {
            return new MemoParseResult(Tipo.Gasto, TipoMovimento.Boleto, "Pagamento de Fatura");
        }

        // ── Pagamento / Resgate de empréstimo ───────────────────────────────────
        if (lower.startsWith("pagamento de empr") || lower.startsWith("resgate de empr")
                || lower.startsWith("parcela de empr") || lower.startsWith("amortização")) {
            return new MemoParseResult(Tipo.Gasto, TipoMovimento.Debito, "Pagamento de Empréstimo");
        }

        // ── Aplicação (RDB, CDB, Poupança) → Poupança ──────────────────────────
        if (lower.startsWith("aplica\u00e7\u00e3o") || lower.startsWith("aplicacao")
                || lower.startsWith("investimento")) {
            return new MemoParseResult(Tipo.Poupanca, TipoMovimento.Debito,
                    sanitizeMerchant(memo));
        }

        // ── Resgate de RDB/CDB/investimento → Recebimento ──────────────────────
        if (lower.startsWith("resgate")
                && (lower.contains("rdb") || lower.contains("cdb")
                    || lower.contains("poupan\u00e7a") || lower.contains("poupanca")
                    || lower.contains("investimento") || lower.contains("rdl"))) {
            return new MemoParseResult(Tipo.Recebimento, TipoMovimento.Credito,
                    sanitizeMerchant(memo));
        }

        // ── Saque ───────────────────────────────────────────────────────────────
        if (lower.startsWith("saque")) {
            return new MemoParseResult(Tipo.Gasto, TipoMovimento.Dinheiro, "Saque");
        }

        // ── TED / DOC enviado → Transferência ──────────────────────────────────
        if (lower.contains("ted enviado") || lower.contains("doc enviado")
                || lower.contains("transfer\u00eancia ted") || lower.contains("transferencia ted")) {
            return new MemoParseResult(Tipo.Transferencia, TipoMovimento.Debito,
                    sanitizeMerchant(extrairNomeAposDash(memo)));
        }

        // ── TED / DOC recebido → Recebimento ───────────────────────────────────
        if (lower.contains("ted recebido") || lower.contains("doc recebido")) {
            return new MemoParseResult(Tipo.Recebimento, TipoMovimento.Credito,
                    sanitizeMerchant(extrairNomeAposDash(memo)));
        }

        // ── Estorno / Devolução → Recebimento ──────────────────────────────────
        if (lower.startsWith("estorno") || lower.startsWith("devolu\u00e7\u00e3o")
                || lower.startsWith("devolucao") || lower.startsWith("reembolso")) {
            return new MemoParseResult(Tipo.Recebimento, TipoMovimento.Credito,
                    "Estorno: " + sanitizeMerchant(extrairNomeAposDash(memo)));
        }

        // ── Rendimento / Juros ──────────────────────────────────────────────────
        if (lower.startsWith("rendimento") || lower.startsWith("juros a receber")
                || lower.startsWith("creditamento")) {
            return new MemoParseResult(Tipo.Recebimento, TipoMovimento.Credito,
                    sanitizeMerchant(memo));
        }

        // ── Fallback: usa TRNTYPE + detecta movimento por palavras-chave ────────
        return new MemoParseResult(
                ofxMapTrnType(trnType, amount),
                detectTipoMovimento(memo, amount),
                sanitizeMerchant(extrairNomeAposDash(memo)));
    }

    /**
     * Extrai o nome/estabelecimento da segunda parte do MEMO (após o primeiro " - ").
     *
     * <p>Exemplos:
     * <ul>
     *   <li>"Compra no débito - LOJA EXEMPLO" → "LOJA EXEMPLO"</li>
     *   <li>"Transferência enviada pelo Pix - João Silva - •••.753-•• - BCO C6..." → "João Silva"</li>
     *   <li>"Pagamento de boleto efetuado - EDP SAO PAULO" → "EDP SAO PAULO"</li>
     * </ul>
     */
    private String extrairNomeAposDash(String memo) {
        if (memo == null || memo.isBlank()) return memo;
        // Divide em até 3 partes pelo separador " - " (com espaços opcionais)
        String[] partes = memo.split("\\s*-\\s*", 3);
        if (partes.length >= 2) {
            String candidato = partes[1].trim();
            // Se o segundo campo parece ser um CNPJ/CPF puro (só dígitos/pontos/barras),
            // tenta o terceiro campo
            if (candidato.matches("[\\d./-]+") && partes.length >= 3) {
                candidato = partes[2].split("\\s*-\\s*")[0].trim();
            }
            return candidato;
        }
        return partes[0].trim();
    }

    /**
     * Mapeia o TRNTYPE do OFX e o sinal do valor para o enum {@link Tipo}.
     */
    private Tipo ofxMapTrnType(String trnType, double amount) {
        if (trnType != null) {
            return switch (trnType.toUpperCase().trim()) {
                case "CREDIT", "INT", "DIV", "DIRECTDEP" -> Tipo.Recebimento;
                case "DEBIT", "CHECK", "PAYMENT", "ATM", "FEE", "SRVCHG" -> Tipo.Gasto;
                case "XFER" -> Tipo.Transferencia;
                default -> amount >= 0 ? Tipo.Recebimento : Tipo.Gasto;
            };
        }
        return amount >= 0 ? Tipo.Recebimento : Tipo.Gasto;
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private Usuario getUsuario(UUID userId) {
        return usuarioRepository.findById(userId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Usuário de id: " + userId + " não encontrado."));
    }

    private EventoFinanceiro buildEventoFinanceiro(UUID userId, Map<String, Object> map) {
        EventoFinanceiro financeiro = new EventoFinanceiro();
        financeiro.setUsuario(getUsuario(userId));
        financeiro.setTipo(Tipo.valueOf((String) map.get("tipo")));
        financeiro.setValor(((Number) map.get("valor")).doubleValue());
        financeiro.setDescricao((String) map.get("descricao"));
        financeiro.setDataEvento(LocalDate.parse((String) map.get("data_evento")));
        return financeiro;
    }

    @SuppressWarnings("unchecked")
    private EventoDetalhe buildDetalheFromJson(UUID userId, Map<String, Object> gastoMap) {
        EventoDetalhe detalhe = new EventoDetalhe();
        if (gastoMap == null) {
            detalhe.setTituloGasto("Registro Importado");
            detalhe.setCategoriaUsuario(new ArrayList<>());
            return detalhe;
        }
        String titulo = (String) gastoMap.get("titulo_gasto");
        detalhe.setTituloGasto(titulo != null && !titulo.isBlank() ? titulo : "Registro Importado");

        List<CategoriaUsuario> categorias = new ArrayList<>();
        List<Map<String, Object>> catList = (List<Map<String, Object>>) gastoMap.get("categorias");
        if (catList != null) {
            for (Map<String, Object> cat : catList) {
                // Prioriza resolução/criação por título (compatível entre usuários diferentes);
                // cai para o ID antigo apenas se o título não estiver presente (exportações antigas).
                String catTitulo = (String) cat.get("titulo");
                CategoriaUsuario cu = catTitulo != null
                        ? resolveOuCriarCategoriaUsuario(userId, catTitulo)
                        : null;
                if (cu == null && cat.get("id") != null) {
                    cu = new CategoriaUsuario();
                    cu.setId(((Number) cat.get("id")).intValue());
                }
                if (cu != null) categorias.add(cu);
            }
        }
        detalhe.setCategoriaUsuario(categorias);
        return detalhe;
    }

    private RegistroResponseDto persistirRegistro(EventoFinanceiro financeiro,
                                                   List<EventoInstituicao> instituicoes,
                                                   EventoDetalhe detalhe) {
        return persistirRegistro(financeiro, instituicoes, detalhe, null);
    }

    /**
     * Persiste o registro e, se fornecida, vincula cada EventoInstituicao recém-criada
     * à sua respectiva RecorrenciaFinanceira (mesma ordem/índice da lista de instituições).
     */
    private RegistroResponseDto persistirRegistro(EventoFinanceiro financeiro,
                                                   List<EventoInstituicao> instituicoes,
                                                   EventoDetalhe detalhe,
                                                   List<RecorrenciaFinanceira> recorrenciaPorInstituicao) {
        Registro registro = registroService.createEventoFinanceiro(financeiro, instituicoes, detalhe);
        EventoFinanceiro ef = registro.getEventosFinanceiros().getFirst();
        List<EventoInstituicao> eis = registro.getInstituicoesPorEvento().getOrDefault(ef, List.of());
        EventoDetalhe ed = registro.getDetalhePorEvento().get(ef);
        if (ed == null) return null;

        if (recorrenciaPorInstituicao != null) {
            for (int i = 0; i < eis.size() && i < recorrenciaPorInstituicao.size(); i++) {
                RecorrenciaFinanceira rec = recorrenciaPorInstituicao.get(i);
                if (rec != null) {
                    EventoInstituicao ei = eis.get(i);
                    ei.setRecorrenciaFinanceira(rec);
                    eventoInstituicaoRepository.save(ei);
                }
            }
        }

        return RegistrosMapper.toResponse(ef, eis, ed);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> "";
            default -> cell.getStringCellValue();
        };
    }

    // =====================================================================
    // CSV EXTRATO BANCARIO IMPORT
    // =====================================================================

    /**
     * Importa transacoes a partir de um extrato bancario em CSV.
     * Suporta o formato do Banco Inter e outros bancos brasileiros com layout similar:
     * cabecalho com metadados, seguido de linha "Data Lancamento;Descricao;Valor;Saldo".
     * Separadores aceitos: ponto-e-virgula ou virgula.
     * Datas: DD/MM/YYYY ou YYYY-MM-DD. Valores no padrao brasileiro (-1.234,56).
     *
     * @param bancoNome nome (parcial) do banco para vincular a instituicao cadastrada
     */
    public ImportResultDto importFromBankStatementCsv(UUID userId, byte[] content, String bancoNome) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        try {
            String csvContent = new String(content, StandardCharsets.UTF_8);
            if (csvContent.startsWith("\uFEFF")) csvContent = csvContent.substring(1);

            String[] lines = csvContent.split("\r?\n", -1);

            // Localiza cabecalho de dados (linha que comeca com "Data" e contem separador)
            int dataStartIndex = -1;
            for (int i = 0; i < lines.length; i++) {
                String lower = lines[i].trim().toLowerCase();
                if (lower.startsWith("data") && (lower.contains(";") || lower.contains(","))) {
                    dataStartIndex = i + 1;
                    break;
                }
            }
            if (dataStartIndex < 0) {
                erros.add("Formato CSV nao reconhecido: linha de cabecalho com 'Data' nao encontrada.");
                return new ImportResultDto(0, importados, erros);
            }

            InstituicaoUsuario matchedInstituicao = resolveInstituicao(userId, bancoNome);

            for (int i = dataStartIndex; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isBlank()) continue;

                String sep = line.contains(";") ? ";" : ",";
                String[] parts = line.split(sep, -1);
                if (parts.length < 3) continue;

                String dateStr   = parts[0].trim();
                String descricao = parts[1].trim();
                String valorStr  = parts[2].trim();

                if (!dateStr.matches("\\d{2}/\\d{2}/\\d{4}") && !dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) continue;

                try {
                    LocalDate data   = parseBrDate(dateStr);
                    double amount    = Double.parseDouble(valorStr.replaceAll("\\.", "").replace(",", "."));
                    double absAmount = Math.abs(amount);
                    Tipo tipo        = amount >= 0 ? Tipo.Recebimento : Tipo.Gasto;
                    TipoMovimento mov = detectTipoMovimento(descricao, amount);

                    EventoFinanceiro financeiro = new EventoFinanceiro();
                    financeiro.setUsuario(getUsuario(userId));
                    financeiro.setTipo(tipo);
                    financeiro.setValor(absAmount);
                    financeiro.setDescricao(descricao.length() > 500 ? descricao.substring(0, 500) : descricao);
                    financeiro.setDataEvento(data);

                    List<EventoInstituicao> instituicoes = new ArrayList<>();
                    if (matchedInstituicao != null) {
                        EventoInstituicao ei = new EventoInstituicao();
                        ei.setInstituicaoUsuario(matchedInstituicao);
                        ei.setTipoMovimento(mov);
                        ei.setValor(absAmount);
                        ei.setParcelas(1);
                        instituicoes.add(ei);
                    }

                    EventoDetalhe detalhe = new EventoDetalhe();
                    detalhe.setTituloGasto(extractBankTitulo(descricao));
                    detalhe.setCategoriaUsuario(new ArrayList<>());

                    RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe);
                    if (dto != null) importados.add(dto);

                } catch (Exception e) {
                    erros.add("Linha " + (i + 1) + " [" + dateStr + "]: " + e.getMessage());
                }
            }

            if (importados.isEmpty() && erros.isEmpty()) {
                erros.add("Nenhum registro encontrado no CSV.");
            }
        } catch (Exception e) {
            erros.add("Erro ao processar CSV bancario: " + e.getMessage());
        }

        return new ImportResultDto(importados.size(), importados, erros);
    }

    // =====================================================================
    // PDF EXTRATO BANCARIO IMPORT
    // =====================================================================

    /**
     * Importa transacoes a partir de um extrato bancario em PDF.
     * Detecta linhas no formato DD/MM/YYYY descricao valor[,saldo] tipico de bancos brasileiros.
     * Nota: PDFs nao sao padronizados entre bancos; prefira OFX ou CSV quando disponivel.
     */
    public ImportResultDto importFromBankStatementPdf(UUID userId, byte[] content, String bancoNome) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        try {
            InstituicaoUsuario matchedInstituicao = resolveInstituicao(userId, bancoNome);
            int currentYear = LocalDate.now().getYear();

            // Padrao: DD/MM/YYYY descricao -1.234,56 [saldo]
            Pattern fullLine = Pattern.compile(
                "^(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+([-]?\\d{1,3}(?:\\.\\d{3})*,\\d{2})" +
                "(?:\\s+[-]?[\\d,.]+)?\\s*$");
            // Padrao sem ano: DD/MM
            Pattern shortLine = Pattern.compile(
                "^(\\d{2}/\\d{2})\\s+(.+?)\\s+([-]?\\d{1,3}(?:\\.\\d{3})*,\\d{2})" +
                "(?:\\s+[-]?[\\d,.]+)?\\s*$");

            try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(content)))) {
                for (int page = 1; page <= pdfDoc.getNumberOfPages(); page++) {
                    String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page));
                    for (String rawLine : pageText.split("\n")) {
                        String line = rawLine.trim();
                        if (line.isBlank()) continue;

                        Matcher m = fullLine.matcher(line);
                        if (m.find()) {
                            processarLinhaBancaria(userId, m.group(1), m.group(2), m.group(3),
                                    matchedInstituicao, importados, erros, "PDF p." + page);
                            continue;
                        }
                        Matcher ms = shortLine.matcher(line);
                        if (ms.find()) {
                            processarLinhaBancaria(userId, ms.group(1) + "/" + currentYear,
                                    ms.group(2), ms.group(3),
                                    matchedInstituicao, importados, erros, "PDF p." + page);
                        }
                    }
                }
            }

            if (importados.isEmpty() && erros.isEmpty()) {
                erros.add("Nenhum registro reconhecido no PDF bancario. Use OFX ou CSV para maior confiabilidade.");
            }
        } catch (Exception e) {
            erros.add("Erro ao processar PDF bancario: " + e.getMessage());
        }

        return new ImportResultDto(importados.size(), importados, erros);
    }

    private void processarLinhaBancaria(UUID userId, String dateStr, String descricao, String valorStr,
            InstituicaoUsuario matchedInstituicao,
            List<RegistroResponseDto> importados, List<String> erros, String ctx) {
        try {
            LocalDate data   = parseBrDate(dateStr);
            double amount    = Double.parseDouble(valorStr.replaceAll("\\.", "").replace(",", "."));
            double absAmount = Math.abs(amount);
            Tipo tipo        = amount >= 0 ? Tipo.Recebimento : Tipo.Gasto;
            TipoMovimento mov = detectTipoMovimento(descricao, amount);

            EventoFinanceiro financeiro = new EventoFinanceiro();
            financeiro.setUsuario(getUsuario(userId));
            financeiro.setTipo(tipo);
            financeiro.setValor(absAmount);
            financeiro.setDescricao(descricao.length() > 500 ? descricao.substring(0, 500) : descricao);
            financeiro.setDataEvento(data);

            List<EventoInstituicao> instituicoes = new ArrayList<>();
            if (matchedInstituicao != null) {
                EventoInstituicao ei = new EventoInstituicao();
                ei.setInstituicaoUsuario(matchedInstituicao);
                ei.setTipoMovimento(mov);
                ei.setValor(absAmount);
                ei.setParcelas(1);
                instituicoes.add(ei);
            }

            EventoDetalhe detalhe = new EventoDetalhe();
            detalhe.setTituloGasto(extractBankTitulo(descricao));
            detalhe.setCategoriaUsuario(new ArrayList<>());

            RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe);
            if (dto != null) importados.add(dto);
        } catch (Exception e) {
            erros.add(ctx + " [" + dateStr + "]: " + e.getMessage());
        }
    }

    // =====================================================================
    // HELPERS COMPARTILHADOS — extrato bancario (CSV / OFX / PDF)
    // =====================================================================

    /**
     * Busca a InstituicaoUsuario ativa cujo nome corresponda (contains) a qualquer dos candidatos.
     * Util para OFX (usa tag ORG), CSV e PDF (usa parametro bancoNome).
     */
    private InstituicaoUsuario resolveInstituicao(UUID userId, String... candidateNames) {
        List<InstituicaoUsuario> userInst =
                instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
        for (String candidate : candidateNames) {
            if (candidate == null || candidate.isBlank()) continue;
            final String lower = candidate.toLowerCase();
            for (InstituicaoUsuario iu : userInst) {
                String nome = iu.getInstituicao().getNome().toLowerCase();
                if (nome.contains(lower) || lower.contains(nome)) return iu;
            }
        }
        return null;
    }

    /** Converte DD/MM/YYYY (formato BR) ou YYYY-MM-DD (ISO) para LocalDate. */
    private LocalDate parseBrDate(String dateStr) {
        if (dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
            String[] p = dateStr.split("/");
            return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        }
        return LocalDate.parse(dateStr);
    }

    /**
     * Infere TipoMovimento a partir da descricao e sinal do valor.
     * Reconhece padroes do Inter, Nubank, Itau, Bradesco etc.
     */
    private TipoMovimento detectTipoMovimento(String desc, double amount) {
        if (desc != null) {
            String lower = desc.toLowerCase();
            if (lower.contains("pix"))                                  return TipoMovimento.Pix;
            if (lower.contains("debito") || lower.contains("d\u00e9bito")) return TipoMovimento.Debito;
            if (lower.contains("credito") || lower.contains("cr\u00e9dito")) return TipoMovimento.Credito;
            if (lower.contains("boleto"))                               return TipoMovimento.Boleto;
            if (lower.contains("voucher"))                              return TipoMovimento.Voucher;
            if (lower.contains("dinheiro"))                             return TipoMovimento.Dinheiro;
        }
        return amount < 0 ? TipoMovimento.Debito : TipoMovimento.Credito;
    }

    /**
     * Extrai titulo curto (max 50 chars) de descricoes bancarias brasileiras.
     * Reconhece os padroes do Inter:
     *   "Pix enviado: \"Cp :CNPJ-NOME\""          -> Nome
     *   "Compra no debito: \"No estabelecimento LOJA CIDADE PAIS\""  -> Loja
     */
    private String extractBankTitulo(String descricao) {
        if (descricao == null || descricao.isBlank()) return "Extrato Importado";

        // Extrai conteudo entre aspas
        Matcher qm = Pattern.compile("\"([^\"]+)\"").matcher(descricao);
        String inner = qm.find() ? qm.group(1).trim() : descricao;

        // Padrao "Cp :XXXXXXXX-NOME"
        Matcher cpM = Pattern.compile("Cp\\s*:\\s*\\d+-(.+)", Pattern.CASE_INSENSITIVE).matcher(inner);
        if (cpM.find()) return sanitizeMerchant(cpM.group(1));

        // Padrao "No estabelecimento LOJA CIDADE PAIS" (Inter/Visa/Mastercard)
        Matcher estM = Pattern.compile("No\\s+estabelecimento\\s+(.+)", Pattern.CASE_INSENSITIVE).matcher(inner);
        if (estM.find()) {
            // OFX usa espacos multiplos para separar cidade; CSV usa espaco simples
            String[] parts = estM.group(1).split("\\s{2,}");
            return sanitizeMerchant(parts[0]);
        }

        return sanitizeMerchant(inner);
    }

    /** Remove CPF/CNPJ no final, normaliza espacos, aplica Title Case e trunca em 50 chars. */
    private String sanitizeMerchant(String name) {
        if (name == null || name.isBlank()) return "Extrato Importado";
        name = name.replaceAll("\\s+\\d{8,14}$", "").trim();
        name = name.replaceAll("\\s+", " ").trim();
        // Title Case
        String[] words = name.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append(" ");
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        name = sb.toString();
        return name.length() > 50 ? name.substring(0, 50) : name;
    }

    /** Sobrecarga de importFromOfx aceitando nome do banco para fallback de instituicao. */
    public ImportResultDto importFromOfxWithBank(UUID userId, byte[] content, String bancoNome) {
        // Delega para o metodo principal; a logica de resolucao usa ORG + bancoNome via resolveInstituicao
        return importFromOfx(userId, content, bancoNome);
    }

    /**
     * importFromOfx com suporte a bancoNome como fallback quando a tag ORG nao casa.
     * Mantido aqui como sobrecarga privada interna; o controller usa importFromOfxWithBank.
     */
    private ImportResultDto importFromOfx(UUID userId, byte[] content, String bancoNome) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        try {
            String rawHeader = new String(content, 0, Math.min(content.length, 512), StandardCharsets.ISO_8859_1).toUpperCase();
            java.nio.charset.Charset charset = rawHeader.contains("CHARSET:1252") || rawHeader.contains("CHARSET:ISO")
                    ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8;
            String ofxContent = new String(content, charset);

            int ofxStart = ofxContent.indexOf("<OFX>");
            if (ofxStart < 0) ofxStart = ofxContent.toUpperCase().indexOf("<OFX>");
            if (ofxStart < 0) {
                erros.add("Arquivo OFX invalido: tag <OFX> nao encontrada.");
                return new ImportResultDto(0, importados, erros);
            }
            String body = ofxContent.substring(ofxStart);

            // Resolve instituicao: tag <ORG> tem prioridade, bancoNome e fallback
            String orgName = ofxExtractTagValue(body, "ORG");
            InstituicaoUsuario matchedInstituicao = resolveInstituicao(userId, orgName, bancoNome);

            Pattern trnPattern = Pattern.compile("<STMTTRN>(.+?)</STMTTRN>",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher trnMatcher = trnPattern.matcher(body);
            int transacaoIndex = 0;

            while (trnMatcher.find()) {
                transacaoIndex++;
                String trn = trnMatcher.group(1);
                String fitId = ofxExtractTagValue(trn, "FITID");
                String id = fitId != null ? fitId : "transacao-" + transacaoIndex;

                try {
                    String trnType  = ofxExtractTagValue(trn, "TRNTYPE");
                    String dtPosted = ofxExtractTagValue(trn, "DTPOSTED");
                    String amtStr   = ofxExtractTagValue(trn, "TRNAMT");
                    String name     = ofxExtractTagValue(trn, "NAME");
                    String memo     = ofxExtractTagValue(trn, "MEMO");

                    if (amtStr == null || dtPosted == null) {
                        erros.add("Transacao '" + id + "': TRNAMT ou DTPOSTED ausentes.");
                        continue;
                    }

                    double amount    = Double.parseDouble(amtStr.replace(",", "."));
                    double absAmount = Math.abs(amount);
                    LocalDate data   = ofxParseDate(dtPosted);

                    // Analisa MEMO para determinar tipo, movimento e título de forma inteligente
                    String memoText = memo != null && !memo.isBlank() ? memo
                            : (name != null ? name : "");
                    MemoParseResult parsed = parseMemoOFX(memoText, trnType, amount);

                    EventoFinanceiro financeiro = new EventoFinanceiro();
                    financeiro.setUsuario(getUsuario(userId));
                    financeiro.setTipo(parsed.tipo());
                    financeiro.setValor(absAmount);
                    String descricao = memoText.isBlank() ? "Importado via OFX" : memoText;
                    financeiro.setDescricao(descricao.length() > 500 ? descricao.substring(0, 500) : descricao);
                    financeiro.setDataEvento(data);

                    List<EventoInstituicao> instituicoes = new ArrayList<>();
                    if (matchedInstituicao != null) {
                        EventoInstituicao ei = new EventoInstituicao();
                        ei.setInstituicaoUsuario(matchedInstituicao);
                        ei.setTipoMovimento(parsed.tipoMovimento());
                        ei.setValor(absAmount);
                        ei.setParcelas(1);
                        instituicoes.add(ei);
                    }

                    EventoDetalhe detalhe = new EventoDetalhe();
                    String tituloFinal = parsed.titulo();
                    if ((tituloFinal == null || tituloFinal.equals("Registro OFX"))
                            && name != null && !name.isBlank()) {
                        tituloFinal = sanitizeMerchant(name);
                    }
                    detalhe.setTituloGasto(tituloFinal != null ? tituloFinal : "Registro OFX");
                    detalhe.setCategoriaUsuario(new ArrayList<>());

                    RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe);
                    if (dto != null) importados.add(dto);

                } catch (Exception e) {
                    erros.add("Transacao '" + id + "': " + e.getMessage());
                }
            }
            if (transacaoIndex == 0) erros.add("Nenhuma transacao encontrada no OFX.");

        } catch (Exception e) {
            erros.add("Erro ao processar OFX: " + e.getMessage());
        }
        return new ImportResultDto(importados.size(), importados, erros);
    }
}


