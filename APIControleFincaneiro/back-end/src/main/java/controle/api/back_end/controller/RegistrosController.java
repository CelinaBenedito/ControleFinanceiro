package controle.api.back_end.controller;

import controle.api.back_end.dto.registros.in.EventoFinanceiroCreateDto;
import controle.api.back_end.dto.registros.in.EventoInstituicaoCreateDto;
import controle.api.back_end.dto.registros.in.GastoDetalheCreateDto;
import controle.api.back_end.dto.registros.in.RegistroCompletoCreateDto;
import controle.api.back_end.dto.registros.mapper.RegistrosMapper;
import controle.api.back_end.dto.registros.out.RegistroUsuarioResponseDto;
import controle.api.back_end.model.eventoFinanceiro.EventoFinanceiro;
import controle.api.back_end.model.eventoFinanceiro.EventoInstituicao;
import controle.api.back_end.model.eventoFinanceiro.GastoDetalhe;
import controle.api.back_end.service.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/registros")
public class RegistrosController {
    private final RegistroService registroService;

    public RegistrosController(RegistroService registroService) {
        this.registroService = registroService;
    }

    @PostMapping
    public ResponseEntity<RegistroUsuarioResponseDto> createRegistroCompleto(@RequestBody @Valid RegistroCompletoCreateDto dto){
        EventoFinanceiro eventoCreated = registroService.createEventoFinanceiro(
                RegistrosMapper.toEntity(dto.getFinanceiro()));

        EventoInstituicao instituicaoCreated = registroService.createEventoInstituicao(
                RegistrosMapper.toEntity(dto.getInstituicao()), eventoCreated);

        GastoDetalhe gastoCreated = registroService.createGastoDetalhe(
                RegistrosMapper.toEntity(dto.getDetalhe()), eventoCreated);

        RegistroUsuarioResponseDto response = RegistrosMapper.toResponseUser(
                eventoCreated, instituicaoCreated, gastoCreated);

        return ResponseEntity.status(200).body(response);

    }
}
