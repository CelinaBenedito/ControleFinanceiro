package controle.api.back_end.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.categoria.CategoriaUsuarioRepository;
import controle.api.back_end.repository.configuracoes.ConfiguracoesRepository;
import controle.api.back_end.repository.configuracoes.LimitePorCategoriaRepository;
import controle.api.back_end.repository.configuracoes.LimitePorInstituicaoRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Serviço responsável pela exportação dos dados financeiros do usuário.
 * Formatos suportados: JSON, SQL, Excel (.xlsx) e PDF.
 */
@Service
public class RegistroExportacaoService {

    private final UsuarioRepository usuarioRepository;
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoDetalheRepository eventoDetalheRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final ConfiguracoesRepository configuracoesRepository;
    private final LimitePorInstituicaoRepository limitePorInstituicaoRepository;
    private final LimitePorCategoriaRepository limitePorCategoriaRepository;

    public RegistroExportacaoService(UsuarioRepository usuarioRepository,
                                     EventoFinanceiroRepository eventoFinanceiroRepository,
                                     EventoInstituicaoRepository eventoInstituicaoRepository,
                                     EventoDetalheRepository eventoDetalheRepository,
                                     CategoriaUsuarioRepository categoriaUsuarioRepository,
                                     InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                                     ConfiguracoesRepository configuracoesRepository,
                                     LimitePorInstituicaoRepository limitePorInstituicaoRepository,
                                     LimitePorCategoriaRepository limitePorCategoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoDetalheRepository = eventoDetalheRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.configuracoesRepository = configuracoesRepository;
        this.limitePorInstituicaoRepository = limitePorInstituicaoRepository;
        this.limitePorCategoriaRepository = limitePorCategoriaRepository;
    }

    // =========================================================================
    // API PÚBLICA
    // =========================================================================

    /**
     * Gera o arquivo de exportação no formato solicitado.
     *
     * @param userId ID do usuário dono dos dados.
     * @param tipo   "json" | "sql" | "excel" | "pdf"
     * @return resultado contendo o conteúdo, o nome sugerido e o content-type do arquivo.
     */
    @Transactional(readOnly = true)
    public ExportacaoResultado exportar(UUID userId, String tipo) {
        Usuario usuario = buscarUsuarioOuErro(userId);
        return switch (tipo.toLowerCase()) {
            case "json"  -> exportarJson(usuario);
            case "sql"   -> exportarSql(usuario);
            case "excel" -> exportarExcel(usuario);
            case "pdf"   -> exportarPdf(usuario);
            default -> throw new IllegalArgumentException("Formato não suportado: " + tipo);
        };
    }

    // =========================================================================
    // EXPORTAÇÃO JSON
    // =========================================================================

    private ExportacaoResultado exportarJson(Usuario usuario) {
        Map<String, Object> jsonMap = new LinkedHashMap<>();

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", usuario.getId().toString());
        userInfo.put("nome", usuario.getNome());
        userInfo.put("sobrenome", usuario.getSobrenome());
        userInfo.put("data_nascimento", usuario.getDataNascimento().toString());
        userInfo.put("sexo", usuario.getSexo().toString());
        userInfo.put("email", usuario.getEmail());
        jsonMap.put("usuario", userInfo);

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(usuario.getId());
        List<Map<String, Object>> eventosList = new ArrayList<>();

        for (EventoFinanceiro evento : eventos) {
            Map<String, Object> eventoMap = new LinkedHashMap<>();
            eventoMap.put("id", evento.getId().toString());
            eventoMap.put("tipo", evento.getTipo().toString());
            eventoMap.put("valor", evento.getValor());
            eventoMap.put("descricao", evento.getDescricao());
            eventoMap.put("data_evento", evento.getDataEvento().toString());
            eventoMap.put("data_registro", evento.getDataRegistro().toString());

            List<EventoInstituicao> instituicoes = eventoInstituicaoRepository
                    .findEventoInstituicaoByEventoFinanceiro_Id(evento.getId());
            List<Map<String, Object>> instList = new ArrayList<>();
            for (EventoInstituicao ei : instituicoes) {
                Map<String, Object> instMap = new LinkedHashMap<>();
                instMap.put("id", ei.getId().toString());
                instMap.put("instituicao_usuario_id", ei.getInstituicaoUsuario().getId());
                instMap.put("tipo_movimento", ei.getTipoMovimento().toString());
                instMap.put("valor", ei.getValor());
                instMap.put("parcelas", ei.getParcelas());
                instList.add(instMap);
            }
            eventoMap.put("instituicoes", instList);

            EventoDetalhe detalhe = eventoDetalheRepository.findGastoDetalheByEventoFinanceiro(evento);
            if (detalhe != null) {
                Map<String, Object> detalheMap = new LinkedHashMap<>();
                detalheMap.put("id", detalhe.getId().toString());
                detalheMap.put("titulo_gasto", detalhe.getTituloGasto());
                List<Map<String, Object>> categoriasList = new ArrayList<>();
                for (CategoriaUsuario cat : detalhe.getCategoriaUsuario()) {
                    Map<String, Object> catMap = new LinkedHashMap<>();
                    catMap.put("id", cat.getId());
                    catMap.put("categoria", cat.getCategoria().getTitulo());
                    categoriasList.add(catMap);
                }
                detalheMap.put("categorias", categoriasList);
                eventoMap.put("gasto_detalhe", detalheMap);
            }
            eventosList.add(eventoMap);
        }
        jsonMap.put("eventos_financeiros", eventosList);

        try {
            String conteudo = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(jsonMap);
            return new ExportacaoResultado(conteudo.getBytes(),
                    "registros_%s_%s.json".formatted(usuario.getNome(), LocalDate.now()),
                    "application/json");
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao gerar exportação JSON.", e);
        }
    }

    // =========================================================================
    // EXPORTAÇÃO SQL
    // =========================================================================

    private ExportacaoResultado exportarSql(Usuario usuario) {
        UUID userId = usuario.getId();
        StringBuilder sql = new StringBuilder();

        sql.append("-- Usuário\n")
           .append("INSERT INTO usuario (id, nome, sobrenome, data_nascimento, sexo, imagem, email, senha) VALUES (")
           .append("'").append(userId).append("', '").append(usuario.getNome()).append("', '")
           .append(usuario.getSobrenome()).append("', '").append(usuario.getDataNascimento()).append("', '")
           .append(usuario.getSexo()).append("', ")
           .append(usuario.getImagem() == null ? "null" : "'" + usuario.getImagem() + "'").append(", '")
           .append(usuario.getEmail()).append("', '").append(usuario.getSenha()).append("');\n\n");

        List<InstituicaoUsuario> instUsuarios = instituicaoUsuarioRepository
                .findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
        sql.append("-- Instituições do usuário\n");
        for (InstituicaoUsuario iu : instUsuarios) {
            sql.append("INSERT INTO instituicao_usuario (id, usuario_id, instituicao_id, is_ativo) VALUES (")
               .append(iu.getId()).append(", '").append(userId).append("', ")
               .append(iu.getInstituicao().getId()).append(", ").append(iu.getIsAtivo()).append(");\n");
        }
        sql.append("\n");

        List<CategoriaUsuario> catUsuarios = categoriaUsuarioRepository.findAllByUsuario_Id(userId);
        sql.append("-- Categorias do usuário\n");
        for (CategoriaUsuario cu : catUsuarios) {
            sql.append("INSERT INTO categoria_usuario (id, usuario_id, categoria_id, is_ativo) VALUES (")
               .append(cu.getId()).append(", '").append(userId).append("', ")
               .append(cu.getCategoria().getId()).append(", ").append(cu.getAtivo()).append(");\n");
        }
        sql.append("\n");

        List<Configuracoes> configs = configuracoesRepository.findAllByUsuario_Id(userId);
        sql.append("-- Configurações do usuário\n");
        for (Configuracoes conf : configs) {
            sql.append("INSERT INTO configuracoes (id, usuario_id, inicio_mes_fiscal, ultima_atualizacao, limite_desejado_mensal) VALUES (")
               .append("'").append(conf.getId()).append("', '").append(userId).append("', ")
               .append(conf.getInicioMesFiscal()).append(", '").append(conf.getUltimaAtualizacao())
               .append("', ").append(conf.getLimiteDesejadoMensal()).append(");\n");
        }
        sql.append("\n");

        List<LimitePorInstituicao> limitesInst = limitePorInstituicaoRepository
                .findByInstituicaoUsuario_Usuario_Id(userId);
        sql.append("-- Limites por instituição\n");
        for (LimitePorInstituicao li : limitesInst) {
            sql.append("INSERT INTO limite_por_instituicao (id, instituicao_usuario_id, limite_desejado, configuracoes_id) VALUES (")
               .append("'").append(li.getId()).append("', ")
               .append(li.getInstituicaoUsuario().getId()).append(", ")
               .append(li.getLimiteDesejado()).append(", '").append(li.getConfiguracoes().getId()).append("');\n");
        }
        sql.append("\n");

        List<LimitePorCategoria> limitesCat = limitePorCategoriaRepository
                .findByCategoriaUsuario_Usuario_Id(userId);
        sql.append("-- Limites por categoria\n");
        for (LimitePorCategoria lc : limitesCat) {
            sql.append("INSERT INTO limite_por_categoria (id, categoria_usuario_id, limite_desejado, configuracoes_id) VALUES (")
               .append("'").append(lc.getId()).append("', ")
               .append(lc.getCategoriaUsuario().getId()).append(", ")
               .append(lc.getLimiteDesejado()).append(", '").append(lc.getConfiguracoes().getId()).append("');\n");
        }
        sql.append("\n");

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);
        sql.append("-- Eventos financeiros\n");
        for (EventoFinanceiro ev : eventos) {
            sql.append("INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro) VALUES (")
               .append("'").append(ev.getId()).append("', '").append(userId).append("', '")
               .append(ev.getTipo()).append("', ").append(ev.getValor()).append(", '")
               .append(ev.getDescricao() != null ? ev.getDescricao().replace("'", "''") : "").append("', '")
               .append(ev.getDataEvento()).append("', '").append(ev.getDataRegistro()).append("');\n");

            for (EventoInstituicao ei : eventoInstituicaoRepository
                    .findEventoInstituicaoByEventoFinanceiro_Id(ev.getId())) {
                sql.append("INSERT INTO evento_instituicao (id, fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas) VALUES (")
                   .append(ei.getId()).append(", '").append(ev.getId()).append("', ")
                   .append(ei.getInstituicaoUsuario().getId()).append(", '")
                   .append(ei.getTipoMovimento()).append("', ")
                   .append(ei.getValor()).append(", ").append(ei.getParcelas()).append(");\n");
            }

            EventoDetalhe detalhe = eventoDetalheRepository.findGastoDetalheByEventoFinanceiro(ev);
            if (detalhe != null) {
                sql.append("INSERT INTO gasto_detalhe (id, fk_evento, titulo_gasto) VALUES (")
                   .append("'").append(detalhe.getId()).append("', '").append(ev.getId()).append("', '")
                   .append(detalhe.getTituloGasto().replace("'", "''")).append("');\n");
                for (CategoriaUsuario cat : detalhe.getCategoriaUsuario()) {
                    sql.append("INSERT INTO gasto_detalhe_categoria (gasto_detalhe_id, categoria_usuario_id) VALUES (")
                       .append("'").append(detalhe.getId()).append("', ").append(cat.getId()).append(");\n");
                }
            }
            sql.append("\n");
        }

        return new ExportacaoResultado(sql.toString().getBytes(),
                "registros_%s_%s.sql".formatted(usuario.getNome(), LocalDate.now()),
                "application/sql");
    }

    // =========================================================================
    // EXPORTAÇÃO EXCEL — Apache POI XSSF com gráficos, fórmulas e formatação
    //
    // Estrutura do workbook:
    //   1. "Resumo Geral"    — visão executiva: totais, top categorias, top
    //                          instituições + gráficos de pizza e barras
    //   2. "Análise Mensal"  — evolução mês a mês + gráfico de barras agrupadas
    //   3. "Janeiro 2026"    — detalhe com todas as transações do mês (uma aba
    //                          por mês), linhas coloridas por tipo e totais reais
    // =========================================================================

    private ExportacaoResultado exportarExcel(Usuario usuario) {
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository
                .findEventoFinanceiroByUsuarioOrderByDataEventoDesc(usuario);
        // Ordena do mais antigo para o mais recente para agrupar por mês corretamente
        eventos.sort(Comparator.comparing(EventoFinanceiro::getDataEvento));

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Estilos est = criarEstilos(wb);

            // Agrupa por "Mês Ano" (ex.: "Junho 2026") mantendo ordem cronológica
            Map<String, List<EventoFinanceiro>> eventosPorMes = agruparPorMes(eventos);

            // Cria uma aba por mês e coleta os totais para as abas analíticas
            Map<String, double[]> totaisMensais = new LinkedHashMap<>(); // [receitas, gastos]
            for (var entry : eventosPorMes.entrySet()) {
                double[] totais = criarSheetMensal(wb, est, entry.getKey(), entry.getValue());
                totaisMensais.put(entry.getKey(), totais);
            }

            // Aba de análise mensal (criada antes do Resumo Geral para poder referenciar)
            XSSFSheet sheetAnalise = wb.createSheet("Análise Mensal");
            criarSheetAnaliseMensal(sheetAnalise, est, totaisMensais);

            // Aba de resumo geral (inserida depois, mas movida para o início)
            XSSFSheet sheetResumo = wb.createSheet("Resumo Geral");
            criarSheetResumoGeral(sheetResumo, est, usuario, eventos, totaisMensais);

            // Reposiciona: Resumo Geral = 0, Análise Mensal = 1, meses = 2+
            wb.setSheetOrder("Resumo Geral",   0);
            wb.setSheetOrder("Análise Mensal", 1);

            wb.write(out);
            return new ExportacaoResultado(
                    out.toByteArray(),
                    "registros_%s_%s.xlsx".formatted(usuario.getNome(), LocalDate.now()),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar exportação Excel.", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Sheet 1 — Resumo Geral
    // ──────────────────────────────────────────────────────────────────────────

    private void criarSheetResumoGeral(XSSFSheet sheet, Estilos est, Usuario usuario,
                                        List<EventoFinanceiro> eventos,
                                        Map<String, double[]> totaisMensais) {
        // Larguras das colunas (em 1/256 de caractere)
        sheet.setColumnWidth(0, 32 * 256);  // label
        sheet.setColumnWidth(1, 20 * 256);  // valor
        sheet.setColumnWidth(2, 14 * 256);  // %
        sheet.setColumnWidth(3, 14 * 256);  // ocorrências
        sheet.setColumnWidth(4,  2 * 256);  // espaço
        // colunas 5-12 usadas pelo gráfico

        int r = 0;

        // ── Título principal ────────────────────────────────────────────────
        r = tituloMerge(sheet, est, r, "MyFinance — Relatório Financeiro", 0, 3, 32f);
        r++;

        // ── Informações do usuário ───────────────────────────────────────────
        r = secaoMerge(sheet, est, r, "DADOS DO USUÁRIO", 0, 3);
        r = parInfo(sheet, est, r, "Nome",       usuario.getNome() + " " + usuario.getSobrenome());
        r = parInfo(sheet, est, r, "E-mail",     usuario.getEmail());
        r = parInfo(sheet, est, r, "Gerado em",  LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        r++;

        // ── Cálculo de totais ────────────────────────────────────────────────
        double totReceitas = 0, totGastos = 0, totTransferencias = 0, totPoupanca = 0;
        for (EventoFinanceiro ev : eventos) {
            switch (ev.getTipo()) {
                case Recebimento, Emprestimo -> totReceitas       += ev.getValor();
                case Gasto                   -> totGastos         += ev.getValor();
                case Transferencia           -> totTransferencias += ev.getValor();
                case Poupanca                -> totPoupanca       += ev.getValor();
            }
        }
        double saldoAtual = totReceitas - totGastos - totTransferencias - totPoupanca;

        // ── Resumo financeiro ────────────────────────────────────────────────
        r = secaoMerge(sheet, est, r, "RESUMO FINANCEIRO", 0, 3);
        r = linhaSummary(sheet, est, r, "Receitas (Recebimentos + Empréstimos)", totReceitas,  est.moedaReceita);
        r = linhaSummary(sheet, est, r, "Gastos",                                totGastos,    est.moedaGasto);
        r = linhaSummary(sheet, est, r, "Transferências",                        totTransferencias, est.moedaTransferencia);
        r = linhaSummary(sheet, est, r, "Poupança",                              totPoupanca,  est.moedaPoupanca);
        // Linha de saldo com fórmula Excel real: =B(receitasRow)-B(gastosRow)-...
        int saldoRow = r;
        Row rowSaldo = sheet.createRow(r++);
        rowSaldo.setHeightInPoints(20);
        celula(rowSaldo, 0, "Saldo Atual", est.summaryLabel);
        var cSaldo = rowSaldo.createCell(1);
        // Linha de saldo referencia as quatro linhas anteriores (receitas - gastos - transf - poupança)
        cSaldo.setCellFormula("B%d-B%d-B%d-B%d".formatted(saldoRow - 3, saldoRow - 2, saldoRow - 1, saldoRow));
        cSaldo.setCellStyle(saldoAtual >= 0 ? est.moedaReceita : est.moedaGasto);
        r++;

        // ── Gastos por categoria ─────────────────────────────────────────────
        Map<String, double[]> catMap = new LinkedHashMap<>(); // {total, contagem}
        for (EventoFinanceiro ev : eventos) {
            if (ev.getTipo() == Tipo.Gasto) {
                EventoDetalhe det = ev.getGastoDetalhe();
                if (det != null && det.getCategoriaUsuario() != null) {
                    for (var cat : det.getCategoriaUsuario()) {
                        String nome = cat.getCategoria().getTitulo();
                        catMap.computeIfAbsent(nome, k -> new double[]{0, 0});
                        catMap.get(nome)[0] += ev.getValor();
                        catMap.get(nome)[1]++;
                    }
                }
            }
        }
        List<Map.Entry<String, double[]>> catSorted = catMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .toList();

        int catSecaoRow = r;
        r = secaoMerge(sheet, est, r, "GASTOS POR CATEGORIA", 0, 3);
        Row cabCat = sheet.createRow(r++);
        cabCat.setHeightInPoints(20);
        celula(cabCat, 0, "Categoria",    est.cabecalho);
        celula(cabCat, 1, "Total (R$)",   est.cabecalho);
        celula(cabCat, 2, "% do Total",   est.cabecalho);
        celula(cabCat, 3, "Ocorrências",  est.cabecalho);
        int catDataInicio = r;
        boolean alt = false;
        for (var entry : catSorted) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(18);
            celula(row, 0, entry.getKey(),                     alt ? est.dadosAlt : est.dados);
            celulaNum(row, 1, entry.getValue()[0],             alt ? est.moedaAlt : est.moeda);
            double pct = totGastos > 0 ? (entry.getValue()[0] / totGastos) * 100 : 0;
            celulaNum(row, 2, pct,                             est.percentual);
            celula(row, 3, String.valueOf((int) entry.getValue()[1]), alt ? est.dadosAlt : est.dados);
            alt = !alt;
        }
        int catDataFim = r - 1;
        // Linha de total com fórmula
        Row totCat = sheet.createRow(r++);
        totCat.setHeightInPoints(20);
        celula(totCat, 0, "TOTAL", est.total);
        var cTotCat = totCat.createCell(1);
        cTotCat.setCellFormula("SUM(B%d:B%d)".formatted(catDataInicio + 1, catDataFim + 1));
        cTotCat.setCellStyle(est.totalMoeda);
        celula(totCat, 2, "100%", est.total);
        r++;

        // ── Gastos por instituição ───────────────────────────────────────────
        Map<String, Double> instMap = new LinkedHashMap<>();
        for (EventoFinanceiro ev : eventos) {
            if (ev.getTipo() == Tipo.Gasto || ev.getTipo() == Tipo.Transferencia) {
                for (var ei : eventoInstituicaoRepository
                        .findEventoInstituicaoByEventoFinanceiro_Id(ev.getId())) {
                    String nome = ei.getInstituicaoUsuario().getInstituicao().getNome();
                    instMap.merge(nome, ei.getValor(), Double::sum);
                }
            }
        }
        List<Map.Entry<String, Double>> instSorted = instMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .toList();
        double totInst = instMap.values().stream().mapToDouble(Double::doubleValue).sum();

        int instSecaoRow = r;
        r = secaoMerge(sheet, est, r, "GASTOS POR INSTITUIÇÃO", 0, 2);
        Row cabInst = sheet.createRow(r++);
        cabInst.setHeightInPoints(20);
        celula(cabInst, 0, "Instituição",  est.cabecalho);
        celula(cabInst, 1, "Total (R$)",   est.cabecalho);
        celula(cabInst, 2, "% do Total",   est.cabecalho);
        int instDataInicio = r;
        alt = false;
        for (var entry : instSorted) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(18);
            celula(row, 0, entry.getKey(),                    alt ? est.dadosAlt : est.dados);
            celulaNum(row, 1, entry.getValue(),               alt ? est.moedaAlt : est.moeda);
            double pct = totInst > 0 ? (entry.getValue() / totInst) * 100 : 0;
            celulaNum(row, 2, pct,                            est.percentual);
            alt = !alt;
        }
        int instDataFim = r - 1;
        Row totInst2 = sheet.createRow(r++);
        totInst2.setHeightInPoints(20);
        celula(totInst2, 0, "TOTAL", est.total);
        var cTotInst = totInst2.createCell(1);
        cTotInst.setCellFormula("SUM(B%d:B%d)".formatted(instDataInicio + 1, instDataFim + 1));
        cTotInst.setCellStyle(est.totalMoeda);
        celula(totInst2, 2, "100%", est.total);

        // NOTA: gráficos removidos desta sheet por incompatibilidade do Apache POI 4.1.2
        // com o formato de XML de chart esperado pelo Excel 365.
        // Os gráficos estão na aba "Análise Mensal".
        // Para reativar aqui, atualize poi / poi-ooxml para 5.x no pom.xml.
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Sheet 2 — Análise Mensal
    // ──────────────────────────────────────────────────────────────────────────

    private void criarSheetAnaliseMensal(XSSFSheet sheet, Estilos est,
                                          Map<String, double[]> totaisMensais) {
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 18 * 256);
        sheet.setColumnWidth(3, 18 * 256);
        sheet.setColumnWidth(4,  2 * 256);

        int r = 0;
        r = tituloMerge(sheet, est, r, "Análise Mensal — Evolução Financeira", 0, 3, 28f);
        r++;

        Row cab = sheet.createRow(r++);
        cab.setHeightInPoints(22);
        celula(cab, 0, "Mês",              est.cabecalho);
        celula(cab, 1, "Receitas (R$)",    est.cabecalho);
        celula(cab, 2, "Gastos (R$)",      est.cabecalho);
        celula(cab, 3, "Saldo do Mês (R$)", est.cabecalho);

        int dataInicio = r;
        boolean alt = false;
        for (var entry : totaisMensais.entrySet()) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(18);
            double saldo = entry.getValue()[0] - entry.getValue()[1];
            celula(row, 0, entry.getKey(),         alt ? est.dadosAlt : est.dados);
            celulaNum(row, 1, entry.getValue()[0], alt ? est.moedaAlt : est.moeda);
            celulaNum(row, 2, entry.getValue()[1], alt ? est.moedaAlt : est.moeda);
            celulaNum(row, 3, saldo, saldo >= 0
                    ? (alt ? est.moedaAlt : est.moedaReceita)
                    : (alt ? est.moedaAlt : est.moedaGasto));
            alt = !alt;
        }
        int dataFim = r - 1;

        r++;
        // Linha de totais com fórmulas reais
        Row totRow = sheet.createRow(r);
        totRow.setHeightInPoints(22);
        celula(totRow, 0, "TOTAIS GERAIS", est.total);
        var cR = totRow.createCell(1);
        cR.setCellFormula("SUM(B%d:B%d)".formatted(dataInicio + 1, dataFim + 1));
        cR.setCellStyle(est.totalMoeda);
        var cG = totRow.createCell(2);
        cG.setCellFormula("SUM(C%d:C%d)".formatted(dataInicio + 1, dataFim + 1));
        cG.setCellStyle(est.totalMoeda);
        var cS = totRow.createCell(3);
        cS.setCellFormula("B%d-C%d".formatted(r + 1, r + 1));
        cS.setCellStyle(est.totalMoeda);

        // Gráfico de barras agrupadas: receitas vs gastos por mês
        if (!totaisMensais.isEmpty()) {
            XSSFDrawing drawingAnalise = sheet.createDrawingPatriarch();
            graficoBarrasAgrupadas(drawingAnalise, "Receitas vs Gastos por Mês",
                    sheet, dataInicio, dataFim, 0, 1, 2,
                    5, 2, 14, dataInicio + 20);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Sheet 3+ — Detalhe Mensal (uma por mês)
    // ──────────────────────────────────────────────────────────────────────────

    private double[] criarSheetMensal(XSSFWorkbook wb, Estilos est,
                                       String nomeMes, List<EventoFinanceiro> eventos) {
        XSSFSheet sheet = wb.createSheet(nomeMes);

        sheet.setColumnWidth(0, 13 * 256);  // Data
        sheet.setColumnWidth(1, 30 * 256);  // Título
        sheet.setColumnWidth(2, 15 * 256);  // Valor
        sheet.setColumnWidth(3, 16 * 256);  // Tipo
        sheet.setColumnWidth(4, 35 * 256);  // Descrição
        sheet.setColumnWidth(5, 24 * 256);  // Instituição(ões)
        sheet.setColumnWidth(6, 15 * 256);  // Movimentação
        sheet.setColumnWidth(7, 10 * 256);  // Parcelas
        sheet.setColumnWidth(8, 30 * 256);  // Categorias

        // Congela a primeira e segunda linha (título + cabeçalho)
        sheet.createFreezePane(0, 2);

        int r = 0;
        r = tituloMerge(sheet, est, r, nomeMes, 0, 8, 26f);

        // Cabeçalho
        Row cab = sheet.createRow(r++);
        cab.setHeightInPoints(22);
        String[] headers = {"Data", "Título", "Valor (R$)", "Tipo", "Descrição",
                            "Instituição(ões)", "Movimentação", "Parcelas", "Categorias"};
        for (int i = 0; i < headers.length; i++) {
            celula(cab, i, headers[i], est.cabecalho);
        }

        int dataInicio = r;
        double totalReceitas = 0, totalGastos = 0, totalTransferencias = 0, totalPoupanca = 0;
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (EventoFinanceiro ev : eventos) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(20);

            // Estilo da linha e da célula de moeda variam por tipo de evento
            XSSFCellStyle estiloLinha = switch (ev.getTipo()) {
                case Recebimento, Emprestimo -> est.linhaReceita;
                case Gasto                   -> est.linhaGasto;
                case Transferencia           -> est.linhaTransferencia;
                case Poupanca                -> est.linhaPoupanca;
            };
            XSSFCellStyle estiloMoeda = switch (ev.getTipo()) {
                case Recebimento, Emprestimo -> est.moedaReceita;
                case Gasto                   -> est.moedaGasto;
                case Transferencia           -> est.moedaTransferencia;
                case Poupanca                -> est.moedaPoupanca;
            };

            EventoDetalhe det = ev.getGastoDetalhe();
            String titulo = (det != null) ? det.getTituloGasto() : "-";

            // Concatena todas as instituições (pode ser mais de uma)
            List<EventoInstituicao> insts = eventoInstituicaoRepository
                    .findEventoInstituicaoByEventoFinanceiro_Id(ev.getId());
            String instNomes = insts.stream()
                    .map(ei -> ei.getInstituicaoUsuario().getInstituicao().getNome())
                    .distinct().reduce((a, b) -> a + " / " + b).orElse("-");
            String tipoMov = insts.stream()
                    .map(ei -> ei.getTipoMovimento().toString())
                    .distinct().reduce((a, b) -> a + " / " + b).orElse("-");
            int maxParcelas = insts.stream().mapToInt(EventoInstituicao::getParcelas).max().orElse(1);

            // Concatena todas as categorias (pode ser mais de uma)
            String categorias = (det != null && det.getCategoriaUsuario() != null)
                    ? det.getCategoriaUsuario().stream()
                         .map(c -> c.getCategoria().getTitulo())
                         .reduce((a, b) -> a + " / " + b).orElse("-")
                    : "-";

            celula(row, 0, ev.getDataEvento().format(dtFmt), estiloLinha);
            celula(row, 1, titulo,                            estiloLinha);
            celulaNum(row, 2, ev.getValor(),                  estiloMoeda);
            celula(row, 3, ev.getTipo().toString(),           estiloLinha);
            celula(row, 4, ev.getDescricao() != null ? ev.getDescricao() : "-", estiloLinha);
            celula(row, 5, instNomes,                         estiloLinha);
            celula(row, 6, tipoMov,                           estiloLinha);
            celula(row, 7, maxParcelas == 1 ? "À vista" : maxParcelas + "x", estiloLinha);
            celula(row, 8, categorias,                        estiloLinha);

            switch (ev.getTipo()) {
                case Recebimento, Emprestimo -> totalReceitas       += ev.getValor();
                case Gasto                   -> totalGastos         += ev.getValor();
                case Transferencia           -> totalTransferencias += ev.getValor();
                case Poupanca                -> totalPoupanca       += ev.getValor();
            }
        }
        int dataFim = r - 1;

        r++;
        // Linha de total da coluna de valor (fórmula real)
        Row totRow = sheet.createRow(r++);
        totRow.setHeightInPoints(22);
        celula(totRow, 0, "TOTAL DO MÊS", est.total);
        var cTot = totRow.createCell(2);
        cTot.setCellFormula("SUM(C%d:C%d)".formatted(dataInicio + 1, dataFim + 1));
        cTot.setCellStyle(est.totalMoeda);
        for (int i : new int[]{1, 3, 4, 5, 6, 7, 8}) {
            totRow.createCell(i).setCellStyle(est.total);
        }
        r++;

        // ── Resumo do mês com fórmulas SUMIF ────────────────────────────────
        // (a coluna D contém o tipo, a coluna C o valor)
        String colValor = "C";
        String colTipo  = "D";
        String intervalo = "$%s$%d:$%s$%d".formatted(colValor, dataInicio + 1, colValor, dataFim + 1);
        String intervaloTipo = "$%s$%d:$%s$%d".formatted(colTipo, dataInicio + 1, colTipo, dataFim + 1);

        r = escreverResumoMes(sheet, est, r, "Receitas:",      intervalo, intervaloTipo, "Recebimento", est.moedaReceita);
        r = escreverResumoMes(sheet, est, r, "Empréstimos:",   intervalo, intervaloTipo, "Emprestimo",  est.moedaReceita);
        r = escreverResumoMes(sheet, est, r, "Gastos:",        intervalo, intervaloTipo, "Gasto",       est.moedaGasto);
        r = escreverResumoMes(sheet, est, r, "Transferências:",intervalo, intervaloTipo, "Transferencia", est.moedaTransferencia);
        r = escreverResumoMes(sheet, est, r, "Poupança:",      intervalo, intervaloTipo, "Poupanca",    est.moedaPoupanca);

        // Saldo = receitas + empréstimos - gastos - transferências - poupança
        Row rowSaldo = sheet.createRow(r);
        rowSaldo.setHeightInPoints(20);
        celula(rowSaldo, 0, "Saldo do Mês:", est.total);
        var cSaldo = rowSaldo.createCell(1);
        double saldo = totalReceitas - totalGastos - totalTransferencias - totalPoupanca;
        // Fórmula: soma as duas linhas de entrada e subtrai as três de saída
        cSaldo.setCellFormula("B%d+B%d-B%d-B%d-B%d".formatted(r - 4, r - 3, r - 2, r - 1, r));
        cSaldo.setCellStyle(saldo >= 0 ? est.moedaReceita : est.moedaGasto);

        return new double[]{totalReceitas, totalGastos};
    }

    private int escreverResumoMes(XSSFSheet sheet, Estilos est, int r,
                                   String label, String intervaloValor,
                                   String intervaloTipo, String tipo,
                                   XSSFCellStyle estiloMoeda) {
        Row row = sheet.createRow(r);
        row.setHeightInPoints(18);
        celula(row, 0, label, est.summaryLabel);
        var c = row.createCell(1);
        c.setCellFormula("SUMIF(%s,\"%s\",%s)".formatted(intervaloTipo, tipo, intervaloValor));
        c.setCellStyle(estiloMoeda);
        return r + 1;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Gráficos XDDF
    //
    // Apenas gráficos de BAR são usados — compatíveis com Apache POI 4.1.2.
    // XDDFPieChartData gera XML inválido para Excel 365 nessa versão do POI;
    // para reativar gráficos de pizza, atualize poi / poi-ooxml para 5.x no pom.xml.
    // ──────────────────────────────────────────────────────────────────────────

    /** Gráfico de barras agrupadas (duas séries: receitas e gastos por mês). */
    private void graficoBarrasAgrupadas(XSSFDrawing drawing, String titulo,
                                         XSSFSheet sheet,
                                         int linhaInicio, int linhaFim,
                                         int colunaLabel, int colunaReceita, int colunaGasto,
                                         int ancCol1, int ancRow1, int ancCol2, int ancRow2) {
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, ancCol1, ancRow1, ancCol2, ancRow2);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(titulo);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis eixoX = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis eixoY = chart.createValueAxis(AxisPosition.LEFT);
        eixoY.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFBarChartData bar = (XDDFBarChartData) chart.createData(ChartTypes.BAR, eixoX, eixoY);
        bar.setBarDirection(BarDirection.COL);
        // CLUSTERED agrupa receitas e gastos lado a lado por mês
        bar.setBarGrouping(BarGrouping.CLUSTERED);

        XDDFCategoryDataSource labels = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new CellRangeAddress(linhaInicio, linhaFim, colunaLabel, colunaLabel));
        XDDFNumericalDataSource<Double> dadosReceita = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new CellRangeAddress(linhaInicio, linhaFim, colunaReceita, colunaReceita));
        XDDFNumericalDataSource<Double> dadosGasto = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new CellRangeAddress(linhaInicio, linhaFim, colunaGasto, colunaGasto));

        XDDFBarChartData.Series sReceita = (XDDFBarChartData.Series) bar.addSeries(labels, dadosReceita);
        sReceita.setTitle("Receitas", null);
        XDDFBarChartData.Series sGasto = (XDDFBarChartData.Series) bar.addSeries(labels, dadosGasto);
        sGasto.setTitle("Gastos", null);

        chart.plot(bar);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers de escrita de células
    // ──────────────────────────────────────────────────────────────────────────

    private int tituloMerge(XSSFSheet sheet, Estilos est, int r,
                             String texto, int col1, int col2, float altura) {
        Row row = sheet.createRow(r);
        row.setHeightInPoints(altura);
        var cell = row.createCell(col1);
        cell.setCellValue(texto);
        cell.setCellStyle(est.titulo);
        sheet.addMergedRegion(new CellRangeAddress(r, r, col1, col2));
        for (int c = col1 + 1; c <= col2; c++) row.createCell(c).setCellStyle(est.titulo);
        return r + 1;
    }

    private int secaoMerge(XSSFSheet sheet, Estilos est, int r,
                            String texto, int col1, int col2) {
        Row row = sheet.createRow(r);
        row.setHeightInPoints(22);
        var cell = row.createCell(col1);
        cell.setCellValue(texto);
        cell.setCellStyle(est.secao);
        sheet.addMergedRegion(new CellRangeAddress(r, r, col1, col2));
        for (int c = col1 + 1; c <= col2; c++) row.createCell(c).setCellStyle(est.secao);
        return r + 1;
    }

    private int parInfo(XSSFSheet sheet, Estilos est, int r, String label, String valor) {
        Row row = sheet.createRow(r);
        row.setHeightInPoints(18);
        celula(row, 0, label + ":", est.summaryLabel);
        celula(row, 1, valor,       est.dados);
        return r + 1;
    }

    private int linhaSummary(XSSFSheet sheet, Estilos est, int r,
                              String label, double valor, XSSFCellStyle estiloMoeda) {
        Row row = sheet.createRow(r);
        row.setHeightInPoints(20);
        celula(row, 0, label, est.summaryLabel);
        celulaNum(row, 1, valor, estiloMoeda);
        return r + 1;
    }

    private void celula(Row row, int col, String valor, XSSFCellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue(valor != null ? valor : "");
        if (style != null) cell.setCellStyle(style);
    }

    private void celulaNum(Row row, int col, double valor, XSSFCellStyle style) {
        var cell = row.createCell(col);
        cell.setCellValue(valor);
        if (style != null) cell.setCellStyle(style);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Agrupamento
    // ──────────────────────────────────────────────────────────────────────────

    private Map<String, List<EventoFinanceiro>> agruparPorMes(List<EventoFinanceiro> eventos) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("pt", "BR"));
        Map<String, List<EventoFinanceiro>> mapa = new LinkedHashMap<>();
        for (EventoFinanceiro ev : eventos) {
            String chave = capitalizar(ev.getDataEvento().format(fmt));
            mapa.computeIfAbsent(chave, k -> new ArrayList<>()).add(ev);
        }
        return mapa;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Sistema de estilos
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Agrupa todos os estilos utilizados no workbook. Criados uma única vez para
     * respeitar o limite de 64.000 estilos do formato XLSX.
     */
    private record Estilos(
            XSSFCellStyle titulo, XSSFCellStyle secao, XSSFCellStyle cabecalho,
            XSSFCellStyle dados, XSSFCellStyle dadosAlt,
            XSSFCellStyle linhaReceita, XSSFCellStyle linhaGasto,
            XSSFCellStyle linhaTransferencia, XSSFCellStyle linhaPoupanca,
            XSSFCellStyle moeda, XSSFCellStyle moedaAlt,
            XSSFCellStyle moedaReceita, XSSFCellStyle moedaGasto,
            XSSFCellStyle moedaTransferencia, XSSFCellStyle moedaPoupanca,
            XSSFCellStyle percentual,
            XSSFCellStyle total, XSSFCellStyle totalMoeda,
            XSSFCellStyle summaryLabel,
            XSSFCellStyle info
    ) {}

    private Estilos criarEstilos(XSSFWorkbook wb) {
        XSSFDataFormat fmt  = wb.createDataFormat();
        short fmtMoeda = fmt.getFormat("\"R$ \"#,##0.00");
        short fmtPct   = fmt.getFormat("0.0\"%\"");

        // Paleta de cores
        byte[] corTealEscuro   = {(byte) 27,  (byte) 94,  (byte) 107};
        byte[] corTeal         = {(byte) 46,  (byte) 155, (byte) 153};
        byte[] corTealClaro    = {(byte) 224, (byte) 245, (byte) 245};
        byte[] corBranco       = {(byte) 255, (byte) 255, (byte) 255};
        byte[] corCinzaClaro   = {(byte) 245, (byte) 248, (byte) 248};
        byte[] corCinzaMedio   = {(byte) 189, (byte) 189, (byte) 189};
        byte[] corVerdeClaro   = {(byte) 232, (byte) 245, (byte) 233};
        byte[] corVerde        = {(byte) 46,  (byte) 125, (byte) 50};
        byte[] corVermelhoClaro= {(byte) 255, (byte) 235, (byte) 238};
        byte[] corVermelho     = {(byte) 198, (byte) 40,  (byte) 40};
        byte[] corAmareloClaro = {(byte) 255, (byte) 249, (byte) 196};
        byte[] corAmarelo      = {(byte) 245, (byte) 127, (byte) 23};
        byte[] corRoxoClaro    = {(byte) 243, (byte) 229, (byte) 245};
        byte[] corRoxo         = {(byte) 106, (byte) 27,  (byte) 154};
        byte[] corTexto        = {(byte) 33,  (byte) 33,  (byte) 33};

        // ── Título ─────────────────────────────────────────────────────────
        XSSFCellStyle titulo = wb.createCellStyle();
        titulo.setFillForegroundColor(new XSSFColor(corTeal, null));
        titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titulo.setAlignment(HorizontalAlignment.CENTER);
        titulo.setVerticalAlignment(VerticalAlignment.CENTER);
        titulo.setFont(fonte(wb, corBranco, 15, true));
        borda(titulo, BorderStyle.MEDIUM);

        // ── Seção ──────────────────────────────────────────────────────────
        XSSFCellStyle secao = wb.createCellStyle();
        secao.setFillForegroundColor(new XSSFColor(corTealEscuro, null));
        secao.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        secao.setAlignment(HorizontalAlignment.LEFT);
        secao.setVerticalAlignment(VerticalAlignment.CENTER);
        secao.setFont(fonte(wb, corBranco, 10, true));
        borda(secao, BorderStyle.THIN);

        // ── Cabeçalho de tabela ────────────────────────────────────────────
        XSSFCellStyle cabecalho = wb.createCellStyle();
        cabecalho.setFillForegroundColor(new XSSFColor(corTealClaro, null));
        cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cabecalho.setAlignment(HorizontalAlignment.CENTER);
        cabecalho.setVerticalAlignment(VerticalAlignment.CENTER);
        cabecalho.setWrapText(true);
        cabecalho.setFont(fonte(wb, corTealEscuro, 10, true));
        borda(cabecalho, BorderStyle.THIN);

        // ── Dados branco / alternado ───────────────────────────────────────
        XSSFCellStyle dados    = dadosBase(wb, corBranco,     corTexto, false, fmtMoeda);
        XSSFCellStyle dadosAlt = dadosBase(wb, corCinzaClaro, corTexto, false, fmtMoeda);

        // ── Linhas coloridas por tipo de evento ────────────────────────────
        XSSFCellStyle linhaReceita       = dadosBase(wb, corVerdeClaro,   corVerde,    false, (short) 0);
        XSSFCellStyle linhaGasto         = dadosBase(wb, corVermelhoClaro,corVermelho, false, (short) 0);
        XSSFCellStyle linhaTransferencia = dadosBase(wb, corAmareloClaro, corAmarelo,  false, (short) 0);
        XSSFCellStyle linhaPoupanca      = dadosBase(wb, corRoxoClaro,    corRoxo,     false, (short) 0);

        // ── Moeda neutra ───────────────────────────────────────────────────
        XSSFCellStyle moeda    = dadosBase(wb, corBranco,     corTexto, false, fmtMoeda);
        XSSFCellStyle moedaAlt = dadosBase(wb, corCinzaClaro, corTexto, false, fmtMoeda);

        // ── Moeda com cor de fundo por tipo ────────────────────────────────
        XSSFCellStyle moedaReceita       = dadosBase(wb, corVerdeClaro,   corVerde,   false, fmtMoeda);
        XSSFCellStyle moedaGasto         = dadosBase(wb, corVermelhoClaro,corVermelho,false, fmtMoeda);
        XSSFCellStyle moedaTransferencia = dadosBase(wb, corAmareloClaro, corAmarelo, false, fmtMoeda);
        XSSFCellStyle moedaPoupanca      = dadosBase(wb, corRoxoClaro,    corRoxo,    false, fmtMoeda);

        // ── Percentual ─────────────────────────────────────────────────────
        XSSFCellStyle percentual = dadosBase(wb, corBranco, corTexto, false, fmtPct);

        // ── Total ──────────────────────────────────────────────────────────
        XSSFCellStyle total = wb.createCellStyle();
        total.setFillForegroundColor(new XSSFColor(corTeal, null));
        total.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        total.setAlignment(HorizontalAlignment.LEFT);
        total.setVerticalAlignment(VerticalAlignment.CENTER);
        total.setFont(fonte(wb, corBranco, 10, true));
        borda(total, BorderStyle.THIN);

        XSSFCellStyle totalMoeda = wb.createCellStyle();
        totalMoeda.cloneStyleFrom(total);
        totalMoeda.setDataFormat(fmtMoeda);

        // ── Rótulo de resumo ───────────────────────────────────────────────
        XSSFCellStyle summaryLabel = dadosBase(wb, corCinzaClaro, corTealEscuro, true, (short) 0);

        // ── Info genérico ──────────────────────────────────────────────────
        XSSFCellStyle info = dadosBase(wb, corBranco, corTexto, false, (short) 0);

        return new Estilos(titulo, secao, cabecalho, dados, dadosAlt,
                linhaReceita, linhaGasto, linhaTransferencia, linhaPoupanca,
                moeda, moedaAlt, moedaReceita, moedaGasto, moedaTransferencia, moedaPoupanca,
                percentual, total, totalMoeda, summaryLabel, info);
    }

    /** Cria um estilo de dados com fundo, cor de fonte e opcionalmente negrito/formato. */
    private XSSFCellStyle dadosBase(XSSFWorkbook wb, byte[] bgRgb, byte[] fontRgb,
                                     boolean bold, short dataFormat) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(bgRgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        borda(style, BorderStyle.THIN);
        style.setFont(fonte(wb, fontRgb, 10, bold));
        if (dataFormat > 0) style.setDataFormat(dataFormat);
        return style;
    }

    private XSSFFont fonte(XSSFWorkbook wb, byte[] rgbCor, int tamanho, boolean bold) {
        XSSFFont font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) tamanho);
        font.setBold(bold);
        if (rgbCor != null) font.setColor(new XSSFColor(rgbCor, null));
        return font;
    }

    private void borda(XSSFCellStyle style, BorderStyle bs) {
        style.setBorderTop(bs);
        style.setBorderBottom(bs);
        style.setBorderLeft(bs);
        style.setBorderRight(bs);
    }

    // =========================================================================
    // EXPORTAÇÃO PDF — iText 7 — Layout moderno com análise financeira
    // =========================================================================

    /** Paleta de cores usada em todo o PDF. */
    private record PdfTheme(
            DeviceRgb primaria, DeviceRgb priMed, DeviceRgb priLight,
            DeviceRgb branco, DeviceRgb texto, DeviceRgb cinza, DeviceRgb bordaCinza,
            DeviceRgb recBg, DeviceRgb recFg,
            DeviceRgb gasBg, DeviceRgb gasFg,
            DeviceRgb pouBg, DeviceRgb pouFg,
            DeviceRgb traBg, DeviceRgb traFg
    ) {}

    private ExportacaoResultado exportarPdf(Usuario usuario) {
        PdfTheme t = new PdfTheme(
                new DeviceRgb(27,  94,  107), new DeviceRgb(46, 155, 153), new DeviceRgb(224, 245, 245),
                new DeviceRgb(255, 255, 255), new DeviceRgb(33,  33,  33),
                new DeviceRgb(248, 250, 250), new DeviceRgb(206, 212, 218),
                new DeviceRgb(232, 245, 233), new DeviceRgb(27,  94,  32),
                new DeviceRgb(255, 235, 238), new DeviceRgb(183, 28,  28),
                new DeviceRgb(243, 229, 245), new DeviceRgb(74,  20, 140),
                new DeviceRgb(255, 248, 225), new DeviceRgb(230, 81,   0)
        );

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository
                .findEventoFinanceiroByUsuarioOrderByDataEventoDesc(usuario);
        eventos.sort(Comparator.comparing(EventoFinanceiro::getDataEvento));

        // Pré-carrega instituições (evita N+1)
        Map<UUID, List<EventoInstituicao>> instsByEvento = new LinkedHashMap<>();
        for (EventoFinanceiro ev : eventos) {
            instsByEvento.put(ev.getId(),
                    eventoInstituicaoRepository.findEventoInstituicaoByEventoFinanceiro_Id(ev.getId()));
        }

        // Agrupa por ano → mês em ordem cronológica
        Map<Integer, Map<Integer, List<EventoFinanceiro>>> porAnoMes = new LinkedHashMap<>();
        for (EventoFinanceiro ev : eventos) {
            porAnoMes
                    .computeIfAbsent(ev.getDataEvento().getYear(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(ev.getDataEvento().getMonthValue(), k -> new ArrayList<>())
                    .add(ev);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            Document doc = new Document(pdf);
            doc.setMargins(36, 36, 36, 36);

            Locale ptBR = new Locale("pt", "BR");
            DateTimeFormatter dtFmt    = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter dtDiaMes = DateTimeFormatter.ofPattern("dd/MM");

            LocalDate periodoInicio = eventos.isEmpty() ? LocalDate.now() : eventos.get(0).getDataEvento();
            LocalDate periodoFim    = eventos.isEmpty() ? LocalDate.now() : eventos.get(eventos.size() - 1).getDataEvento();

            // ── CAPA ──────────────────────────────────────────────────────────
            pdfCapa(doc, usuario, periodoInicio, periodoFim, eventos.size(), dtFmt, t);
            doc.add(new AreaBreak());

            // ── SUMÁRIO ───────────────────────────────────────────────────────
            pdfSumario(doc, porAnoMes, ptBR, t);
            doc.add(new AreaBreak());

            // ── REGISTROS POR ANO / MÊS ───────────────────────────────────────
            BigDecimal totalRecGeral = BigDecimal.ZERO, totalGasGeral = BigDecimal.ZERO;
            BigDecimal totalPouGeral = BigDecimal.ZERO, totalTraGeral = BigDecimal.ZERO;

            for (Map.Entry<Integer, Map<Integer, List<EventoFinanceiro>>> anoEntry : porAnoMes.entrySet()) {
                int ano = anoEntry.getKey();
                BigDecimal totalRecAno = BigDecimal.ZERO, totalGasAno = BigDecimal.ZERO;
                BigDecimal totalPouAno = BigDecimal.ZERO, totalTraAno = BigDecimal.ZERO;

                // Cabeçalho do ano
                Table anoHdr = new Table(UnitValue.createPercentArray(new float[]{1}))
                        .setWidth(UnitValue.createPercentValue(100));
                anoHdr.addCell(new Cell()
                        .add(new Paragraph(String.valueOf(ano))
                                .setFontSize(22).setBold().setFontColor(t.branco())
                                .setTextAlignment(TextAlignment.LEFT))
                        .setBackgroundColor(t.primaria()).setPadding(14).setBorder(Border.NO_BORDER));
                doc.add(anoHdr);
                PdfExplicitDestination destAno = PdfExplicitDestination.createFit(pdf.getLastPage());
                pdf.addNamedDestination("ano_" + ano, destAno.getPdfObject());

                for (Map.Entry<Integer, List<EventoFinanceiro>> mesEntry : anoEntry.getValue().entrySet()) {
                    int mes = mesEntry.getKey();
                    List<EventoFinanceiro> evMes = mesEntry.getValue();
                    String nomeMes = capitalizar(Month.of(mes).getDisplayName(TextStyle.FULL, ptBR));

                    // Cabeçalho do mês
                    Table mesHdr = new Table(UnitValue.createPercentArray(new float[]{1}))
                            .setWidth(UnitValue.createPercentValue(100)).setMarginTop(18);
                    mesHdr.addCell(new Cell()
                            .add(new Paragraph(nomeMes + " " + ano)
                                    .setFontSize(13).setBold().setFontColor(t.branco()))
                            .setBackgroundColor(t.priMed()).setPadding(8).setBorder(Border.NO_BORDER));
                    doc.add(mesHdr);
                    PdfExplicitDestination destMes = PdfExplicitDestination.createFit(pdf.getLastPage());
                    pdf.addNamedDestination("mes_" + ano + "_" + mes, destMes.getPdfObject());

                    // Tabela de transações
                    float[] cols = {5f, 13f, 9f, 9f, 14f, 12f, 8f, 7f, 13f};
                    Table tabela = new Table(UnitValue.createPercentArray(cols))
                            .setWidth(UnitValue.createPercentValue(100)).setMarginTop(4);

                    String[] hdrs = {"Data", "Título", "Valor (R$)", "Tipo", "Descrição",
                                     "Instituição", "Movimento", "Parcelas", "Categorias"};
                    for (String h : hdrs) {
                        tabela.addHeaderCell(new Cell()
                                .add(new Paragraph(h).setFontSize(7.5f).setBold().setFontColor(t.branco())
                                        .setTextAlignment(TextAlignment.CENTER))
                                .setBackgroundColor(t.primaria()).setPadding(5f)
                                .setBorder(new SolidBorder(t.branco(), 0.5f)));
                    }

                    BigDecimal recM = BigDecimal.ZERO, gasM = BigDecimal.ZERO;
                    BigDecimal pouM = BigDecimal.ZERO, traM = BigDecimal.ZERO;

                    for (EventoFinanceiro ev : evMes) {
                        DeviceRgb bg, fg;
                        switch (ev.getTipo()) {
                            case Recebimento, Emprestimo -> { bg = t.recBg(); fg = t.recFg(); }
                            case Gasto                   -> { bg = t.gasBg(); fg = t.gasFg(); }
                            case Transferencia           -> { bg = t.traBg(); fg = t.traFg(); }
                            case Poupanca                -> { bg = t.pouBg(); fg = t.pouFg(); }
                            default                      -> { bg = t.cinza(); fg = t.texto(); }
                        }

                        EventoDetalhe det = ev.getGastoDetalhe();
                        String titulo = det != null ? det.getTituloGasto() : "-";
                        String cats = det != null && det.getCategoriaUsuario() != null
                                ? det.getCategoriaUsuario().stream()
                                      .map(c -> c.getCategoria().getTitulo())
                                      .reduce((a, b) -> a + ", " + b).orElse("-") : "-";

                        List<EventoInstituicao> insts = instsByEvento.getOrDefault(ev.getId(), List.of());
                        String instN = insts.stream()
                                .map(ei -> ei.getInstituicaoUsuario().getInstituicao().getNome())
                                .distinct().reduce((a, b) -> a + " / " + b).orElse("-");
                        String tipoMov = insts.stream()
                                .map(ei -> ei.getTipoMovimento().toString())
                                .distinct().reduce((a, b) -> a + " / " + b).orElse("-");
                        int maxParc = insts.stream().mapToInt(EventoInstituicao::getParcelas).max().orElse(1);

                        String[] celulas = {
                            ev.getDataEvento().format(dtDiaMes),
                            truncar(titulo, 35),
                            formatarMoeda(ev.getValor()),
                            ev.getTipo().toString(),
                            truncar(ev.getDescricao() != null ? ev.getDescricao() : "-", 50),
                            truncar(instN, 30),
                            tipoMov,
                            maxParc <= 1 ? "À vista" : maxParc + "x",
                            truncar(cats, 35)
                        };

                        for (String celVal : celulas) {
                            tabela.addCell(new Cell()
                                    .add(new Paragraph(celVal).setFontSize(7.5f).setFontColor(fg))
                                    .setBackgroundColor(bg).setPadding(4f)
                                    .setBorder(new SolidBorder(t.bordaCinza(), 0.3f)));
                        }

                        switch (ev.getTipo()) {
                            case Recebimento, Emprestimo -> recM = recM.add(BigDecimal.valueOf(ev.getValor()));
                            case Gasto                   -> gasM = gasM.add(BigDecimal.valueOf(ev.getValor()));
                            case Transferencia           -> traM = traM.add(BigDecimal.valueOf(ev.getValor()));
                            case Poupanca                -> pouM = pouM.add(BigDecimal.valueOf(ev.getValor()));
                        }
                    }

                    doc.add(tabela);

                    BigDecimal saldoM = recM.subtract(gasM).subtract(traM).subtract(pouM);
                    doc.add(pdfBoxResumoMes(nomeMes + " " + ano, recM, gasM, pouM, traM, saldoM, t));

                    totalRecAno = totalRecAno.add(recM);
                    totalGasAno = totalGasAno.add(gasM);
                    totalPouAno = totalPouAno.add(pouM);
                    totalTraAno = totalTraAno.add(traM);
                }

                // Resumo anual
                BigDecimal saldoAno = totalRecAno.subtract(totalGasAno).subtract(totalTraAno).subtract(totalPouAno);
                doc.add(pdfBoxResumoAno(ano, totalRecAno, totalGasAno, totalPouAno, totalTraAno, saldoAno, t));

                totalRecGeral = totalRecGeral.add(totalRecAno);
                totalGasGeral = totalGasGeral.add(totalGasAno);
                totalPouGeral = totalPouGeral.add(totalPouAno);
                totalTraGeral = totalTraGeral.add(totalTraAno);

                doc.add(new AreaBreak());
            }

            // ── ANÁLISE FINANCEIRA ────────────────────────────────────────────
            PdfExplicitDestination destAnalise = PdfExplicitDestination.createFit(pdf.getLastPage());
            pdf.addNamedDestination("analise_financeira", destAnalise.getPdfObject());

            pdfAnaliseFinanceira(doc, usuario, eventos, instsByEvento, porAnoMes,
                    totalRecGeral, totalGasGeral, totalPouGeral, totalTraGeral, t, ptBR, dtFmt);

            doc.close();

            return new ExportacaoResultado(out.toByteArray(),
                    "registros_%s_%s.pdf".formatted(usuario.getNome(), LocalDate.now()),
                    "application/pdf");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar exportação PDF.", e);
        }
    }

    // ─── Capa ────────────────────────────────────────────────────────────────

    private void pdfCapa(Document doc, Usuario usuario,
                          LocalDate inicio, LocalDate fim, int totalEventos,
                          DateTimeFormatter dtFmt, PdfTheme t) {
        // Banner principal
        Table banner = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100));
        banner.addCell(new Cell()
                .add(new Paragraph("MyFinance")
                        .setFontSize(40).setBold().setFontColor(t.branco())
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4))
                .add(new Paragraph("Relatório Financeiro Completo")
                        .setFontSize(15).setFontColor(t.priLight())
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(t.primaria()).setPadding(55).setBorder(Border.NO_BORDER));
        doc.add(banner);

        // Informações do usuário
        Table info = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                .setWidth(UnitValue.createPercentValue(75))
                .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                .setMarginTop(28);

        Object[][] campos = {
            {"Usuário",            usuario.getNome() + " " + usuario.getSobrenome()},
            {"E-mail",             usuario.getEmail()},
            {"Período analisado",  inicio.format(dtFmt) + " até " + fim.format(dtFmt)},
            {"Total de registros", String.valueOf(totalEventos)},
            {"Documento gerado",   LocalDate.now().format(dtFmt)}
        };

        for (Object[] campo : campos) {
            info.addCell(new Cell()
                    .add(new Paragraph(campo[0].toString()).setFontSize(9.5f).setBold().setFontColor(t.primaria()))
                    .setBackgroundColor(t.priLight()).setPadding(8)
                    .setBorder(new SolidBorder(t.bordaCinza(), 0.5f)));
            info.addCell(new Cell()
                    .add(new Paragraph(campo[1].toString()).setFontSize(9.5f).setFontColor(t.texto()))
                    .setPadding(8).setBorder(new SolidBorder(t.bordaCinza(), 0.5f)));
        }
        doc.add(info);
    }

    // ─── Sumário ─────────────────────────────────────────────────────────────

    private void pdfSumario(Document doc,
                              Map<Integer, Map<Integer, List<EventoFinanceiro>>> porAnoMes,
                              Locale ptBR, PdfTheme t) {
        doc.add(pdfTituloSecao("Sumário", t));

        for (Map.Entry<Integer, Map<Integer, List<EventoFinanceiro>>> anoEntry : porAnoMes.entrySet()) {
            int ano = anoEntry.getKey();
            doc.add(new Paragraph(
                    new Link(String.valueOf(ano), PdfAction.createGoTo("ano_" + ano)))
                    .setFontSize(13).setBold().setFontColor(t.primaria())
                    .setMarginTop(10).setMarginLeft(12));

            for (int mes : anoEntry.getValue().keySet()) {
                String nomeMes = capitalizar(Month.of(mes).getDisplayName(TextStyle.FULL, ptBR));
                doc.add(new Paragraph(
                        new Link("    ▸  " + nomeMes + " " + ano,
                                PdfAction.createGoTo("mes_" + ano + "_" + mes)))
                        .setFontSize(11).setFontColor(t.texto()).setMarginLeft(28));
            }
        }

        doc.add(new Paragraph(
                new Link("► Análise Financeira e Saúde Financeira",
                        PdfAction.createGoTo("analise_financeira")))
                .setFontSize(13).setBold().setFontColor(t.priMed())
                .setMarginTop(22).setMarginLeft(12));
    }

    // ─── Box resumo mensal ───────────────────────────────────────────────────

    private Table pdfBoxResumoMes(String titulo, BigDecimal rec, BigDecimal gas,
                                    BigDecimal pou, BigDecimal tra, BigDecimal saldo,
                                    PdfTheme t) {
        Table box = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(8);

        // Cabeçalhos
        for (String h : new String[]{"Receitas", "Gastos", "Poupança", "Transferências", "Saldo"}) {
            box.addCell(new Cell()
                    .add(new Paragraph(h).setFontSize(8f).setBold().setFontColor(t.branco())
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(t.priMed()).setPadding(5f).setBorder(Border.NO_BORDER));
        }

        // Valores
        DeviceRgb saldoBg = saldo.compareTo(BigDecimal.ZERO) >= 0 ? t.recBg() : t.gasBg();
        DeviceRgb saldoFg = saldo.compareTo(BigDecimal.ZERO) >= 0 ? t.recFg() : t.gasFg();

        box.addCell(pdfBoxCell(formatarMoeda(rec.doubleValue()), t.recBg(), t.recFg()));
        box.addCell(pdfBoxCell(formatarMoeda(gas.doubleValue()), t.gasBg(), t.gasFg()));
        box.addCell(pdfBoxCell(formatarMoeda(pou.doubleValue()), t.pouBg(), t.pouFg()));
        box.addCell(pdfBoxCell(formatarMoeda(tra.doubleValue()), t.traBg(), t.traFg()));
        box.addCell(pdfBoxCell(formatarMoeda(saldo.doubleValue()), saldoBg, saldoFg));

        return box;
    }

    // ─── Box resumo anual ────────────────────────────────────────────────────

    private Table pdfBoxResumoAno(int ano, BigDecimal rec, BigDecimal gas,
                                   BigDecimal pou, BigDecimal tra, BigDecimal saldo,
                                   PdfTheme t) {
        Table box = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(14);

        box.addCell(new Cell(1, 5)
                .add(new Paragraph("Resumo Anual — " + ano)
                        .setFontSize(11f).setBold().setFontColor(t.branco()))
                .setBackgroundColor(t.primaria()).setPadding(7f).setBorder(Border.NO_BORDER));

        for (String h : new String[]{"Receitas", "Gastos", "Poupança", "Transferências", "Saldo do Ano"}) {
            box.addCell(new Cell()
                    .add(new Paragraph(h).setFontSize(8f).setBold().setFontColor(t.primaria())
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(t.priLight()).setPadding(5f)
                    .setBorder(new SolidBorder(t.bordaCinza(), 0.5f)));
        }

        DeviceRgb saldoBg = saldo.compareTo(BigDecimal.ZERO) >= 0 ? t.recBg() : t.gasBg();
        DeviceRgb saldoFg = saldo.compareTo(BigDecimal.ZERO) >= 0 ? t.recFg() : t.gasFg();

        box.addCell(pdfBoxCell(formatarMoeda(rec.doubleValue()), t.recBg(), t.recFg()));
        box.addCell(pdfBoxCell(formatarMoeda(gas.doubleValue()), t.gasBg(), t.gasFg()));
        box.addCell(pdfBoxCell(formatarMoeda(pou.doubleValue()), t.pouBg(), t.pouFg()));
        box.addCell(pdfBoxCell(formatarMoeda(tra.doubleValue()), t.traBg(), t.traFg()));
        box.addCell(pdfBoxCell(formatarMoeda(saldo.doubleValue()), saldoBg, saldoFg));

        return box;
    }

    // ─── Análise financeira completa ─────────────────────────────────────────

    private void pdfAnaliseFinanceira(Document doc, Usuario usuario,
                                       List<EventoFinanceiro> eventos,
                                       Map<UUID, List<EventoInstituicao>> instsByEvento,
                                       Map<Integer, Map<Integer, List<EventoFinanceiro>>> porAnoMes,
                                       BigDecimal totalRec, BigDecimal totalGas,
                                       BigDecimal totalPou, BigDecimal totalTra,
                                       PdfTheme t, Locale ptBR, DateTimeFormatter dtFmt) {

        // Marcador textual que o importador usa para parar de ler registros
        doc.add(pdfTituloSecao("ANÁLISE FINANCEIRA", t));

        BigDecimal saldoGeral = totalRec.subtract(totalGas).subtract(totalTra).subtract(totalPou);

        // ── 1. Resumo financeiro global ────────────────────────────────────────
        doc.add(pdfSubtituloSecao("1. Resumo Financeiro Global", t));

        Table resumo = new Table(UnitValue.createPercentArray(new float[]{45, 55}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(6);

        Object[][] linhasResumo = {
            {"Total de Receitas (Recebimentos + Empréstimos)", formatarMoeda(totalRec.doubleValue())},
            {"Total de Gastos", formatarMoeda(totalGas.doubleValue())},
            {"Total em Poupança", formatarMoeda(totalPou.doubleValue())},
            {"Total em Transferências", formatarMoeda(totalTra.doubleValue())},
            {"Saldo Geral (Receitas − Gastos − Transf. − Poupança)", formatarMoeda(saldoGeral.doubleValue())}
        };

        boolean altResumo = false;
        for (int i = 0; i < linhasResumo.length; i++) {
            boolean isSaldo = i == linhasResumo.length - 1;
            DeviceRgb bg = isSaldo
                    ? (saldoGeral.compareTo(BigDecimal.ZERO) >= 0 ? t.recBg() : t.gasBg())
                    : (altResumo ? t.cinza() : t.branco());
            DeviceRgb fg = isSaldo
                    ? (saldoGeral.compareTo(BigDecimal.ZERO) >= 0 ? t.recFg() : t.gasFg())
                    : t.texto();

            Paragraph pLabel = new Paragraph(linhasResumo[i][0].toString())
                    .setFontSize(9f).setFontColor(fg);
            Paragraph pValue = new Paragraph(linhasResumo[i][1].toString())
                    .setFontSize(9f).setFontColor(fg).setTextAlignment(TextAlignment.RIGHT);
            if (isSaldo) { pLabel.setBold(); pValue.setBold(); }

            resumo.addCell(new Cell()
                    .add(pLabel).setBackgroundColor(bg).setPadding(7f)
                    .setBorder(new SolidBorder(t.bordaCinza(), 0.4f)));
            resumo.addCell(new Cell()
                    .add(pValue).setBackgroundColor(bg).setPadding(7f)
                    .setBorder(new SolidBorder(t.bordaCinza(), 0.4f)));
            altResumo = !altResumo;
        }
        doc.add(resumo);

        // Taxas
        double taxaGastos  = totalRec.compareTo(BigDecimal.ZERO) > 0
                ? totalGas.divide(totalRec, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;
        double taxaPoupanca = totalRec.compareTo(BigDecimal.ZERO) > 0
                ? totalPou.divide(totalRec, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue() : 0;

        Table taxas = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(60)).setMarginTop(8);
        taxas.addCell(pdfBoxCell("Taxa de Gastos: " + String.format("%.1f%%", taxaGastos),
                taxaGastos > 80 ? t.gasBg() : t.recBg(), taxaGastos > 80 ? t.gasFg() : t.recFg()));
        taxas.addCell(pdfBoxCell("Taxa de Poupança: " + String.format("%.1f%%", taxaPoupanca),
                taxaPoupanca >= 10 ? t.recBg() : t.gasBg(), taxaPoupanca >= 10 ? t.recFg() : t.gasFg()));
        doc.add(taxas);

        // ── 2. Análise por Categoria ───────────────────────────────────────────
        doc.add(pdfSubtituloSecao("2. Gastos por Categoria", t));

        Map<String, double[]> catMap = new LinkedHashMap<>();
        for (EventoFinanceiro ev : eventos) {
            if (ev.getTipo() == Tipo.Gasto) {
                EventoDetalhe det = ev.getGastoDetalhe();
                if (det != null && det.getCategoriaUsuario() != null) {
                    for (CategoriaUsuario cu : det.getCategoriaUsuario()) {
                        String nome = cu.getCategoria().getTitulo();
                        catMap.computeIfAbsent(nome, k -> new double[]{0, 0});
                        catMap.get(nome)[0] += ev.getValor();
                        catMap.get(nome)[1]++;
                    }
                }
            }
        }
        List<Map.Entry<String, double[]>> catSorted = catMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .toList();

        if (!catSorted.isEmpty()) {
            Table catTable = new Table(UnitValue.createPercentArray(new float[]{40, 25, 20, 15}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(6);
            for (String h : new String[]{"Categoria", "Total (R$)", "% dos Gastos", "Ocorrências"}) {
                catTable.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFontSize(8f).setBold().setFontColor(t.branco()))
                        .setBackgroundColor(t.primaria()).setPadding(6f).setBorder(Border.NO_BORDER));
            }
            boolean altCat = false;
            double totGas = totalGas.doubleValue();
            for (Map.Entry<String, double[]> e : catSorted) {
                DeviceRgb bg = altCat ? t.cinza() : t.branco();
                double pct = totGas > 0 ? (e.getValue()[0] / totGas) * 100 : 0;
                catTable.addCell(pdfDataCell(e.getKey(), bg, t.texto()));
                catTable.addCell(pdfDataCell(formatarMoeda(e.getValue()[0]), bg, t.texto()));
                catTable.addCell(pdfDataCell(String.format("%.1f%%", pct), bg, t.texto()));
                catTable.addCell(pdfDataCell(String.valueOf((int) e.getValue()[1]), bg, t.texto()));
                altCat = !altCat;
            }
            doc.add(catTable);
        } else {
            doc.add(new Paragraph("Nenhum gasto categorizado encontrado.")
                    .setFontSize(9f).setFontColor(t.texto()).setMarginTop(4));
        }

        // ── 3. Análise por Instituição ─────────────────────────────────────────
        doc.add(pdfSubtituloSecao("3. Movimentações por Instituição", t));

        Map<String, double[]> instMap = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<EventoInstituicao>> e : instsByEvento.entrySet()) {
            for (EventoInstituicao ei : e.getValue()) {
                String nome = ei.getInstituicaoUsuario().getInstituicao().getNome();
                instMap.computeIfAbsent(nome, k -> new double[]{0, 0});
                instMap.get(nome)[0] += ei.getValor();
                instMap.get(nome)[1]++;
            }
        }
        List<Map.Entry<String, double[]>> instSorted = instMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .toList();
        double totInst = instMap.values().stream().mapToDouble(v -> v[0]).sum();

        if (!instSorted.isEmpty()) {
            Table instTable = new Table(UnitValue.createPercentArray(new float[]{40, 25, 20, 15}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginTop(6);
            for (String h : new String[]{"Instituição", "Volume (R$)", "% do Volume", "Transações"}) {
                instTable.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFontSize(8f).setBold().setFontColor(t.branco()))
                        .setBackgroundColor(t.primaria()).setPadding(6f).setBorder(Border.NO_BORDER));
            }
            boolean altInst = false;
            for (Map.Entry<String, double[]> e : instSorted) {
                DeviceRgb bg = altInst ? t.cinza() : t.branco();
                double pct = totInst > 0 ? (e.getValue()[0] / totInst) * 100 : 0;
                instTable.addCell(pdfDataCell(e.getKey(), bg, t.texto()));
                instTable.addCell(pdfDataCell(formatarMoeda(e.getValue()[0]), bg, t.texto()));
                instTable.addCell(pdfDataCell(String.format("%.1f%%", pct), bg, t.texto()));
                instTable.addCell(pdfDataCell(String.valueOf((int) e.getValue()[1]), bg, t.texto()));
                altInst = !altInst;
            }
            doc.add(instTable);
        }

        // ── 4. Evolução Mensal ─────────────────────────────────────────────────
        doc.add(pdfSubtituloSecao("4. Evolução Mensal", t));

        Table evolTable = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20, 20}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(6);
        for (String h : new String[]{"Mês / Ano", "Receitas", "Gastos", "Saldo", "Situação"}) {
            evolTable.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFontSize(8f).setBold().setFontColor(t.branco()))
                    .setBackgroundColor(t.primaria()).setPadding(6f).setBorder(Border.NO_BORDER));
        }

        for (Map.Entry<Integer, Map<Integer, List<EventoFinanceiro>>> anoEntry : porAnoMes.entrySet()) {
            int ano = anoEntry.getKey();
            for (Map.Entry<Integer, List<EventoFinanceiro>> mesEntry : anoEntry.getValue().entrySet()) {
                int mes = mesEntry.getKey();
                String nomeMes = capitalizar(Month.of(mes).getDisplayName(TextStyle.FULL, ptBR));
                double recM = 0, gasM = 0;
                for (EventoFinanceiro ev : mesEntry.getValue()) {
                    if (ev.getTipo() == Tipo.Recebimento || ev.getTipo() == Tipo.Emprestimo) recM += ev.getValor();
                    else if (ev.getTipo() == Tipo.Gasto) gasM += ev.getValor();
                }
                double saldoM = recM - gasM;
                boolean pos = saldoM >= 0;
                DeviceRgb bg = pos ? t.recBg() : t.gasBg();
                DeviceRgb fg = pos ? t.recFg() : t.gasFg();
                String situacao = pos ? "Positivo ✓" : "Negativo ✗";

                evolTable.addCell(pdfDataCell(nomeMes + "/" + ano, t.branco(), t.texto()));
                evolTable.addCell(pdfDataCell(formatarMoeda(recM), t.branco(), t.recFg()));
                evolTable.addCell(pdfDataCell(formatarMoeda(gasM), t.branco(), t.gasFg()));
                evolTable.addCell(pdfDataCell(formatarMoeda(saldoM), bg, fg));
                evolTable.addCell(pdfDataCell(situacao, bg, fg));
            }
        }
        doc.add(evolTable);

        // ── 5. Principais Indicadores ──────────────────────────────────────────
        doc.add(pdfSubtituloSecao("5. Principais Indicadores", t));

        // Maior gasto individual
        EventoFinanceiro maiorGasto = eventos.stream()
                .filter(e -> e.getTipo() == Tipo.Gasto)
                .max(Comparator.comparingDouble(EventoFinanceiro::getValor))
                .orElse(null);

        // Mês com maior gasto
        double[] maiores = {0};
        String[] mesMaiorGasto = {"-"};
        for (Map.Entry<Integer, Map<Integer, List<EventoFinanceiro>>> anoEntry : porAnoMes.entrySet()) {
            for (Map.Entry<Integer, List<EventoFinanceiro>> mesEntry : anoEntry.getValue().entrySet()) {
                double gasM = mesEntry.getValue().stream()
                        .filter(e -> e.getTipo() == Tipo.Gasto).mapToDouble(EventoFinanceiro::getValor).sum();
                if (gasM > maiores[0]) {
                    maiores[0] = gasM;
                    mesMaiorGasto[0] = capitalizar(Month.of(mesEntry.getKey()).getDisplayName(TextStyle.FULL, ptBR))
                            + "/" + anoEntry.getKey();
                }
            }
        }

        long qtdMeses = porAnoMes.values().stream().mapToLong(m -> m.size()).sum();
        double mediaGastoMes = qtdMeses > 0 ? totalGas.doubleValue() / qtdMeses : 0;
        long qtdPositivos = porAnoMes.values().stream().flatMap(m -> m.entrySet().stream())
                .filter(me -> {
                    double r = me.getValue().stream().filter(e -> e.getTipo() == Tipo.Recebimento || e.getTipo() == Tipo.Emprestimo).mapToDouble(EventoFinanceiro::getValor).sum();
                    double g = me.getValue().stream().filter(e -> e.getTipo() == Tipo.Gasto).mapToDouble(EventoFinanceiro::getValor).sum();
                    return r >= g;
                }).count();

        Table indicadores = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(6);

        Object[][] indRows = {
            {"Total de transações no período", String.valueOf(eventos.size())},
            {"Total de meses analisados", String.valueOf(qtdMeses)},
            {"Meses com saldo positivo", qtdPositivos + " de " + qtdMeses},
            {"Média de gastos mensais", formatarMoeda(mediaGastoMes)},
            {"Mês com maior volume de gastos", mesMaiorGasto[0] + " (" + formatarMoeda(maiores[0]) + ")"},
            {"Maior gasto individual", maiorGasto != null
                    ? formatarMoeda(maiorGasto.getValor()) + " — " +
                      (maiorGasto.getGastoDetalhe() != null ? maiorGasto.getGastoDetalhe().getTituloGasto() : "-") +
                      " (" + maiorGasto.getDataEvento().format(dtFmt) + ")"
                    : "-"},
            {"Categoria com maior impacto", catSorted.isEmpty() ? "-"
                    : catSorted.get(0).getKey() + " — " + formatarMoeda(catSorted.get(0).getValue()[0])},
            {"Instituição com maior volume", instSorted.isEmpty() ? "-"
                    : instSorted.get(0).getKey() + " — " + formatarMoeda(instSorted.get(0).getValue()[0])}
        };

        boolean altInd = false;
        for (Object[] row : indRows) {
            DeviceRgb bg = altInd ? t.cinza() : t.branco();
            indicadores.addCell(new Cell()
                    .add(new Paragraph(row[0].toString()).setFontSize(9f).setBold().setFontColor(t.primaria()))
                    .setBackgroundColor(bg).setPadding(7f).setBorder(new SolidBorder(t.bordaCinza(), 0.4f)));
            indicadores.addCell(new Cell()
                    .add(new Paragraph(row[1].toString()).setFontSize(9f).setFontColor(t.texto()))
                    .setBackgroundColor(bg).setPadding(7f).setBorder(new SolidBorder(t.bordaCinza(), 0.4f)));
            altInd = !altInd;
        }
        doc.add(indicadores);

        // ── 6. Saúde Financeira ────────────────────────────────────────────────
        doc.add(pdfSubtituloSecao("6. Saúde Financeira", t));

        int score = calcularScoreSaude(taxaGastos, taxaPoupanca, saldoGeral, qtdPositivos, qtdMeses);
        String[] classif = classificarSaude(score);

        Table saude = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(6);

        DeviceRgb saudeBg = score >= 75 ? t.recBg() : score >= 50 ? t.priLight() : score >= 30 ? t.traBg() : t.gasBg();
        DeviceRgb saudeFg = score >= 75 ? t.recFg() : score >= 50 ? t.primaria() : score >= 30 ? t.traFg() : t.gasFg();

        saude.addCell(new Cell()
                .add(new Paragraph("Pontuação: " + score + "/100 — " + classif[0])
                        .setFontSize(16f).setBold().setFontColor(saudeFg)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(saudeBg).setPadding(18f).setBorder(Border.NO_BORDER));
        doc.add(saude);

        // Descrição e recomendações
        doc.add(new Paragraph(classif[1])
                .setFontSize(9.5f).setFontColor(t.texto()).setMarginTop(8).setMarginLeft(4));

        doc.add(pdfSubtituloSecao("Critérios de avaliação:", t));

        String[][] criterios = {
            {"Taxa de Gastos (" + String.format("%.1f%%", taxaGastos) + ")",
             taxaGastos <= 60 ? "Excelente — abaixo de 60% da receita"
             : taxaGastos <= 75 ? "Bom — entre 60% e 75%"
             : taxaGastos <= 85 ? "Atenção — entre 75% e 85%"
             : "Crítico — acima de 85%"},
            {"Taxa de Poupança (" + String.format("%.1f%%", taxaPoupanca) + ")",
             taxaPoupanca >= 20 ? "Excelente — acima de 20%"
             : taxaPoupanca >= 10 ? "Bom — entre 10% e 20%"
             : taxaPoupanca >= 5  ? "Regular — entre 5% e 10%"
             : "Baixo — abaixo de 5%"},
            {"Saldo Geral", saldoGeral.compareTo(BigDecimal.ZERO) >= 0
             ? "Positivo (" + formatarMoeda(saldoGeral.doubleValue()) + ")"
             : "Negativo (" + formatarMoeda(saldoGeral.doubleValue()) + ") — requer atenção"},
            {"Meses positivos", qtdPositivos + " de " + qtdMeses
             + (qtdMeses > 0 ? " (" + String.format("%.0f%%", (double) qtdPositivos / qtdMeses * 100) + ")" : "")}
        };

        Table crit = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(4);
        boolean altCrit = false;
        for (String[] cr : criterios) {
            DeviceRgb bg = altCrit ? t.cinza() : t.branco();
            crit.addCell(new Cell()
                    .add(new Paragraph(cr[0]).setFontSize(9f).setBold().setFontColor(t.primaria()))
                    .setBackgroundColor(bg).setPadding(7f).setBorder(new SolidBorder(t.bordaCinza(), 0.4f)));
            crit.addCell(new Cell()
                    .add(new Paragraph(cr[1]).setFontSize(9f).setFontColor(t.texto()))
                    .setBackgroundColor(bg).setPadding(7f).setBorder(new SolidBorder(t.bordaCinza(), 0.4f)));
            altCrit = !altCrit;
        }
        doc.add(crit);

        // Rodapé
        doc.add(new Paragraph("\nRelatório gerado pelo MyFinance em " + LocalDate.now().format(dtFmt) +
                " | " + usuario.getNome() + " " + usuario.getSobrenome())
                .setFontSize(8f).setFontColor(t.priMed())
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
    }

    // ─── Helpers de score / classificação ────────────────────────────────────

    private int calcularScoreSaude(double taxaGastos, double taxaPoupanca,
                                    BigDecimal saldoGeral, long qtdPositivos, long qtdMeses) {
        int score = 50;

        // Fator taxa de gastos
        if (taxaGastos <= 60) score += 20;
        else if (taxaGastos <= 75) score += 10;
        else if (taxaGastos <= 85) score -= 5;
        else score -= 20;

        // Fator taxa de poupança
        if (taxaPoupanca >= 20) score += 20;
        else if (taxaPoupanca >= 10) score += 10;
        else if (taxaPoupanca >= 5) score += 2;
        else score -= 8;

        // Fator saldo geral
        if (saldoGeral.compareTo(BigDecimal.ZERO) >= 0) score += 10;
        else score -= 15;

        // Fator meses positivos
        if (qtdMeses > 0) {
            double pctPos = (double) qtdPositivos / qtdMeses;
            if (pctPos >= 0.8) score += 10;
            else if (pctPos >= 0.6) score += 5;
            else if (pctPos < 0.4) score -= 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String[] classificarSaude(int score) {
        if (score >= 80) return new String[]{
            "Excelente",
            "Suas finanças estão em ótima forma! Você mantém um bom controle de gastos, " +
            "possui uma taxa de poupança saudável e a maioria dos seus meses termina com saldo positivo. " +
            "Continue assim e considere diversificar seus investimentos para maximizar seu patrimônio."
        };
        if (score >= 60) return new String[]{
            "Boa",
            "Suas finanças estão em boa situação. Você demonstra controle razoável sobre seus gastos. " +
            "Há espaço para melhorar a taxa de poupança e reduzir gastos em categorias não essenciais " +
            "para alcançar a faixa de excelência."
        };
        if (score >= 40) return new String[]{
            "Regular",
            "Suas finanças precisam de atenção. Identifique as categorias onde os gastos estão elevados " +
            "e estabeleça metas mensais de poupança. Considere criar um fundo de emergência equivalente " +
            "a 3-6 meses de despesas."
        };
        if (score >= 20) return new String[]{
            "Atenção",
            "Sua situação financeira requer cuidados imediatos. Os gastos estão comprometendo " +
            "significativamente a receita. Revise seus hábitos de consumo, priorize dívidas e " +
            "procure reduzir gastos supérfluos. Estabeleça um orçamento mensal rígido."
        };
        return new String[]{
            "Crítica",
            "Situação financeira crítica. É necessário agir urgentemente: revisar todos os gastos, " +
            "eliminar despesas não essenciais, buscar fontes adicionais de renda e, se necessário, " +
            "procurar orientação financeira profissional."
        };
    }

    // ─── Helpers de layout PDF ───────────────────────────────────────────────

    private Table pdfTituloSecao(String texto, PdfTheme t) {
        Table tbl = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(10);
        tbl.addCell(new Cell()
                .add(new Paragraph(texto).setFontSize(14f).setBold().setFontColor(t.branco()))
                .setBackgroundColor(t.primaria()).setPadding(10f).setBorder(Border.NO_BORDER));
        return tbl;
    }

    private Paragraph pdfSubtituloSecao(String texto, PdfTheme t) {
        return new Paragraph(texto)
                .setFontSize(11f).setBold().setFontColor(t.primaria())
                .setMarginTop(14).setMarginBottom(4);
    }

    private Cell pdfBoxCell(String texto, DeviceRgb bg, DeviceRgb fg) {
        return new Cell()
                .add(new Paragraph(texto).setFontSize(9f).setBold().setFontColor(fg)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(bg).setPadding(7f)
                .setBorder(new SolidBorder(new DeviceRgb(255, 255, 255), 1f));
    }

    private Cell pdfDataCell(String texto, DeviceRgb bg, DeviceRgb fg) {
        return new Cell()
                .add(new Paragraph(texto).setFontSize(8.5f).setFontColor(fg))
                .setBackgroundColor(bg).setPadding(5f)
                .setBorder(new SolidBorder(new DeviceRgb(206, 212, 218), 0.3f));
    }

    // ─── Formatação de valores ───────────────────────────────────────────────

    private String formatarMoeda(double valor) {
        return String.format(new Locale("pt", "BR"), "R$ %,.2f", valor);
    }

    private String truncar(String texto, int limite) {
        if (texto == null) return "-";
        return texto.length() > limite ? texto.substring(0, limite - 1) + "…" : texto;
    }

    // =========================================================================
    // UTILITÁRIOS
    // =========================================================================

    private Usuario buscarUsuarioOuErro(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "Usuário de id: %s não encontrado.".formatted(id)));
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    // =========================================================================
    // RECORD — resultado de exportação
    // =========================================================================

    /** Encapsula o resultado de uma exportação: bytes, nome do arquivo e content-type. */
    public record ExportacaoResultado(byte[] conteudo, String nomeArquivo, String contentType) {}
}


