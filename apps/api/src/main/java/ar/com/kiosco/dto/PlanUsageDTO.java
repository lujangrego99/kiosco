package ar.com.kiosco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for plan usage information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanUsageDTO {

    private String plan;
    private LimitUsage productos;
    private LimitUsage usuarios;
    private LimitUsage ventasMes;
    private String proximoLimite;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimitUsage {
        private int current;
        private Integer limit;  // null = ilimitado
        private int percentage;

        public static LimitUsage of(int current, Integer limit) {
            int percentage = limit == null || limit == 0 ? 0 : (current * 100 / limit);
            return LimitUsage.builder()
                    .current(current)
                    .limit(limit)
                    .percentage(Math.min(percentage, 100))
                    .build();
        }
    }
}
