# 010 - AFIP Setup y Configuración

> Configuración inicial para facturación electrónica AFIP.

## Priority: 6

## Status: COMPLETE

---

## Requirements

### Modelo de Datos

```sql
-- Configuración fiscal del kiosco
CREATE TABLE config_fiscal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cuit VARCHAR(13) NOT NULL,
    razon_social VARCHAR(200) NOT NULL,
    condicion_iva VARCHAR(30) NOT NULL,  -- MONOTRIBUTO, RESP_INSCRIPTO, EXENTO
    domicilio_fiscal TEXT NOT NULL,
    inicio_actividades DATE,
    punto_venta INT NOT NULL,
    certificado_path TEXT,               -- Path al certificado .crt
    clave_privada_path TEXT,             -- Path al .key
    ambiente VARCHAR(10) DEFAULT 'testing',  -- testing, production
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Tipos de comprobante (catálogo global)
-- Ya viene precargado: Factura A, B, C, Nota Crédito A, B, C, etc.
```

### Enum Condición IVA

```java
public enum CondicionIva {
    RESPONSABLE_INSCRIPTO("Responsable Inscripto", 1),
    MONOTRIBUTO("Monotributo", 6),
    EXENTO("Exento", 4),
    CONSUMIDOR_FINAL("Consumidor Final", 5);

    private final String descripcion;
    private final int codigoAfip;
}
```

### Servicio de Certificados

```java
@Service
public class CertificadoService {
    // Guardar certificado .crt y .key de forma segura
    void guardarCertificado(MultipartFile crt, MultipartFile key);

    // Verificar que el certificado es válido
    boolean verificarCertificado();

    // Obtener fecha de vencimiento
    LocalDate getVencimiento();
}
```

### API Endpoints

```
GET    /api/config/fiscal              → Obtener config fiscal
POST   /api/config/fiscal              → Guardar config fiscal
POST   /api/config/fiscal/certificado  → Subir certificado AFIP
GET    /api/config/fiscal/verificar    → Verificar conexión con AFIP
```

### Frontend

#### Página `/configuracion/fiscal`

Wizard paso a paso:

**Paso 1: Datos del contribuyente**
- CUIT (con validación)
- Razón Social
- Condición IVA (select)
- Domicilio fiscal
- Inicio de actividades

**Paso 2: Punto de venta**
- Número de punto de venta
- Explicación de cómo obtenerlo en AFIP

**Paso 3: Certificado digital**
- Subir archivo .crt
- Subir archivo .key
- Instrucciones paso a paso para generar certificado
- Link a AFIP

**Paso 4: Verificación**
- Probar conexión con AFIP (ambiente testing)
- Mostrar resultado

#### Componente de estado
```tsx
<ConfigFiscalStatus />
// 🔴 Sin configurar | 🟡 Certificado vencido | 🟢 Configurado
```

### Guía para el kiosquero

Incluir instrucciones claras:
1. Cómo obtener certificado en AFIP
2. Cómo habilitar punto de venta
3. Diferencia testing vs producción
4. Qué hacer si el certificado vence

---

## Acceptance Criteria

- [x] Modelo de config fiscal creado
- [x] Se puede guardar configuración fiscal
- [x] Se puede subir certificado .crt y .key
- [x] Validación de CUIT (dígito verificador)
- [x] Wizard de configuración paso a paso
- [x] Verificación de conexión con AFIP
- [x] Instrucciones claras para el usuario
- [x] Almacenamiento seguro de certificados
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Usar ambiente TESTING de AFIP para desarrollo
- NO emitir facturas reales hasta tener todo probado
- Certificados deben almacenarse de forma segura (no en Git)
