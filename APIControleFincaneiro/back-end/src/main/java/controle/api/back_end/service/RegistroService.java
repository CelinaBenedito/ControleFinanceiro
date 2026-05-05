package controle.api.back_end.service;

import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
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
import com.itextpdf.layout.element.*;

import com.itextpdf.layout.properties.UnitValue;
import controle.api.back_end.dto.registros.in.TransferenciaDTO;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.exception.InstituicaoInativaException;
import controle.api.back_end.exception.SaldoInsuficienteException;
import controle.api.back_end.factory.MovimentoFactory;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.configuracoes.LimitePorCategoria;
import controle.api.back_end.model.configuracoes.LimitePorInstituicao;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.repository.*;
import controle.api.back_end.specifications.EventoFinanceiroSpecifications;
import controle.api.back_end.strategy.eventoFinanceiro.TransferenciaEvento;
import controle.api.back_end.strategy.movimento.MovimentoResultado;
import controle.api.back_end.strategy.movimento.MovimentoStrategy;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;

import java.io.ByteArrayOutputStream;

import java.time.LocalDate;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistroService {
    private final EventoFinanceiroRepository eventoFinanceiroRepository;
    private final EventoInstituicaoRepository eventoInstituicaoRepository;
    private final EventoDetalheRepository eventoDetalheRepository;
    private final CategoriaUsuarioRepository categoriaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    private final MovimentoFactory movimentoFactory;
    private final InstituicaoRepository instituicaoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InstituicaoService instituicaoService;
    private final LimitePorInstituicaoRepository limitePorInstituicaoRepository;
    private final LimitePorCategoriaRepository limitePorCategoriaRepository;
    private final ConfiguracoesRepository configuracoesRepository;
    private final TransferenciaEvento transferenciaEvento;

    public RegistroService(EventoFinanceiroRepository eventoFinanceiroRepository, EventoInstituicaoRepository eventoInstituicaoRepository, EventoDetalheRepository eventoDetalheRepository, CategoriaUsuarioRepository categoriaUsuarioRepository, UsuarioRepository usuarioRepository, InstituicaoUsuarioRepository instituicaoUsuarioRepository, MovimentoFactory movimentoFactory, InstituicaoRepository instituicaoRepository, CategoriaRepository categoriaRepository, InstituicaoService instituicaoService, LimitePorInstituicaoRepository limitePorInstituicaoRepository, LimitePorCategoriaRepository limitePorCategoriaRepository, ConfiguracoesRepository configuracoesRepository, TransferenciaEvento transferenciaEvento) {
        this.eventoFinanceiroRepository = eventoFinanceiroRepository;
        this.eventoInstituicaoRepository = eventoInstituicaoRepository;
        this.eventoDetalheRepository = eventoDetalheRepository;
        this.categoriaUsuarioRepository = categoriaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoUsuarioRepository = instituicaoUsuarioRepository;
        this.movimentoFactory = movimentoFactory;
        this.instituicaoRepository = instituicaoRepository;
        this.categoriaRepository = categoriaRepository;
        this.instituicaoService = instituicaoService;
        this.limitePorInstituicaoRepository = limitePorInstituicaoRepository;
        this.limitePorCategoriaRepository = limitePorCategoriaRepository;
        this.configuracoesRepository = configuracoesRepository;
        this.transferenciaEvento = transferenciaEvento;
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
        entity.setDataRegistro(LocalDateTime.now());
        return eventoFinanceiroRepository.save(entity);
    }

    public List<EventoInstituicao> createEventoInstituicaoTransferencia(EventoInstituicao eventoInstituicao,
                                                                        EventoFinanceiro eventoFinanceiro,
                                                                        Integer destino_id){

        if (!eventoFinanceiroRepository.existsById(eventoFinanceiro.getId())) {
            throw new EntidadeNaoEncontradaException(
                    "Evento Financeiro de id: %s não encontrado"
                            .formatted(eventoFinanceiro.getId())
            );
        }
        List<InstituicaoUsuario> instituicaoUsuario = instituicaoUsuarioRepository.findInstituicaoUsuarioByEventoInstituicao_Id(eventoInstituicao.getId());
        eventoInstituicao.setInstituicaoUsuario(instituicaoUsuario.getFirst());


        InstituicaoUsuario destino = instituicaoUsuarioRepository.findById(destino_id)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException("Instituição usuario de id: %d não encontrado."
                                .formatted(destino_id)
                        )
                );

        TransferenciaDTO processar = transferenciaEvento.processar(eventoFinanceiro, eventoInstituicao, destino);
        List<EventoInstituicao> listaRecebedora = new ArrayList<>();

        EventoInstituicao recebedora = processar.getEventoInstituicao();
        EventoFinanceiro eventoRecebedora = processar.getEventoFinanceiro();

        recebedora.setTipoMovimento(eventoInstituicao.getTipoMovimento());
        recebedora.setEventoFinanceiro(eventoRecebedora);
        recebedora.setParcelas(1);
        recebedora.setValor(eventoInstituicao.getValor());
        recebedora.setInstituicaoUsuario(destino);
        listaRecebedora.add(recebedora);
        eventoFinanceiroRepository.save(eventoRecebedora);
        eventoInstituicaoRepository.save(recebedora);
        eventoInstituicao.setEventoFinanceiro(eventoFinanceiro);
        eventoInstituicaoRepository.save(eventoInstituicao);

        return listaRecebedora;
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

    public EventoDetalhe createGastoDetalhe(EventoDetalhe entity,
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

        return eventoDetalheRepository.save(entity);
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

    public List<EventoDetalhe> getGastosDetalhesByEventoFinanceiro(
            List<EventoFinanceiro> eventosFinanceiros) {
        List<EventoDetalhe> eventoDetalhes = new ArrayList<>();
        for (EventoFinanceiro evento : eventosFinanceiros){
            if(!eventoFinanceiroRepository.existsById(evento.getId())){
                throw new EntidadeNaoEncontradaException("Evento financeiro de id: %s não encontrado."
                        .formatted(evento.getId()
                        )
                );
            }
            EventoDetalhe eventoDetalheByEventoFinanceiro = eventoDetalheRepository.findGastoDetalheByEventoFinanceiro(evento);

            eventoDetalhes.add(eventoDetalheByEventoFinanceiro);
        }
        return eventoDetalhes;
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

    public EventoDetalhe editGastoDetalhe(UUID eventoId, EventoDetalhe entity) {
        EventoFinanceiro financeiro = eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento financeiro de id: %s não encontrado"
                                        .formatted(eventoId))
                );

        EventoDetalhe eventoDetalhe = eventoDetalheRepository.findGastoDetalheByEventoFinanceiro_Id(eventoId);

        // Atualiza título se mudou
        if (!Objects.equals(entity.getTituloGasto(), eventoDetalhe.getTituloGasto())) {
            eventoDetalhe.setTituloGasto(entity.getTituloGasto());
        }

        // Atualiza categorias (adiciona novas e remove as que não estão mais)
        List<CategoriaUsuario> categoriasExistentes = eventoDetalhe.getCategoriaUsuario();
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

        eventoDetalhe.setCategoriaUsuario(categoriasExistentes);
        eventoDetalhe.setEventoFinanceiro(financeiro);

        return eventoDetalheRepository.save(eventoDetalhe);
    }

    public void deleteRegistroByEventoFinanceiro_Id(UUID eventoId) {
        EventoFinanceiro financeiro = eventoFinanceiroRepository.findById(eventoId)
                .orElseThrow(() ->
                        new EntidadeNaoEncontradaException(
                                "Evento Financeiro de id: %s não encontrado"
                                        .formatted(eventoId)
                        )
                );

        EventoDetalhe gastos = eventoDetalheRepository.findGastoDetalheByEventoFinanceiro_Id(eventoId);
        eventoDetalheRepository.delete(gastos);

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
        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() ->
                new EntidadeNaoEncontradaException(
                        "Usuário de id: %s não encontrado.".formatted(userId)
                )
        );

        Map<String, Object> jsonMap = new LinkedHashMap<>();

        // Informações básicas do usuário
        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", usuario.getId().toString());
        userInfo.put("nome", usuario.getNome());
        userInfo.put("sobrenome", usuario.getSobrenome());
        userInfo.put("data_nascimento", usuario.getDataNascimento().toString());
        userInfo.put("sexo", usuario.getSexo().toString());
        userInfo.put("email", usuario.getEmail());

        jsonMap.put("usuario", userInfo);

        // Eventos financeiros
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);
        List<Map<String, Object>> eventosList = new ArrayList<>();

        for (EventoFinanceiro e : eventos) {
            Map<String, Object> eventoMap = new LinkedHashMap<>();
            eventoMap.put("id", e.getId().toString());
            eventoMap.put("tipo", e.getTipo().toString());
            eventoMap.put("valor", e.getValor());
            eventoMap.put("descricao", e.getDescricao());
            eventoMap.put("data_evento", e.getDataEvento().toString());
            eventoMap.put("data_registro", e.getDataRegistro().toString());

            // Instituições vinculadas ao evento
            List<EventoInstituicao> insts = eventoInstituicaoRepository.findEventoInstituicaoByEventoFinanceiro_Id(e.getId());
            List<Map<String, Object>> instList = new ArrayList<>();
            for (EventoInstituicao ei : insts) {
                Map<String, Object> instMap = new LinkedHashMap<>();
                instMap.put("id", ei.getId().toString());
                instMap.put("instituicao_usuario_id", ei.getInstituicaoUsuario().getId());
                instMap.put("tipo_movimento", ei.getTipoMovimento().toString());
                instMap.put("valor", ei.getValor());
                instMap.put("parcelas", ei.getParcelas());
                instList.add(instMap);
            }
            eventoMap.put("instituicoes", instList);

            // Detalhes de gasto
            EventoDetalhe gasto = eventoDetalheRepository.findGastoDetalheByEventoFinanceiro(e);
            if (gasto != null) {
                Map<String, Object> gastoMap = new LinkedHashMap<>();
                gastoMap.put("id", gasto.getId().toString());
                gastoMap.put("titulo_gasto", gasto.getTituloGasto());

                List<Map<String, Object>> categoriasList = new ArrayList<>();
                for (CategoriaUsuario cat : gasto.getCategoriaUsuario()) {
                    Map<String, Object> catMap = new LinkedHashMap<>();
                    catMap.put("id", cat.getId());
                    catMap.put("categoria", cat.getCategoria().getTitulo());
                    categoriasList.add(catMap);
                }
                gastoMap.put("categorias", categoriasList);

                eventoMap.put("gasto_detalhe", gastoMap);
            }

            eventosList.add(eventoMap);
        }

        jsonMap.put("eventos_financeiros", eventosList);

        // Serializar para JSON
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT); // deixa bonito
            return mapper.writeValueAsString(jsonMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao gerar JSON", e);
        }
    }

    public String createSql(UUID userId) {
        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() ->
                new EntidadeNaoEncontradaException(
                        "Usuário de id: %s não encontrado.".formatted(userId)
                )
        );

        StringBuilder sql = new StringBuilder();

        // Usuário
        sql.append("-- Usuário\n");
        sql.append("INSERT INTO usuario (id, nome, sobrenome, data_nascimento, sexo, imagem, email, senha) VALUES (")
                .append("'").append(usuario.getId()).append("', ")
                .append("'").append(usuario.getNome()).append("', ")
                .append("'").append(usuario.getSobrenome()).append("', ")
                .append("'").append(usuario.getDataNascimento()).append("', ")
                .append("'").append(usuario.getSexo()).append("', ")
                .append(usuario.getImagem() == null ? "null" : "'" + usuario.getImagem() + "'").append(", ")
                .append("'").append(usuario.getEmail()).append("', ")
                .append("'").append(usuario.getSenha()).append("');\n\n");

        // Instituições do usuário
        List<InstituicaoUsuario> instUsuarios = instituicaoUsuarioRepository.findInstituicaoUsuarioByUsuario_IdAndIsAtivoIsTrue(userId);
        sql.append("-- Instituições do usuário\n");
        for (InstituicaoUsuario iu : instUsuarios) {
            sql.append("INSERT INTO instituicao_usuario (id, usuario_id, instituicao_id, is_ativo) VALUES (")
                    .append("'").append(iu.getId()).append("', ")
                    .append("'").append(userId).append("', ")
                    .append("'").append(iu.getInstituicao().getId()).append("', ")
                    .append(iu.getIsAtivo()).append(");\n");
        }
        sql.append("\n");

        // Categorias do usuário
        List<CategoriaUsuario> catUsuarios = categoriaUsuarioRepository.findAllByUsuario_Id(userId);
        sql.append("-- Categorias do usuário\n");
        for (CategoriaUsuario cu : catUsuarios) {
            sql.append("INSERT INTO categoria_usuario (id, usuario_id, categoria_id, is_ativo) VALUES (")
                    .append("'").append(cu.getId()).append("', ")
                    .append("'").append(userId).append("', ")
                    .append("'").append(cu.getCategoria().getId()).append("', ")
                    .append(cu.getAtivo()).append(");\n");
        }
        sql.append("\n");

        // Configurações do usuário
        List<Configuracoes> configs = configuracoesRepository.findAllByUsuario_Id(userId);
        sql.append("-- Configurações do usuário\n");
        for (Configuracoes conf : configs) {
            sql.append("INSERT INTO configuracoes (id, usuario_id, inicio_mes_fiscal, ultima_atualizacao, limite_desejado_mensal) VALUES (")
                    .append("'").append(conf.getId()).append("', ")
                    .append("'").append(userId).append("', ")
                    .append(conf.getInicioMesFiscal()).append(", ")
                    .append("'").append(conf.getUltimaAtualizacao()).append("', ")
                    .append(conf.getLimiteDesejadoMensal())
                    .append(");\n");
        }
        sql.append("\n");

        // Limites por instituição
        List<LimitePorInstituicao> limitesInst = limitePorInstituicaoRepository.findByInstituicaoUsuario_Usuario_Id(userId);
        sql.append("-- Limites por instituição\n");
        for (LimitePorInstituicao li : limitesInst) {
            sql.append("INSERT INTO limite_por_instituicao (id, institucao_usuario_id, limite_desejado, configuracoes_id) VALUES (")
                    .append("'").append(li.getId()).append("', ")
                    .append(li.getInstituicaoUsuario().getId()).append(", ")
                    .append(li.getLimiteDesejado()).append(", ")
                    .append("'").append(li.getConfiguracoes().getId()).append("');\n");
        }
        sql.append("\n");

        // Limites por categoria
        List<LimitePorCategoria> limitesCat = limitePorCategoriaRepository.findByCategoriaUsuario_Usuario_Id(userId);
        sql.append("-- Limites por categoria\n");
        for (LimitePorCategoria lc : limitesCat) {
            sql.append("INSERT INTO limite_por_categoria (id, categoria_usuario_id, limite_desejado, configuracoes_id) VALUES (")
                    .append("'").append(lc.getId()).append("', ")
                    .append(lc.getCategoriaUsuario().getId()).append(", ")
                    .append(lc.getLimiteDesejado()).append(", ")
                    .append("'").append(lc.getConfiguracoes().getId()).append("');\n");
        }
        sql.append("\n");

        // Eventos financeiros do usuário
        List<EventoFinanceiro> eventos = eventoFinanceiroRepository.findAllByUsuario_Id(userId);
        sql.append("-- Eventos financeiros\n");
        for (EventoFinanceiro e : eventos) {
            sql.append("INSERT INTO evento_financeiro (id, usuario_id, tipo, valor, descricao, data_evento, data_registro) VALUES (")
                    .append("'").append(e.getId()).append("', ")
                    .append("'").append(userId).append("', ")
                    .append("'").append(e.getTipo()).append("', ")
                    .append(e.getValor()).append(", ")
                    .append("'").append(e.getDescricao().replace("'", "''")).append("', ")
                    .append("'").append(e.getDataEvento()).append("', ")
                    .append("'").append(e.getDataRegistro()).append("');\n");

            // Instituições vinculadas ao evento
            List<EventoInstituicao> insts = eventoInstituicaoRepository.findEventoInstituicaoByEventoFinanceiro_Id(e.getId());
            for (EventoInstituicao ei : insts) {
                sql.append("INSERT INTO evento_instituicao (id, fk_evento, instituicao_usuario_id, tipo_movimento, valor, parcelas) VALUES (")
                        .append("'").append(ei.getId()).append("', ")
                        .append("'").append(e.getId()).append("', ")
                        .append(ei.getInstituicaoUsuario().getId()).append(", ")
                        .append("'").append(ei.getTipoMovimento()).append("', ")
                        .append(ei.getValor()).append(", ")
                        .append(ei.getParcelas()).append(");\n");
            }

            // Detalhes de gasto
            EventoDetalhe gasto = eventoDetalheRepository.findGastoDetalheByEventoFinanceiro(e);
            if (gasto != null) {
                sql.append("INSERT INTO gasto_detalhe (id, fk_evento, titulo_gasto) VALUES (")
                        .append("'").append(gasto.getId()).append("', ")
                        .append("'").append(e.getId()).append("', ")
                        .append("'").append(gasto.getTituloGasto().replace("'", "''")).append("');\n");

                for (CategoriaUsuario cat : gasto.getCategoriaUsuario()) {
                    sql.append("INSERT INTO gasto_detalhe_categoria (gasto_detalhe_id, categoria_usuario_id) VALUES (")
                            .append("'").append(gasto.getId()).append("', ")
                            .append("'").append(cat.getId()).append("');\n");
                }
            }
            sql.append("\n");
        }

        return sql.toString();
    }

    public byte[] createExcel(UUID userId) {
        Usuario usuario = usuarioRepository.findById(userId).orElseThrow(() ->
                new EntidadeNaoEncontradaException(
                        "Usuário de id: %s não encontrado.".formatted(userId)
                )
        );

        List<EventoFinanceiro> eventosFinanceiros = eventoFinanceiroRepository
                .findEventoFinanceiroByUsuarioOrderByDataEventoDesc(usuario);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ===== Estilos =====
            WriteCellStyle headStyle = new WriteCellStyle();
            headStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex()); // fundo teal escuro
            WriteFont headFont = new WriteFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex()); // texto branco
            headStyle.setWriteFont(headFont);

            WriteCellStyle contentStyle = new WriteCellStyle();
            contentStyle.setWrapped(true);
            contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

            HorizontalCellStyleStrategy styleStrategy = new HorizontalCellStyleStrategy(headStyle, contentStyle);

            // ===== Writer =====
            ExcelWriter writer = EasyExcel.write(out)
                    .registerWriteHandler(styleStrategy) // aplica estilos
                    .build();

            // ===== Sheet 1: Informações básicas =====
            List<List<String>> headInfo = Arrays.asList(
                    Arrays.asList("Campo"),
                    Arrays.asList("Valor")
            );

            List<List<String>> infoSheet = new ArrayList<>();
            infoSheet.add(Arrays.asList("Nome", usuario.getNome() + " " + usuario.getSobrenome()));
            infoSheet.add(Arrays.asList("Data de Nascimento", usuario.getDataNascimento().toString()));
            infoSheet.add(Arrays.asList("Sexo", usuario.getSexo().toString()));
            infoSheet.add(Arrays.asList("Email", usuario.getEmail()));

            BigDecimal ganhosTotais = BigDecimal.ZERO;
            BigDecimal gastosTotais = BigDecimal.ZERO;
            BigDecimal transferenciasTotais = BigDecimal.ZERO;

            for (EventoFinanceiro evento : eventosFinanceiros) {
                if (evento.getTipo() == Tipo.Recebimento) {
                    ganhosTotais = ganhosTotais.add(BigDecimal.valueOf(evento.getValor()));
                } else if (evento.getTipo() == Tipo.Gasto) {
                    gastosTotais = gastosTotais.add(BigDecimal.valueOf(evento.getValor()));
                } else if (evento.getTipo() == Tipo.Transferencia) {
                    transferenciasTotais = transferenciasTotais.add(BigDecimal.valueOf(evento.getValor()));
                }
            }

            infoSheet.add(Arrays.asList("Resumo Geral", ""));
            infoSheet.add(Arrays.asList("Ganhos", "R$ " + ganhosTotais));
            infoSheet.add(Arrays.asList("Gastos", "R$ " + gastosTotais));
            infoSheet.add(Arrays.asList("Transferências", "R$ " + transferenciasTotais));
            infoSheet.add(Arrays.asList("Saldo", "R$ " + ganhosTotais.subtract(gastosTotais.add(transferenciasTotais))));

            WriteSheet sheetInfo = EasyExcel.writerSheet("Informações Básicas")
                    .head(headInfo) // cabeçalho explícito
                    .build();
            writer.write(infoSheet, sheetInfo);

            // ===== Sheets mensais =====
            Map<String, List<List<String>>> dadosPorMes = new LinkedHashMap<>();

            for (EventoFinanceiro evento : eventosFinanceiros) {
                LocalDate data = evento.getDataEvento();
                String nomeSheet = data.getMonth().toString().substring(0,3).toLowerCase() + "-" + data.getYear();

                dadosPorMes.putIfAbsent(nomeSheet, new ArrayList<>());

                List<String> linha = new ArrayList<>();
                linha.add(data.toString());
                linha.add(evento.getGastoDetalhe().getTituloGasto());
                linha.add(evento.getValor().toString());
                linha.add(evento.getTipo().toString());
                linha.add(evento.getDescricao());

                List<EventoInstituicao> instituicoes = eventoInstituicaoRepository
                        .findEventoInstituicaoByEventoFinanceiro_Id(evento.getId());

                if (!instituicoes.isEmpty()) {
                    EventoInstituicao inst = instituicoes.get(0);
                    linha.add(inst.getInstituicaoUsuario().getInstituicao().getNome());
                    linha.add(inst.getTipoMovimento().toString());
                    linha.add(String.valueOf(inst.getParcelas()));
                } else {
                    linha.add("-");
                    linha.add("-");
                    linha.add("-");
                }

                if (!evento.getGastoDetalhe().getCategoriaUsuario().isEmpty()) {
                    linha.add(evento.getGastoDetalhe().getCategoriaUsuario().get(0).getCategoria().getTitulo());
                } else {
                    linha.add("-");
                }

                dadosPorMes.get(nomeSheet).add(linha);
            }

            for (Map.Entry<String, List<List<String>>> entry : dadosPorMes.entrySet()) {
                List<List<String>> headMes = Arrays.asList(
                        Arrays.asList("Data"),
                        Arrays.asList("Título"),
                        Arrays.asList("Valor"),
                        Arrays.asList("Tipo"),
                        Arrays.asList("Descrição"),
                        Arrays.asList("Instituição"),
                        Arrays.asList("Movimentação"),
                        Arrays.asList("Parcelas"),
                        Arrays.asList("Categoria")
                );

                List<List<String>> linhasMes = new ArrayList<>(entry.getValue());

                // Resumo do mês
                BigDecimal ganhosMes = BigDecimal.ZERO;
                BigDecimal gastosMes = BigDecimal.ZERO;
                for (List<String> linha : entry.getValue()) {
                    String tipo = linha.get(3);
                    BigDecimal valor = new BigDecimal(linha.get(2));
                    if ("Recebimento".equals(tipo)) ganhosMes = ganhosMes.add(valor);
                    else if ("Gasto".equals(tipo)) gastosMes = gastosMes.add(valor);
                }
                linhasMes.add(Arrays.asList(""));
                linhasMes.add(Arrays.asList("Resumo do Mês", ""));
                linhasMes.add(Arrays.asList("Ganhos", "R$ " + ganhosMes));
                linhasMes.add(Arrays.asList("Gastos", "R$ " + gastosMes));
                linhasMes.add(Arrays.asList("Saldo", "R$ " + ganhosMes.subtract(gastosMes)));

                WriteSheet sheetMes = EasyExcel.writerSheet(entry.getKey())
                        .head(headMes) // cabeçalho explícito
                        .build();
                writer.write(linhasMes, sheetMes);
            }

            writer.finish();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
