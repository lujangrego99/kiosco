# 027 - Tests de Aislamiento Multi-Tenant

> Suite de tests que verifican que un tenant no puede ver datos de otro.

## Priority: 8

## Status: PENDING

---

## Context

El multi-tenancy depende del correcto funcionamiento del `search_path`. Necesitamos tests automatizados que verifiquen el aislamiento.

## Requirements

### 1. Test de Aislamiento Basico

```java
@SpringBootTest
@Transactional
public class TenantIsolationTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TenantSchemaManager tenantSchemaManager;

    @Test
    void tenantA_cannot_see_tenantB_products() {
        // Setup: crear dos tenants
        UUID kioscoA = createTestKiosco("Kiosco A");
        UUID kioscoB = createTestKiosco("Kiosco B");

        // Crear producto en tenant A
        KioscoContext.setContext(kioscoA, "owner", userA, "a@test.com");
        Producto prodA = productoRepository.save(
            Producto.builder().nombre("Producto A").build()
        );

        // Crear producto en tenant B
        KioscoContext.setContext(kioscoB, "owner", userB, "b@test.com");
        Producto prodB = productoRepository.save(
            Producto.builder().nombre("Producto B").build()
        );

        // Verificar: desde A solo veo producto A
        KioscoContext.setContext(kioscoA, "owner", userA, "a@test.com");
        List<Producto> fromA = productoRepository.findAll();
        assertThat(fromA).hasSize(1);
        assertThat(fromA.get(0).getNombre()).isEqualTo("Producto A");

        // Verificar: desde B solo veo producto B
        KioscoContext.setContext(kioscoB, "owner", userB, "b@test.com");
        List<Producto> fromB = productoRepository.findAll();
        assertThat(fromB).hasSize(1);
        assertThat(fromB.get(0).getNombre()).isEqualTo("Producto B");

        // Verificar: no puedo acceder por ID cruzado
        KioscoContext.setContext(kioscoA, "owner", userA, "a@test.com");
        assertThat(productoRepository.findById(prodB.getId())).isEmpty();
    }
}
```

### 2. Test de Aislamiento para Cada Entidad

Repetir el patron para:
- Categorias
- Clientes
- Ventas
- Lotes
- Proveedores
- Config (fiscal, impresora, pagos)

### 3. Test de Context Limpio

```java
@Test
void requests_without_context_cannot_access_tenant_data() {
    // Crear producto en tenant A
    KioscoContext.setContext(kioscoA, ...);
    Producto prod = productoRepository.save(...);
    KioscoContext.clear();

    // Sin context, no deberia poder acceder
    // (deberia usar schema public o fallar)
    assertThrows(TenantNotSetException.class, () -> {
        productoRepository.findAll();
    });
}
```

### 4. Test de Injection SQL

```java
@Test
void schema_name_is_sanitized() {
    // Intentar inyectar SQL via schema name
    String maliciousId = "abc'; DROP TABLE productos; --";

    assertThrows(InvalidTenantException.class, () -> {
        tenantSchemaManager.getSchemaName(maliciousId);
    });
}
```

### 5. Test E2E via API

```java
@Test
void api_isolation_test() {
    // Login como usuario A
    String tokenA = login("userA@test.com", "pass");

    // Crear producto
    String prodId = createProducto(tokenA, "Producto A");

    // Login como usuario B (otro kiosco)
    String tokenB = login("userB@test.com", "pass");

    // Intentar acceder al producto de A
    RestAssured.given()
        .header("Authorization", "Bearer " + tokenB)
        .get("/api/productos/" + prodId)
        .then()
        .statusCode(404); // No encontrado (no 403)
}
```

### 6. CI Integration

```yaml
# .github/workflows/test.yml
jobs:
  isolation-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: kiosco_test
    steps:
      - uses: actions/checkout@v3
      - name: Run isolation tests
        run: ./gradlew test --tests "*IsolationTest*"
```

---

## Acceptance Criteria

- [ ] Test verifica que tenant A no ve productos de tenant B
- [ ] Test para cada entidad principal (categoria, cliente, venta, etc)
- [ ] Test verifica que sin context no se accede a datos
- [ ] Test de sanitizacion de schema name
- [ ] Test E2E via API con dos usuarios diferentes
- [ ] Tests integrados en CI/CD
- [ ] Todos los tests pasan consistentemente

---

## Notes

- Usar @Transactional con rollback para limpiar datos
- Crear tenants de prueba con prefijo `test_` para identificar
- Considerar property-based testing para casos edge
- Documentar los tests como especificacion de seguridad
