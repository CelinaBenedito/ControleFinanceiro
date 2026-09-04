package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.model.usuario.GeneroUsuario;
import controle.api.back_end.repository.categoria.CategoriaUsuarioRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import controle.api.back_end.factory.EventoFinanceiroFactory;
import controle.api.back_end.factory.MovimentoFactory;
import controle.api.back_end.factory.RecorrenciaFactory;
import controle.api.back_end.exception.SaldoInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistroService - testes unitários")
class RegistroServiceTest {

    // Dependências do RegistroService refatorado
    @Mock EventoFinanceiroRepository eventoFinanceiroRepository;
    @Mock EventoInstituicaoRepository eventoInstituicaoRepository;
    @Mock EventoDetalheRepository eventoDetalheRepository;
    @Mock CategoriaUsuarioRepository categoriaUsuarioRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    @Mock MovimentoFactory movimentoFactory;
    @Mock EventoFinanceiroFactory eventoFinanceiroFactory;
    @Mock RecorrenciaFactory recorrenciaFactory;
    @Mock InstituicaoService instituicaoService;
    @Mock controle.api.back_end.repository.configuracoes.ConfiguracoesRepository configuracoesRepository;
    @Mock EmailService emailService;
    @Mock controle.api.back_end.repository.poupanca.CaixinhaRepository caixinhaRepository;
    @Mock controle.api.back_end.repository.eventoFinanceiro.RecorrenciaFinanceiraRepository recorrenciaFinanceiraRepository;

    @InjectMocks RegistroService registroService;

    private UUID userId;
    private UUID eventoId;
    private Usuario usuario;
    private EventoFinanceiro eventoFinanceiro;

    @BeforeEach
    void setUp() {
        userId   = UUID.randomUUID();
        eventoId = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setId(userId);
        usuario.setNome("Maria");
        usuario.setSobrenome("Souza");
        usuario.setEmail("maria@email.com");
        usuario.setGenero(GeneroUsuario.MULHER_CIS);
        usuario.setDataNascimento(LocalDate.of(1993, 3, 10));

        eventoFinanceiro = new EventoFinanceiro();
        eventoFinanceiro.setId(eventoId);
        eventoFinanceiro.setUsuario(usuario);
        eventoFinanceiro.setTipo(Tipo.Gasto);
        eventoFinanceiro.setValor(200.0);
        eventoFinanceiro.setDescricao("Supermercado");
        eventoFinanceiro.setDataEvento(LocalDate.of(2026, 1, 20));
        eventoFinanceiro.setDataRegistro(LocalDateTime.now());
    }

    // ── getEventosFinanceirosByUser ────────────────────────────────────────
    @Test
    @DisplayName("getEventosFinanceirosByUser: retorna eventos quando usuário existe")
    void getEventosFinanceirosByUser_retornaEventos() {
        when(usuarioRepository.existsById(userId)).thenReturn(true);
        when(eventoFinanceiroRepository.getEventoFinanceirosByUsuario_id(userId))
                .thenReturn(List.of(eventoFinanceiro));

        List<EventoFinanceiro> resultado = registroService.getEventosFinanceirosByUser(userId);

        assertEquals(1, resultado.size());
        assertEquals(eventoId, resultado.get(0).getId());
    }

    @Test
    @DisplayName("getEventosFinanceirosByUser: lança exceção quando usuário não existe")
    void getEventosFinanceirosByUser_usuarioNaoExiste_lancaExcecao() {
        when(usuarioRepository.existsById(userId)).thenReturn(false);

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> registroService.getEventosFinanceirosByUser(userId));
    }

    // ── getGastosDetalhesByEventoFinanceiro ───────────────────────────────
    @Test
    @DisplayName("getGastosDetalhesByEventoFinanceiro: retorna detalhes para lista de eventos")
    void getGastosDetalhesByEventoFinanceiro_retornaDetalhes() {
        EventoDetalhe detalhe = new EventoDetalhe();
        detalhe.setId(1L);
        detalhe.setTituloGasto("Mercado");
        detalhe.setCategoriaUsuario(List.of());

        when(eventoFinanceiroRepository.existsById(eventoId)).thenReturn(true);
        when(eventoDetalheRepository.findGastoDetalheByEventoFinanceiro(eventoFinanceiro))
                .thenReturn(detalhe);

        List<EventoDetalhe> resultado =
                registroService.getGastosDetalhesByEventoFinanceiro(List.of(eventoFinanceiro));

        assertEquals(1, resultado.size());
        assertEquals("Mercado", resultado.get(0).getTituloGasto());
    }

    @Test
    @DisplayName("getGastosDetalhesByEventoFinanceiro: lança exceção quando evento não existe")
    void getGastosDetalhesByEventoFinanceiro_eventoNaoExiste_lancaExcecao() {
        when(eventoFinanceiroRepository.existsById(eventoId)).thenReturn(false);

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> registroService.getGastosDetalhesByEventoFinanceiro(List.of(eventoFinanceiro)));
    }

    // ── editEventoFinanceiro ──────────────────────────────────────────────
    @Test
    @DisplayName("editEventoFinanceiro: atualiza campos e salva")
    void editEventoFinanceiro_atualizaCampos() {
        EventoFinanceiro novosDados = new EventoFinanceiro();
        novosDados.setDescricao("Farmácia");
        novosDados.setDataEvento(LocalDate.of(2026, 2, 1));
        novosDados.setTipo(Tipo.Gasto);

        when(eventoFinanceiroRepository.findById(eventoId)).thenReturn(Optional.of(eventoFinanceiro));
        when(eventoFinanceiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventoFinanceiro resultado = registroService.editEventoFinanceiro(eventoId, novosDados);

        assertEquals("Farmácia", resultado.getDescricao());
        verify(eventoFinanceiroRepository).save(eventoFinanceiro);
    }

    @Test
    @DisplayName("editEventoFinanceiro: lança exceção quando evento não existe")
    void editEventoFinanceiro_naoExiste_lancaExcecao() {
        when(eventoFinanceiroRepository.findById(eventoId)).thenReturn(Optional.empty());

        assertThrows(EntidadeNaoEncontradaException.class,
                () -> registroService.editEventoFinanceiro(eventoId, eventoFinanceiro));
    }

    // ── getEventosInstituicoesByEventoFinanceiro ──────────────────────────
    @Test
    @DisplayName("getEventosInstituicoesByEventoFinanceiro: retorna listas de instituições por evento")
    void getEventosInstituicoesByEventoFinanceiro_retornaListas() {
        EventoInstituicao ei = new EventoInstituicao();
        ei.setId(1);

        when(eventoInstituicaoRepository.findEventoInstituicaoByEventoFinanceiro_Id(eventoId))
                .thenReturn(List.of(ei));

        List<List<EventoInstituicao>> resultado =
                registroService.getEventosInstituicoesByEventoFinanceiro(List.of(eventoFinanceiro));

        assertEquals(1, resultado.size());
        assertEquals(1, resultado.get(0).size());
    }

    // ── createGastoDetalhe ────────────────────────────────────────────────
    @Test
    @DisplayName("createGastoDetalhe: salva detalhe com categoria válida")
    void createGastoDetalhe_salvaCategoriaValida() {
        Categoria categoria = new Categoria(1, "Lazer");
        CategoriaUsuario cu = new CategoriaUsuario();
        cu.setId(1);
        cu.setCategoria(categoria);

        EventoDetalhe detalhe = new EventoDetalhe();
        detalhe.setTituloGasto("Cinema");
        detalhe.setCategoriaUsuario(List.of(cu));

        when(eventoFinanceiroRepository.existsById(eventoId)).thenReturn(true);
        when(categoriaUsuarioRepository.findById(1)).thenReturn(Optional.of(cu));
        when(eventoDetalheRepository.save(any())).thenReturn(detalhe);

        EventoDetalhe resultado = registroService.createGastoDetalhe(detalhe, eventoFinanceiro);

        assertNotNull(resultado);
        assertEquals("Cinema", resultado.getTituloGasto());
    }

    // ── createEventoInstituicao com Crédito ──────────────────────────────────
    @Test
    @DisplayName("createEventoInstituicao: permite compra no crédito mesmo sem saldo")
    void createEventoInstituicao_permiteCreditoSemSaldo() {
        // Arrange: Instituição com limite de crédito mas saldo zero
        Instituicao inst = new Instituicao();
        inst.setId(1);
        inst.setNome("Banco XYZ");

        InstituicaoUsuario instUsuario = new InstituicaoUsuario();
        instUsuario.setId(1);
        instUsuario.setInstituicao(inst);
        instUsuario.setUsuario(usuario);
        instUsuario.setIsAtivo(true);
        instUsuario.setLimiteCredito(java.math.BigDecimal.valueOf(5000.0));

        EventoInstituicao pagamento = new EventoInstituicao();
        pagamento.setInstituicaoUsuario(instUsuario);
        pagamento.setTipoMovimento(TipoMovimento.Credito);
        pagamento.setValor(300.0);
        pagamento.setParcelas(1);

        // Mock: estratégia de movimento
        controle.api.back_end.strategy.movimento.MovimentoStrategy strategy = mock(controle.api.back_end.strategy.movimento.MovimentoStrategy.class);
        controle.api.back_end.strategy.movimento.MovimentoResultado resultado =
            new controle.api.back_end.strategy.movimento.MovimentoResultado(pagamento, 1, 300.0);

        when(instituicaoUsuarioRepository.findById(1)).thenReturn(Optional.of(instUsuario));
        when(movimentoFactory.getStrategy(eq(TipoMovimento.Credito), any())).thenReturn(strategy);
        when(strategy.processar(pagamento)).thenReturn(resultado);
        when(eventoFinanceiroRepository.existsById(eventoId)).thenReturn(true);
        when(eventoInstituicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act: cria evento de compra no crédito (mesmo sem saldo disponível)
        List<EventoInstituicao> resultados = registroService.createEventoInstituicao(
            List.of(pagamento), eventoFinanceiro, true);

        // Assert: deve criar o evento SEM lançar SaldoInsuficienteException
        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        assertEquals(TipoMovimento.Credito, resultados.get(0).getTipoMovimento());

        // Verifica que NÃO consultou saldo (porque crédito não precisa validar)
        verify(instituicaoService, never()).getSaldoDebitoByInstituicao(anyInt());
    }

    @Test
    @DisplayName("createEventoInstituicao: bloqueia gasto de hoje sem saldo")
    void createEventoInstituicao_bloqueiaGastoAtualSemSaldo() {
        eventoFinanceiro.setDataEvento(LocalDate.now());

        Instituicao inst = new Instituicao();
        inst.setId(1);
        inst.setNome("Banco XYZ");

        InstituicaoUsuario instUsuario = new InstituicaoUsuario();
        instUsuario.setId(1);
        instUsuario.setInstituicao(inst);
        instUsuario.setUsuario(usuario);
        instUsuario.setIsAtivo(true);

        EventoInstituicao pagamento = new EventoInstituicao();
        pagamento.setInstituicaoUsuario(instUsuario);
        pagamento.setTipoMovimento(TipoMovimento.Debito);
        pagamento.setValor(300.0);
        pagamento.setParcelas(1);

        controle.api.back_end.strategy.movimento.MovimentoStrategy strategy =
                mock(controle.api.back_end.strategy.movimento.MovimentoStrategy.class);
        controle.api.back_end.strategy.movimento.MovimentoResultado resultado =
                new controle.api.back_end.strategy.movimento.MovimentoResultado(pagamento, 1, 300.0);

        when(instituicaoUsuarioRepository.findById(1)).thenReturn(Optional.of(instUsuario));
        when(movimentoFactory.getStrategy(eq(TipoMovimento.Debito), any())).thenReturn(strategy);
        when(strategy.processar(pagamento)).thenReturn(resultado);
        when(eventoFinanceiroRepository.existsById(eventoId)).thenReturn(true);
        when(instituicaoService.getSaldoDebitoByInstituicao(1)).thenReturn(BigDecimal.ZERO);

        assertThrows(SaldoInsuficienteException.class,
                () -> registroService.createEventoInstituicao(List.of(pagamento), eventoFinanceiro, true));

        verify(eventoInstituicaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("createEventoInstituicao: permite gasto futuro sem saldo")
    void createEventoInstituicao_permiteGastoFuturoSemSaldo() {
        eventoFinanceiro.setDataEvento(LocalDate.now().plusDays(1));

        Instituicao inst = new Instituicao();
        inst.setId(1);
        inst.setNome("Banco XYZ");

        InstituicaoUsuario instUsuario = new InstituicaoUsuario();
        instUsuario.setId(1);
        instUsuario.setInstituicao(inst);
        instUsuario.setUsuario(usuario);
        instUsuario.setIsAtivo(true);

        EventoInstituicao pagamento = new EventoInstituicao();
        pagamento.setInstituicaoUsuario(instUsuario);
        pagamento.setTipoMovimento(TipoMovimento.Debito);
        pagamento.setValor(300.0);
        pagamento.setParcelas(1);

        controle.api.back_end.strategy.movimento.MovimentoStrategy strategy =
                mock(controle.api.back_end.strategy.movimento.MovimentoStrategy.class);
        controle.api.back_end.strategy.movimento.MovimentoResultado resultado =
                new controle.api.back_end.strategy.movimento.MovimentoResultado(pagamento, 1, 300.0);

        when(instituicaoUsuarioRepository.findById(1)).thenReturn(Optional.of(instUsuario));
        when(movimentoFactory.getStrategy(eq(TipoMovimento.Debito), any())).thenReturn(strategy);
        when(strategy.processar(pagamento)).thenReturn(resultado);
        when(eventoFinanceiroRepository.existsById(eventoId)).thenReturn(true);
        when(eventoInstituicaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<EventoInstituicao> resultados = registroService.createEventoInstituicao(
                List.of(pagamento), eventoFinanceiro, true);

        assertNotNull(resultados);
        assertEquals(1, resultados.size());
        verify(instituicaoService, never()).getSaldoDebitoByInstituicao(anyInt());
    }
}
