package ar.com.kiosco.service;

import ar.com.kiosco.domain.ConfigImpresora;
import ar.com.kiosco.domain.Venta;
import ar.com.kiosco.dto.VentaDTO;
import ar.com.kiosco.dto.VentaItemDTO;
import ar.com.kiosco.repository.ConfigImpresoraRepository;
import ar.com.kiosco.repository.VentaRepository;
import ar.com.kiosco.util.EscPosBuilder;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService {

    private final ConfigImpresoraRepository configRepository;
    private final VentaRepository ventaRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    @Transactional(readOnly = true)
    public byte[] generarTicketVenta(UUID ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada: " + ventaId));

        ConfigImpresora config = configRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(ConfigImpresora.builder().build());

        VentaDTO ventaDTO = VentaDTO.fromEntity(venta);
        return generarTicketVenta(ventaDTO, config);
    }

    public byte[] generarTicketVenta(VentaDTO venta, ConfigImpresora config) {
        int anchoPapel = config.getAnchoPapel() != null ? config.getAnchoPapel() : 80;

        EscPosBuilder builder = new EscPosBuilder(anchoPapel);

        // Header
        builder.centrar()
                .tamano(2)
                .negrita(true);

        if (config.getNombreNegocio() != null && !config.getNombreNegocio().isBlank()) {
            builder.linea(config.getNombreNegocio());
        } else {
            builder.linea("KIOSCO");
        }

        builder.tamano(1)
                .negrita(false);

        if (config.getDireccionNegocio() != null && !config.getDireccionNegocio().isBlank()) {
            builder.linea(config.getDireccionNegocio());
        }

        if (config.getTelefonoNegocio() != null && !config.getTelefonoNegocio().isBlank()) {
            builder.linea("Tel: " + config.getTelefonoNegocio());
        }

        builder.separadorDoble()
                .izquierda();

        // Ticket info
        builder.columnas("Fecha:", venta.getFecha() != null ? venta.getFecha().format(DATE_FORMAT) : "-")
                .columnas("Ticket:", String.format("#%07d", venta.getNumero()));

        if (venta.getClienteNombre() != null) {
            builder.columnas("Cliente:", venta.getClienteNombre());
        }

        builder.separador();

        // Items header
        builder.columnas3("Cant", "Descripcion", "Precio")
                .separador();

        // Items
        if (venta.getItems() != null) {
            for (VentaItemDTO item : venta.getItems()) {
                String cantidad = item.getCantidad().stripTrailingZeros().toPlainString();
                String nombre = item.getProductoNombre() != null ? item.getProductoNombre() : "Producto";
                String precio = formatearPrecio(item.getSubtotal());

                builder.columnas3(cantidad, nombre, precio);
            }
        }

        builder.separador();

        // Totals
        if (venta.getDescuento() != null && venta.getDescuento().compareTo(BigDecimal.ZERO) > 0) {
            builder.columnas("Subtotal:", formatearPrecio(venta.getSubtotal()))
                    .columnas("Descuento:", "-" + formatearPrecio(venta.getDescuento()));
        }

        builder.negrita(true)
                .columnas("TOTAL:", formatearPrecio(venta.getTotal()))
                .negrita(false)
                .separador();

        // Payment info
        builder.columnas("Medio de pago:", formatMedioPago(venta.getMedioPago()));

        if ("EFECTIVO".equals(venta.getMedioPago()) && venta.getMontoRecibido() != null) {
            builder.columnas("Recibido:", formatearPrecio(venta.getMontoRecibido()));
            if (venta.getVuelto() != null && venta.getVuelto().compareTo(BigDecimal.ZERO) > 0) {
                builder.columnas("Vuelto:", formatearPrecio(venta.getVuelto()));
            }
        }

        builder.separador();

        // Footer
        builder.centrar();

        String mensajePie = config.getMensajePie() != null ? config.getMensajePie() : "Gracias por su compra!";
        builder.linea(mensajePie);

        builder.separadorDoble()
                .cortar();

        return builder.build();
    }

    public String generarTicketVentaTexto(UUID ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada: " + ventaId));

        ConfigImpresora config = configRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(ConfigImpresora.builder().build());

        VentaDTO ventaDTO = VentaDTO.fromEntity(venta);
        return generarTicketVentaTexto(ventaDTO, config);
    }

    public String generarTicketVentaTexto(VentaDTO venta, ConfigImpresora config) {
        int anchoLinea = config.getAnchoPapel() != null && config.getAnchoPapel() == 58 ? 32 : 48;
        StringBuilder sb = new StringBuilder();

        String separator = "=".repeat(anchoLinea);
        String dashSeparator = "-".repeat(anchoLinea);

        // Header
        sb.append(separator).append("\n");

        String nombreNegocio = config.getNombreNegocio() != null ? config.getNombreNegocio() : "KIOSCO";
        sb.append(center(nombreNegocio, anchoLinea)).append("\n");

        if (config.getDireccionNegocio() != null && !config.getDireccionNegocio().isBlank()) {
            sb.append(center(config.getDireccionNegocio(), anchoLinea)).append("\n");
        }

        if (config.getTelefonoNegocio() != null && !config.getTelefonoNegocio().isBlank()) {
            sb.append(center("Tel: " + config.getTelefonoNegocio(), anchoLinea)).append("\n");
        }

        sb.append(separator).append("\n");

        // Ticket info
        sb.append(columns("Fecha:", venta.getFecha() != null ? venta.getFecha().format(DATE_FORMAT) : "-", anchoLinea)).append("\n");
        sb.append(columns("Ticket:", String.format("#%07d", venta.getNumero()), anchoLinea)).append("\n");

        if (venta.getClienteNombre() != null) {
            sb.append(columns("Cliente:", venta.getClienteNombre(), anchoLinea)).append("\n");
        }

        sb.append(dashSeparator).append("\n");

        // Items header
        sb.append("Cant  Descripcion").append(" ".repeat(Math.max(1, anchoLinea - 24))).append("Precio\n");
        sb.append(dashSeparator).append("\n");

        // Items
        if (venta.getItems() != null) {
            for (VentaItemDTO item : venta.getItems()) {
                String cantidad = String.format("%4s", item.getCantidad().stripTrailingZeros().toPlainString());
                String nombre = item.getProductoNombre() != null ? item.getProductoNombre() : "Producto";
                String precio = formatearPrecio(item.getSubtotal());

                if (nombre.length() > anchoLinea - 18) {
                    nombre = nombre.substring(0, anchoLinea - 19) + ".";
                }

                int espacios = anchoLinea - cantidad.length() - nombre.length() - precio.length() - 2;
                if (espacios < 1) espacios = 1;

                sb.append(cantidad).append("  ").append(nombre)
                        .append(" ".repeat(espacios)).append(precio).append("\n");
            }
        }

        sb.append(dashSeparator).append("\n");

        // Totals
        if (venta.getDescuento() != null && venta.getDescuento().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(columns("Subtotal:", formatearPrecio(venta.getSubtotal()), anchoLinea)).append("\n");
            sb.append(columns("Descuento:", "-" + formatearPrecio(venta.getDescuento()), anchoLinea)).append("\n");
        }

        sb.append(columns("TOTAL:", formatearPrecio(venta.getTotal()), anchoLinea)).append("\n");
        sb.append(dashSeparator).append("\n");

        // Payment info
        sb.append(columns("Medio de pago:", formatMedioPago(venta.getMedioPago()), anchoLinea)).append("\n");

        if ("EFECTIVO".equals(venta.getMedioPago()) && venta.getMontoRecibido() != null) {
            sb.append(columns("Recibido:", formatearPrecio(venta.getMontoRecibido()), anchoLinea)).append("\n");
            if (venta.getVuelto() != null && venta.getVuelto().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(columns("Vuelto:", formatearPrecio(venta.getVuelto()), anchoLinea)).append("\n");
            }
        }

        sb.append(dashSeparator).append("\n");

        // Footer
        String mensajePie = config.getMensajePie() != null ? config.getMensajePie() : "Gracias por su compra!";
        sb.append(center(mensajePie, anchoLinea)).append("\n");
        sb.append(separator).append("\n");

        return sb.toString();
    }

    public byte[] generarTicketVentaPdf(UUID ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada: " + ventaId));

        ConfigImpresora config = configRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(ConfigImpresora.builder().build());

        VentaDTO ventaDTO = VentaDTO.fromEntity(venta);
        String ticketText = generarTicketVentaTexto(ventaDTO, config);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);

            // Use narrow page size to simulate ticket
            float width = 226f;  // ~80mm at 72dpi
            float height = 842f; // A4 height, will be cropped
            pdf.setDefaultPageSize(new com.itextpdf.kernel.geom.PageSize(width, height));

            Document document = new Document(pdf);
            document.setMargins(10, 10, 10, 10);

            // Add ticket content
            for (String line : ticketText.split("\n")) {
                Paragraph p = new Paragraph(line)
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.LEFT)
                        .setMarginBottom(0)
                        .setMarginTop(0);
                document.add(p);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generando PDF del ticket", e);
            throw new RuntimeException("Error generando PDF del ticket", e);
        }
    }

    public byte[] generarTicketPrueba() {
        ConfigImpresora config = configRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(ConfigImpresora.builder().build());

        int anchoPapel = config.getAnchoPapel() != null ? config.getAnchoPapel() : 80;

        EscPosBuilder builder = new EscPosBuilder(anchoPapel);

        builder.centrar()
                .tamano(2)
                .negrita(true)
                .linea("TICKET DE PRUEBA")
                .tamano(1)
                .negrita(false)
                .lineaVacia()
                .linea("Si puede ver este ticket,")
                .linea("la impresora esta configurada")
                .linea("correctamente.")
                .lineaVacia()
                .separadorDoble()
                .linea("Ancho papel: " + anchoPapel + "mm")
                .linea("Tipo: " + (config.getTipo() != null ? config.getTipo().name() : "N/A"))
                .separadorDoble()
                .lineaVacia()
                .linea("KIOSCO - Sistema de Ventas")
                .lineaVacia()
                .cortar();

        return builder.build();
    }

    public String generarTicketPruebaTexto() {
        ConfigImpresora config = configRepository.findFirstByOrderByCreatedAtDesc()
                .orElse(ConfigImpresora.builder().build());

        int anchoLinea = config.getAnchoPapel() != null && config.getAnchoPapel() == 58 ? 32 : 48;
        String separator = "=".repeat(anchoLinea);

        StringBuilder sb = new StringBuilder();
        sb.append(separator).append("\n");
        sb.append(center("TICKET DE PRUEBA", anchoLinea)).append("\n");
        sb.append(separator).append("\n\n");
        sb.append("Si puede ver este ticket,\n");
        sb.append("la impresora esta configurada\n");
        sb.append("correctamente.\n\n");
        sb.append(separator).append("\n");
        sb.append("Ancho papel: ").append(config.getAnchoPapel() != null ? config.getAnchoPapel() : 80).append("mm\n");
        sb.append("Tipo: ").append(config.getTipo() != null ? config.getTipo().name() : "N/A").append("\n");
        sb.append(separator).append("\n\n");
        sb.append(center("KIOSCO - Sistema de Ventas", anchoLinea)).append("\n\n");

        return sb.toString();
    }

    private String formatearPrecio(BigDecimal precio) {
        if (precio == null) return "$0";
        return CURRENCY_FORMAT.format(precio);
    }

    private String formatMedioPago(String medioPago) {
        if (medioPago == null) return "-";
        return switch (medioPago) {
            case "EFECTIVO" -> "Efectivo";
            case "MERCADOPAGO" -> "Mercado Pago";
            case "TRANSFERENCIA" -> "Transferencia";
            case "FIADO" -> "Fiado";
            default -> medioPago;
        };
    }

    private String center(String text, int width) {
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    private String columns(String left, String right, int width) {
        int spaces = width - left.length() - right.length();
        if (spaces < 1) spaces = 1;
        return left + " ".repeat(spaces) + right;
    }
}
