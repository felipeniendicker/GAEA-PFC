package Gaea.Api.controller;

import Gaea.Api.dto.DashboardResumoResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public DashboardResumoResponse obterResumo(Authentication authentication) {
        return dashboardService.obterResumo((Usuario) authentication.getPrincipal());
    }
}
