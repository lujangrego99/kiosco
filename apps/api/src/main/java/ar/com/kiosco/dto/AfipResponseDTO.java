package ar.com.kiosco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfipResponseDTO {

    private boolean aprobado;
    private String cae;
    private LocalDate caeVencimiento;
    private Long numeroComprobante;

    @Builder.Default
    private List<String> observaciones = new ArrayList<>();

    @Builder.Default
    private List<String> errores = new ArrayList<>();

    public static AfipResponseDTO error(String mensaje) {
        return AfipResponseDTO.builder()
                .aprobado(false)
                .errores(List.of(mensaje))
                .build();
    }

    public static AfipResponseDTO exito(String cae, LocalDate caeVencimiento, Long numero) {
        return AfipResponseDTO.builder()
                .aprobado(true)
                .cae(cae)
                .caeVencimiento(caeVencimiento)
                .numeroComprobante(numero)
                .build();
    }

    public void addObservacion(String obs) {
        if (observaciones == null) {
            observaciones = new ArrayList<>();
        }
        observaciones.add(obs);
    }

    public void addError(String error) {
        if (errores == null) {
            errores = new ArrayList<>();
        }
        errores.add(error);
    }
}
