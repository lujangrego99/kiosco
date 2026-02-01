package ar.com.kiosco.service;

import ar.com.kiosco.domain.ConfigPagos;
import ar.com.kiosco.repository.ConfigPagosRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrService {

    private final ConfigPagosRepository configPagosRepository;

    private static final int QR_SIZE = 300;

    private ConfigPagos getConfig() {
        return configPagosRepository.findFirstByOrderByCreatedAtDesc()
                .orElseThrow(() -> new EntityNotFoundException("Configuración de pagos no encontrada"));
    }

    @Transactional(readOnly = true)
    public QrInteroperableResponse generarQrInteroperable(BigDecimal monto, String descripcion) {
        ConfigPagos config = getConfig();
        if (!config.isQrConfigurado()) {
            throw new IllegalStateException("QR interoperable no está configurado");
        }

        // Generate EMVCo compliant QR content for Argentina
        String qrContent = buildEmvCoQrContent(config, monto, descripcion);

        // Generate QR image as base64
        String qrImageBase64 = generateQrImageBase64(qrContent);

        log.info("QR interoperable generado: alias={}, monto={}", config.getQrAlias(), monto);

        return new QrInteroperableResponse(
                qrContent,
                qrImageBase64,
                config.getQrAlias(),
                monto,
                descripcion
        );
    }

    @Transactional(readOnly = true)
    public QrEstatico generarQrEstatico() {
        ConfigPagos config = getConfig();
        if (!config.isQrConfigurado()) {
            throw new IllegalStateException("QR interoperable no está configurado");
        }

        // Generate static QR (without amount)
        String qrContent = buildEmvCoQrContent(config, null, null);
        String qrImageBase64 = generateQrImageBase64(qrContent);

        return new QrEstatico(
                qrContent,
                qrImageBase64,
                config.getQrAlias(),
                config.getQrCbu()
        );
    }

    private String buildEmvCoQrContent(ConfigPagos config, BigDecimal monto, String descripcion) {
        // EMVCo QR Code specification for Argentina (BCRA standard)
        // This is a simplified implementation of the EMVCo Merchant Presented QR spec

        StringBuilder qr = new StringBuilder();

        // Payload Format Indicator (ID 00)
        qr.append(buildTlv("00", "01"));

        // Point of Initiation Method (ID 01)
        // 11 = Static, 12 = Dynamic
        qr.append(buildTlv("01", monto != null ? "12" : "11"));

        // Merchant Account Information (ID 26-51, we use 26)
        // This contains the alias or CBU
        String merchantInfo = buildMerchantInfo(config);
        qr.append(buildTlv("26", merchantInfo));

        // Merchant Category Code (ID 52) - Retail/Kiosk
        qr.append(buildTlv("52", "5411"));

        // Transaction Currency (ID 53) - ARS = 032
        qr.append(buildTlv("53", "032"));

        // Transaction Amount (ID 54) - optional
        if (monto != null) {
            qr.append(buildTlv("54", formatAmount(monto)));
        }

        // Country Code (ID 58) - Argentina
        qr.append(buildTlv("58", "AR"));

        // Merchant Name (ID 59)
        String merchantName = "KIOSCO";
        qr.append(buildTlv("59", truncate(merchantName, 25)));

        // Merchant City (ID 60)
        qr.append(buildTlv("60", "BUENOS AIRES"));

        // Additional Data Field (ID 62) - optional
        if (descripcion != null && !descripcion.isBlank()) {
            String additionalData = buildTlv("05", truncate(descripcion, 25));
            qr.append(buildTlv("62", additionalData));
        }

        // CRC (ID 63) - calculated at the end
        String dataWithoutCrc = qr.toString() + "6304";
        String crc = calculateCrc16(dataWithoutCrc);
        qr.append(buildTlv("63", crc));

        return qr.toString();
    }

    private String buildMerchantInfo(ConfigPagos config) {
        StringBuilder info = new StringBuilder();

        // Globally Unique Identifier for Argentina QR payments
        info.append(buildTlv("00", "ar.com.qr"));

        // Alias (ID 01)
        if (config.getQrAlias() != null && !config.getQrAlias().isBlank()) {
            info.append(buildTlv("01", config.getQrAlias()));
        }

        // CBU (ID 02) - optional if alias is present
        if (config.getQrCbu() != null && !config.getQrCbu().isBlank()) {
            info.append(buildTlv("02", config.getQrCbu()));
        }

        return info.toString();
    }

    private String buildTlv(String id, String value) {
        String length = String.format("%02d", value.length());
        return id + length + value;
    }

    private String formatAmount(BigDecimal amount) {
        // Format amount without decimals for ARS (integer pesos)
        return amount.setScale(0, java.math.RoundingMode.HALF_UP).toString();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    private String calculateCrc16(String data) {
        // CRC-16/CCITT-FALSE algorithm
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        int crc = 0xFFFF;
        int polynomial = 0x1021;

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i)) & 1) == 1;
                boolean c15 = ((crc >> 15) & 1) == 1;
                crc <<= 1;
                if (c15 ^ bit) {
                    crc ^= polynomial;
                }
            }
        }
        crc &= 0xFFFF;
        return String.format("%04X", crc);
    }

    private String generateQrImageBase64(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | IOException e) {
            log.error("Error generando imagen QR: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar imagen QR", e);
        }
    }

    public record QrInteroperableResponse(
            String qrContent,
            String qrImageBase64,
            String alias,
            BigDecimal monto,
            String descripcion
    ) {}

    public record QrEstatico(
            String qrContent,
            String qrImageBase64,
            String alias,
            String cbu
    ) {}
}
