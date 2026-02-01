package ar.com.kiosco.dto.reporte;

public record InsightDTO(
    String tipo,          // SUCCESS, WARNING, INFO, DANGER
    String icono,         // emoji
    String titulo,
    String descripcion,
    String accion         // optional action suggestion
) {}
