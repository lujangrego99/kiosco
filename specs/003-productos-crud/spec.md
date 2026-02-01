# 003 - Productos CRUD

> API REST y UI para gestionar productos y categorias.

## Priority: 3

## Status: INCOMPLETE

---

## Requirements

### Backend API

#### Categorias
```
GET    /api/categorias              → Lista todas las categorias activas
POST   /api/categorias              → Crear categoria
PUT    /api/categorias/{id}         → Actualizar categoria
DELETE /api/categorias/{id}         → Desactivar categoria (soft delete)
```

#### Productos
```
GET    /api/productos               → Lista productos (con filtros)
GET    /api/productos/{id}          → Detalle de producto
GET    /api/productos/buscar?q=xxx  → Buscar por nombre/codigo
GET    /api/productos/barcode/{code}→ Buscar por codigo de barras
POST   /api/productos               → Crear producto
PUT    /api/productos/{id}          → Actualizar producto
DELETE /api/productos/{id}          → Desactivar producto (soft delete)
GET    /api/productos/favoritos     → Lista favoritos
GET    /api/productos/stock-bajo    → Productos con stock bajo
```

### DTOs

```java
// Request
ProductoCreateDTO {
    codigo, codigoBarras, nombre, descripcion,
    categoriaId, precioCosto, precioVenta,
    stockActual, stockMinimo, esFavorito
}

// Response
ProductoDTO {
    id, codigo, codigoBarras, nombre, descripcion,
    categoria: { id, nombre, color },
    precioCosto, precioVenta, margen,
    stockActual, stockMinimo, stockBajo,
    esFavorito, activo
}
```

### Frontend UI

#### Pagina `/productos`
- Tabla de productos con:
  - Busqueda por nombre/codigo
  - Filtro por categoria
  - Ordenamiento por nombre/precio/stock
- Boton "Nuevo Producto"
- Acciones: Editar, Eliminar, Marcar favorito

#### Pagina `/productos/nuevo` y `/productos/[id]/editar`
- Formulario con todos los campos
- Selector de categoria
- Preview del margen de ganancia
- Validacion en tiempo real

#### Pagina `/categorias`
- Lista de categorias
- CRUD simple
- Color picker para cada categoria

### Componentes shadcn/ui a usar
- Table, Input, Button, Select
- Dialog (para confirmaciones)
- Form (react-hook-form)
- Toast (notificaciones)

---

## Acceptance Criteria

- [ ] Todos los endpoints responden correctamente
- [ ] Se puede crear/editar/eliminar productos desde la UI
- [ ] Se puede crear/editar/eliminar categorias desde la UI
- [ ] Busqueda de productos funciona
- [ ] Filtro por categoria funciona
- [ ] Marcar favorito funciona
- [ ] Lista de stock bajo funciona
- [ ] `./gradlew test` pasa
- [ ] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- Usar react-hook-form + zod para validacion
- Debounce en busqueda (300ms)
- Paginacion no es necesaria todavia (MVP)
