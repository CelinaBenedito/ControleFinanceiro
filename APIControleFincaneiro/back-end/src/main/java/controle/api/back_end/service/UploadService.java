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
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.EventoDetalhe;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.Tipo;
import controle.api.back_end.model.eventoFinanceiro.TipoMovimento;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.categoria.CategoriaUsuarioRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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
                         CategoriaUsuarioRepository categoriaUsuarioRepository) {
        this.registroService = registroService;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
    }

    // =====================================================================
    // JSON IMPORT
    // =====================================================================

    public ImportResultDto importFromJson(UUID userId, byte[] content) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();

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

            for (Map<String, Object> evento : eventos) {
                String eventoId = (String) evento.get("id");
                try {
                    EventoFinanceiro financeiro = buildEventoFinanceiro(userId, evento);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> instList =
                            (List<Map<String, Object>>) evento.get("instituicoes");
                    List<EventoInstituicao> instituicoes = buildInstituicoesFromIds(instList);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> gastoMap =
                            (Map<String, Object>) evento.get("gasto_detalhe");
                    EventoDetalhe detalhe = buildDetalheFromJson(gastoMap);

                    RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe);
                    if (dto != null) importados.add(dto);

                } catch (Exception e) {
                    erros.add("Evento '" + eventoId + "': " + e.getMessage());
                }
            }

        } catch (Exception e) {
            erros.add("Erro ao processar arquivo JSON: " + e.getMessage());
        }

        return new ImportResultDto(importados.size(), importados, erros);
    }

    // =====================================================================
    // SQL IMPORT
    // =====================================================================

    public ImportResultDto importFromSql(UUID userId, byte[] content) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        String sqlContent = new String(content, StandardCharsets.UTF_8);
        String[] lines = sqlContent.split("\n");

        // Ordered map to preserve insertion order (event id → fields)
        Map<String, Map<String, Object>> eventos = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> instituicoesMap = new HashMap<>();
        Map<String, Map<String, Object>> detalhesMap = new HashMap<>();   // eventoId → detalhe
        Map<String, List<Integer>> categoriaDetalheMap = new HashMap<>();  // gastoId  → categoria ids

        // Patterns based on the SQL generated by RegistroService.createSql()
        Pattern eventoPattern = Pattern.compile(
                "INSERT INTO evento_financeiro \\([^)]+\\) VALUES \\('([0-9a-f\\-]+)',\\s*'[0-9a-f\\-]+',\\s*'([^']+)',\\s*([\\d.]+),\\s*'((?:[^']|'')*)',\\s*'(\\d{4}-\\d{2}-\\d{2})',",
                Pattern.CASE_INSENSITIVE
        );
        Pattern instPattern = Pattern.compile(
                "INSERT INTO evento_instituicao \\([^)]+\\) VALUES \\('[^']+',\\s*'([0-9a-f\\-]+)',\\s*(\\d+),\\s*'([^']+)',\\s*([\\d.]+),\\s*(\\d+)\\)",
                Pattern.CASE_INSENSITIVE
        );
        Pattern gastoPattern = Pattern.compile(
                "INSERT INTO gasto_detalhe \\([^)]+\\) VALUES \\('(\\d+)',\\s*'([0-9a-f\\-]+)',\\s*'((?:[^']|'')*)'\\)",
                Pattern.CASE_INSENSITIVE
        );
        Pattern catPattern = Pattern.compile(
                "INSERT INTO gasto_detalhe_categoria \\([^)]+\\) VALUES \\('(\\d+)',\\s*'(\\d+)'\\)",
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

            Matcher mi = instPattern.matcher(line);
            if (mi.find()) {
                String eventoId = mi.group(1);
                Map<String, Object> inst = new LinkedHashMap<>();
                inst.put("instituicao_usuario_id", Integer.parseInt(mi.group(2)));
                inst.put("tipo_movimento", mi.group(3));
                inst.put("valor", Double.parseDouble(mi.group(4)));
                inst.put("parcelas", Integer.parseInt(mi.group(5)));
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
                int catId = Integer.parseInt(mc.group(2));
                categoriaDetalheMap.computeIfAbsent(gastoId, k -> new ArrayList<>()).add(catId);
            }
        }

        for (Map.Entry<String, Map<String, Object>> entry : eventos.entrySet()) {
            String eventoId = entry.getKey();
            Map<String, Object> ev = entry.getValue();

            try {
                EventoFinanceiro financeiro = buildEventoFinanceiro(userId, ev);

                List<EventoInstituicao> instituicoes = new ArrayList<>();
                for (Map<String, Object> inst : instituicoesMap.getOrDefault(eventoId, List.of())) {
                    EventoInstituicao ei = new EventoInstituicao();
                    InstituicaoUsuario iu = new InstituicaoUsuario();
                    iu.setId((Integer) inst.get("instituicao_usuario_id"));
                    ei.setInstituicaoUsuario(iu);
                    ei.setTipoMovimento(TipoMovimento.valueOf((String) inst.get("tipo_movimento")));
                    ei.setValor((Double) inst.get("valor"));
                    ei.setParcelas((Integer) inst.get("parcelas"));
                    instituicoes.add(ei);
                }

                Map<String, Object> gastoMap = detalhesMap.get(eventoId);
                EventoDetalhe detalhe = new EventoDetalhe();
                if (gastoMap != null) {
                    String gastoId = (String) gastoMap.get("id");
                    detalhe.setTituloGasto((String) gastoMap.get("titulo_gasto"));
                    List<CategoriaUsuario> categorias = new ArrayList<>();
                    for (Integer catId : categoriaDetalheMap.getOrDefault(gastoId, List.of())) {
                        CategoriaUsuario cu = new CategoriaUsuario();
                        cu.setId(catId);
                        categorias.add(cu);
                    }
                    detalhe.setCategoriaUsuario(categorias);
                } else {
                    detalhe.setTituloGasto("Registro Importado");
                    detalhe.setCategoriaUsuario(new ArrayList<>());
                }

                RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe);
                if (dto != null) importados.add(dto);

            } catch (Exception e) {
                erros.add("Evento '" + eventoId + "': " + e.getMessage());
            }
        }

        return new ImportResultDto(importados.size(), importados, erros);
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
    // PDF IMPORT
    // =====================================================================

    public ImportResultDto importFromPdf(UUID userId, byte[] content) {
        List<RegistroResponseDto> importados = new ArrayList<>();
        List<String> erros = new ArrayList<>();

        try {
            // Load institution names and category titles for smart matching
            List<InstituicaoUsuario> userInstituicoes =
                    instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
            List<CategoriaUsuario> userCategorias =
                    categoriaUsuarioRepository.findAllByUsuario_Id(userId);

            List<String> instNomes = userInstituicoes.stream()
                    .map(i -> i.getInstituicao().getNome())
                    .toList();

            String tipoRegex = "Gasto|Recebimento|Transferencia|Poupanca|Emprestimo";
            String movRegex  = "Debito|Credito|Dinheiro|Pix|Boleto|Voucher";

            // Row pattern: <day> <...> <valor> <Tipo> <...> <TipoMovimento> <parcelas> <...>
            Pattern rowPattern = Pattern.compile(
                    "^(\\d{1,2})\\s+(.+?)\\s+([\\d]+[.,]?[\\d]*)\\s+(" + tipoRegex + ")\\s+(.*?)\\s+(" + movRegex + ")\\s+(\\d+)\\s+(.*)$"
            );

            int currentYear  = LocalDate.now().getYear();
            int currentMonth = 1;
            boolean inTable  = false;
            boolean pastSummary = false; // skip sumário pages

            try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(content)))) {

                for (int page = 1; page <= pdfDoc.getNumberOfPages(); page++) {
                    String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page));
                    String[] lines = pageText.split("\n");

                    for (String rawLine : lines) {
                        String line = rawLine.trim();
                        if (line.isEmpty()) continue;

                        // Skip sumário section (first page)
                        if (line.equalsIgnoreCase("Sumário") || line.startsWith("MyFinance")) {
                            pastSummary = false;
                            inTable = false;
                            continue;
                        }

                        // Detect year (4-digit integer)
                        if (line.matches("^\\d{4}$")) {
                            currentYear = Integer.parseInt(line);
                            pastSummary = true;
                            inTable = false;
                            continue;
                        }

                        // Detect Portuguese month name
                        Integer mes = MESES_PT.get(line.toLowerCase());
                        if (mes != null) {
                            currentMonth = mes;
                            inTable = false;
                            continue;
                        }

                        // Detect table header
                        if (line.contains("Título") && line.contains("Valor") && line.contains("Tipo")) {
                            inTable = pastSummary;
                            continue;
                        }

                        // Detect end of table (summary section)
                        if (line.startsWith("Resumo") || line.startsWith("Ganhos") || line.startsWith("Saldo")) {
                            inTable = false;
                            continue;
                        }

                        if (!inTable) continue;

                        Matcher m = rowPattern.matcher(line);
                        if (!m.find()) continue;

                        try {
                            int dia          = Integer.parseInt(m.group(1));
                            String titulo    = m.group(2).trim();
                            double valor     = Double.parseDouble(m.group(3).replace(",", "."));
                            Tipo tipo        = Tipo.valueOf(m.group(4));
                            String afterTipo = m.group(5).trim();
                            TipoMovimento mv = TipoMovimento.valueOf(m.group(6));
                            int parcelas     = Integer.parseInt(m.group(7));
                            String catStr    = m.group(8).trim();

                            // Split afterTipo into description + institution name
                            String descricao = afterTipo;
                            String instNome  = null;
                            for (String nome : instNomes) {
                                if (afterTipo.contains(nome)) {
                                    instNome = nome;
                                    descricao = afterTipo.replace(nome, "").trim();
                                    break;
                                }
                            }

                            LocalDate data = LocalDate.of(currentYear, currentMonth, dia);

                            EventoFinanceiro financeiro = new EventoFinanceiro();
                            financeiro.setUsuario(getUsuario(userId));
                            financeiro.setTipo(tipo);
                            financeiro.setValor(valor);
                            financeiro.setDescricao(descricao);
                            financeiro.setDataEvento(data);

                            List<EventoInstituicao> instituicoes = new ArrayList<>();
                            if (instNome != null) {
                                final String finalInstNome = instNome;
                                userInstituicoes.stream()
                                        .filter(i -> i.getInstituicao().getNome().equals(finalInstNome))
                                        .findFirst()
                                        .ifPresent(iu -> {
                                            EventoInstituicao ei = new EventoInstituicao();
                                            ei.setInstituicaoUsuario(iu);
                                            ei.setTipoMovimento(mv);
                                            ei.setValor(valor);
                                            ei.setParcelas(parcelas);
                                            instituicoes.add(ei);
                                        });
                            }

                            EventoDetalhe detalhe = new EventoDetalhe();
                            detalhe.setTituloGasto(titulo.isEmpty() ? "Registro Importado" : titulo);
                            List<CategoriaUsuario> categorias = new ArrayList<>();
                            final String finalCatStr = catStr;
                            userCategorias.stream()
                                    .filter(c -> c.getCategoria().getTitulo().equals(finalCatStr))
                                    .findFirst()
                                    .ifPresent(categorias::add);
                            detalhe.setCategoriaUsuario(categorias);

                            RegistroResponseDto dto = persistirRegistro(financeiro, instituicoes, detalhe);
                            if (dto != null) importados.add(dto);

                        } catch (Exception e) {
                            erros.add("PDF página " + page + " - '" + line.substring(0, Math.min(line.length(), 60)) + "': " + e.getMessage());
                        }
                    }
                }
            }

        } catch (Exception e) {
            erros.add("Erro ao processar arquivo PDF: " + e.getMessage());
        }

        if (importados.isEmpty() && erros.isEmpty()) {
            erros.add("Nenhum registro importado do PDF. O PDF pode não conter tabelas de dados reconhecíveis. " +
                    "Para importação confiável, prefira o formato JSON ou Excel.");
        }

        return new ImportResultDto(importados.size(), importados, erros);
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
                    Tipo tipo        = ofxMapTrnType(trnType, amount);
                    LocalDate data   = ofxParseDate(dtPosted);

                    // EventoFinanceiro
                    EventoFinanceiro financeiro = new EventoFinanceiro();
                    financeiro.setUsuario(getUsuario(userId));
                    financeiro.setTipo(tipo);
                    financeiro.setValor(absAmount);
                    String descricao = memo != null && !memo.isBlank() ? memo
                            : (name != null ? name : "Importado via OFX");
                    financeiro.setDescricao(descricao.length() > 500 ? descricao.substring(0, 500) : descricao);
                    financeiro.setDataEvento(data);

                    // EventoInstituicao (somente se encontrou correspondência)
                    List<EventoInstituicao> instituicoes = new ArrayList<>();
                    if (matchedInstituicao != null) {
                        EventoInstituicao ei = new EventoInstituicao();
                        ei.setInstituicaoUsuario(matchedInstituicao);
                        ei.setTipoMovimento(amount < 0 ? TipoMovimento.Debito : TipoMovimento.Credito);
                        ei.setValor(absAmount);
                        ei.setParcelas(1);
                        instituicoes.add(ei);
                    }

                    // EventoDetalhe
                    EventoDetalhe detalhe = new EventoDetalhe();
                    String titulo = name != null && !name.isBlank() ? name : "Registro OFX";
                    detalhe.setTituloGasto(titulo.length() > 50 ? titulo.substring(0, 50) : titulo);
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

    private List<EventoInstituicao> buildInstituicoesFromIds(List<Map<String, Object>> instList) {
        List<EventoInstituicao> result = new ArrayList<>();
        if (instList == null) return result;
        for (Map<String, Object> inst : instList) {
            EventoInstituicao ei = new EventoInstituicao();
            InstituicaoUsuario iu = new InstituicaoUsuario();
            iu.setId(((Number) inst.get("instituicao_usuario_id")).intValue());
            ei.setInstituicaoUsuario(iu);
            ei.setTipoMovimento(TipoMovimento.valueOf((String) inst.get("tipo_movimento")));
            ei.setValor(((Number) inst.get("valor")).doubleValue());
            ei.setParcelas(((Number) inst.get("parcelas")).intValue());
            result.add(ei);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private EventoDetalhe buildDetalheFromJson(Map<String, Object> gastoMap) {
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
                CategoriaUsuario cu = new CategoriaUsuario();
                cu.setId(((Number) cat.get("id")).intValue());
                categorias.add(cu);
            }
        }
        detalhe.setCategoriaUsuario(categorias);
        return detalhe;
    }

    private RegistroResponseDto persistirRegistro(EventoFinanceiro financeiro,
                                                   List<EventoInstituicao> instituicoes,
                                                   EventoDetalhe detalhe) {
        Registro registro = registroService.createEventoFinanceiro(financeiro, instituicoes, detalhe);
        EventoFinanceiro ef = registro.getEventosFinanceiros().getFirst();
        List<EventoInstituicao> eis = registro.getInstituicoesPorEvento().getOrDefault(ef, List.of());
        EventoDetalhe ed = registro.getDetalhePorEvento().get(ef);
        if (ed == null) return null;
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
                    Tipo tipo        = ofxMapTrnType(trnType, amount);
                    LocalDate data   = ofxParseDate(dtPosted);

                    EventoFinanceiro financeiro = new EventoFinanceiro();
                    financeiro.setUsuario(getUsuario(userId));
                    financeiro.setTipo(tipo);
                    financeiro.setValor(absAmount);
                    String descricao = memo != null && !memo.isBlank() ? memo : (name != null ? name : "Importado via OFX");
                    financeiro.setDescricao(descricao.length() > 500 ? descricao.substring(0, 500) : descricao);
                    financeiro.setDataEvento(data);

                    List<EventoInstituicao> instituicoes = new ArrayList<>();
                    if (matchedInstituicao != null) {
                        EventoInstituicao ei = new EventoInstituicao();
                        ei.setInstituicaoUsuario(matchedInstituicao);
                        ei.setTipoMovimento(detectTipoMovimento(memo != null ? memo : "", amount));
                        ei.setValor(absAmount);
                        ei.setParcelas(1);
                        instituicoes.add(ei);
                    }

                    EventoDetalhe detalhe = new EventoDetalhe();
                    // Campo NAME do OFX pode conter "Loja     Cidade     Pais" com espacos duplos
                    detalhe.setTituloGasto(sanitizeMerchant(name != null && !name.isBlank() ? name : "Registro OFX"));
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


