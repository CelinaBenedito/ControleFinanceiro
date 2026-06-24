package controle.api.back_end.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import controle.api.back_end.dto.registros.in.EventoDetalheCreateDto;
import controle.api.back_end.dto.registros.in.EventoFinanceiroCreateDto;
import controle.api.back_end.dto.registros.in.EventoInstituicaoCreateDto;
import controle.api.back_end.dto.registros.in.RegistroCompletoCreateDto;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.categoria.Categoria;
import controle.api.back_end.model.categoria.CategoriaUsuario;
import controle.api.back_end.model.eventoFinanceiro.*;
import controle.api.back_end.model.instituicao.Instituicao;
import controle.api.back_end.model.instituicao.InstituicaoUsuario;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.model.usuario.UsuarioSexo;
import controle.api.back_end.service.RegistroExportacaoService;
import controle.api.back_end.service.RegistroService;
import controle.api.back_end.strategy.eventoFinanceiro.Registro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrosController.class)
@DisplayName("RegistrosController — testes da camada web")
class RegistrosControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  RegistroService registroService;
    @MockBean  RegistroExportacaoService exportacaoService;

    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID eventoId;
    private RegistroResponseDto registroDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        userId   = UUID.randomUUID();
        eventoId = UUID.randomUUID();

        // RegistroResponseDto de exemplo
        RegistroResponseDto.EventoFinanceiroDto efDto = new RegistroResponseDto.EventoFinanceiroDto();
        efDto.setId(eventoId);
        efDto.setTipo(Tipo.Gasto);
        efDto.setValor(300.0);
        efDto.setDescricao("Padaria");
        efDto.setDataEvento(LocalDate.of(2026, 3, 5));

        RegistroResponseDto.EventoInstituicaoDto.InstituicaoDto instDto =
                new RegistroResponseDto.EventoInstituicaoDto.InstituicaoDto();
        instDto.setId(1);
        instDto.setNome("Nubank");

        RegistroResponseDto.EventoInstituicaoDto eiDto = new RegistroResponseDto.EventoInstituicaoDto();
        eiDto.setId(1);
        eiDto.setInstituicao(instDto);
        eiDto.setTipoMovimento(TipoMovimento.Debito);
        eiDto.setValor(300.0);
        eiDto.setParcelas(1);

        RegistroResponseDto.GastoDetalheDto.CategoriaDto catDto =
                new RegistroResponseDto.GastoDetalheDto.CategoriaDto();
        catDto.setId(1);
        catDto.setTitulo("Alimentação");

        RegistroResponseDto.GastoDetalheDto gasto = new RegistroResponseDto.GastoDetalheDto();
        gasto.setId(1L);
        gasto.setTituloGasto("Pão");
        gasto.setCategoria(List.of(catDto));

        registroDto = new RegistroResponseDto();
        registroDto.setEventoFinanceiro(efDto);
        registroDto.setEventoInstituicao(List.of(eiDto));
        registroDto.setGastoDetalhe(gasto);
        registroDto.setDataRegistro(LocalDateTime.now());
    }

    // ── GET /registros/{user_id} ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /registros/{user_id}: retorna 200 com lista quando há registros")
    void listarRegistros_retornaLista() throws Exception {
        Usuario usuario      = criarUsuario();
        EventoFinanceiro ev  = criarEvento(usuario);
        EventoInstituicao ei = criarEventoInstituicao(ev);
        EventoDetalhe det    = criarDetalhe(ev);

        when(registroService.getEventosFinanceirosByUser(userId)).thenReturn(List.of(ev));
        when(registroService.getEventosInstituicoesByEventoFinanceiro(List.of(ev)))
                .thenReturn(List.of(List.of(ei)));
        when(registroService.getGastosDetalhesByEventoFinanceiro(List.of(ev)))
                .thenReturn(List.of(det));

        mockMvc.perform(get("/registros/{user_id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /registros/{user_id}: retorna 204 quando lista vazia")
    void listarRegistros_retorna204QuandoVazio() throws Exception {
        when(registroService.getEventosFinanceirosByUser(userId)).thenReturn(List.of());

        mockMvc.perform(get("/registros/{user_id}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /registros/{user_id}: retorna 404 quando usuário não encontrado")
    void listarRegistros_retorna404() throws Exception {
        when(registroService.getEventosFinanceirosByUser(userId))
                .thenThrow(new EntidadeNaoEncontradaException("não encontrado"));

        mockMvc.perform(get("/registros/{user_id}", userId))
                .andExpect(status().isNotFound());
    }

    // ── GET /registros/saldo-poupanca/usuarios/{user_id} ────────────────────

    @Test
    @DisplayName("GET /registros/saldo-poupanca/usuarios/{user_id}: retorna 200 com saldo")
    void getSaldoPoupanca_retornaSaldo() throws Exception {
        when(registroService.getSaldoPoupanca(userId)).thenReturn(500.0);

        mockMvc.perform(get("/registros/saldo-poupanca/usuarios/{user_id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("500.0"));
    }

    // ── POST /registros ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /registros: cria registro e retorna 201")
    void criarRegistro_retorna201() throws Exception {
        RegistroCompletoCreateDto dto = criarRegistroDto();

        EventoFinanceiro ev  = criarEvento(criarUsuario());
        EventoInstituicao ei = criarEventoInstituicao(ev);
        EventoDetalhe det    = criarDetalhe(ev);

        Map<EventoFinanceiro, List<EventoInstituicao>> instMap = new HashMap<>();
        instMap.put(ev, List.of(ei));
        Map<EventoFinanceiro, EventoDetalhe> detMap = new HashMap<>();
        detMap.put(ev, det);

        when(registroService.createEventoFinanceiro(any(), any(), any()))
                .thenReturn(new Registro(List.of(ev), instMap, detMap));

        mockMvc.perform(post("/registros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    // ── DELETE /registros/{evento_id} ────────────────────────────────────────

    @Test
    @DisplayName("DELETE /registros/{evento_id}: retorna 204")
    void deletarRegistro_retorna204() throws Exception {
        doNothing().when(registroService).deleteRegistroByEventoFinanceiro_Id(eventoId);

        mockMvc.perform(delete("/registros/{evento_id}", eventoId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /registros/{evento_id}: retorna 404 quando não encontrado")
    void deletarRegistro_retorna404() throws Exception {
        doThrow(new EntidadeNaoEncontradaException("não encontrado"))
                .when(registroService).deleteRegistroByEventoFinanceiro_Id(eventoId);

        mockMvc.perform(delete("/registros/{evento_id}", eventoId))
                .andExpect(status().isNotFound());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Usuario criarUsuario() {
        Usuario u = new Usuario();
        u.setId(userId);
        u.setNome("Teste");
        u.setSobrenome("User");
        u.setEmail("teste@email.com");
        u.setSexo(UsuarioSexo.Feminino);
        u.setDataNascimento(LocalDate.of(1995, 1, 1));
        return u;
    }

    private EventoFinanceiro criarEvento(Usuario usuario) {
        EventoFinanceiro e = new EventoFinanceiro();
        e.setId(eventoId);
        e.setUsuario(usuario);
        e.setTipo(Tipo.Gasto);
        e.setValor(300.0);
        e.setDescricao("Padaria");
        e.setDataEvento(LocalDate.of(2026, 3, 5));
        e.setDataRegistro(LocalDateTime.now());
        return e;
    }

    private EventoInstituicao criarEventoInstituicao(EventoFinanceiro evento) {
        Instituicao inst = new Instituicao(1, "Nubank");
        InstituicaoUsuario iu = new InstituicaoUsuario();
        iu.setId(1);
        iu.setInstituicao(inst);

        EventoInstituicao ei = new EventoInstituicao();
        ei.setId(1);
        ei.setEventoFinanceiro(evento);
        ei.setInstituicaoUsuario(iu);
        ei.setTipoMovimento(TipoMovimento.Debito);
        ei.setValor(300.0);
        ei.setParcelas(1);
        return ei;
    }

    private EventoDetalhe criarDetalhe(EventoFinanceiro evento) {
        Categoria categoria = new Categoria(1, "Alimentação");
        CategoriaUsuario cu = new CategoriaUsuario();
        cu.setId(1);
        cu.setCategoria(categoria);

        EventoDetalhe d = new EventoDetalhe();
        d.setId(1L);
        d.setTituloGasto("Pão");
        d.setEventoFinanceiro(evento);
        d.setCategoriaUsuario(List.of(cu));
        return d;
    }

    private RegistroCompletoCreateDto criarRegistroDto() {
        EventoFinanceiroCreateDto finDto = new EventoFinanceiroCreateDto();
        finDto.setUsuario_id(userId);
        finDto.setTipo(Tipo.Gasto);
        finDto.setValor(300.0);
        finDto.setDescricao("Padaria");
        finDto.setDataEvento(LocalDate.of(2026, 3, 5));

        EventoInstituicaoCreateDto instDto = new EventoInstituicaoCreateDto();
        instDto.setInstituicaoUsuario_id(1);
        instDto.setTipoMovimento(TipoMovimento.Debito);
        instDto.setValor(300.0);
        instDto.setParcelas(1);

        EventoDetalheCreateDto detDto = new EventoDetalheCreateDto();
        detDto.setCategoriaUsuario_id(List.of(1));
        detDto.setTituloGasto("Pão");

        RegistroCompletoCreateDto dto = new RegistroCompletoCreateDto();
        dto.setFinanceiro(finDto);
        dto.setInstituicao(List.of(instDto));
        dto.setDetalhe(detDto);
        return dto;
    }
}
