package controle.api.back_end.controller;

import controle.api.back_end.dto.dashboard.CategoriaEPorcentagens;
import controle.api.back_end.dto.dashboard.MaiorGastoDoMes;
import controle.api.back_end.dto.dashboard.GastoTotalDoMes;
import controle.api.back_end.dto.registros.out.RegistroResponseDto;
import controle.api.back_end.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Endpoints referentes a dashboard da aplicação, de onde vem os dados para as KPI's e gráficos como cálculos e buscas.")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/saldo-total-mes/{data}/usuarios/{user_id}")
    public ResponseEntity<GastoTotalDoMes> getGastoTotalMes(@PathVariable LocalDate data, UUID user_id){
        GastoTotalDoMes gastoTotaldoMesAtual = dashboardService.getGastoTotalDoMes(data, user_id);

        return ResponseEntity.status(200).body(gastoTotaldoMesAtual);
    }

    @GetMapping("/maior-gasto-do-mes/{data}/usuarios/{user_id}")
    public ResponseEntity<MaiorGastoDoMes> getMaiorGastoDoMes(@PathVariable LocalDate data, UUID user_id){
        MaiorGastoDoMes maiorGastoDoMes = dashboardService.getMaiorGastoDoMes(data, user_id);
        return ResponseEntity.status(200).body(maiorGastoDoMes);
    }

    @GetMapping("/percentual-por-categoria/{data}/usuarios/{user_id}")
    public ResponseEntity<CategoriaEPorcentagens> getCategoriasEPorcentagens(@PathVariable LocalDate data, UUID user_id){
        CategoriaEPorcentagens categoriasEPorcentagens = dashboardService.getCategoriasEPorcentagens(data, user_id);
        return ResponseEntity.status(200).body(categoriasEPorcentagens);
    }

    @GetMapping("/gastos-por-tempo/{data}/usuarios/{user_id}")
    public ResponseEntity<List<RegistroResponseDto>> getGastosPorPeriodoDeTempo(@PathVariable LocalDate data, UUID user_id){
        List<RegistroResponseDto> response = dashboardService.getGastosPorPeriodoDeTempo(data, user_id);

        response.sort(Comparator.comparing(r -> r.getEventoFinanceiro().getDataEvento()));

        return ResponseEntity.status(200).body(response);
    }

}
