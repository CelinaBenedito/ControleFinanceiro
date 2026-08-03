package controle.api.back_end.controller;

import controle.api.back_end.dto.emprestimo.in.EmprestimoCreateDTO;
import controle.api.back_end.dto.emprestimo.in.EmprestimoPagamentoParcialDTO;
import controle.api.back_end.dto.emprestimo.out.EmprestimoResponseDTO;
import controle.api.back_end.dto.emprestimo.out.EmprestimoResumoDTO;
import controle.api.back_end.model.emprestimo.TipoEmprestimo;
import controle.api.back_end.service.EmprestimoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/emprestimos")
@Tag(name = "Empréstimos",
     description = "Gerenciamento de empréstimos feitos pelo usuário ou recebidos. " +
                   "Controla quanto você emprestou para outras pessoas e quanto você deve.")
public class EmprestimoController {

    private final EmprestimoService emprestimoService;

    public EmprestimoController(EmprestimoService emprestimoService) {
        this.emprestimoService = emprestimoService;
    }

    // =========================================================================
    // CRIAR
    // =========================================================================

    @PostMapping
    @Operation(summary = "Criar um novo empréstimo",
               description = """
                       Registra um empréstimo feito ou recebido.
                       
                       **Tipos:**
                       - `EMPRESTEI` – você emprestou dinheiro/cartão para alguém
                       - `PEDI_EMPRESTADO` – você pegou dinheiro emprestado de alguém
                       
                       O status inicial é sempre `PENDENTE`.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empréstimo criado com sucesso.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<EmprestimoResponseDTO> criar(@Valid @RequestBody EmprestimoCreateDTO dto) {
        return ResponseEntity.status(201).body(emprestimoService.criar(dto));
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    @GetMapping("/usuarios/{usuarioId}")
    @Operation(summary = "Listar todos os empréstimos do usuário",
               description = "Retorna todos os empréstimos (pendentes, parciais e quitados) ordenados por data de criação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de empréstimos.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "204", description = "Nenhum empréstimo encontrado.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<List<EmprestimoResponseDTO>> listarPorUsuario(@PathVariable UUID usuarioId) {
        List<EmprestimoResponseDTO> lista = emprestimoService.listarPorUsuario(usuarioId);
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/usuarios/{usuarioId}/nao-quitados")
    @Operation(summary = "Listar empréstimos não quitados",
               description = "Retorna apenas empréstimos com status PENDENTE ou PAGO_PARCIAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de empréstimos não quitados.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "204", description = "Nenhum empréstimo não quitado.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<List<EmprestimoResponseDTO>> listarNaoQuitados(@PathVariable UUID usuarioId) {
        List<EmprestimoResponseDTO> lista = emprestimoService.listarNaoQuitados(usuarioId);
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/usuarios/{usuarioId}/tipo/{tipo}")
    @Operation(summary = "Listar empréstimos por tipo",
               description = "Filtra empréstimos por EMPRESTEI ou PEDI_EMPRESTADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de empréstimos do tipo especificado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "204", description = "Nenhum empréstimo encontrado.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<List<EmprestimoResponseDTO>> listarPorTipo(
            @PathVariable UUID usuarioId,
            @PathVariable TipoEmprestimo tipo) {
        List<EmprestimoResponseDTO> lista = emprestimoService.listarPorTipo(usuarioId, tipo);
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/{emprestimoId}")
    @Operation(summary = "Buscar empréstimo por ID",
               description = "Retorna os dados completos de um empréstimo específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empréstimo encontrado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado.", content = @Content)
    })
    public ResponseEntity<EmprestimoResponseDTO> buscarPorId(@PathVariable UUID emprestimoId) {
        return ResponseEntity.ok(emprestimoService.buscarPorId(emprestimoId));
    }

    @GetMapping("/usuarios/{usuarioId}/resumo")
    @Operation(summary = "Obter resumo de empréstimos",
               description = """
                       Retorna um resumo consolidado:
                       - Quantas pessoas devem ao usuário e o valor total a receber
                       - Quantas pessoas o usuário deve e o valor total a pagar
                       
                       Útil para exibir notificações na tela inicial.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumo calculado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResumoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado.", content = @Content)
    })
    public ResponseEntity<EmprestimoResumoDTO> obterResumo(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(emprestimoService.obterResumo(usuarioId));
    }

    // =========================================================================
    // ATUALIZAR
    // =========================================================================

    @PutMapping("/{emprestimoId}")
    @Operation(summary = "Editar um empréstimo",
               description = "Atualiza os dados do empréstimo. O status é recalculado automaticamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empréstimo atualizado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado.", content = @Content)
    })
    public ResponseEntity<EmprestimoResponseDTO> editar(
            @PathVariable UUID emprestimoId,
            @Valid @RequestBody EmprestimoCreateDTO dto) {
        return ResponseEntity.ok(emprestimoService.editar(emprestimoId, dto));
    }

    @PatchMapping("/{emprestimoId}/pagamento-parcial")
    @Operation(summary = "Registrar pagamento parcial",
               description = """
                       Adiciona um valor ao campo `valorPago` do empréstimo.
                       O status é atualizado automaticamente:
                       - Se valorPago = valorTotal → QUITADO
                       - Se 0 < valorPago < valorTotal → PAGO_PARCIAL
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento registrado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Valor inválido.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado.", content = @Content)
    })
    public ResponseEntity<EmprestimoResponseDTO> registrarPagamentoParcial(
            @PathVariable UUID emprestimoId,
            @Valid @RequestBody EmprestimoPagamentoParcialDTO dto) {
        return ResponseEntity.ok(emprestimoService.registrarPagamentoParcial(emprestimoId, dto));
    }

    @PatchMapping("/{emprestimoId}/quitar")
    @Operation(summary = "Marcar como quitado",
               description = "Define o empréstimo como totalmente pago, ajustando valorPago = valorTotal e status = QUITADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empréstimo quitado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado.", content = @Content)
    })
    public ResponseEntity<EmprestimoResponseDTO> marcarComoQuitado(@PathVariable UUID emprestimoId) {
        return ResponseEntity.ok(emprestimoService.marcarComoQuitado(emprestimoId));
    }

    @PatchMapping("/{emprestimoId}/reabrir")
    @Operation(summary = "Reabrir empréstimo quitado",
               description = "Desfaz a quitação, voltando o status para PENDENTE e zerando o valorPago.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empréstimo reaberto.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado.", content = @Content)
    })
    public ResponseEntity<EmprestimoResponseDTO> reabrir(@PathVariable UUID emprestimoId) {
        return ResponseEntity.ok(emprestimoService.reabrir(emprestimoId));
    }

    // =========================================================================
    // DELETAR
    // =========================================================================

    @DeleteMapping("/{emprestimoId}")
    @Operation(summary = "Deletar um empréstimo",
               description = "Remove permanentemente o registro de empréstimo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Empréstimo deletado.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado.", content = @Content)
    })
    public ResponseEntity<Void> deletar(@PathVariable UUID emprestimoId) {
        emprestimoService.deletar(emprestimoId);
        return ResponseEntity.noContent().build();
    }
}

