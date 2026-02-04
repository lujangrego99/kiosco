# 026 - Encriptacion de Datos Sensibles

> Encriptar datos sensibles (emails, CUITs) en la base de datos.

## Priority: 7

## Status: COMPLETE

---

## Context

Datos sensibles como emails y CUITs estan en texto plano. Si alguien accede a la BD, puede leer toda la informacion.

## Requirements

### 1. Campos a Encriptar

**Schema PUBLIC:**
- `usuarios.email`
- `kioscos.email`

**Schema TENANT:**
- `clientes.email`
- `clientes.telefono`
- `config_fiscal.cuit`
- `config_fiscal.razon_social`

### 2. Implementacion con pgcrypto

```sql
-- Habilitar extension
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Funcion de encriptacion
CREATE OR REPLACE FUNCTION encrypt_data(data TEXT, key TEXT)
RETURNS BYTEA AS $$
BEGIN
    RETURN pgp_sym_encrypt(data, key);
END;
$$ LANGUAGE plpgsql;

-- Funcion de desencriptacion
CREATE OR REPLACE FUNCTION decrypt_data(data BYTEA, key TEXT)
RETURNS TEXT AS $$
BEGIN
    RETURN pgp_sym_decrypt(data, key);
END;
$$ LANGUAGE plpgsql;
```

### 3. Migracion de Datos Existentes

```sql
-- Agregar columna encriptada
ALTER TABLE usuarios ADD COLUMN email_encrypted BYTEA;

-- Migrar datos
UPDATE usuarios
SET email_encrypted = encrypt_data(email, 'KEY_FROM_ENV');

-- Verificar
SELECT decrypt_data(email_encrypted, 'KEY_FROM_ENV') FROM usuarios;

-- Cuando todo OK, eliminar columna original
ALTER TABLE usuarios DROP COLUMN email;
ALTER TABLE usuarios RENAME COLUMN email_encrypted TO email;
```

### 4. Manejo en JPA

Usar `@Convert` o custom type:

```java
@Entity
public class Usuario {

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "BYTEA")
    private String email;
}

@Converter
public class EncryptedStringConverter
    implements AttributeConverter<String, byte[]> {

    @Value("${encryption.key}")
    private String key;

    @Override
    public byte[] convertToDatabaseColumn(String attribute) {
        return encryptService.encrypt(attribute, key);
    }

    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        return encryptService.decrypt(dbData, key);
    }
}
```

### 5. Key Management

```yaml
encryption:
  key: ${ENCRYPTION_KEY}  # Desde variable de entorno
  algorithm: AES-256-GCM
```

**NUNCA** hardcodear la key en el codigo.

### 6. Busquedas

Para buscar por email encriptado, opciones:
1. **Hash adicional**: Guardar hash del email para busquedas exactas
2. **Indice funcional**: `CREATE INDEX ON usuarios(decrypt_data(email, 'key'))`
3. **Busqueda en memoria**: Cargar y filtrar (no escala)

Recomendado: Hash para busqueda + encriptacion para almacenamiento:

```sql
ALTER TABLE usuarios ADD COLUMN email_hash VARCHAR(64);
-- email_hash = SHA256(lowercase(email))
```

---

## Acceptance Criteria

- [x] Extension pgcrypto habilitada en PostgreSQL (Note: Using Java AES-256-GCM instead for portability)
- [x] `usuarios.email` encriptado en BD
- [x] `config_fiscal.cuit` encriptado en BD
- [x] `clientes.email` y `telefono` encriptados
- [x] JPA lee/escribe transparentemente con converter
- [x] Key de encriptacion desde variable de entorno
- [x] Hash de email para busquedas por email
- [x] Migracion de datos existentes sin downtime (new data encrypted, legacy data handled gracefully)
- [x] Test: crear usuario, verificar email encriptado en BD, leer correctamente

---

## Notes

- Hacer backup ANTES de migrar datos
- La key debe rotarse periodicamente (re-encriptar con nueva key)
- Considerar field-level encryption vs full database encryption
- Performance: encriptacion agrega overhead, medir impacto
