package controle.api.back_end.service.ofx;

import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê arquivos .ofx da pasta uploads/ofx, importa as transações como
 * EventoFinanceiro no banco de dados e apaga o arquivo após o processamento.
 *
 * <h3>Formato OFX suportado</h3>
 * Suporta OFX SGML (versão legada — maioria dos bancos BR) e OFX XML (versão moderna).
 * Deteção automática baseada no cabecalho.
 *
 * <h3>Encoding</h3>
 * Le o campo CHARSET do cabecalho OFX:
 * - 1252 → Windows-1252 (maioria dos bancos BR)
 * - UTF-8 → UTF-8
 * - Outros → ISO-8859-1 como fallback
 *
 * <h3>Mapeamento TRNTYPE → Tipo</h3>
 * - CREDIT, INT, DIV, DEP, DIRECTDEP, REPEATPMT → Recebimento
 * - XFER → Transferencia
 * - DEBIT, ATM, POS, CHECK, PAYMENT, CASH, DIRECTDEBIT, FEE, SRVCHG, OTHER → Gasto
 * - Sinal do TAMT tem prioridade: negativo = Gasto, positivo = Recebimento
 *
 * <h3>Descrição</h3>
 * Combina NAME (estabelecimento) + MEMO (descrição) para maior detalhe.
 *
 * <h3>Deduplicação</h3>
 * FITIDs já processados ficam em uploads/ofx/.processed_ids.
 */
@Service
public class OFXImportService {

    private static final Logger log = Logger.getLogger(OFXImportService.class.getName());
    private static final String PROCESSED_IDS_FILE = ".processed_ids";

    @Value("${ofx.output.dir:uploads/ofx}")
    private String ofxOutputDir;

    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoDetalheRepository eventoDetalheRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final UsuarioRepository usuarioRepository;

    public OFXImportService(EventoFinanceiroRepository eventoFinanceiroRepository,
                             EventoInstituicaoRepository eventoInstituicaoRepository,
                             EventoDetalheRepository eventoDetalheRepository,
                             InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                             UsuarioRepository usuarioRepository) {
        this.eventoFinanceiroRepository   = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository  = eventoInstituicaoRepository;
        this.eventoDetalheRepository      = eventoDetalheRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.usuarioRepository            = usuarioRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ResultadoImportacao importar(String nomeArquivo, UUID userId, Integer instituicaoUsuarioId) {
        Path arquivo = Paths.get(ofxOutputDir).toAbsolutePath().resolve(nomeArquivo);

        if (!Files.exists(arquivo)) {
            throw new EntidadeNaoEncontradaException("Arquivo OFX nao encontrado: " + arquivo);
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuario " + userId + " nao encontrado"));

        InstituicaoUsuario instUsuario = instituicaoUsuarioRepository.findById(instituicaoUsuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException(
                        "InstituicaoUsuario " + instituicaoUsuarioId + " nao encontrado"));

        try {
            // Detecta encoding pelo cabecalho antes de ler o conteudo completo
            Charset charset = detectarEncoding(arquivo);
            log.info("[OFXImportService] Encoding detectado: " + charset.name() + " | arquivo: " + nomeArquivo);

            String conteudo = Files.readString(arquivo, charset);
            List<OFXTransacao> transacoes = parsearOFX(conteudo);
            log.info("[OFXImportService] " + transacoes.size() + " transacoes encontradas no OFX");

            Set<String> processados = carregarProcessados();
            int importadas = 0;
            int duplicadas = 0;
            List<String> erros = new ArrayList<>();

            for (OFXTransacao tx : transacoes) {
                if (processados.contains(tx.fitId())) {
                    duplicadas++;
                    continue;
                }
                try {
                    salvarTransacao(tx, usuario, instUsuario);
                    processados.add(tx.fitId());
                    importadas++;
                } catch (Exception e) {
                    log.warning("[OFXImportService] Erro ao salvar FITID " + tx.fitId() + ": " + e.getMessage());
                    erros.add("FITID " + tx.fitId() + ": " + e.getMessage());
                }
            }

            salvarProcessados(processados);
            deletarArquivo(arquivo);

            log.info("[OFXImportService] Concluido — importadas: " + importadas +
                    " | duplicadas: " + duplicadas + " | erros: " + erros.size());

            return new ResultadoImportacao(importadas, duplicadas, erros);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo OFX: " + nomeArquivo, e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deteção de encoding
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Le os primeiros bytes do arquivo para detectar o campo CHARSET do cabecalho OFX.
     * Exemplos de cabecalho:
     *   CHARSET:1252
     *   CHARSET:ISO-8859-1
     *   CHARSET:UTF-8
     */
    private Charset detectarEncoding(Path arquivo) throws IOException {
        // Le apenas os primeiros 512 bytes em Latin-1 para extrair o cabecalho
        byte[] primeirosBytes = Arrays.copyOf(Files.readAllBytes(arquivo),
                (int) Math.min(512, Files.size(arquivo)));
        String cabecalho = new String(primeirosBytes, StandardCharsets.ISO_8859_1);

        Matcher m = Pattern.compile("CHARSET[:\\s]+(\\S+)", Pattern.CASE_INSENSITIVE).matcher(cabecalho);
        if (m.find()) {
            String valor = m.group(1).trim().toUpperCase();
            return switch (valor) {
                case "1252", "WINDOWS-1252", "CP1252" -> Charset.forName("windows-1252");
                case "UTF-8", "UTF8"                   -> StandardCharsets.UTF_8;
                case "ISO-8859-1", "ISO8859-1",
                     "LATIN-1", "1"                    -> StandardCharsets.ISO_8859_1;
                default -> {
                    log.warning("[OFXImportService] CHARSET desconhecido '" + valor + "' — usando windows-1252");
                    yield Charset.forName("windows-1252");
                }
            };
        }

        // OFX XML geralmente e UTF-8 sem cabecalho CHARSET
        if (cabecalho.contains("<?xml") || cabecalho.contains("<?OFX")) {
            return StandardCharsets.UTF_8;
        }

        // Padrao para bancos brasileiros: windows-1252
        return Charset.forName("windows-1252");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parser OFX principal
    // ─────────────────────────────────────────────────────────────────────────

    private List<OFXTransacao> parsearOFX(String conteudo) {
        // OFX XML: comeca com <?OFX, <?xml ou <OFX>
        String inicio = conteudo.stripLeading().substring(0, Math.min(20, conteudo.length())).toUpperCase();
        if (inicio.startsWith("<?OFX") || inicio.startsWith("<?XML") || inicio.startsWith("<OFX>")) {
            return parsearOFXXml(conteudo);
        }
        return parsearOFXSgml(conteudo);
    }

    /**
     * Parser para o formato SGML legado (maioria dos bancos brasileiros).
     * Extrai blocos STMTTRN e campos chave:valor.
     */
    private List<OFXTransacao> parsearOFXSgml(String conteudo) {
        List<OFXTransacao> lista = new ArrayList<>();

        // Descarta o cabecalho SGML (tudo antes de <OFX)
        int ofxStart = conteudo.indexOf("<OFX");
        if (ofxStart < 0) ofxStart = conteudo.indexOf("<ofx");
        if (ofxStart > 0) conteudo = conteudo.substring(ofxStart);

        Pattern blocoPattern = Pattern.compile(
                "<STMTTRN>(.*?)</STMTTRN>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Matcher blocoMatcher = blocoPattern.matcher(conteudo);

        while (blocoMatcher.find()) {
            String bloco = blocoMatcher.group(1);
            try {
                OFXTransacao tx = parsearBloco(bloco);
                if (tx != null) lista.add(tx);
            } catch (Exception e) {
                log.warning("[OFXImportService] Bloco ignorado: " + e.getMessage());
            }
        }
        return lista;
    }

    /** OFX XML reutiliza o mesmo parser SGML pois a estrutura das tags e identica. */
    private List<OFXTransacao> parsearOFXXml(String conteudo) {
        return parsearOFXSgml(conteudo);
    }

    private OFXTransacao parsearBloco(String bloco) {
        String trnType  = extrairCampo(bloco, "TRNTYPE");
        String dtPosted = extrairCampo(bloco, "DTPOSTED");
        String tamt     = extrairCampo(bloco, "TAMT");
        String fitId    = extrairCampo(bloco, "FITID");
        String memo     = extrairCampo(bloco, "MEMO");
        String name     = extrairCampo(bloco, "NAME");

        if (fitId == null || tamt == null || dtPosted == null) {
            log.fine("[OFXImportService] Bloco incompleto — FITID=" + fitId +
                    " TAMT=" + tamt + " DTPOSTED=" + dtPosted);
            return null;
        }

        // Normaliza TAMT: troca virgula por ponto
        double tamtValor = Double.parseDouble(tamt.replace(",", ".").trim());
        double valor     = Math.abs(tamtValor);
        LocalDate data   = parsearData(dtPosted);

        // Determina o Tipo: sinal do TAMT tem prioridade sobre TRNTYPE
        Tipo tipo = determinarTipo(trnType, tamtValor);

        // Descricao: combina NAME (estabelecimento) + MEMO (observacao)
        String descricao = montarDescricao(name, memo);

        return new OFXTransacao(fitId.trim(), tipo, valor, data, descricao);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapeamento TRNTYPE → Tipo
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Determina o Tipo da transacao.
     *
     * Regra de prioridade:
     * 1. Sinal do TAMT: negativo = Gasto, positivo = Recebimento (regra mais confiavel)
     * 2. TRNTYPE como desempate quando TAMT = 0 (improvavel mas possivel)
     */
    private Tipo determinarTipo(String trnType, double tamtValor) {
        // Sinal tem prioridade
        if (tamtValor < 0)  return Tipo.Gasto;
        if (tamtValor > 0)  return tipoByTrnType(trnType);

        // TAMT = 0: usa TRNTYPE
        return tipoByTrnType(trnType);
    }

    private Tipo tipoByTrnType(String trnType) {
        if (trnType == null) return Tipo.Gasto;
        return switch (trnType.toUpperCase().trim()) {
            // Entradas
            case "CREDIT", "INT", "DIV", "DEP", "DIRECTDEP",
                 "REPEATPMT"                              -> Tipo.Recebimento;
            // Transferencias
            case "XFER"                                   -> Tipo.Transferencia;
            // Saidas (incluindo tudo que nao e identificado — default conservador)
            case "DEBIT", "ATM", "POS", "CHECK",
                 "PAYMENT", "CASH", "DIRECTDEBIT",
                 "FEE", "SRVCHG", "OTHER"                -> Tipo.Gasto;
            default -> {
                log.fine("[OFXImportService] TRNTYPE desconhecido '" + trnType + "' — tratado como Gasto");
                yield Tipo.Gasto;
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de parsing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extrai o valor de uma tag OFX SGML.
     * Suporta: {@code <TAG>valor} (sem fechamento) e {@code <TAG>valor</TAG>}.
     */
    private String extrairCampo(String bloco, String campo) {
        Pattern p = Pattern.compile(
                "<" + campo + ">([^<\\r\\n]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = p.matcher(bloco);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Parseia datas OFX nos formatos:
     * - {@code 20240115120000}           (sem timezone)
     * - {@code 20240115}                 (so data)
     * - {@code 20240115120000.000}       (com milissegundos)
     * - {@code 20240115120000.000[-3:BRT]} (com offset BR)
     * - {@code 20240115120000[-3:BRT]}   (sem milissegundos, com offset)
     */
    private LocalDate parsearData(String dtPosted) {
        // Remove qualquer coisa apos ponto ou colchete (milissegundos e timezone)
        String limpo = dtPosted.replaceAll("[.\\[\\+\\-].*", "").trim();

        return switch (limpo.length()) {
            case 14 -> LocalDate.parse(limpo.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
            case 8  -> LocalDate.parse(limpo, DateTimeFormatter.BASIC_ISO_DATE);
            default -> {
                // Tenta extrair os 8 primeiros digitos de qualquer forma
                if (limpo.length() > 8) {
                    yield LocalDate.parse(limpo.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
                }
                throw new IllegalArgumentException("Formato de data OFX nao reconhecido: " + dtPosted);
            }
        };
    }

    /**
     * Combina NAME e MEMO para uma descricao mais rica.
     * Exemplo: "Mercado Livre | Compra aprovada" ou "Compra aprovada" ou "Transacao OFX"
     */
    private String montarDescricao(String name, String memo) {
        String n = (name != null && !name.isBlank()) ? name.trim() : null;
        String m = (memo != null && !memo.isBlank()) ? memo.trim() : null;

        if (n != null && m != null && !m.equalsIgnoreCase(n)) {
            return truncar(n + " | " + m, 500);
        }
        if (n != null) return truncar(n, 500);
        if (m != null) return truncar(m, 500);
        return "Transacao OFX";
    }

    private String truncar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Persistencia
    // ─────────────────────────────────────────────────────────────────────────

    private void salvarTransacao(OFXTransacao tx, Usuario usuario, InstituicaoUsuario instUsuario) {
        EventoFinanceiro evento = new EventoFinanceiro();
        evento.setUsuario(usuario);
        evento.setTipo(tx.tipo());
        evento.setValor(tx.valor());
        evento.setDescricao(truncar(tx.descricao(), 500));
        evento.setDataEvento(tx.data());
        evento.setDataRegistro(LocalDateTime.now());
        eventoFinanceiroRepository.save(evento);

        EventoInstituicao eventoInst = new EventoInstituicao();
        eventoInst.setEventoFinanceiro(evento);
        eventoInst.setInstituicaoUsuario(instUsuario);
        eventoInst.setValor(tx.valor());
        eventoInst.setParcelas(1);
        eventoInst.setTipoMovimento(resolverTipoMovimento(tx.tipo()));
        eventoInstituicaoRepository.save(eventoInst);

        EventoDetalhe detalhe = new EventoDetalhe();
        detalhe.setEventoFinanceiro(evento);
        detalhe.setTituloGasto(truncar(tx.descricao(), 50));
        eventoDetalheRepository.save(detalhe);
    }

    /**
     * Converte Tipo financeiro em TipoMovimento de conta.
     * Recebimentos sao Credito, demais sao Debito.
     */
    private TipoMovimento resolverTipoMovimento(Tipo tipo) {
        return tipo == Tipo.Recebimento ? TipoMovimento.Credito : TipoMovimento.Debito;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deduplicacao via arquivo .processed_ids
    // ─────────────────────────────────────────────────────────────────────────

    private Set<String> carregarProcessados() throws IOException {
        Path arquivo = Paths.get(ofxOutputDir).toAbsolutePath().resolve(PROCESSED_IDS_FILE);
        if (!Files.exists(arquivo)) return new HashSet<>();
        return new HashSet<>(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
    }

    private void salvarProcessados(Set<String> ids) throws IOException {
        Path arquivo = Paths.get(ofxOutputDir).toAbsolutePath().resolve(PROCESSED_IDS_FILE);
        Files.write(arquivo, ids, StandardCharsets.UTF_8);
    }

    private void deletarArquivo(Path arquivo) {
        try {
            Files.deleteIfExists(arquivo);
            log.info("[OFXImportService] Arquivo deletado: " + arquivo.getFileName());
        } catch (IOException e) {
            log.warning("[OFXImportService] Nao foi possivel deletar " + arquivo + ": " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Records internos
    // ─────────────────────────────────────────────────────────────────────────

    public record OFXTransacao(
            String fitId,
            Tipo tipo,
            double valor,
            LocalDate data,
            String descricao
    ) {}

    public record ResultadoImportacao(
            int importadas,
            int duplicadas,
            List<String> erros
    ) {}
}

