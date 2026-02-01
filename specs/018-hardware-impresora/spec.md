# 018 - Impresora Térmica y Tickets

> Soporte para impresoras térmicas y generación de tickets.

## Priority: 14

## Status: COMPLETE

---

## Requirements

### Configuración

```sql
-- Config de impresora por kiosco
CREATE TABLE config_impresora (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(20) NOT NULL,        -- USB, BLUETOOTH, RED
    nombre VARCHAR(100),
    direccion VARCHAR(200),           -- IP o MAC
    puerto INT,
    ancho_papel INT DEFAULT 80,       -- 58 o 80 mm
    activa BOOLEAN DEFAULT true,
    imprimir_automatico BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Servicio de Impresión (Backend)

```java
@Service
public class TicketService {

    // Generar contenido del ticket en formato ESC/POS
    byte[] generarTicketVenta(VentaDTO venta);

    // Generar ticket de cierre de caja
    byte[] generarTicketCierreCaja(CierreDTO cierre);

    // Generar ticket de prueba
    byte[] generarTicketPrueba();
}
```

### Formato ESC/POS

```java
public class EscPosBuilder {
    public EscPosBuilder centrar();
    public EscPosBuilder izquierda();
    public EscPosBuilder negrita(boolean on);
    public EscPosBuilder tamano(int size);  // 1 = normal, 2 = doble
    public EscPosBuilder linea(String texto);
    public EscPosBuilder separador();
    public EscPosBuilder codigo(String codigo);  // código de barras
    public EscPosBuilder qr(String data);
    public EscPosBuilder cortar();
    public byte[] build();
}
```

### Plantilla de Ticket

```
========================================
           NOMBRE KIOSCO
        Dirección del local
         Tel: 11-1234-5678
========================================
Fecha: 01/02/2026 14:30
Ticket: #0001234
----------------------------------------
Cant  Descripción             Precio
----------------------------------------
 2    Coca Cola 500ml         $2.400
 1    Papas Lays              $1.500
 3    Alfajor Jorgito           $900
----------------------------------------
                    TOTAL:    $4.800
----------------------------------------
Medio de pago: EFECTIVO
Recibido: $5.000
Vuelto: $200
----------------------------------------
      ¡Gracias por su compra!
========================================
```

### API Endpoints

```
GET    /api/config/impresora           → Config actual
PUT    /api/config/impresora           → Guardar config
POST   /api/impresora/test             → Imprimir ticket de prueba
POST   /api/impresora/imprimir/venta/{id}
POST   /api/impresora/imprimir/cierre/{id}
GET    /api/tickets/venta/{id}         → Obtener ticket en formato texto
GET    /api/tickets/venta/{id}/pdf     → Obtener ticket en PDF
```

### Frontend

#### Página `/configuracion/impresora`
- Tipo de conexión (USB, Bluetooth, Red)
- Dirección/Puerto
- Ancho de papel (58mm / 80mm)
- Toggle "Imprimir automáticamente"
- Botón "Imprimir prueba"

#### En POS (después de cobrar)
```tsx
<TicketActions ventaId={venta.id}>
  <Button>🖨️ Imprimir</Button>
  <Button>📱 Enviar WhatsApp</Button>
  <Button>📧 Enviar Email</Button>
</TicketActions>
```

#### Modal de ticket digital
```tsx
<TicketPreview ventaId={venta.id} />
// Muestra el ticket formateado
// Botones: Copiar | Compartir | Descargar PDF
```

### Web Bluetooth API (para impresoras BT)

```typescript
// lib/printer.ts
export class BluetoothPrinter {
  async connect(): Promise<void>;
  async print(data: Uint8Array): Promise<void>;
  async disconnect(): Promise<void>;
  isConnected(): boolean;
}
```

### Envío por WhatsApp

```typescript
// Usar Web Share API o link directo
const shareWhatsApp = (telefono: string, ticket: string) => {
  const url = `https://wa.me/${telefono}?text=${encodeURIComponent(ticket)}`;
  window.open(url, '_blank');
};
```

---

## Acceptance Criteria

- [x] Modelo de config de impresora
- [x] Generación de ticket en formato ESC/POS
- [x] Plantilla de ticket configurable
- [x] Impresión por USB funciona
- [x] Impresión por Bluetooth funciona
- [x] Impresión por red funciona
- [x] Ticket de prueba
- [x] Preview de ticket en pantalla
- [x] Envío por WhatsApp
- [x] Descarga como PDF
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- ESC/POS es el estándar de impresoras térmicas
- Web Bluetooth requiere HTTPS
- USB requiere WebUSB API o app nativa
- Como fallback, mostrar ticket en pantalla para screenshot
