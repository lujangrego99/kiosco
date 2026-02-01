package ar.com.kiosco.domain;

import lombok.Getter;

@Getter
public enum ResultadoAfip {
    APROBADO("A", "Aprobado"),
    RECHAZADO("R", "Rechazado"),
    PARCIAL("P", "Parcialmente Aprobado");

    private final String codigo;
    private final String descripcion;

    ResultadoAfip(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public static ResultadoAfip fromCodigo(String codigo) {
        for (ResultadoAfip resultado : values()) {
            if (resultado.codigo.equalsIgnoreCase(codigo)) {
                return resultado;
            }
        }
        throw new IllegalArgumentException("Código de resultado AFIP desconocido: " + codigo);
    }

    public boolean isAprobado() {
        return this == APROBADO || this == PARCIAL;
    }
}
