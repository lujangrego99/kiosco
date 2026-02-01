# 007 - Redis Cache y Sesiones

> Implementar Redis para cache de datos frecuentes y manejo de sesiones.

## Priority: 3

## Status: COMPLETE

---

## Requirements

### Dependencias

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.session:spring-session-data-redis'
```

### Configuración

```yaml
# application.yml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hora
  session:
    store-type: redis
    redis:
      namespace: kiosco:sessions
```

### Cache de Productos

```java
@Service
@CacheConfig(cacheNames = "productos")
public class ProductoService {

    @Cacheable(key = "#kioscoId + ':all'")
    public List<ProductoDTO> findAll();

    @Cacheable(key = "#kioscoId + ':' + #id")
    public ProductoDTO findById(UUID id);

    @CacheEvict(key = "#kioscoId + ':all'")
    @CachePut(key = "#kioscoId + ':' + #result.id")
    public ProductoDTO save(ProductoCreateDTO dto);

    @CacheEvict(allEntries = true)
    public void evictAllCache();
}
```

### Cache de Categorías

```java
@Service
@CacheConfig(cacheNames = "categorias")
public class CategoriaService {

    @Cacheable(key = "#kioscoId + ':all'")
    public List<CategoriaDTO> findAll();
}
```

### Configuración de Cache

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
            .cacheDefaults(cacheConfiguration())
            .withCacheConfiguration("productos",
                cacheConfiguration().entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("categorias",
                cacheConfiguration().entryTtl(Duration.ofHours(2)))
            .build();
    }
}
```

### Rate Limiting (opcional)

```java
@Component
public class RateLimiter {
    private final StringRedisTemplate redis;

    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = "ratelimit:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count == 1) {
            redis.expire(redisKey, window);
        }
        return count <= maxRequests;
    }
}
```

---

## Acceptance Criteria

- [x] Redis conecta correctamente
- [x] Productos se cachean en Redis
- [x] Categorías se cachean en Redis
- [x] Cache se invalida al crear/actualizar productos
- [x] Cache incluye tenant ID en las keys
- [x] Sesiones se almacenan en Redis
- [x] TTL configurado correctamente (productos 30min, categorías 2h)
- [x] `./gradlew test` pasa
- [x] Verificar en Redis CLI que las keys existen

---

## Notes

- Keys con formato: `kiosco:{tenantId}:{entity}:{id}`
- Invalidar cache al modificar datos
- Redis ya está en docker-compose
