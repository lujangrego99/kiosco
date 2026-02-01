package ar.com.kiosco.repository;

import ar.com.kiosco.domain.Categoria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    void shouldSaveAndRetrieveCategoria() {
        Categoria categoria = Categoria.builder()
                .nombre("Bebidas")
                .descripcion("Bebidas frias y calientes")
                .color("#FF0000")
                .orden(1)
                .build();

        Categoria saved = categoriaRepository.save(categoria);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNombre()).isEqualTo("Bebidas");
        assertThat(saved.getActivo()).isTrue();
    }

    @Test
    void shouldFindActiveCategoriasOrderedByOrden() {
        Categoria bebidas = Categoria.builder()
                .nombre("Bebidas")
                .orden(2)
                .build();
        Categoria golosinas = Categoria.builder()
                .nombre("Golosinas")
                .orden(1)
                .build();
        Categoria inactiva = Categoria.builder()
                .nombre("Inactiva")
                .orden(0)
                .activo(false)
                .build();

        categoriaRepository.saveAll(List.of(bebidas, golosinas, inactiva));

        List<Categoria> activas = categoriaRepository.findByActivoTrueOrderByOrdenAsc();

        assertThat(activas).hasSize(2);
        assertThat(activas.get(0).getNombre()).isEqualTo("Golosinas");
        assertThat(activas.get(1).getNombre()).isEqualTo("Bebidas");
    }
}
