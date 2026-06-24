package controle.api.back_end.service;

import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.model.usuario.UsuarioSexo;
import controle.api.back_end.repository.categoria.CategoriaRepository;
import controle.api.back_end.repository.categoria.CategoriaUsuarioRepository;
import controle.api.back_end.repository.configuracoes.ConfiguracoesRepository;
import controle.api.back_end.repository.configuracoes.LimitePorCategoriaRepository;
import controle.api.back_end.repository.configuracoes.LimitePorInstituicaoRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoDetalheRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoFinanceiroRepository;
import controle.api.back_end.repository.eventoFinanceiro.EventoInstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoRepository;
import controle.api.back_end.repository.instituicao.InstituicaoUsuarioRepository;
import controle.api.back_end.repository.usuario.UsuarioRepository;
import controle.api.back_end.factory.EventoFinanceiroFactory;
import controle.api.back_end.factory.MovimentoFactory;
import controle.api.back_end.factory.RecorrenciaFactory;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistroService - testes unitários")
class RegistroServiceTest {

    @Mock EventoFinanceiroRepository eventoFinanceiroRepository;
    @Mock EventoInstituicaoRepository eventoInstituicaoRepository;
    @Mock EventoDetalheRepository eventoDetalheRepository;
    @Mock CategoriaUsuarioRepository categoriaUsuarioRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock InstituicaoUsuarioRepository instituicaoUsuarioRepository;
    @Mock MovimentoFactory movimentoFactory;
    @Mock EventoFinanceiroFactory eventoFinanceiroFactory;
    @Mock RecorrenciaFactory recorrenciaFactory;
    @Mock InstituicaoRepository instituicaoRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock InstituicaoService instituicaoService;
    @Mock LimitePorInstituicaoRepository limitePorInstituicaoRepository;
    @Mock LimitePorCategoriaRepository limitePorCategoriaRepository;
    @Mock ConfiguracoesRepository configuracoesRepository;
    @Mock UsuarioService usuarioService;

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
        usuario.setSexo(UsuarioSexo.Feminino);
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
}

