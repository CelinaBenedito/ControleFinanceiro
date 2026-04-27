package controle.api.back_end.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@CrossOrigin
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Endpoints referentes a dashboard da aplicação, de onde vem os dados para as KPI's e gráficos como cálculos e buscas.")
public class DashboardController {
}
