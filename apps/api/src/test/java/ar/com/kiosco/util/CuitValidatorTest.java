package ar.com.kiosco.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CuitValidatorTest {

    @Test
    @DisplayName("CUIT válido sin guiones debe retornar true")
    void validCuitWithoutDashes() {
        assertTrue(CuitValidator.isValid("20123456786"));
    }

    @Test
    @DisplayName("CUIT válido con guiones debe retornar true")
    void validCuitWithDashes() {
        assertTrue(CuitValidator.isValid("20-12345678-6"));
    }

    @Test
    @DisplayName("Calcular dígito verificador y validar")
    void calculateAndValidate() {
        // Calcular el dígito verificador correcto para 20-20000000-X
        int digito = CuitValidator.calcularDigitoVerificador("2020000000");
        String cuit = "20-20000000-" + digito;
        assertTrue(CuitValidator.isValid(cuit), "CUIT con dígito calculado debería ser válido: " + cuit);
    }

    @Test
    @DisplayName("CUIT con dígito verificador incorrecto debe retornar false")
    void invalidCuitWrongCheckDigit() {
        assertFalse(CuitValidator.isValid("20-12345678-0"));
        assertFalse(CuitValidator.isValid("20-12345678-9"));
    }

    @Test
    @DisplayName("CUIT nulo debe retornar false")
    void nullCuit() {
        assertFalse(CuitValidator.isValid(null));
    }

    @Test
    @DisplayName("CUIT vacío debe retornar false")
    void emptyCuit() {
        assertFalse(CuitValidator.isValid(""));
        assertFalse(CuitValidator.isValid("   "));
    }

    @Test
    @DisplayName("CUIT con longitud incorrecta debe retornar false")
    void wrongLengthCuit() {
        assertFalse(CuitValidator.isValid("201234567"));    // Muy corto
        assertFalse(CuitValidator.isValid("201234567890")); // Muy largo
    }

    @Test
    @DisplayName("CUIT con caracteres no numéricos debe retornar false")
    void nonNumericCuit() {
        assertFalse(CuitValidator.isValid("2012345678A"));
        assertFalse(CuitValidator.isValid("20-1234567X-6"));
    }

    @Test
    @DisplayName("CUIT con tipo de contribuyente inválido debe retornar false")
    void invalidContributorType() {
        // Tipo 11 no existe
        assertFalse(CuitValidator.isValid("11-12345678-6"));
        // Tipo 99 no existe
        assertFalse(CuitValidator.isValid("99-12345678-6"));
    }

    @Test
    @DisplayName("Formatear CUIT válido")
    void formatValidCuit() {
        assertEquals("20-12345678-6", CuitValidator.formatear("20123456786"));
        assertEquals("20-12345678-6", CuitValidator.formatear("20-12345678-6"));
    }

    @Test
    @DisplayName("Formatear CUIT inválido retorna el input")
    void formatInvalidCuit() {
        assertEquals("123", CuitValidator.formatear("123"));
        assertNull(CuitValidator.formatear(null));
    }
}
