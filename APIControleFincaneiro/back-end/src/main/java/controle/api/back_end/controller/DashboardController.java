package controle.api.back_end.controller;

import controle.api.back_end.dto.MaiorGastoDoMes;
import controle.api.back_end.dto.dashboard.GastoTotalDoMes;
import controle.api.back_end.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping("/gasto-total-mes/{data}/usuarios/{user_id}")
    public ResponseEntity<GastoTotalDoMes> getGastoTotalMes(@PathVariable LocalDate data, UUID user_id){
        GastoTotalDoMes gastoTotaldoMesAtual = dashboardService.getGastoTotaldoMesAtual(data, user_id);

        return ResponseEntity.status(200).body(gastoTotaldoMesAtual);
    }

    @GetMapping("/maior-gasto-do-mes/{data}/usuarios/{user_id}")
    public ResponseEntity<MaiorGastoDoMes> getMaiorGastoDoMes(@PathVariable LocalDate data, UUID user_id){
        MaiorGastoDoMes maiorGastoDoMes = dashboardService.getMaiorGastoDoMes(data, user_id);
        return ResponseEntity.status(200).body(maiorGastoDoMes);
    }

}
