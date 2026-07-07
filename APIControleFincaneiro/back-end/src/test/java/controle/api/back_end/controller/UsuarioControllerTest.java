package controle.api.back_end.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import controle.api.back_end.dto.usuario.in.UsuarioCreateDTO;
import controle.api.back_end.dto.usuario.out.UsuarioResponseDTO;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.model.usuario.UsuarioSexo;
import controle.api.back_end.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@DisplayName("UsuarioController - testes de integração da camada web")
class UsuarioControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  UsuarioService usuarioService;

    private ObjectMapper objectMapper;
    private UUID userId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        userId = UUID.randomUUID();

        usuario = new Usuario();
        usuario.setId(userId);
        usuario.setNome("Carlos");
        usuario.setSobrenome("Lima");
        usuario.setEmail("carlos@email.com");
        usuario.setSexo(UsuarioSexo.Masculino);
        usuario.setDataNascimento(LocalDate.of(1990, 4, 25));
        usuario.setSenha("Senha@123");
    }

    // ── GET /usuarios ──────────────────────────────────────────────────────
    @Test
    @DisplayName("GET /usuarios: retorna 200 com lista quando há usuários")
    void getUsuarios_retornaLista() throws Exception {
        when(usuarioService.getUsuarios()).thenReturn(List.of(usuario));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome", is("Carlos")));
    }

    @Test
    @DisplayName("GET /usuarios: retorna 204 quando lista vazia")
    void getUsuarios_retorna204QuandoVazio() throws Exception {
        when(usuarioService.getUsuarios()).thenReturn(List.of());

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isNoContent());
    }

    // ── GET /usuarios/{id} ─────────────────────────────────────────────────
    @Test
    @DisplayName("GET /usuarios/{id}: retorna 200 com usuário encontrado")
    void getUsuarioById_retornaUsuario() throws Exception {
        when(usuarioService.getUsuarioById(userId)).thenReturn(usuario);

        mockMvc.perform(get("/usuarios/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", is("Carlos")))
                .andExpect(jsonPath("$.email", is("carlos@email.com")));
    }

    @Test
    @DisplayName("GET /usuarios/{id}: retorna 404 quando usuário não encontrado")
    void getUsuarioById_retorna404() throws Exception {
        when(usuarioService.getUsuarioById(userId))
                .thenThrow(new EntidadeNaoEncontradaException("não encontrado"));

        mockMvc.perform(get("/usuarios/{id}", userId))
                .andExpect(status().isNotFound());
    }

    // ── GET /usuarios/saldo/{user_id} ──────────────────────────────────────
    @Test
    @DisplayName("GET /usuarios/saldo/{user_id}: retorna 200 com saldo")
    void getSaldoByUsuario_retornaSaldo() throws Exception {
        when(usuarioService.getSaldoByUsuario(userId))
                .thenReturn(java.math.BigDecimal.valueOf(1250.50));

        mockMvc.perform(get("/usuarios/saldo/{user_id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("1250.5"));
    }

    // ── POST /usuarios ─────────────────────────────────────────────────────
    @Test
    @DisplayName("POST /usuarios: cria usuário e retorna 201")
    void createUsuario_retorna201() throws Exception {
        UsuarioCreateDTO dto = new UsuarioCreateDTO();
        dto.setNome("Carlos");
        dto.setSobrenome("Lima");
        dto.setEmail("carlos@email.com");
        dto.setSexo(UsuarioSexo.Masculino);
        dto.setDataNascimento(LocalDate.of(1990, 4, 25));
        dto.setSenha("Senha@123");

        when(usuarioService.createUsuario(any())).thenReturn(usuario);
        doNothing().when(usuarioService).createConfiguracao(any());

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome", is("Carlos")));
    }

    @Test
    @DisplayName("POST /usuarios: retorna 400 para corpo inválido")
    void createUsuario_retorna400ParaBodyInvalido() throws Exception {
        // body vazio → falha de validação
        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /usuarios/{user_id} ─────────────────────────────────────────
    @Test
    @DisplayName("DELETE /usuarios/{user_id}: retorna 204 ao deletar")
    void deleteUserById_retorna204() throws Exception {
        doNothing().when(usuarioService).deleteUserbyId(userId);

        mockMvc.perform(delete("/usuarios/{user_id}", userId))
                .andExpect(status().isNoContent());
    }
}

