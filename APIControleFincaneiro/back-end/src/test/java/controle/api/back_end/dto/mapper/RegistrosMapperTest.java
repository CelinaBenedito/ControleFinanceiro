package controle.api.back_end.dto.mapper;

import controle.api.back_end.dto.registros.in.EventoDetalheCreateDto;
import controle.api.back_end.dto.registros.in.EventoFinanceiroCreateDto;
import controle.api.back_end.dto.registros.in.EventoInstituicaoCreateDto;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.model.usuario.UsuarioSexo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RegistrosMapper - testes unitários")
class RegistrosMapperTest {

    private EventoFinanceiro eventoFinanceiro;
    private EventoInstituicao eventoInstituicao;
    private EventoDetalhe eventoDetalhe;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNome("Pedro");
        usuario.setSobrenome("Alves");
        usuario.setEmail("pedro@email.com");
        usuario.setSexo(UsuarioSexo.Masculino);
        usuario.setDataNascimento(LocalDate.of(1995, 6, 20));

        Instituicao instituicao = new Instituicao(1, "Nubank");
        InstituicaoUsuario iu = new InstituicaoUsuario();
        iu.setId(1);
        iu.setInstituicao(instituicao);
        iu.setUsuario(usuario);

        Categoria categoria = new Categoria(1, "Alimentação");
        CategoriaUsuario cu = new CategoriaUsuario();
        cu.setId(1);
        cu.setCategoria(categoria);

        eventoFinanceiro = new EventoFinanceiro();
        eventoFinanceiro.setId(UUID.randomUUID());
        eventoFinanceiro.setUsuario(usuario);
        eventoFinanceiro.setTipo(Tipo.Gasto);
        eventoFinanceiro.setValor(150.0);
        eventoFinanceiro.setDescricao("Jantar");
        eventoFinanceiro.setDataEvento(LocalDate.of(2026, 1, 15));
        eventoFinanceiro.setDataRegistro(LocalDateTime.now());

        eventoInstituicao = new EventoInstituicao();
        eventoInstituicao.setId(10);
        eventoInstituicao.setInstituicaoUsuario(iu);
        eventoInstituicao.setTipoMovimento(TipoMovimento.Debito);
        eventoInstituicao.setValor(150.0);
        eventoInstituicao.setParcelas(1);
        eventoInstituicao.setEventoFinanceiro(eventoFinanceiro);

        eventoDetalhe = new EventoDetalhe();
        eventoDetalhe.setId(5L);
        eventoDetalhe.setTituloGasto("Restaurante");
        eventoDetalhe.setCategoriaUsuario(List.of(cu));
        eventoDetalhe.setEventoFinanceiro(eventoFinanceiro);
    }

    // ── toEntityFinanceiro ─────────────────────────────────────────────────
    @Test
    @DisplayName("toEntityFinanceiro: mapeia campos do DTO corretamente")
    void toEntityFinanceiro_mapeia() {
        UUID userId = UUID.randomUUID();
        EventoFinanceiroCreateDto dto = new EventoFinanceiroCreateDto();
        dto.setUsuario_id(userId);
        dto.setTipo(Tipo.Gasto);
        dto.setValor(200.0);
        dto.setDescricao("Compras");
        dto.setDataEvento(LocalDate.of(2026, 3, 10));

        EventoFinanceiro entity = RegistrosMapper.toEntityFinanceiro(dto);

        assertNotNull(entity);
        assertEquals(userId, entity.getUsuario().getId());
        assertEquals(Tipo.Gasto, entity.getTipo());
        assertEquals(200.0, entity.getValor());
        assertEquals("Compras", entity.getDescricao());
        assertEquals(LocalDate.of(2026, 3, 10), entity.getDataEvento());
    }

    @Test
    @DisplayName("toEntityFinanceiro: retorna null para entrada null")
    void toEntityFinanceiro_comNull_retornaNull() {
        assertNull(RegistrosMapper.toEntityFinanceiro((EventoFinanceiroCreateDto) null));
    }

    // ── toEntityEvento ─────────────────────────────────────────────────────
    @Test
    @DisplayName("toEntityEvento: mapeia campos do DTO corretamente")
    void toEntityEvento_mapeia() {
        EventoInstituicaoCreateDto dto = new EventoInstituicaoCreateDto();
        dto.setInstituicaoUsuario_id(3);
        dto.setTipoMovimento(TipoMovimento.Pix);
        dto.setValor(99.9);
        dto.setParcelas(2);

        EventoInstituicao entity = RegistrosMapper.toEntityEvento(dto);

        assertNotNull(entity);
        assertEquals(3, entity.getInstituicaoUsuario().getId());
        assertEquals(TipoMovimento.Pix, entity.getTipoMovimento());
        assertEquals(99.9, entity.getValor());
        assertEquals(2, entity.getParcelas());
    }

    @Test
    @DisplayName("toEntityEvento: retorna null para entrada null")
    void toEntityEvento_comNull_retornaNull() {
        assertNull(RegistrosMapper.toEntityEvento((EventoInstituicaoCreateDto) null));
    }

    // ── toEntityGasto ──────────────────────────────────────────────────────
    @Test
    @DisplayName("toEntityGasto: mapeia campos do DTO corretamente")
    void toEntityGasto_mapeia() {
        EventoDetalheCreateDto dto = new EventoDetalheCreateDto();
        dto.setCategoriaUsuario_id(List.of(1, 2));
        dto.setTituloGasto("Cinema");

        EventoDetalhe entity = RegistrosMapper.toEntityGasto(dto);

        assertNotNull(entity);
        assertEquals("Cinema", entity.getTituloGasto());
        assertEquals(2, entity.getCategoriaUsuario().size());
        assertEquals(1, entity.getCategoriaUsuario().get(0).getId());
        assertEquals(2, entity.getCategoriaUsuario().get(1).getId());
    }

    @Test
    @DisplayName("toEntityGasto: retorna null para entrada null")
    void toEntityGasto_comNull_retornaNull() {
        assertNull(RegistrosMapper.toEntityGasto((EventoDetalheCreateDto) null));
    }

    // ── toResponse ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("toResponse: mapeia EventoFinanceiro, Instituição e Detalhe")
    void toResponse_mapeiaCompleto() {
        RegistroResponseDto dto = RegistrosMapper.toResponse(
                eventoFinanceiro,
                List.of(eventoInstituicao),
                eventoDetalhe
        );

        assertNotNull(dto);
        assertEquals(eventoFinanceiro.getId(), dto.getEventoFinanceiro().getId());
        assertEquals(Tipo.Gasto, dto.getEventoFinanceiro().getTipo());
        assertEquals(150.0, dto.getEventoFinanceiro().getValor());

        assertFalse(dto.getEventoInstituicao().isEmpty());
        assertEquals(TipoMovimento.Debito, dto.getEventoInstituicao().get(0).getTipoMovimento());
        assertEquals("Nubank", dto.getEventoInstituicao().get(0).getInstituicao().getNome());

        assertEquals("Restaurante", dto.getGastoDetalhe().getTituloGasto());
        assertEquals("Alimentação", dto.getGastoDetalhe().getCategoria().get(0).getTitulo());
    }

    @Test
    @DisplayName("toResponse: retorna null quando qualquer argumento é null")
    void toResponse_comArgumentoNull_retornaNull() {
        assertNull(RegistrosMapper.toResponse(null, List.of(eventoInstituicao), eventoDetalhe));
        assertNull(RegistrosMapper.toResponse(eventoFinanceiro, null, eventoDetalhe));
        assertNull(RegistrosMapper.toResponse(eventoFinanceiro, List.of(eventoInstituicao), null));
    }

    // ── toResponse(lista) ──────────────────────────────────────────────────
    @Test
    @DisplayName("toResponse(listas): mapeia listas de mesmo tamanho corretamente")
    void toResponseListas_mapeiaCorretamente() {
        List<RegistroResponseDto> resultado = RegistrosMapper.toResponse(
                List.of(eventoFinanceiro),
                List.of(List.of(eventoInstituicao)),
                List.of(eventoDetalhe)
        );
        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("toResponse(listas): lança exceção para listas de tamanhos diferentes")
    void toResponseListas_lançaExcecaoParaTamanhosDiferentes() {
        assertThrows(IllegalArgumentException.class, () ->
                RegistrosMapper.toResponse(
                        List.of(eventoFinanceiro),
                        List.of(),
                        List.of(eventoDetalhe)
                )
        );
    }
}

