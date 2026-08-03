package controle.api.back_end.controller;
import controle.api.back_end.dto.emprestimo.bancario.EmprestimoBancarioCreateDTO;
import controle.api.back_end.dto.emprestimo.bancario.EmprestimoBancarioPagamentoDTO;
import controle.api.back_end.dto.emprestimo.bancario.EmprestimoBancarioResponseDTO;
import controle.api.back_end.service.EmprestimoBancarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@CrossOrigin
@RestController
@RequestMapping("/emprestimos-bancarios")
public class EmprestimoBancarioController {
    private final EmprestimoBancarioService service;
    public EmprestimoBancarioController(EmprestimoBancarioService service) {
        this.service = service;
    }
    @PostMapping
    public ResponseEntity<EmprestimoBancarioResponseDTO> criar(
            @Valid @RequestBody EmprestimoBancarioCreateDTO dto) {
        return ResponseEntity.status(201).body(service.criar(dto));
    }
    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<List<EmprestimoBancarioResponseDTO>> listar(
            @PathVariable UUID usuarioId) {
        List<EmprestimoBancarioResponseDTO> lista = service.listarPorUsuario(usuarioId);
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }
    @PatchMapping("/{id}/pagar-parcela")
    public ResponseEntity<EmprestimoBancarioResponseDTO> pagarParcela(
            @PathVariable UUID id,
            @RequestBody EmprestimoBancarioPagamentoDTO dto) {
        return ResponseEntity.ok(service.pagarParcela(id, dto));
    }
    @PatchMapping("/{id}/quitar")
    public ResponseEntity<EmprestimoBancarioResponseDTO> quitar(
            @PathVariable UUID id,
            @RequestBody(required = false) EmprestimoBancarioPagamentoDTO dto) {
        return ResponseEntity.ok(service.quitar(id, dto));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}