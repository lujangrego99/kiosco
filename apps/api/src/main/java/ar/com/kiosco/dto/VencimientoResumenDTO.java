package ar.com.kiosco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VencimientoResumenDTO {
    private int proximosAVencer;
    private int vencidos;
    private int totalLotesActivos;
}
