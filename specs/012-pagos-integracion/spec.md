# 012 - Integración de Pagos

> Integrar Mercado Pago, QR y otros medios de pago.

## Priority: 8

## Status: COMPLETE

---

## Requirements

### Dependencias

```gradle
implementation 'com.mercadopago:sdk-java:2.1.+'
```

### Configuración

```sql
-- Config de pagos por kiosco
CREATE TABLE config_pagos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Mercado Pago
    mp_access_token TEXT,
    mp_public_key TEXT,
    mp_user_id VARCHAR(50),

    -- QR Interoperable
    qr_alias VARCHAR(50),            -- Alias de CBU/CVU
    qr_cbu VARCHAR(22),

    -- General
    acepta_efectivo BOOLEAN DEFAULT true,
    acepta_debito BOOLEAN DEFAULT true,
    acepta_credito BOOLEAN DEFAULT true,
    acepta_mercadopago BOOLEAN DEFAULT false,
    acepta_qr BOOLEAN DEFAULT false,
    acepta_transferencia BOOLEAN DEFAULT true,

    created_at TIMESTAMP DEFAULT NOW()
);
```

### Servicio Mercado Pago

```java
@Service
public class MercadoPagoService {

    // Crear preferencia de pago (para checkout)
    PreferenceResponse crearPreferencia(VentaDTO venta);

    // Crear QR dinámico
    QrResponse crearQrDinamico(BigDecimal monto, String descripcion);

    // Verificar estado de pago
    PaymentStatus verificarPago(String paymentId);

    // Webhook de notificación
    void procesarWebhook(WebhookPayload payload);
}
```

### QR Interoperable

```java
@Service
public class QrService {

    // Generar QR estático con monto
    String generarQrEstatico(BigDecimal monto);

    // El QR usa el estándar EMVCo para Argentina
    // Incluye: alias, monto, descripción
}
```

### API Endpoints

```
POST   /api/pagos/mp/preferencia       → Crear preferencia MP
POST   /api/pagos/mp/qr                → Generar QR de MP
GET    /api/pagos/mp/status/{id}       → Estado de pago MP
POST   /api/pagos/mp/webhook           → Webhook de MP

POST   /api/pagos/qr/generar           → Generar QR interoperable
GET    /api/config/pagos               → Config de pagos
PUT    /api/config/pagos               → Actualizar config
```

### Frontend

#### En POS - Selección de pago
```tsx
<PaymentMethodSelector
  total={total}
  onSelect={(method, data) => handlePayment(method, data)}
/>
```

Métodos:
- **Efectivo**: Input de monto recibido, calcula vuelto
- **Débito/Crédito**: Solo confirmar
- **Mercado Pago**: Muestra QR o link de pago
- **QR Interoperable**: Muestra QR con monto
- **Transferencia**: Muestra datos bancarios + monto

#### Página `/configuracion/pagos`
- Toggle para cada método de pago
- Config de Mercado Pago (access token, public key)
- Config de QR (alias, CBU)

#### Componente QR
```tsx
<QrPayment
  amount={total}
  type="mercadopago" | "interoperable"
  onPaymentConfirmed={() => completarVenta()}
/>
```

### Verificación de pago

Para MP y QR, implementar polling o webhooks para confirmar el pago:

```typescript
// Polling cada 3 segundos
const checkPayment = async (paymentId: string) => {
  const status = await api.get(`/pagos/mp/status/${paymentId}`);
  if (status === 'approved') {
    completarVenta();
  }
};
```

---

## Acceptance Criteria

- [x] Config de pagos guardada por kiosco
- [x] Mercado Pago integrado (QR y checkout)
- [x] QR interoperable genera correctamente
- [x] Selector de método de pago en POS
- [x] Efectivo calcula vuelto
- [x] QR de MP se muestra y funciona
- [x] Webhook de MP procesa pagos
- [x] Página de configuración de pagos
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- MP tiene ambiente sandbox para testing
- QR interoperable sigue estándar BCRA
- Guardar access_token de forma segura
