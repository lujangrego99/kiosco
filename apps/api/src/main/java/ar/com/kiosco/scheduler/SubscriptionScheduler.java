package ar.com.kiosco.scheduler;

import ar.com.kiosco.dto.SuscripcionDTO;
import ar.com.kiosco.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled tasks for subscription management.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SuscripcionService suscripcionService;

    /**
     * Check for expired subscriptions every day at midnight.
     * Marks subscriptions as VENCIDA if their fecha_fin has passed.
     */
    @Scheduled(cron = "0 0 0 * * *") // Every day at midnight
    public void checkExpiredSubscriptions() {
        log.info("Starting expired subscription check...");

        try {
            List<SuscripcionDTO> vencidas = suscripcionService.procesarVencidas();

            if (vencidas.isEmpty()) {
                log.info("No expired subscriptions found");
            } else {
                log.info("Processed {} expired subscriptions", vencidas.size());
                for (SuscripcionDTO s : vencidas) {
                    log.info("  - Kiosco: {} (ID: {})", s.kioscoNombre(), s.kioscoId());
                }
            }
        } catch (Exception e) {
            log.error("Error processing expired subscriptions", e);
        }
    }
}
