# 008 - Clientes CRUD

> Gestión básica de clientes del kiosco.

## Priority: 4

## Status: COMPLETE

---

## Requirements

### Modelo de Datos

```sql
-- En schema tenant
CREATE TABLE clientes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(200) NOT NULL,
    documento VARCHAR(20),           -- DNI o CUIT
    tipo_documento VARCHAR(10),      -- DNI, CUIT, OTRO
    telefono VARCHAR(50),
    email VARCHAR(200),
    direccion TEXT,
    notas TEXT,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_clientes_documento ON clientes(documento);
CREATE INDEX idx_clientes_nombre ON clientes(nombre);
```

### Entidad JPA

```java
@Entity
@Table(name = "clientes")
public class Cliente {
    @Id
    private UUID id;
    private String nombre;
    private String documento;
    private String tipoDocumento;
    private String telefono;
    private String email;
    private String direccion;
    private String notas;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Repository

```java
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    List<Cliente> findByActivoTrue();
    Optional<Cliente> findByDocumento(String documento);
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}
```

### API Endpoints

```
GET    /api/clientes              → Lista clientes activos
GET    /api/clientes/{id}         → Detalle cliente
GET    /api/clientes/buscar?q=xxx → Buscar por nombre o documento
POST   /api/clientes              → Crear cliente
PUT    /api/clientes/{id}         → Actualizar cliente
DELETE /api/clientes/{id}         → Desactivar cliente
```

### DTOs

```java
public record ClienteDTO(
    UUID id,
    String nombre,
    String documento,
    String tipoDocumento,
    String telefono,
    String email,
    String direccion,
    String notas,
    Boolean activo
) {}

public record ClienteCreateDTO(
    @NotBlank String nombre,
    String documento,
    String tipoDocumento,
    String telefono,
    String email,
    String direccion,
    String notas
) {}
```

### Frontend

#### Página `/clientes`
- Tabla de clientes
- Búsqueda por nombre/documento
- Botón "Nuevo Cliente"
- Acciones: Ver, Editar, Eliminar

#### Página `/clientes/nuevo` y `/clientes/[id]/editar`
- Formulario con campos:
  - Nombre (requerido)
  - Tipo documento (select: DNI, CUIT, Otro)
  - Documento
  - Teléfono
  - Email
  - Dirección
  - Notas

#### Componente selector de cliente
```tsx
<ClienteSelect
  value={clienteId}
  onChange={setClienteId}
  allowCreate={true}  // Permite crear nuevo desde el selector
/>
```

---

## Acceptance Criteria

- [x] Migration crea tabla clientes
- [x] CRUD completo en backend
- [x] Búsqueda por nombre y documento funciona
- [x] UI de lista de clientes
- [x] UI de formulario crear/editar
- [x] Componente ClienteSelect reutilizable
- [x] Soft delete (desactivar, no borrar)
- [x] `./gradlew test` pasa
- [x] `pnpm lint && pnpm typecheck` pasa

---

## Notes

- El cliente es opcional en ventas (consumidor final)
- Preparar para cuenta corriente (siguiente spec)
