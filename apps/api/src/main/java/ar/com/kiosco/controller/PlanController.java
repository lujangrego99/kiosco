package ar.com.kiosco.controller;

import ar.com.kiosco.dto.PlanUsageDTO;
import ar.com.kiosco.service.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanLimitService planLimitService;

    /**
     * Returns the current plan usage for the authenticated kiosco.
     */
    @GetMapping("/usage")
    public ResponseEntity<PlanUsageDTO> getUsage() {
        return ResponseEntity.ok(planLimitService.getUsage());
    }
}
