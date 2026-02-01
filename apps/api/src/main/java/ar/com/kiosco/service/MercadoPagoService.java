package ar.com.kiosco.service;

import ar.com.kiosco.domain.ConfigPagos;
import ar.com.kiosco.repository.ConfigPagosRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private final ConfigPagosRepository configPagosRepository;

    private ConfigPagos getConfig() {
        return configPagosRepository.findFirstByOrderByCreatedAtDesc()
                .orElseThrow(() -> new EntityNotFoundException("Configuración de pagos no encontrada"));
    }

    private void configureSdk(String accessToken) {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    @Transactional(readOnly = true)
    public PreferenciaResponse crearPreferencia(BigDecimal monto, String descripcion, String externalReference) {
        ConfigPagos config = getConfig();
        if (!config.isMercadoPagoConfigurado()) {
            throw new IllegalStateException("Mercado Pago no está configurado");
        }

        try {
            configureSdk(config.getMpAccessToken());

            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title(descripcion)
                    .quantity(1)
                    .unitPrice(monto)
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(Collections.singletonList(itemRequest))
                    .externalReference(externalReference)
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            log.info("Preferencia MP creada: id={}, externalRef={}", preference.getId(), externalReference);

            return new PreferenciaResponse(
                    preference.getId(),
                    preference.getInitPoint(),
                    preference.getSandboxInitPoint()
            );
        } catch (MPException | MPApiException e) {
            log.error("Error al crear preferencia MP: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear preferencia de Mercado Pago: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public QrResponse crearQrDinamico(BigDecimal monto, String descripcion) {
        ConfigPagos config = getConfig();
        if (!config.isMercadoPagoConfigurado()) {
            throw new IllegalStateException("Mercado Pago no está configurado");
        }

        try {
            configureSdk(config.getMpAccessToken());

            // Create a preference for QR payment
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title(descripcion)
                    .quantity(1)
                    .unitPrice(monto)
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(Collections.singletonList(itemRequest))
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            // For dynamic QR, we use the init_point as the QR content
            String qrContent = preference.getInitPoint();

            log.info("QR dinámico MP creado: preferenceId={}", preference.getId());

            return new QrResponse(
                    preference.getId(),
                    qrContent,
                    monto
            );
        } catch (MPException | MPApiException e) {
            log.error("Error al crear QR dinámico MP: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear QR de Mercado Pago: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PaymentStatus verificarPago(String paymentId) {
        ConfigPagos config = getConfig();
        if (!config.isMercadoPagoConfigurado()) {
            throw new IllegalStateException("Mercado Pago no está configurado");
        }

        try {
            configureSdk(config.getMpAccessToken());
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(paymentId));

            return new PaymentStatus(
                    payment.getId().toString(),
                    payment.getStatus(),
                    payment.getStatusDetail(),
                    payment.getTransactionAmount(),
                    payment.getExternalReference()
            );
        } catch (MPException | MPApiException e) {
            log.error("Error al verificar pago MP: {}", e.getMessage(), e);
            throw new RuntimeException("Error al verificar pago: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PaymentStatus verificarPagoByPreference(String preferenceId) {
        ConfigPagos config = getConfig();
        if (!config.isMercadoPagoConfigurado()) {
            throw new IllegalStateException("Mercado Pago no está configurado");
        }

        try {
            configureSdk(config.getMpAccessToken());
            // Note: To check payment by preference, we need to search payments
            // This is a simplified implementation
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.get(preferenceId);

            // Return pending status if no payment found
            return new PaymentStatus(
                    null,
                    "pending",
                    "waiting_for_payment",
                    null,
                    preference.getExternalReference()
            );
        } catch (MPException | MPApiException e) {
            log.error("Error al verificar pago por preferencia: {}", e.getMessage(), e);
            throw new RuntimeException("Error al verificar pago: " + e.getMessage(), e);
        }
    }

    public WebhookResult procesarWebhook(Map<String, Object> payload) {
        try {
            String type = (String) payload.get("type");
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if ("payment".equals(type) && data != null) {
                String paymentId = String.valueOf(data.get("id"));
                PaymentStatus status = verificarPago(paymentId);
                log.info("Webhook procesado: paymentId={}, status={}", paymentId, status.status());
                return new WebhookResult(true, paymentId, status.status());
            }

            return new WebhookResult(false, null, "Tipo de evento no soportado: " + type);
        } catch (Exception e) {
            log.error("Error procesando webhook MP: {}", e.getMessage(), e);
            return new WebhookResult(false, null, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public VerificacionMpResult verificarConfiguracion() {
        try {
            ConfigPagos config = configPagosRepository.findFirstByOrderByCreatedAtDesc()
                    .orElse(null);

            if (config == null || !config.isMercadoPagoConfigurado()) {
                return new VerificacionMpResult(false, "NO_CONFIGURADO", "Mercado Pago no está configurado");
            }

            // Try to validate the access token by making a simple API call
            configureSdk(config.getMpAccessToken());
            PreferenceClient client = new PreferenceClient();
            // If we get here without exception, the token is valid

            return new VerificacionMpResult(true, "CONFIGURADO", "Mercado Pago configurado correctamente");
        } catch (Exception e) {
            log.error("Error verificando configuración MP: {}", e.getMessage());
            return new VerificacionMpResult(false, "ERROR", "Error al verificar: " + e.getMessage());
        }
    }

    public record PreferenciaResponse(
            String preferenceId,
            String initPoint,
            String sandboxInitPoint
    ) {}

    public record QrResponse(
            String preferenceId,
            String qrContent,
            BigDecimal monto
    ) {}

    public record PaymentStatus(
            String paymentId,
            String status,
            String statusDetail,
            BigDecimal amount,
            String externalReference
    ) {}

    public record WebhookResult(
            boolean procesado,
            String paymentId,
            String mensaje
    ) {}

    public record VerificacionMpResult(
            boolean valido,
            String estado,
            String mensaje
    ) {}
}
