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
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Link;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
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
import java.time.LocalDate;
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
            sql.append("INSERT INTO limite_por_instituicao (id, institucao_usuario_id, limite_desejado, configuracoes_id) VALUES (")
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
    // para reativar gráficos de pizza, atualize poi/poi-ooxml para 5.x.
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
    // EXPORTAÇÃO PDF
    // =========================================================================

    private ExportacaoResultado exportarPdf(Usuario usuario) {
        DeviceRgb tealEscuro  = new DeviceRgb(54,  115, 115);
        DeviceRgb tealClaro   = new DeviceRgb(180, 217, 213);
        DeviceRgb textoEscuro = new DeviceRgb(26,  26,  26);
        DeviceRgb textoClaro  = new DeviceRgb(255, 255, 255);
        DeviceRgb vermelhoFundo = new DeviceRgb(255, 226, 226);
        DeviceRgb vermelhoTexto = new DeviceRgb(185, 28,  28);
        DeviceRgb verdeFundo    = new DeviceRgb(209, 250, 229);
        DeviceRgb verdeTexto    = new DeviceRgb(21,  128, 61);

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository
                .findEventoFinanceiroByUsuarioOrderByDataEventoDesc(usuario);
        eventos.sort(Comparator.comparing(EventoFinanceiro::getDataEvento).reversed());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter pdfWriter = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(pdfWriter);
            Document doc = new Document(pdf);

            doc.add(new Paragraph("MyFinance — Registros")
                    .setBold().setFontSize(20)
                    .setFontColor(tealEscuro).setBackgroundColor(tealClaro));
            doc.add(new Paragraph("Nome: " + usuario.getNome() + " " + usuario.getSobrenome()));
            doc.add(new Paragraph("Nascimento: " + usuario.getDataNascimento() + "  |  Sexo: " + usuario.getSexo()));
            doc.add(new Paragraph("Email: " + usuario.getEmail()));

            doc.add(new Paragraph("Sumário").setBold().setFontSize(18)
                    .setFontColor(tealEscuro).setMarginBottom(10));
            int anoSumario = -1;
            Set<String> mesesSumario = new HashSet<>();
            for (EventoFinanceiro ev : eventos) {
                LocalDate data = ev.getDataEvento();
                String nomeMes = capitalizar(data.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR")));
                if (data.getYear() != anoSumario) {
                    anoSumario = data.getYear();
                    doc.add(new Paragraph(new Link(String.valueOf(anoSumario),
                            PdfAction.createGoTo("ano_" + anoSumario)))
                            .setBold().setFontSize(14).setFontColor(tealEscuro).setMarginTop(10));
                }
                String chaveMes = anoSumario + "-" + data.getMonthValue();
                if (mesesSumario.add(chaveMes)) {
                    doc.add(new Paragraph(new Link("   " + nomeMes,
                            PdfAction.createGoTo("mes_" + data.getYear() + "_" + data.getMonthValue())))
                            .setFontSize(12).setFontColor(textoEscuro).setMarginLeft(20));
                    doc.add(new Paragraph(new Link("      Resumo de " + nomeMes,
                            PdfAction.createGoTo("resumo_mes_" + data.getYear() + "_" + data.getMonthValue())))
                            .setFontSize(11).setFontColor(textoEscuro).setMarginLeft(25));
                }
            }
            doc.add(new AreaBreak());

            int anoAtual = -1;
            int mesAtual = -1;
            BigDecimal ganhosMes = BigDecimal.ZERO;
            BigDecimal gastosMes = BigDecimal.ZERO;
            BigDecimal ganhosAno = BigDecimal.ZERO;
            BigDecimal gastosAno = BigDecimal.ZERO;
            Table tabela = new Table(9);

            for (int i = 0; i < eventos.size(); i++) {
                EventoFinanceiro ev = eventos.get(i);
                LocalDate data = ev.getDataEvento();

                if (data.getYear() != anoAtual) {
                    anoAtual = data.getYear();
                    doc.add(new Paragraph(String.valueOf(anoAtual))
                            .setBold().setFontSize(18).setFontColor(tealEscuro).setMarginTop(20));
                    PdfExplicitDestination destAno = PdfExplicitDestination.createFit(pdf.getLastPage());
                    pdf.addNamedDestination("ano_" + anoAtual, destAno.getPdfObject());
                }

                if (data.getMonthValue() != mesAtual) {
                    ganhosMes = BigDecimal.ZERO;
                    gastosMes = BigDecimal.ZERO;
                    mesAtual = data.getMonthValue();
                    String nomeMesDisplay = capitalizar(data.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR")));
                    doc.add(new Paragraph(nomeMesDisplay)
                            .setBold().setFontSize(14).setFontColor(textoEscuro).setMarginLeft(20));
                    PdfExplicitDestination destMes = PdfExplicitDestination.createFit(pdf.getLastPage());
                    pdf.addNamedDestination("mes_" + anoAtual + "_" + data.getMonthValue(), destMes.getPdfObject());

                    tabela = new Table(9);
                    String[] cabecalhos = {"Dia", "Título", "Valor", "Tipo", "Descrição",
                                           "Instituições", "Movimentação", "Parcelas", "Categorias"};
                    for (String h : cabecalhos) {
                        tabela.addCell(new Cell().add(new Paragraph(h).setBold().setFontColor(textoClaro))
                                .setBackgroundColor(tealEscuro));
                    }
                }

                EventoDetalhe detalhe = ev.getGastoDetalhe();
                String titulo = (detalhe != null) ? detalhe.getTituloGasto() : "-";
                String categorias = (detalhe != null && detalhe.getCategoriaUsuario() != null)
                        ? detalhe.getCategoriaUsuario().stream()
                                 .map(c -> c.getCategoria().getTitulo())
                                 .reduce((a, b) -> a + " / " + b).orElse("-")
                        : "-";

                List<EventoInstituicao> insts = eventoInstituicaoRepository
                        .findEventoInstituicaoByEventoFinanceiro_Id(ev.getId());
                String instNome    = insts.isEmpty() ? "-" : insts.get(0).getInstituicaoUsuario().getInstituicao().getNome();
                String tipoMov     = insts.isEmpty() ? "-" : insts.get(0).getTipoMovimento().toString();
                String parcelasStr = insts.isEmpty() ? "-" : String.valueOf(insts.get(0).getParcelas());

                tabela.addCell(String.valueOf(data.getDayOfMonth()));
                tabela.addCell(titulo);
                tabela.addCell(ev.getValor().toString());
                tabela.addCell(ev.getTipo().toString());
                tabela.addCell(ev.getDescricao() != null ? ev.getDescricao() : "-");
                tabela.addCell(instNome);
                tabela.addCell(tipoMov);
                tabela.addCell(parcelasStr);
                tabela.addCell(categorias);

                if (ev.getTipo() == Tipo.Recebimento) {
                    ganhosMes = ganhosMes.add(BigDecimal.valueOf(ev.getValor()));
                } else if (ev.getTipo() == Tipo.Gasto || ev.getTipo() == Tipo.Transferencia) {
                    gastosMes = gastosMes.add(BigDecimal.valueOf(ev.getValor()));
                }

                boolean ultimoEvento = (i == eventos.size() - 1);
                boolean proximoMesDiferente = !ultimoEvento
                        && eventos.get(i + 1).getDataEvento().getMonthValue() != mesAtual;

                if (ultimoEvento || proximoMesDiferente) {
                    doc.add(tabela);
                    String nomeMesResumo = capitalizar(data.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR")));
                    doc.add(new Paragraph("Resumo de " + nomeMesResumo)
                            .setBold().setFontSize(12).setFontColor(tealEscuro).setMarginTop(10));
                    PdfPage paginaResumo = pdf.getLastPage();
                    PdfExplicitDestination destResumo = PdfExplicitDestination.createFit(paginaResumo);
                    pdf.addNamedDestination("resumo_mes_" + anoAtual + "_" + mesAtual, destResumo.getPdfObject());

                    Table resumoMes = new Table(3).setWidth(UnitValue.createPercentValue(100));
                    resumoMes.addCell(new Cell().add(new Paragraph("Ganhos")).setBackgroundColor(tealClaro).setFontColor(tealEscuro).setBold());
                    resumoMes.addCell(new Cell().add(new Paragraph("Gastos")).setBackgroundColor(vermelhoFundo).setFontColor(vermelhoTexto).setBold());
                    resumoMes.addCell(new Cell().add(new Paragraph("Saldo")).setBackgroundColor(verdeFundo).setFontColor(verdeTexto).setBold());
                    resumoMes.addCell(new Cell().add(new Paragraph("R$ " + ganhosMes)));
                    resumoMes.addCell(new Cell().add(new Paragraph("R$ " + gastosMes)));
                    resumoMes.addCell(new Cell().add(new Paragraph("R$ " + ganhosMes.subtract(gastosMes))));
                    doc.add(resumoMes);
                    doc.add(new AreaBreak());

                    ganhosAno = ganhosAno.add(ganhosMes);
                    gastosAno = gastosAno.add(gastosMes);
                }
            }

            doc.add(new Paragraph("Resumo do Ano " + anoAtual)
                    .setBold().setFontSize(14).setFontColor(tealEscuro).setMarginTop(15));
            Table resumoAno = new Table(3).setWidth(UnitValue.createPercentValue(100));
            resumoAno.addCell(new Cell().add(new Paragraph("Ganhos")).setBackgroundColor(tealClaro).setFontColor(tealEscuro).setBold());
            resumoAno.addCell(new Cell().add(new Paragraph("Gastos")).setBackgroundColor(vermelhoFundo).setFontColor(vermelhoTexto).setBold());
            resumoAno.addCell(new Cell().add(new Paragraph("Saldo")).setBackgroundColor(verdeFundo).setFontColor(verdeTexto).setBold());
            resumoAno.addCell(new Cell().add(new Paragraph("R$ " + ganhosAno)));
            resumoAno.addCell(new Cell().add(new Paragraph("R$ " + gastosAno)));
            resumoAno.addCell(new Cell().add(new Paragraph("R$ " + ganhosAno.subtract(gastosAno))));
            doc.add(resumoAno);
            doc.close();

            return new ExportacaoResultado(out.toByteArray(),
                    "registros_%s_%s.pdf".formatted(usuario.getNome(), LocalDate.now()),
                    "application/pdf");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar exportação PDF.", e);
        }
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


