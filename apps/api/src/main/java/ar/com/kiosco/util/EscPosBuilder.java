package ar.com.kiosco.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Builder for ESC/POS thermal printer commands.
 * Supports standard thermal printer operations for receipt printing.
 */
public class EscPosBuilder {

    private final ByteArrayOutputStream buffer;
    private final int paperWidth;
    private final Charset charset;

    // ESC/POS Commands
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte LF = 0x0A;

    // Alignment
    private static final byte[] ALIGN_LEFT = {ESC, 'a', 0};
    private static final byte[] ALIGN_CENTER = {ESC, 'a', 1};
    private static final byte[] ALIGN_RIGHT = {ESC, 'a', 2};

    // Text format
    private static final byte[] BOLD_ON = {ESC, 'E', 1};
    private static final byte[] BOLD_OFF = {ESC, 'E', 0};
    private static final byte[] UNDERLINE_ON = {ESC, '-', 1};
    private static final byte[] UNDERLINE_OFF = {ESC, '-', 0};

    // Text size (width x height multiplier)
    private static final byte[] SIZE_NORMAL = {GS, '!', 0x00};
    private static final byte[] SIZE_DOUBLE_HEIGHT = {GS, '!', 0x01};
    private static final byte[] SIZE_DOUBLE_WIDTH = {GS, '!', 0x10};
    private static final byte[] SIZE_DOUBLE = {GS, '!', 0x11};

    // Paper cut
    private static final byte[] CUT_PARTIAL = {GS, 'V', 1};
    private static final byte[] CUT_FULL = {GS, 'V', 0};

    // Initialize printer
    private static final byte[] INIT = {ESC, '@'};

    public EscPosBuilder() {
        this(80);
    }

    public EscPosBuilder(int paperWidthMm) {
        this.buffer = new ByteArrayOutputStream();
        this.paperWidth = paperWidthMm == 58 ? 32 : 48; // Characters per line
        this.charset = Charset.forName("CP437");
        write(INIT);
    }

    public EscPosBuilder izquierda() {
        write(ALIGN_LEFT);
        return this;
    }

    public EscPosBuilder centrar() {
        write(ALIGN_CENTER);
        return this;
    }

    public EscPosBuilder derecha() {
        write(ALIGN_RIGHT);
        return this;
    }

    public EscPosBuilder negrita(boolean on) {
        write(on ? BOLD_ON : BOLD_OFF);
        return this;
    }

    public EscPosBuilder subrayado(boolean on) {
        write(on ? UNDERLINE_ON : UNDERLINE_OFF);
        return this;
    }

    public EscPosBuilder tamano(int size) {
        switch (size) {
            case 1 -> write(SIZE_NORMAL);
            case 2 -> write(SIZE_DOUBLE);
            case 3 -> write(SIZE_DOUBLE_HEIGHT);
            case 4 -> write(SIZE_DOUBLE_WIDTH);
            default -> write(SIZE_NORMAL);
        }
        return this;
    }

    public EscPosBuilder linea(String texto) {
        if (texto == null) texto = "";
        write(texto.getBytes(charset));
        write(new byte[]{LF});
        return this;
    }

    public EscPosBuilder lineaVacia() {
        write(new byte[]{LF});
        return this;
    }

    public EscPosBuilder separador() {
        return separador('-');
    }

    public EscPosBuilder separador(char c) {
        String line = String.valueOf(c).repeat(paperWidth);
        return linea(line);
    }

    public EscPosBuilder separadorDoble() {
        return separador('=');
    }

    public EscPosBuilder columnas(String izquierda, String derecha) {
        int espacios = paperWidth - izquierda.length() - derecha.length();
        if (espacios < 1) espacios = 1;
        String linea = izquierda + " ".repeat(espacios) + derecha;
        return linea(linea);
    }

    public EscPosBuilder columnas3(String col1, String col2, String col3) {
        int col1Width = 4;  // Cantidad
        int col3Width = 12; // Precio
        int col2Width = paperWidth - col1Width - col3Width;

        String formattedCol1 = padRight(col1, col1Width);
        String formattedCol2 = truncateOrPad(col2, col2Width);
        String formattedCol3 = padLeft(col3, col3Width);

        return linea(formattedCol1 + formattedCol2 + formattedCol3);
    }

    public EscPosBuilder codigo(String codigo) {
        // CODE39 barcode
        byte[] barcodeCmd = {
                GS, 'h', 50,    // Height
                GS, 'w', 2,     // Width
                GS, 'H', 2,     // Print text below barcode
                GS, 'k', 4      // CODE39 format
        };
        write(barcodeCmd);
        write(codigo.getBytes(charset));
        write(new byte[]{0x00, LF});
        return this;
    }

    public EscPosBuilder qr(String data) {
        // QR Code commands
        int len = data.length() + 3;

        // Set QR code model
        write(new byte[]{GS, '(', 'k', 4, 0, 49, 65, 50, 0});

        // Set QR code size (1-16)
        write(new byte[]{GS, '(', 'k', 3, 0, 49, 67, 6});

        // Set error correction level (L=48, M=49, Q=50, H=51)
        write(new byte[]{GS, '(', 'k', 3, 0, 49, 69, 49});

        // Store QR code data
        write(new byte[]{GS, '(', 'k', (byte) (len % 256), (byte) (len / 256), 49, 80, 48});
        write(data.getBytes(charset));

        // Print QR code
        write(new byte[]{GS, '(', 'k', 3, 0, 49, 81, 48});
        write(new byte[]{LF});

        return this;
    }

    public EscPosBuilder cortar() {
        // Feed paper before cut
        write(new byte[]{LF, LF, LF, LF});
        write(CUT_PARTIAL);
        return this;
    }

    public EscPosBuilder cortarCompleto() {
        write(new byte[]{LF, LF, LF, LF});
        write(CUT_FULL);
        return this;
    }

    public EscPosBuilder abrirCajon() {
        // Open cash drawer (pulse pin 2)
        write(new byte[]{ESC, 'p', 0, 25, (byte) 250});
        return this;
    }

    public byte[] build() {
        return buffer.toByteArray();
    }

    public String buildAsText() {
        StringBuilder sb = new StringBuilder();
        // Return plain text representation for preview
        return new String(buffer.toByteArray(), charset)
                .replaceAll("[\\x00-\\x1F]", ""); // Remove control chars for text preview
    }

    private void write(byte[] data) {
        try {
            buffer.write(data);
        } catch (IOException e) {
            throw new RuntimeException("Error writing to buffer", e);
        }
    }

    private String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    private String padLeft(String s, int width) {
        if (s.length() >= width) return s.substring(0, width);
        return " ".repeat(width - s.length()) + s;
    }

    private String truncateOrPad(String s, int width) {
        if (s.length() > width) return s.substring(0, width - 1) + ".";
        return padRight(s, width);
    }
}
