package ar.com.kiosco.domain;

import lombok.Getter;

@Getter
public enum TipoComprobante {
    FACTURA_A(1, "Factura A"),
    NOTA_DEBITO_A(2, "Nota de Débito A"),
    NOTA_CREDITO_A(3, "Nota de Crédito A"),
    FACTURA_B(6, "Factura B"),
    NOTA_DEBITO_B(7, "Nota de Débito B"),
    NOTA_CREDITO_B(8, "Nota de Crédito B"),
    FACTURA_C(11, "Factura C"),
    NOTA_DEBITO_C(12, "Nota de Débito C"),
    NOTA_CREDITO_C(13, "Nota de Crédito C");

    private final int codigoAfip;
    private final String descripcion;

    TipoComprobante(int codigoAfip, String descripcion) {
        this.codigoAfip = codigoAfip;
        this.descripcion = descripcion;
    }

    public static TipoComprobante fromCodigoAfip(int codigo) {
        for (TipoComprobante tipo : values()) {
            if (tipo.codigoAfip == codigo) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de comprobante AFIP desconocido: " + codigo);
    }

    public String getLetra() {
        if (this == FACTURA_A || this == NOTA_DEBITO_A || this == NOTA_CREDITO_A) {
            return "A";
        } else if (this == FACTURA_B || this == NOTA_DEBITO_B || this == NOTA_CREDITO_B) {
            return "B";
        } else {
            return "C";
        }
    }

    public boolean isFactura() {
        return this == FACTURA_A || this == FACTURA_B || this == FACTURA_C;
    }

    public boolean isNotaCredito() {
        return this == NOTA_CREDITO_A || this == NOTA_CREDITO_B || this == NOTA_CREDITO_C;
    }

    public boolean isNotaDebito() {
        return this == NOTA_DEBITO_A || this == NOTA_DEBITO_B || this == NOTA_DEBITO_C;
    }

    public static TipoComprobante determinarTipoFactura(CondicionIva emisor, CondicionIva receptor) {
        if (emisor == CondicionIva.RESPONSABLE_INSCRIPTO) {
            if (receptor == CondicionIva.RESPONSABLE_INSCRIPTO) {
                return FACTURA_A;
            } else {
                return FACTURA_B;
            }
        } else if (emisor == CondicionIva.MONOTRIBUTO) {
            return FACTURA_C;
        }
        throw new IllegalArgumentException("Condición IVA no soportada para facturación: " + emisor);
    }
}
