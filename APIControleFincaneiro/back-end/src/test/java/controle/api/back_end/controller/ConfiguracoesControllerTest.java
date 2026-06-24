package controle.api.back_end.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import controle.api.back_end.dto.configuracoes.in.ConfiguracoesCreateDTO;
import controle.api.back_end.dto.configuracoes.out.ConfiguracaoUsuarioResponseDTO;
import controle.api.back_end.dto.configuracoes.out.ConfiguracoesResponsesDTO;
import controle.api.back_end.exception.EntidadeJaExisteException;
import controle.api.back_end.exception.EntidadeNaoEncontradaException;
import controle.api.back_end.model.configuracoes.Configuracoes;
import controle.api.back_end.model.usuario.Usuario;
import controle.api.back_end.model.usuario.UsuarioSexo;
import controle.api.back_end.service.ConfiguracoesService;
import controle.api.back_end.service.UploadService;
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

@WebMvcTest(ConfiguracoesController.class)
@DisplayName("ConfiguracoesController - testes de integração da camada web")
class ConfiguracoesControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  ConfiguracoesService configuracoesService;
    @MockBean  UploadService uploadService;

    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID configId;
    private Configuracoes configuracoes;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        userId   = UUID.randomUUID();
        configId = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setNome("Fernanda");
        usuario.setSobrenome("Ramos");
        usuario.setEmail("fernanda@email.com");
        usuario.setSexo(UsuarioSexo.Feminino);
        usuario.setDataNascimento(LocalDate.of(1997, 7, 20));

        configuracoes = new Configuracoes();
        configuracoes.setId(configId);
        configuracoes.setUsuario(usuario);
        configuracoes.setInicioMesFiscal(1);
        configuracoes.setLimiteDesejadoMensal(3000.0);
        configuracoes.setUltimaAtualizacao(LocalDate.now());
        configuracoes.setLimitePorCategoria(List.of());
        configuracoes.setLimitePorInstituicao(List.of());
    }

    // ── GET /configuracoes ─────────────────────────────────────────────────
    @Test
    @DisplayName("GET /configuracoes: retorna 200 com lista quando há configurações")
    void getConfiguracoes_retornaLista() throws Exception {
        when(configuracoesService.getConfiguracoes()).thenReturn(List.of(configuracoes));

        mockMvc.perform(get("/configuracoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /configuracoes: retorna 204 quando lista vazia")
    void getConfiguracoes_retorna204QuandoVazio() throws Exception {
        when(configuracoesService.getConfiguracoes()).thenReturn(List.of());

        mockMvc.perform(get("/configuracoes"))
                .andExpect(status().isNoContent());
    }

    // ── GET /configuracoes/{id} ────────────────────────────────────────────
    @Test
    @DisplayName("GET /configuracoes/{id}: retorna 200 com configuração")
    void getConfiguracaoById_retornaConfiguracao() throws Exception {
        when(configuracoesService.getConfiguracoesById(configId)).thenReturn(configuracoes);

        mockMvc.perform(get("/configuracoes/{id}", configId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicioMesFiscal", is(1)))
                .andExpect(jsonPath("$.limiteDesejadoMensal", is(3000.0)));
    }

    @Test
    @DisplayName("GET /configuracoes/{id}: retorna 404 quando não encontrado")
    void getConfiguracaoById_retorna404() throws Exception {
        when(configuracoesService.getConfiguracoesById(configId))
                .thenThrow(new EntidadeNaoEncontradaException("não encontrado"));

        mockMvc.perform(get("/configuracoes/{id}", configId))
                .andExpect(status().isNotFound());
    }

    // ── GET /configuracoes/usuarios/{user_id} ──────────────────────────────
    @Test
    @DisplayName("GET /configuracoes/usuarios/{user_id}: retorna 200")
    void getConfiguracaoByUserId_retornaConfiguracao() throws Exception {
        when(configuracoesService.getConfiguracaoByUserId(userId)).thenReturn(configuracoes);

        mockMvc.perform(get("/configuracoes/usuarios/{user_id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.nome", is("Fernanda")));
    }

    @Test
    @DisplayName("GET /configuracoes/usuarios/{user_id}: retorna 404 quando não encontrado")
    void getConfiguracaoByUserId_retorna404() throws Exception {
        when(configuracoesService.getConfiguracaoByUserId(userId))
                .thenThrow(new EntidadeNaoEncontradaException("não encontrado"));

        mockMvc.perform(get("/configuracoes/usuarios/{user_id}", userId))
                .andExpect(status().isNotFound());
    }

    // ── POST /configuracoes ────────────────────────────────────────────────
    @Test
    @DisplayName("POST /configuracoes: cria configuração e retorna 201")
    void createConfiguracao_retorna201() throws Exception {
        ConfiguracoesCreateDTO dto = new ConfiguracoesCreateDTO();
        dto.setFkUsuario(userId);
        dto.setInicioMesFiscal(1);
        dto.setLimiteDesejadoMensal(3000.0);

        when(configuracoesService.createConfiguracao(any(), any())).thenReturn(configuracoes);

        mockMvc.perform(post("/configuracoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inicioMesFiscal", is(1)));
    }

    @Test
    @DisplayName("POST /configuracoes: retorna 409 quando já existe configuração")
    void createConfiguracao_retorna409QuandoJaExiste() throws Exception {
        ConfiguracoesCreateDTO dto = new ConfiguracoesCreateDTO();
        dto.setFkUsuario(userId);
        dto.setInicioMesFiscal(1);

        when(configuracoesService.createConfiguracao(any(), any()))
                .thenThrow(new EntidadeJaExisteException("já existe"));

        mockMvc.perform(post("/configuracoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    // ── DELETE /configuracoes/usuarios/{user_id}/dados/deletar-tudo ────────
    @Test
    @DisplayName("DELETE /configuracoes/usuarios/{user_id}/dados/deletar-tudo: retorna 204")
    void deleteAll_retorna204() throws Exception {
        doNothing().when(configuracoesService).deleteAllByUsuario(userId);

        mockMvc.perform(delete("/configuracoes/usuarios/{user_id}/dados/deletar-tudo", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /configuracoes/usuarios/{user_id}/dados/deletar-tudo: retorna 404 quando sem registros")
    void deleteAll_retorna404QuandoSemRegistros() throws Exception {
        doThrow(new EntidadeNaoEncontradaException("sem registros"))
                .when(configuracoesService).deleteAllByUsuario(userId);

        mockMvc.perform(delete("/configuracoes/usuarios/{user_id}/dados/deletar-tudo", userId))
                .andExpect(status().isNotFound());
    }
}

