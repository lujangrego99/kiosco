package ar.com.kiosco.domain;

import lombok.Getter;

@Getter
public enum CondicionIva {
    RESPONSABLE_INSCRIPTO("Responsable Inscripto", 1),
    MONOTRIBUTO("Monotributo", 6),
    EXENTO("Exento", 4),
    CONSUMIDOR_FINAL("Consumidor Final", 5);

    private final String descripcion;
    private final int codigoAfip;

    CondicionIva(String descripcion, int codigoAfip) {
        this.descripcion = descripcion;
        this.codigoAfip = codigoAfip;
    }

    public static CondicionIva fromCodigoAfip(int codigo) {
        for (CondicionIva condicion : values()) {
            if (condicion.codigoAfip == codigo) {
                return condicion;
            }
        }
        throw new IllegalArgumentException("Código AFIP desconocido: " + codigo);
    }
}
