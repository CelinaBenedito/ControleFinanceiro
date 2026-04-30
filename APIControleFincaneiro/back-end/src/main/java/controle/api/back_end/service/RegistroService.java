package controle.api.back_end.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;

import com.itextpdf.layout.properties.UnitValue;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.exception.InstituicaoInativaException;
import controle.api.back_end.exception.SaldoInsuficienteException;
import controle.api.back_end.factory.MovimentoFactory;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.*;
import controle.api.back_end.specifications.EventoFinanceiroSpecifications;
import controle.api.back_end.strategy.movimento.MovimentoResultado;
import controle.api.back_end.strategy.movimento.MovimentoStrategy;


import java.io.ByteArrayOutputStream;

import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistroService {
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final GastoDetalheRepository gastoDetalheRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final MovimentoFactory movimentoFactory;
    private final InstituicaoService instituicaoService;

    public RegistroService(EventoFinanceiroRepository eventoFinanceiroRepository,
                           EventoInstituicaoRepository eventoInstituicaoRepository,
                           GastoDetalheRepository gastoDetalheRepository,
                           CategoriaUsuarioRepository categoriaUsuarioRepository,
                           UsuarioRepository usuarioRepository,
                           InstituicaoUsuarioRepository instituicaoUsuarioRepository,
                           MovimentoFactory movimentoFactory,
                           InstituicaoService instituicaoService) {
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.gastoDetalheRepository = gastoDetalheRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.movimentoFactory = movimentoFactory;
        this.instituicaoService = instituicaoService;
    }

    public EventoFinanceiro createEventoFinanceiro(EventoFinanceiro entity) {

        Usuario user = usuarioRepository
                .findById(entity.getUsuario().getId())
                        .orElseThrow(() ->
                                new EntidadeNaoEncontradaException(
                                        "Usuário de id: %s não encontrado."
                                        .formatted(entity.getUsuario().getId()
                                        )
                                )
                        );

        entity.setUsuario(user);
        entity.setDataRegistro(LocalDate.now());
        return eventoFinanceiroRepository.save(entity);
    }

    public List<EventoInstituicao> createEventoInstituicao(List<EventoInstituicao> entities,
                                                           EventoFinanceiro eventoFinanceiro) {
        if (!eventoFinanceiroRepository.existsById(eventoFinanceiro.getId())) {
            throw new EntidadeNaoEncontradaException(
                    "Evento Financeiro de id: %s não encontrado"
                            .formatted(eventoFinanceiro.getId())
            );
        }

        List<EventoInstituicao> savedInstituicoes = new ArrayList<>();

        for (EventoInstituicao entity : entities) {
            InstituicaoUsuario instituicaoUsuario = instituicaoUsuarioRepository
                    .findById(entity.getInstituicaoUsuario().getId())
                    .orElseThrow(() ->
                            new EntidadeNaoEncontradaException(
                                    "Instituição associada ao Usuário não encontrada."
                            )
                    );

            if (!instituicaoUsuario.getIsAtivo()) {
                throw new InstituicaoInativaException("Instituição %s está inativa"
                        .formatted(instituicaoUsuario.getInstituicao().getNome()));
            }

            Map<String, Object> params = new HashMap<>();
            params.put("parcelas", entity.getParcelas());

            MovimentoStrategy strategy = movimentoFactory.getStrategy(entity.getTipoMovimento(), params);
            strategy.validar(instituicaoUsuario);
            MovimentoResultado resultado = strategy.processar(entity);

            BigDecimal saldoByInstituicao = instituicaoService.getSaldoByInstituicao(instituicaoUsuario.getId());

            if ((eventoFinanceiro.getTipo() == Tipo.Gasto
                    || eventoFinanceiro.getTipo() == Tipo.Transferencia)
                    && BigDecimal.valueOf(resultado.getValorParcela()).compareTo(saldoByInstituicao) > 0) {
                throw new SaldoInsuficienteException("Saldo insuficiente para realizar a operação.");
            }

            if (resultado.getParcelas() > 1) {
                for (int i = 1; i <= resultado.getParcelas(); i++) {
                    EventoInstituicao parcelaEvento = new EventoInstituicao();
                    parcelaEvento.setParcelas(i);
                    parcelaEvento.setEventoFinanceiro(eventoFinanceiro);
                    parcelaEvento.setInstituicaoUsuario(instituicaoUsuario);
                    parcelaEvento.setTipoMovimento(entity.getTipoMovimento());
                    parcelaEvento.setValor(resultado.getValorParcela());

                    savedInstituicoes.add(eventoInstituicaoRepository.save(parcelaEvento));
                }
            } else {
                entity.setParcelas(resultado.getParcelas());
                entity.setInstituicaoUsuario(instituicaoUsuario);
                entity.setEventoFinanceiro(eventoFinanceiro);
                savedInstituicoes.add(eventoInstituicaoRepository.save(entity));
            }
        }

        return savedInstituicoes;
    }

    public GastoDetalhe createGastoDetalhe(GastoDetalhe entity,
                                           EventoFinanceiro eventoFinanceiro) {
        if (!eventoFinanceiroRepository.existsById(eventoFinanceiro.getId())) {
            throw new EntidadeNaoEncontradaException(
                    "Evento Financeiro de id: %s não encontrado"
                            .formatted(eventoFinanceiro.getId())
            );
        }

        List<CategoriaUsuario> categorias = entity.getCategoriaUsuario().stream()
                .map(cu -> categoriaUsuarioRepository.findById(cu.getId())
                        .orElseThrow(() ->
                                new EntidadeNaoEncontradaException("Categoria Usuário de id: %d não encontrada."
                                        .formatted(cu.getId()))
                        ))
                .toList();

        entity.setEventoFinanceiro(eventoFinanceiro);
        entity.setCategoriaUsuario(categorias);

        return gastoDetalheRepository.save(entity);
    }

    public List<EventoFinanceiro> getEventosFinanceirosByUser(UUID userId) {
        if(!usuarioRepository.existsById(userId)){
            throw new EntidadeNaoEncontradaException("Usuário de id: %s não encontrado."
                    .formatted(userId)
            );
        }

        return eventoFinanceiroRepository.getEventoFinanceirosByUsuario_id(userId);
    }

    public List<List<EventoInstituicao>> getEventosInstituicoesByEventoFinanceiro(List<EventoFinanceiro> eventos) {
        return eventos.stream()
                .map(evento -> eventoInstituicaoRepository.findEventoInstituicaoByEventoFinanceiro_Id(evento.getId()))
                .toList();
    }

    public List<GastoDetalhe> getGastosDetalhesByEventoFinanceiro(
            List<EventoFinanceiro> eventosFinanceiros) {
        List<GastoDetalhe> gastoDetalhes = new ArrayList<>();
        for (EventoFinanceiro evento : eventosFinanceiros){
            if(!eventoFinanceiroRepository.existsById(evento.getId())){
                throw new EntidadeNaoEncontradaException("Evento financeiro de id: %s não encontrado."
                        .formatted(evento.getId()
                        )
                );
            }
            GastoDetalhe gastoDetalheByEventoFinanceiro = gastoDetalheRepository.findGastoDetalheByEventoFinanceiro(evento);

            gastoDetalhes.add(gastoDetalheByEventoFinanceiro);
        }
        return gastoDetalhes;
    }

    public List<RegistroResponseDto> getByFilter(UUID userId, Double valor, List<TipoMovimento> tipoMovimento, List<Tipo> tipo,
                                                 LocalDate dataEvento, List<InstituicaoUsuario> instituicao,
                                                 List<CategoriaUsuario> categoria, String descricao, String titulo){

        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() ->
                new EntidadeNaoEncontradaException(
                        "Usuario de id: %s não encontrado"
                                .formatted(userId)
                )
        );

        Specification<EventoFinanceiro> filtroUsuario =
                (root, query, cb) -> cb.equal(root.get("usuario"), usuario);

        Specification<EventoFinanceiro> spec = EventoFinanceiroSpecifications.porFiltros(
                valor, tipo, dataEvento, descricao, tipoMovimento, instituicao, categoria, titulo
        );

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAll(
                filtroUsuario.and(spec)
        );


        return eventos.stream()
                .map(e -> RegistrosMapper.toResponse(e, e.getEventoInstituicao(), e.getGastoDetalhe()))
                .collect(Collectors.toList());

    }

    public EventoFinanceiro editEventoFinanceiro(UUID eventoId, EventoFinanceiro entity) {
        EventoFinanceiro financeiro = eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento financeiro de id: %s não encontrado"
                                        .formatted(eventoId))
                );

        if(entity.getDataEvento() != financeiro.getDataEvento()){
            financeiro.setDataEvento(entity.getDataEvento());
        }
        if (!Objects.equals(entity.getDescricao(), financeiro.getDescricao())){
            financeiro.setDescricao(entity.getDescricao());
        }
        if (entity.getTipo() != financeiro.getTipo()){
            financeiro.setTipo(entity.getTipo());
        }
        return eventoFinanceiroRepository.save(financeiro);
    }

    public List<EventoInstituicao> editEventoInstituicao(UUID eventoId, List<EventoInstituicao> novasInstituicoes) {
        EventoFinanceiro financeiro = eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento financeiro de id: %s não encontrado"
                                        .formatted(eventoId))
                );

        // Busca todas as instituições já associadas ao evento
        List<EventoInstituicao> existentes = eventoInstituicaoRepository
                .findEventoInstituicaoByEventoFinanceiro_Id(eventoId);

        // IDs das novas instituições enviadas pelo usuário
        Set<Integer> novosIds = novasInstituicoes.stream()
                .map(e -> e.getInstituicaoUsuario().getId())
                .collect(Collectors.toSet());

        // Remove instituições que não estão mais na lista enviada
        for (EventoInstituicao existente : existentes) {
            if (!novosIds.contains(existente.getInstituicaoUsuario().getId())) {
                eventoInstituicaoRepository.delete(existente);
            }
        }

        List<EventoInstituicao> atualizados = new ArrayList<>();

        // Atualiza ou cria instituições conforme necessário
        for (EventoInstituicao nova : novasInstituicoes) {
            EventoInstituicao existente = existentes.stream()
                    .filter(e -> Objects.equals(e.getInstituicaoUsuario().getId(), nova.getInstituicaoUsuario().getId()))
                    .findFirst()
                    .orElse(new EventoInstituicao());
            InstituicaoUsuario instituicaoUsuario = instituicaoUsuarioRepository.findById(nova.getInstituicaoUsuario().getId())
                    .orElseThrow(() -> new EntidadeNaoEncontradaException("Instituição Usuário não encontrada"));

            existente.setInstituicaoUsuario(instituicaoUsuario);
            existente.setValor(nova.getValor());
            existente.setParcelas(nova.getParcelas());
            existente.setTipoMovimento(nova.getTipoMovimento());
            existente.setEventoFinanceiro(financeiro);

            atualizados.add(eventoInstituicaoRepository.save(existente));
        }

        return atualizados;
    }

    public GastoDetalhe editGastoDetalhe(UUID eventoId, GastoDetalhe entity) {
        EventoFinanceiro financeiro = eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento financeiro de id: %s não encontrado"
                                        .formatted(eventoId))
                );

        GastoDetalhe gastoDetalhe = gastoDetalheRepository.findGastoDetalheByEventoFinanceiro_Id(eventoId);

        // Atualiza título se mudou
        if (!Objects.equals(entity.getTituloGasto(), gastoDetalhe.getTituloGasto())) {
            gastoDetalhe.setTituloGasto(entity.getTituloGasto());
        }

        // Atualiza categorias (adiciona novas e remove as que não estão mais)
        List<CategoriaUsuario> categoriasExistentes = gastoDetalhe.getCategoriaUsuario();
        List<CategoriaUsuario> categoriasNovas = entity.getCategoriaUsuario();

        // IDs das novas categorias
        Set<Integer> novosIds = categoriasNovas.stream()
                .map(CategoriaUsuario::getId)
                .collect(Collectors.toSet());

        // Remove categorias que não estão mais na lista enviada
        categoriasExistentes.removeIf(c -> !novosIds.contains(c.getId()));

        // Adiciona ou atualiza categorias novas
        for (CategoriaUsuario nova : categoriasNovas) {
            boolean jaExiste = categoriasExistentes.stream()
                    .anyMatch(c -> Objects.equals(c.getId(), nova.getId()));
            if (!jaExiste) {
                CategoriaUsuario categoria = categoriaUsuarioRepository.findById(nova.getId())
                        .orElseThrow(() ->
                                new EntidadeNaoEncontradaException(
                                        "Categoria Usuário de id: %d não encontrada."
                                                .formatted(nova.getId()))
                        );
                categoriasExistentes.add(categoria);
            }
        }

        gastoDetalhe.setCategoriaUsuario(categoriasExistentes);
        gastoDetalhe.setEventoFinanceiro(financeiro);

        return gastoDetalheRepository.save(gastoDetalhe);
    }

    public void deleteRegistroByEventoFinanceiro_Id(UUID eventoId) {
        EventoFinanceiro financeiro = eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento Financeiro de id: %s não encontrado"
                                        .formatted(eventoId)
                        )
                );

        GastoDetalhe gastos = gastoDetalheRepository.findGastoDetalheByEventoFinanceiro_Id(eventoId);
        gastoDetalheRepository.delete(gastos);

        // Deleta todas as instituições associadas ao evento
        List<EventoInstituicao> instituicoes = eventoInstituicaoRepository.findEventoInstituicaoByEventoFinanceiro_Id(eventoId);
        if (instituicoes.isEmpty()) {
            throw new EntidadeNaoEncontradaException("Evento Instituição não encontrado.");
        }
        instituicoes.forEach(instituicao -> eventoInstituicaoRepository.delete(instituicao));

        // Finalmente, deleta o evento financeiro
        eventoFinanceiroRepository.delete(financeiro);
    }

    public String createJson(UUID userId) {
        return null;
    }

    public String createSql(UUID userId) {
        return null;
    }

    public byte[] createExcel(UUID userId) {
        return null;
    }

    public byte[] createPdf(UUID userId) {
        DeviceRgb tealEscuro = new DeviceRgb(54, 115, 115);
        DeviceRgb tealClaro = new DeviceRgb(180, 217, 213);
        DeviceRgb fundoPagina = new DeviceRgb(235, 244, 244);
        DeviceRgb textoEscuro = new DeviceRgb(26, 26, 26);
        DeviceRgb textoClaro = new DeviceRgb(255, 255, 255);

        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() ->
                new EntidadeNaoEncontradaException(
                        "Usuário de id: %s não encontrado."
                                .formatted(userId)
                )
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // Cria o writer e documento
            PdfWriter escritor = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(escritor);
            Document documento = new Document(pdf);

            // Título
            Paragraph titulo = new Paragraph("MyFinance - Registros")
                    .setBold()
                    .setFontSize(20)
                    .setFontColor(tealEscuro)
                    .setBackgroundColor(tealClaro);
            documento.add(titulo);


            // Dados do usuário
            documento.add(new Paragraph("Nome: " + usuario.getNome() + " Sobrenome: " + usuario.getSobrenome()));
            documento.add(new Paragraph("Data de Nascimento: " + usuario.getDataNascimento() +
                    " Sexo: " + usuario.getSexo().toString()));
            documento.add(new Paragraph("Email: " + usuario.getEmail()));
            List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository
                    .findEventoFinanceiroByUsuarioOrderByDataEventoDesc(usuario);

            Paragraph sumarioTitulo = new Paragraph("Sumário")
                    .setBold()
                    .setFontSize(18)
                    .setFontColor(tealEscuro)
                    .setMarginBottom(10);
            documento.add(sumarioTitulo);

            int anoAnterior = -1;
            Set<String> mesesAdicionados = new HashSet<>();

            //FOR DO SUMÁRIO
            for (EventoFinanceiro evento : eventosFinanceiros) {
                LocalDate data = evento.getDataEvento();
                String nomeMes = data.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
                nomeMes = nomeMes.substring(0,1).toUpperCase() + nomeMes.substring(1);

                // Se mudou o ano, adiciona título de ano
                if (data.getYear() != anoAnterior) {
                    anoAnterior = data.getYear();
                    Link linkAno = new Link(String.valueOf(anoAnterior), PdfAction.createGoTo("ano_" + anoAnterior));
                    documento.add(new Paragraph(linkAno)
                            .setBold()
                            .setFontSize(14)
                            .setFontColor(tealEscuro)
                            .setMarginTop(10));
                }

                String chaveMes = anoAnterior + "-" + data.getMonthValue();

                if (!mesesAdicionados.contains(chaveMes)) {
                    mesesAdicionados.add(chaveMes);

                    // Link para o mês
                    Link linkMes = new Link("   " + nomeMes, PdfAction.createGoTo("mes_" + data.getYear() + "_" + data.getMonthValue()));
                    documento.add(new Paragraph(linkMes)
                            .setFontSize(12)
                            .setFontColor(textoEscuro)
                            .setMarginLeft(20));

                    // Link para resumo do mês
                    Link linkResumoMes = new Link("      Resumo de " + nomeMes, PdfAction.createGoTo("resumo_mes_" + data.getYear() + "_" + data.getMonthValue()));
                    documento.add(new Paragraph(linkResumoMes)
                            .setFontSize(11)
                            .setFontColor(textoEscuro)
                            .setMarginLeft(25));
                }
            }

            documento.add(new AreaBreak());
                // Ordena os eventos por data
            eventosFinanceiros.sort(Comparator.comparing(EventoFinanceiro::getDataEvento).reversed());

            int anoAtual = -1;
            int mesAtual = -1;

            Table tabela = new Table(9);

            BigDecimal ganhosMes = BigDecimal.ZERO;
            BigDecimal gastosMes = BigDecimal.ZERO;

            BigDecimal ganhosAno = BigDecimal.ZERO;
            BigDecimal gastosAno = BigDecimal.ZERO;


                for (EventoFinanceiro evento : eventosFinanceiros) {
                    LocalDate data = evento.getDataEvento();

                    // Se mudou o ano
                    if (data.getYear() != anoAtual) {
                        anoAtual = data.getYear();
                        documento.add(new Paragraph(String.valueOf(anoAtual))
                                .setBold()
                                .setFontSize(18)
                                .setFontColor(tealEscuro)
                                .setMarginTop(20));

                        // Define destino para o ano
                        PdfPage paginaAno = pdf.getLastPage();
                        PdfExplicitDestination destinoAno = PdfExplicitDestination.createFit(paginaAno);
                        pdf.addNamedDestination("ano_" + anoAtual, destinoAno.getPdfObject());


                    }

                    // Se mudou o mês
                    if (data.getMonthValue() != mesAtual) {
                        ganhosMes = BigDecimal.ZERO;
                        gastosMes = BigDecimal.ZERO;

                        mesAtual = data.getMonthValue();
                        documento.add(new Paragraph(data.getMonth()
                                .getDisplayName(TextStyle.FULL, new Locale("pt", "BR")))
                                .setBold()
                                .setFontSize(14)
                                .setFontColor(textoEscuro)
                                .setMarginLeft(20));

                        // Define destino para o mês
                        PdfPage paginaMes = pdf.getLastPage();
                        PdfExplicitDestination destinoMes = PdfExplicitDestination.createFit(paginaMes);
                        pdf.addNamedDestination("mes_" + anoAtual + "_" + data.getMonthValue(), destinoMes.getPdfObject());


                        // Cria tabela para o mês
                        tabela = new Table(9);
                        String[] headers = {"Dia", "Título", "Valor", "Tipo", "Descrição", "Instituições", "Movimentação", "Parcelas", "Categorias"};
                        for (String h : headers) {
                            Cell cell = new Cell().add(new Paragraph(h).setBold().setFontColor(textoClaro));
                            cell.setBackgroundColor(tealEscuro);
                            tabela.addCell(cell);
                        }
                    }

                    // Adiciona linha do evento na tabela atual
                    tabela.addCell(String.valueOf(data.getDayOfMonth()));
                    tabela.addCell(evento.getGastoDetalhe().getTituloGasto());
                    tabela.addCell(evento.getValor().toString());
                    tabela.addCell(evento.getTipo().toString());
                    tabela.addCell(evento.getDescricao());

                    List<EventoInstituicao> instituicoes = eventoInstituicaoRepository
                            .findEventoInstituicaoByEventoFinanceiro_Id(evento.getId());

                    if (!instituicoes.isEmpty()) {
                        EventoInstituicao inst = instituicoes.get(0);
                        tabela.addCell(inst.getInstituicaoUsuario().getInstituicao().getNome());
                        tabela.addCell(inst.getTipoMovimento().toString());
                        tabela.addCell(String.valueOf(inst.getParcelas()));
                    } else {
                        tabela.addCell("-");
                        tabela.addCell("-");
                        tabela.addCell("-");
                    }

                    if (!evento.getGastoDetalhe().getCategoriaUsuario().isEmpty()) {
                        tabela.addCell(evento.getGastoDetalhe().getCategoriaUsuario().get(0).getCategoria().getTitulo());
                    } else {
                        tabela.addCell("-");
                    }
                    if (evento.getTipo() == Tipo.Recebimento) {
                        ganhosMes = ganhosMes.add(BigDecimal.valueOf(evento.getValor()));
                    } else if (evento.getTipo() == Tipo.Gasto || evento.getTipo() == Tipo.Transferencia) {
                        gastosMes = gastosMes.add(BigDecimal.valueOf(evento.getValor()));
                    }
                    ganhosAno = ganhosAno.add(ganhosMes);
                    gastosAno = gastosAno.add(gastosMes);


                    // Quando chegar no último evento do mês, adiciona a tabela ao documento
                    if (evento.equals(eventosFinanceiros.get(eventosFinanceiros.size() - 1)) ||
                            evento.getDataEvento().getMonthValue() != eventosFinanceiros.get(eventosFinanceiros.indexOf(evento) + 1).getDataEvento().getMonthValue()) {
                        documento.add(tabela);

                        //Tabelinha de gastos e ganhos.
                        BigDecimal saldoMes = ganhosMes.subtract(gastosMes);

                        Table resumoMes = new Table(3);
                        resumoMes.setWidth(UnitValue.createPercentValue(100));
                        resumoMes.addCell(new Cell().add(new Paragraph("Ganhos"))
                                .setBackgroundColor(tealClaro)
                                .setFontColor(tealEscuro)
                                .setBold());

                        resumoMes.addCell(new Cell().add(new Paragraph("Gastos"))
                                .setBackgroundColor(new DeviceRgb(255, 226, 226)) // vermelho claro
                                .setFontColor(new DeviceRgb(185, 28, 28)) // vermelho-escuro
                                .setBold());

                        resumoMes.addCell(new Cell().add(new Paragraph("Saldo"))
                                .setBackgroundColor(new DeviceRgb(209, 250, 229)) // verde claro
                                .setFontColor(new DeviceRgb(21, 128, 61)) // verde-escuro
                                .setBold());

                        resumoMes.addCell(new Cell().add(new Paragraph("R$ " + ganhosMes)));
                        resumoMes.addCell(new Cell().add(new Paragraph("R$ " + gastosMes)));
                        resumoMes.addCell(new Cell().add(new Paragraph("R$ " + saldoMes)));

                        documento.add(new Paragraph("Resumo do mês de " + data.getMonth()
                                .getDisplayName(TextStyle.FULL, new Locale("pt", "BR")))
                                .setBold().setFontSize(12).setFontColor(tealEscuro).setMarginTop(10));

                        PdfPage paginaResumoMes = pdf.getLastPage();
                        PdfExplicitDestination destinoResumoMes = PdfExplicitDestination.createFit(paginaResumoMes);
                        pdf.addNamedDestination("resumo_mes_" + anoAtual + "_" + data.getMonthValue(), destinoResumoMes.getPdfObject());

                        documento.add(resumoMes);
                        documento.add(new AreaBreak());
                    }

                }
            BigDecimal saldoAno = ganhosAno.subtract(gastosAno);

            documento.add(new Paragraph("Resumo do Ano " + anoAtual)
                    .setBold().setFontSize(14).setFontColor(tealEscuro).setMarginTop(15));

            Table resumoAno = new Table(3);
            resumoAno.setWidth(UnitValue.createPercentValue(100));

            resumoAno.addCell(new Cell()
                    .add(new Paragraph("Ganhos"))
                    .setBackgroundColor(tealClaro)
                    .setFontColor(tealEscuro)
                    .setBold());

            resumoAno.addCell(new Cell()
                    .add(new Paragraph("Gastos"))
                    .setBackgroundColor(new DeviceRgb(255,226,226))
                    .setFontColor(new DeviceRgb(185,28,28))
                    .setBold());

            resumoAno.addCell(new Cell()
                    .add(new Paragraph("Saldo"))
                    .setBackgroundColor(new DeviceRgb(209,250,229))
                    .setFontColor(new DeviceRgb(21,128,61))
                    .setBold());

            resumoAno.addCell(new Cell().add(new Paragraph("R$ " + ganhosAno)));
            resumoAno.addCell(new Cell().add(new Paragraph("R$ " + gastosAno)));
            resumoAno.addCell(new Cell().add(new Paragraph("R$ " + saldoAno)));
            documento.add(resumoAno);

            // Fecha o documento
            documento.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
}
