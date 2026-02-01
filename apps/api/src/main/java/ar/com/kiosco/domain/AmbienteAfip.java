package ar.com.kiosco.domain;

import lombok.Getter;

@Getter
public enum AmbienteAfip {
    TESTING("testing", "https://wswhomo.afip.gov.ar"),
    PRODUCTION("production", "https://wsw.afip.gov.ar");

    private final String codigo;
    private final String baseUrl;

    AmbienteAfip(String codigo, String baseUrl) {
        this.codigo = codigo;
        this.baseUrl = baseUrl;
    }

    public static AmbienteAfip fromCodigo(String codigo) {
        for (AmbienteAfip ambiente : values()) {
            if (ambiente.codigo.equalsIgnoreCase(codigo)) {
                return ambiente;
            }
        }
        return TESTING; // Default to testing
    }
}
