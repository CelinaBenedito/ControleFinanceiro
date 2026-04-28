package controle.api.back_end.service;

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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
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

    public List<RegistroResponseDto> getByFilter(Double valor, TipoMovimento tipoMovimento, Tipo tipo,
                                                 LocalDate dataEvento, InstituicaoUsuario instituicao,
                                                 CategoriaUsuario categoria, String descricao, String titulo){

        Specification<EventoFinanceiro> spec = EventoFinanceiroSpecifications.porFiltros(
                valor, tipo, dataEvento, descricao, tipoMovimento, instituicao, categoria, titulo
        );

        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAll(spec);

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

            existente.setInstituicaoUsuario(nova.getInstituicaoUsuario());
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
        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() ->
                new EntidadeNaoEncontradaException(
                        "Usuário de id: %s não encontrado."
                                .formatted(userId)
                )
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PDDocument documento = null;

        try{
            documento = new PDDocument();
            PDPage pagina = new PDPage();
            documento.addPage(pagina);

            PDPageContentStream conteudo = new PDPageContentStream(documento,pagina);
            conteudo.beginText();

            //TÍTULO
            conteudo.setFont(PDType1Font.HELVETICA_BOLD, 20);
            conteudo.newLineAtOffset(100, 700);
            conteudo.showText("MyFinance - Registros");
            conteudo.endText();

            conteudo.beginText();
            conteudo.setFont(PDType1Font.HELVETICA, 12);
            conteudo.newLineAtOffset(100, 650);
            conteudo.showText("Nome: " + usuario.getNome() + " Sobrenome: "+ usuario.getSobrenome());
            conteudo.endText();

            conteudo.beginText();
            conteudo.setFont(PDType1Font.HELVETICA, 12);
            conteudo.newLineAtOffset(100, 630);
            conteudo.showText("Data de Nascimento: " + usuario.getDataNascimento() + " Sexo: "+ usuario.getSexo().toString());
            conteudo.endText();

            conteudo.beginText();
            conteudo.setFont(PDType1Font.HELVETICA, 12);
            conteudo.newLineAtOffset(100, 610);
            conteudo.showText("Email: "+ usuario.getEmail());

            conteudo.close();
            documento.save(out);
        }catch (IOException e){
            e.printStackTrace();
        } finally {
            if (documento != null) {
                try {
                    documento.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return out.toByteArray();
    }
}
