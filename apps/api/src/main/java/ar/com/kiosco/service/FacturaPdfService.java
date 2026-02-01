package ar.com.kiosco.service;

import ar.com.kiosco.dto.ComprobanteDTO;
import ar.com.kiosco.dto.VentaDTO;
import ar.com.kiosco.dto.VentaItemDTO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaPdfService {

    private final VentaService ventaService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(22, 163, 74); // green-600

    public byte[] generarPdf(ComprobanteDTO comprobante) {
        log.info("Generando PDF para comprobante: {}", comprobante.getNumeroCompleto());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(30, 30, 30, 30);

            // Header con tipo de comprobante
            addHeader(document, comprobante);

            // Datos del emisor y receptor
            addDatosEmisorReceptor(document, comprobante);

            // Línea separadora
            document.add(new Paragraph("")
                    .setBorderBottom(new SolidBorder(ColorConstants.GRAY, 1))
                    .setMarginBottom(10));

            // Detalle de items (si hay venta asociada)
            if (comprobante.getVentaId() != null) {
                addDetalleItems(document, comprobante);
            }

            // Totales
            addTotales(document, comprobante);

            // CAE y código de barras
            addCaeInfo(document, comprobante);

            // QR de AFIP
            addQrAfip(document, comprobante);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error al generar PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF de factura: " + e.getMessage());
        }
    }

    private void addHeader(Document document, ComprobanteDTO comprobante) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{45, 10, 45}))
                .useAllAvailableWidth();

        // Columna izquierda - Datos del emisor
        Cell leftCell = new Cell()
                .setBorder(new SolidBorder(1))
                .setPadding(10);

        leftCell.add(new Paragraph(comprobante.getRazonSocialEmisor())
                .setFontSize(14)
                .setBold());
        leftCell.add(new Paragraph("CUIT: " + formatCuit(comprobante.getCuitEmisor()))
                .setFontSize(10));
        leftCell.add(new Paragraph(comprobante.getCondicionIvaEmisor())
                .setFontSize(9));

        headerTable.addCell(leftCell);

        // Columna central - Letra
        Cell centerCell = new Cell()
                .setBorder(new SolidBorder(1))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        centerCell.add(new Paragraph(comprobante.getTipoComprobanteLetra())
                .setFontSize(36)
                .setBold());
        centerCell.add(new Paragraph("Cod. " + String.format("%02d", comprobante.getTipoComprobante()))
                .setFontSize(8));

        headerTable.addCell(centerCell);

        // Columna derecha - Datos del comprobante
        Cell rightCell = new Cell()
                .setBorder(new SolidBorder(1))
                .setPadding(10);

        rightCell.add(new Paragraph(comprobante.getTipoComprobanteDescripcion())
                .setFontSize(14)
                .setBold());
        rightCell.add(new Paragraph("Nº " + comprobante.getNumeroCompleto())
                .setFontSize(12));
        rightCell.add(new Paragraph("Fecha: " + comprobante.getFechaEmision().format(DATE_FORMAT))
                .setFontSize(10));

        headerTable.addCell(rightCell);

        document.add(headerTable);
        document.add(new Paragraph("").setMarginBottom(10));
    }

    private void addDatosEmisorReceptor(Document document, ComprobanteDTO comprobante) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(10);

        // Datos del cliente
        Cell clienteCell = new Cell()
                .setBorder(Border.NO_BORDER);

        if (comprobante.getClienteNombre() != null) {
            clienteCell.add(new Paragraph("Cliente: " + comprobante.getClienteNombre())
                    .setFontSize(10));
        } else {
            clienteCell.add(new Paragraph("Cliente: Consumidor Final")
                    .setFontSize(10));
        }

        if (comprobante.getCuitReceptor() != null) {
            clienteCell.add(new Paragraph("CUIT: " + formatCuit(comprobante.getCuitReceptor()))
                    .setFontSize(10));
        }

        if (comprobante.getCondicionIvaReceptor() != null) {
            clienteCell.add(new Paragraph("Condición IVA: " + comprobante.getCondicionIvaReceptor())
                    .setFontSize(10));
        }

        table.addCell(clienteCell);

        // Celda vacía para alineación
        table.addCell(new Cell().setBorder(Border.NO_BORDER));

        document.add(table);
    }

    private void addDetalleItems(Document document, ComprobanteDTO comprobante) {
        try {
            VentaDTO venta = ventaService.obtenerPorId(comprobante.getVentaId());

            if (venta.getItems() == null || venta.getItems().isEmpty()) {
                return;
            }

            document.add(new Paragraph("Detalle")
                    .setFontSize(12)
                    .setBold()
                    .setMarginBottom(5));

            Table table = new Table(UnitValue.createPercentArray(new float[]{10, 45, 15, 15, 15}))
                    .useAllAvailableWidth();

            // Header
            addTableHeader(table, "Cant.", "Descripción", "P. Unit.", "Subtotal", "%");

            // Items
            for (VentaItemDTO item : venta.getItems()) {
                table.addCell(createCell(item.getCantidad().toString()));
                table.addCell(createCell(item.getProductoNombre()));
                table.addCell(createCell(formatMoney(item.getPrecioUnitario())));
                table.addCell(createCell(formatMoney(item.getSubtotal())));
                table.addCell(createCell("")); // IVA %
            }

            document.add(table);
            document.add(new Paragraph("").setMarginBottom(10));

        } catch (Exception e) {
            log.warn("No se pudo cargar detalle de venta: {}", e.getMessage());
        }
    }

    private void addTableHeader(Table table, String... headers) {
        for (String header : headers) {
            Cell cell = new Cell()
                    .add(new Paragraph(header).setFontSize(9).setBold())
                    .setBackgroundColor(new DeviceRgb(240, 240, 240))
                    .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                    .setPadding(5);
            table.addHeaderCell(cell);
        }
    }

    private Cell createCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(9))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setPadding(5);
    }

    private void addTotales(Document document, ComprobanteDTO comprobante) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth()
                .setMarginTop(10);

        // Espacio vacío a la izquierda
        table.addCell(new Cell().setBorder(Border.NO_BORDER));

        // Totales a la derecha
        Cell totalesCell = new Cell()
                .setBorder(new SolidBorder(1))
                .setPadding(10);

        if (comprobante.getImporteNeto() != null &&
            comprobante.getImporteNeto().compareTo(comprobante.getImporteTotal()) != 0) {
            totalesCell.add(createTotalRow("Subtotal Neto:", comprobante.getImporteNeto()));
            totalesCell.add(createTotalRow("IVA 21%:", comprobante.getImporteIva()));
        }

        Paragraph totalFinal = new Paragraph()
                .add(new Text("TOTAL: ").setBold())
                .add(new Text(formatMoney(comprobante.getImporteTotal())).setBold())
                .setFontSize(14)
                .setTextAlignment(TextAlignment.RIGHT);
        totalesCell.add(totalFinal);

        table.addCell(totalesCell);
        document.add(table);
    }

    private Paragraph createTotalRow(String label, BigDecimal amount) {
        return new Paragraph()
                .add(new Text(label).setFontSize(10))
                .add(new Text(" " + formatMoney(amount)).setFontSize(10))
                .setTextAlignment(TextAlignment.RIGHT);
    }

    private void addCaeInfo(Document document, ComprobanteDTO comprobante) {
        document.add(new Paragraph("").setMarginTop(20));

        Table table = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth();

        Cell cell = new Cell()
                .setBorder(new SolidBorder(1))
                .setPadding(10);

        cell.add(new Paragraph("CAE: " + comprobante.getCae())
                .setFontSize(11)
                .setBold());

        cell.add(new Paragraph("Fecha Vto. CAE: " +
                (comprobante.getCaeVencimiento() != null
                        ? comprobante.getCaeVencimiento().format(DATE_FORMAT)
                        : "N/A"))
                .setFontSize(10));

        // Código de barras (representación texto)
        String codigoBarras = buildCodigoBarras(comprobante);
        cell.add(new Paragraph("Código de barras: " + codigoBarras)
                .setFontSize(8)
                .setMarginTop(5));

        table.addCell(cell);
        document.add(table);
    }

    private void addQrAfip(Document document, ComprobanteDTO comprobante) {
        try {
            String qrData = buildQrData(comprobante);
            byte[] qrImage = generateQrCode(qrData, 100, 100);

            Image qr = new Image(ImageDataFactory.create(qrImage))
                    .setWidth(80)
                    .setHeight(80);

            Table table = new Table(UnitValue.createPercentArray(new float[]{80, 20}))
                    .useAllAvailableWidth()
                    .setMarginTop(10);

            Cell textCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            textCell.add(new Paragraph("Comprobante Autorizado")
                    .setFontSize(9));
            textCell.add(new Paragraph("Esta factura fue emitida mediante el sistema de facturación electrónica de AFIP")
                    .setFontSize(7)
                    .setFontColor(ColorConstants.GRAY));

            Cell qrCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT);
            qrCell.add(qr);

            table.addCell(textCell);
            table.addCell(qrCell);
            document.add(table);

        } catch (Exception e) {
            log.warn("No se pudo generar QR: {}", e.getMessage());
        }
    }

    private String buildCodigoBarras(ComprobanteDTO comprobante) {
        String cuit = comprobante.getCuitEmisor().replace("-", "");
        String tipo = String.format("%03d", comprobante.getTipoComprobante());
        String pv = String.format("%05d", comprobante.getPuntoVenta());
        String cae = comprobante.getCae() != null ? comprobante.getCae() : "00000000000000";
        String vto = comprobante.getCaeVencimiento() != null
                ? comprobante.getCaeVencimiento().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                : "00000000";

        return cuit + tipo + pv + cae + vto;
    }

    private String buildQrData(ComprobanteDTO comprobante) {
        // URL del QR de AFIP según especificación
        String baseUrl = "https://www.afip.gob.ar/fe/qr/";

        // Datos en formato JSON codificado en base64
        String json = String.format(
                "{\"ver\":1,\"fecha\":\"%s\",\"cuit\":%s,\"ptoVta\":%d,\"tipoCmp\":%d,\"nroCmp\":%d,\"importe\":%s,\"moneda\":\"PES\",\"ctz\":1,\"tipoDocRec\":%d,\"nroDocRec\":%s,\"tipoCodAut\":\"E\",\"codAut\":%s}",
                comprobante.getFechaEmision(),
                comprobante.getCuitEmisor().replace("-", ""),
                comprobante.getPuntoVenta(),
                comprobante.getTipoComprobante(),
                comprobante.getNumero(),
                comprobante.getImporteTotal().toPlainString(),
                comprobante.getCuitReceptor() != null ? 80 : 99,
                comprobante.getCuitReceptor() != null ? comprobante.getCuitReceptor().replace("-", "") : "0",
                comprobante.getCae()
        );

        String base64 = java.util.Base64.getUrlEncoder().encodeToString(json.getBytes());
        return baseUrl + "?p=" + base64;
    }

    private byte[] generateQrCode(String data, int width, int height) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "$0,00";
        return CURRENCY_FORMAT.format(amount);
    }

    private String formatCuit(String cuit) {
        if (cuit == null) return "";
        cuit = cuit.replace("-", "");
        if (cuit.length() == 11) {
            return cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10);
        }
        return cuit;
    }
}
