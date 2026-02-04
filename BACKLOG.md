# Backlog - Features Post-MVP

> Ideas y features para implementar después del MVP básico.

---

## Arquitectura

- [ ] **Multi-tenancy (schema-per-tenant)** - Cada kiosco tiene su schema como en Pacioli
- [ ] **Offline First (PWA + IndexedDB)** - Vender sin internet, sync cuando vuelve
- [ ] **Redis cache** - Sesiones y cache de productos frecuentes

---

## Facturación AFIP

- [ ] Factura A / B / C
- [ ] Nota de crédito
- [ ] Integración directa con AFIP (CAE automático)
- [ ] Manejo de CUIT / DNI / consumidor final
- [ ] Monotributo + Responsable Inscripto
- [ ] Puntos de venta
- [ ] IVA automático
- [ ] Percepciones / retenciones
- [ ] Exportación para contador
- [ ] **Configuración fiscal guiada** (diferencial)

---

## Pagos

- [ ] Mercado Pago integrado
- [ ] QR interoperable
- [ ] Transferencias
- [ ] Billeteras (Cuenta DNI, MODO)
- [ ] Cierre de caja por usuario
- [ ] Turnos de caja

---

## Inventario Avanzado

- [ ] Variantes de producto (sabor, tamaño)
- [ ] Control por vencimiento (CLAVE en kioscos)
- [ ] Mermas (roturas, vencidos, robos)
- [ ] Órdenes de compra
- [ ] Proveedores con historial de precios
- [ ] Comparación de proveedores
- [ ] **Sugerencia automática de compra** según ventas pasadas

---

## Clientes y Fidelización

- [ ] Clientes frecuentes
- [ ] Cuenta corriente (fiado)
- [ ] Historial de compras por cliente
- [ ] Puntos / descuentos
- [ ] Promos personalizadas

---

## Reportes y Estadísticas

- [ ] Ventas diarias / semanales / mensuales
- [ ] Productos más vendidos
- [ ] Horarios pico
- [ ] Medios de pago
- [ ] Rentabilidad por producto
- [ ] Productos que no rotan
- [ ] Productos que se vencen
- [ ] Comparación mes a mes
- [ ] Tendencias

---

## Multi-Kiosco / Cadenas

- [ ] Varias sucursales
- [ ] Stock independiente o centralizado
- [ ] Reportes globales
- [ ] Usuarios por sucursal

---

## Hardware

- [ ] Lector de código de barras (ya soportado básico)
- [ ] Impresoras térmicas (tickets)
- [ ] Cajón de dinero
- [ ] Soporte tablets

---

## Diferenciales Premium

- [ ] **IA de recomendaciones** ("Subí $X si aumentás este precio")
- [ ] **Control de robos indirecto** (detección de inconsistencias)
- [ ] **WhatsApp integrado** (ticket, recordatorio deuda, avisos)
- [ ] **Modo ultra simple** (3 botones para kioscos muy chicos)
- [ ] **Soporte guiado** (videos cortos, tooltips, "¿Querés que lo haga por vos?")

---

## Admin Panel (Tu Negocio)

- [ ] Alta de clientes (kioscos)
- [ ] Planes y suscripciones
- [ ] Métricas de uso
- [ ] Soporte
- [ ] Feature flags

---

## Notas

Prioridad sugerida post-MVP:
1. Multi-tenancy (necesario para producción)
2. Offline First (diferencial enorme)
3. Facturación AFIP (obligatorio en Argentina)
4. Clientes/fiado (muy pedido)
5. Reportes (valor agregado)

---

**Creado:** 2026-02-01
