# 002 - Database Schema (MVP)

> Crear el modelo de datos basico para productos, categorias y stock.

## Priority: 2

## Status: INCOMPLETE

---

## Requirements

### Entidades

#### Categoria
```sql
CREATE TABLE categorias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color VARCHAR(7),  -- hex color para UI
    orden INT DEFAULT 0,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

#### Producto
```sql
CREATE TABLE productos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50),           -- codigo interno
    codigo_barras VARCHAR(50),    -- EAN/UPC
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    categoria_id UUID REFERENCES categorias(id),

    -- Precios
    precio_costo DECIMAL(12,2) DEFAULT 0,
    precio_venta DECIMAL(12,2) NOT NULL,

    -- Stock
    stock_actual DECIMAL(10,2) DEFAULT 0,
    stock_minimo DECIMAL(10,2) DEFAULT 0,

    -- Config
    es_favorito BOOLEAN DEFAULT false,
    activo BOOLEAN DEFAULT true,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_productos_codigo ON productos(codigo);
CREATE INDEX idx_productos_codigo_barras ON productos(codigo_barras);
CREATE INDEX idx_productos_nombre ON productos(nombre);
CREATE INDEX idx_productos_categoria ON productos(categoria_id);
```

### JPA Entities

Crear entidades JPA en `ar.com.kiosco.domain`:
- `Categoria.java`
- `Producto.java`

Con:
- Anotaciones JPA (@Entity, @Table, @Id, etc)
- Lombok (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
- Timestamps automaticos (@CreatedDate, @LastModifiedDate)

### Repositories

Crear en `ar.com.kiosco.repository`:
- `CategoriaRepository.java`
- `ProductoRepository.java`

Con metodos:
```java
// CategoriaRepository
List<Categoria> findByActivoTrueOrderByOrdenAsc();

// ProductoRepository
List<Producto> findByActivoTrue();
Optional<Producto> findByCodigo(String codigo);
Optional<Producto> findByCodigoBarras(String codigoBarras);
List<Producto> findByNombreContainingIgnoreCase(String nombre);
List<Producto> findByCategoriaId(UUID categoriaId);
List<Producto> findByEsFavoritoTrue();
List<Producto> findByStockActualLessThanStockMinimo();  // alertas
```

### Flyway Migration

Crear `apps/api/src/main/resources/db/migration/V1__initial_schema.sql`

---

## Acceptance Criteria

- [ ] Migration V1 ejecuta sin errores
- [ ] Entidades Categoria y Producto creadas con JPA
- [ ] Repositories funcionan (verificar con test)
- [ ] `./gradlew test` pasa
- [ ] La app inicia sin errores de schema

---

## Notes

- NO crear multi-tenancy todavia (schema unico por ahora)
- Usar UUID para IDs (mejor para sync offline futuro)
- Decimal para stock (permite medidas fraccionadas: 0.5 kg)
