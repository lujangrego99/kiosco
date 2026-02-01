package ar.com.kiosco.util;

/**
 * Validador de CUIT/CUIL argentino.
 * Valida el dígito verificador según el algoritmo oficial de AFIP.
 */
public class CuitValidator {

    private static final int[] MULTIPLICADORES = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private CuitValidator() {
        // Utility class
    }

    /**
     * Valida que un CUIT sea válido (formato y dígito verificador).
     *
     * @param cuit El CUIT a validar (puede tener guiones o no)
     * @return true si el CUIT es válido
     */
    public static boolean isValid(String cuit) {
        if (cuit == null || cuit.isBlank()) {
            return false;
        }

        // Normalizar: remover guiones y espacios
        String normalizado = cuit.replace("-", "").replace(" ", "").trim();

        // Debe tener exactamente 11 dígitos
        if (normalizado.length() != 11) {
            return false;
        }

        // Debe ser numérico
        if (!normalizado.matches("\\d{11}")) {
            return false;
        }

        // Validar tipo de contribuyente (primeros 2 dígitos)
        int tipo = Integer.parseInt(normalizado.substring(0, 2));
        if (!esTipoValido(tipo)) {
            return false;
        }

        // Calcular dígito verificador
        int digitoCalculado = calcularDigitoVerificador(normalizado);
        int digitoIngresado = Character.getNumericValue(normalizado.charAt(10));

        return digitoCalculado == digitoIngresado;
    }

    /**
     * Calcula el dígito verificador de un CUIT.
     *
     * @param cuit Los primeros 10 dígitos del CUIT
     * @return El dígito verificador calculado
     */
    public static int calcularDigitoVerificador(String cuit) {
        String normalizado = cuit.replace("-", "").replace(" ", "").trim();

        int suma = 0;
        for (int i = 0; i < 10; i++) {
            int digito = Character.getNumericValue(normalizado.charAt(i));
            suma += digito * MULTIPLICADORES[i];
        }

        int resto = suma % 11;
        int verificador = 11 - resto;

        // Casos especiales
        if (verificador == 11) {
            return 0;
        }
        if (verificador == 10) {
            return 9; // En algunos casos especiales
        }

        return verificador;
    }

    /**
     * Verifica si el tipo de contribuyente es válido.
     * Tipos válidos: 20, 23, 24, 27, 30, 33, 34
     */
    private static boolean esTipoValido(int tipo) {
        return tipo == 20 || tipo == 23 || tipo == 24 || tipo == 27 ||
               tipo == 30 || tipo == 33 || tipo == 34;
    }

    /**
     * Formatea un CUIT con guiones (XX-XXXXXXXX-X).
     *
     * @param cuit El CUIT sin guiones
     * @return El CUIT formateado
     */
    public static String formatear(String cuit) {
        if (cuit == null) {
            return null;
        }
        String normalizado = cuit.replace("-", "").replace(" ", "").trim();
        if (normalizado.length() != 11) {
            return cuit;
        }
        return normalizado.substring(0, 2) + "-" +
               normalizado.substring(2, 10) + "-" +
               normalizado.substring(10);
    }
}
