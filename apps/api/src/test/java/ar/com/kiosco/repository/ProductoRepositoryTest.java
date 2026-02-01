package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Categoria;
import ar.com.kiosco.domain.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = categoriaRepository.save(
                Categoria.builder()
                        .nombre("Bebidas")
                        .build()
        );
    }

    @Test
    void shouldSaveAndRetrieveProducto() {
        Producto producto = Producto.builder()
                .nombre("Coca Cola 500ml")
                .codigo("CC500")
                .codigoBarras("7790895000041")
                .precioVenta(new BigDecimal("1500.00"))
                .precioCosto(new BigDecimal("1000.00"))
                .categoria(categoria)
                .build();

        Producto saved = productoRepository.save(producto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNombre()).isEqualTo("Coca Cola 500ml");
        assertThat(saved.getActivo()).isTrue();
    }

    @Test
    void shouldFindByCodigo() {
        Producto producto = productoRepository.save(
                Producto.builder()
                        .nombre("Sprite 500ml")
                        .codigo("SP500")
                        .precioVenta(new BigDecimal("1500.00"))
                        .build()
        );

        Optional<Producto> found = productoRepository.findByCodigo("SP500");

        assertThat(found).isPresent();
        assertThat(found.get().getNombre()).isEqualTo("Sprite 500ml");
    }

    @Test
    void shouldFindByCodigoBarras() {
        Producto producto = productoRepository.save(
                Producto.builder()
                        .nombre("Fanta 500ml")
                        .codigoBarras("7790895000058")
                        .precioVenta(new BigDecimal("1500.00"))
                        .build()
        );

        Optional<Producto> found = productoRepository.findByCodigoBarras("7790895000058");

        assertThat(found).isPresent();
        assertThat(found.get().getNombre()).isEqualTo("Fanta 500ml");
    }

    @Test
    void shouldFindByNombreContaining() {
        productoRepository.saveAll(List.of(
                Producto.builder().nombre("Coca Cola 500ml").precioVenta(new BigDecimal("1500")).build(),
                Producto.builder().nombre("Coca Cola 1L").precioVenta(new BigDecimal("2000")).build(),
                Producto.builder().nombre("Sprite 500ml").precioVenta(new BigDecimal("1500")).build()
        ));

        List<Producto> found = productoRepository.findByNombreContainingIgnoreCase("coca");

        assertThat(found).hasSize(2);
    }

    @Test
    void shouldFindByCategoriaId() {
        Categoria otraCategoria = categoriaRepository.save(
                Categoria.builder().nombre("Golosinas").build()
        );

        productoRepository.saveAll(List.of(
                Producto.builder().nombre("Coca Cola").precioVenta(new BigDecimal("1500")).categoria(categoria).build(),
                Producto.builder().nombre("Alfajor").precioVenta(new BigDecimal("500")).categoria(otraCategoria).build()
        ));

        List<Producto> bebidas = productoRepository.findByCategoriaId(categoria.getId());

        assertThat(bebidas).hasSize(1);
        assertThat(bebidas.get(0).getNombre()).isEqualTo("Coca Cola");
    }

    @Test
    void shouldFindFavoritos() {
        productoRepository.saveAll(List.of(
                Producto.builder().nombre("Coca Cola").precioVenta(new BigDecimal("1500")).esFavorito(true).build(),
                Producto.builder().nombre("Sprite").precioVenta(new BigDecimal("1500")).esFavorito(false).build(),
                Producto.builder().nombre("Fanta").precioVenta(new BigDecimal("1500")).esFavorito(true).build()
        ));

        List<Producto> favoritos = productoRepository.findByEsFavoritoTrue();

        assertThat(favoritos).hasSize(2);
    }

    @Test
    void shouldFindProductosConStockBajo() {
        productoRepository.saveAll(List.of(
                Producto.builder()
                        .nombre("Producto OK")
                        .precioVenta(new BigDecimal("1000"))
                        .stockActual(new BigDecimal("10"))
                        .stockMinimo(new BigDecimal("5"))
                        .build(),
                Producto.builder()
                        .nombre("Producto Bajo Stock")
                        .precioVenta(new BigDecimal("1000"))
                        .stockActual(new BigDecimal("2"))
                        .stockMinimo(new BigDecimal("5"))
                        .build(),
                Producto.builder()
                        .nombre("Producto Inactivo Bajo")
                        .precioVenta(new BigDecimal("1000"))
                        .stockActual(new BigDecimal("0"))
                        .stockMinimo(new BigDecimal("5"))
                        .activo(false)
                        .build()
        ));

        List<Producto> stockBajo = productoRepository.findByStockBajo();

        assertThat(stockBajo).hasSize(1);
        assertThat(stockBajo.get(0).getNombre()).isEqualTo("Producto Bajo Stock");
    }
}
