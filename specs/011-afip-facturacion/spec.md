# 011 - AFIP Facturación Electrónica

> Emisión de facturas electrónicas A, B, C con AFIP.

## Priority: 7

## Status: COMPLETE

---

## Requirements

### Dependencias

```gradle
// Web services AFIP (SOAP)
implementation 'org.apache.cxf:cxf-rt-frontend-jaxws:4.0.3'
implementation 'org.apache.cxf:cxf-rt-transports-http:4.0.3'
```

### Modelo de Datos

```sql
-- Comprobantes emitidos
CREATE TABLE comprobantes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id UUID REFERENCES ventas(id),
    cliente_id UUID REFERENCES clientes(id),

    -- Tipo y número
    tipo_comprobante INT NOT NULL,    -- 1=FA, 6=FB, 11=FC, etc.
    punto_venta INT NOT NULL,
    numero BIGINT NOT NULL,

    -- Datos fiscales
    cuit_emisor VARCHAR(13) NOT NULL,
    cuit_receptor VARCHAR(13),
    condicion_iva_receptor INT,

    -- Importes
    importe_neto DECIMAL(12,2),
    importe_iva DECIMAL(12,2),
    importe_total DECIMAL(12,2) NOT NULL,

    -- AFIP response
    cae VARCHAR(20),
    cae_vencimiento DATE,
    resultado VARCHAR(10),            -- A=Aprobado, R=Rechazado
    observaciones TEXT,

    fecha_emision DATE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_comprobantes_venta ON comprobantes(venta_id);
CREATE UNIQUE INDEX idx_comprobantes_numero ON comprobantes(tipo_comprobante, punto_venta, numero);
```

### Servicio AFIP

```java
@Service
public class AfipService {

    // Obtener último número de comprobante
    long getUltimoComprobante(int tipoComprobante, int puntoVenta);

    // Solicitar CAE
    ComprobanteResponse solicitarCAE(ComprobanteRequest request);

    // Consultar comprobante
    ComprobanteResponse consultarComprobante(int tipo, int pv, long numero);
}
```

### Request/Response

```java
public record ComprobanteRequest(
    int tipoComprobante,
    int puntoVenta,
    long numero,
    LocalDate fecha,
    int condicionIvaReceptor,
    String cuitReceptor,           // null para consumidor final
    BigDecimal importeNeto,
    BigDecimal importeIva,
    BigDecimal importeTotal
) {}

public record ComprobanteResponse(
    boolean aprobado,
    String cae,
    LocalDate caeVencimiento,
    List<String> observaciones,
    List<String> errores
) {}
```

### Lógica de tipo de factura

```java
public int determinarTipoFactura(CondicionIva emisor, CondicionIva receptor) {
    if (emisor == RESPONSABLE_INSCRIPTO) {
        if (receptor == RESPONSABLE_INSCRIPTO) return 1;  // Factura A
        else return 6;  // Factura B
    } else if (emisor == MONOTRIBUTO) {
        return 11;  // Factura C
    }
    throw new IllegalStateException("Condición no soportada");
}
```

### API Endpoints

```
POST   /api/facturacion/emitir         → Emitir factura para venta
GET    /api/facturacion/{ventaId}      → Obtener comprobante de venta
GET    /api/facturacion/comprobantes   → Listar comprobantes
POST   /api/facturacion/reenviar/{id}  → Reenviar por email/WhatsApp
```

### Frontend

#### En POS (después de cobrar)
- Preguntar si quiere factura
- Si sí, pedir datos del cliente (CUIT, condición IVA)
- Emitir factura automáticamente
- Mostrar CAE y opción de imprimir/enviar

#### Página `/facturacion`
- Lista de comprobantes emitidos
- Filtros por fecha, tipo, estado
- Ver detalle de comprobante
- Reimprimir / Reenviar

#### Página `/facturacion/{id}`
- Detalle del comprobante
- Vista previa de factura
- Botones: Imprimir, Enviar Email, Enviar WhatsApp

### Generación de PDF

```java
@Service
public class FacturaPdfService {
    byte[] generarPdf(Comprobante comprobante);
}
```

El PDF debe incluir:
- Datos del emisor
- Datos del receptor
- Detalle de productos
- CAE y código de barras
- QR de AFIP

---

## Acceptance Criteria

- [x] Conexión con web service AFIP funciona
- [x] Se puede emitir Factura A, B y C
- [x] CAE se obtiene correctamente
- [x] Comprobante se guarda en BD
- [x] PDF de factura se genera correctamente
- [x] Se puede reimprimir factura
- [ ] Se puede enviar por email
- [x] Manejo de errores de AFIP
- [x] Lista de comprobantes emitidos
- [x] `./gradlew test` pasa

---

## Notes

- Usar WSDL de AFIP para generar clientes SOAP
- Ambiente testing: https://wswhomo.afip.gov.ar
- Ambiente producción: https://servicios1.afip.gov.ar
- El CAE tiene vencimiento, mostrarlo claramente
