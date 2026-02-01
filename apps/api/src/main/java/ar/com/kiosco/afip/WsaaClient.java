package ar.com.kiosco.afip;

import ar.com.kiosco.domain.AmbienteAfip;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Component
public class WsaaClient {

    private static final String WSAA_TESTING_URL = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    private static final String WSAA_PRODUCTION_URL = "https://wsaa.afip.gov.ar/ws/services/LoginCms";

    private static final DateTimeFormatter AFIP_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    public String authenticate(String certificadoPath, String clavePrivadaPath,
                               String service, AmbienteAfip ambiente) throws Exception {
        log.info("Iniciando autenticación WSAA para servicio: {} en ambiente: {}", service, ambiente);

        String tra = generateTRA(service);
        log.debug("TRA generado: {}", tra);

        String cms = signTRA(tra, certificadoPath, clavePrivadaPath);
        log.debug("CMS firmado generado");

        String loginResponse = callWsaa(cms, ambiente);
        log.debug("Respuesta WSAA recibida");

        return extractToken(loginResponse);
    }

    private String generateTRA(String service) throws Exception {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(43200); // 12 horas

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element loginTicketRequest = doc.createElement("loginTicketRequest");
        loginTicketRequest.setAttribute("version", "1.0");
        doc.appendChild(loginTicketRequest);

        Element header = doc.createElement("header");
        loginTicketRequest.appendChild(header);

        Element uniqueId = doc.createElement("uniqueId");
        uniqueId.setTextContent(String.valueOf(System.currentTimeMillis() / 1000));
        header.appendChild(uniqueId);

        Element generationTime = doc.createElement("generationTime");
        generationTime.setTextContent(AFIP_DATE_FORMAT.format(now.minusSeconds(60)));
        header.appendChild(generationTime);

        Element expirationTime = doc.createElement("expirationTime");
        expirationTime.setTextContent(AFIP_DATE_FORMAT.format(expiration));
        header.appendChild(expirationTime);

        Element serviceElement = doc.createElement("service");
        serviceElement.setTextContent(service);
        loginTicketRequest.appendChild(serviceElement);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.toString();
    }

    private String signTRA(String tra, String certificadoPath, String clavePrivadaPath) throws Exception {
        X509Certificate cert = loadCertificate(certificadoPath);
        PrivateKey privateKey = loadPrivateKey(clavePrivadaPath);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(tra.getBytes(StandardCharsets.UTF_8));
        byte[] signedData = signature.sign();

        // Crear estructura CMS/PKCS#7 simplificada
        // En producción real, usar BouncyCastle para CMS completo
        // Por ahora, usamos una versión simplificada
        StringBuilder cms = new StringBuilder();
        cms.append("-----BEGIN PKCS7-----\n");
        cms.append(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(signedData));
        cms.append("\n-----END PKCS7-----");

        return Base64.getEncoder().encodeToString(cms.toString().getBytes(StandardCharsets.UTF_8));
    }

    private X509Certificate loadCertificate(String certificadoPath) throws Exception {
        Path path = Path.of(certificadoPath);
        byte[] certBytes = Files.readAllBytes(path);
        String certPem = new String(certBytes, StandardCharsets.UTF_8);

        certPem = certPem.replace("-----BEGIN CERTIFICATE-----", "")
                        .replace("-----END CERTIFICATE-----", "")
                        .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(certPem);
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
    }

    private PrivateKey loadPrivateKey(String clavePrivadaPath) throws Exception {
        Path path = Path.of(clavePrivadaPath);
        byte[] keyBytes = Files.readAllBytes(path);
        String keyPem = new String(keyBytes, StandardCharsets.UTF_8);

        keyPem = keyPem.replace("-----BEGIN PRIVATE KEY-----", "")
                      .replace("-----END PRIVATE KEY-----", "")
                      .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                      .replace("-----END RSA PRIVATE KEY-----", "")
                      .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(keyPem);
        java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private String callWsaa(String cms, AmbienteAfip ambiente) throws Exception {
        String wsaaUrl = ambiente == AmbienteAfip.PRODUCTION ? WSAA_PRODUCTION_URL : WSAA_TESTING_URL;

        String soapEnvelope = buildSoapEnvelope(cms);

        URL url = URI.create(wsaaUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapEnvelope.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String buildSoapEnvelope(String cms) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                              xmlns:wsaa="http://wsaa.view.sua.dvadac.desein.afip.gov">
                <soapenv:Header/>
                <soapenv:Body>
                    <wsaa:loginCms>
                        <wsaa:in0>%s</wsaa:in0>
                    </wsaa:loginCms>
                </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(cms);
    }

    private String extractToken(String response) throws Exception {
        // Extraer token y sign del response XML
        // Formato esperado: <token>...</token> y <sign>...</sign>
        int tokenStart = response.indexOf("<token>");
        int tokenEnd = response.indexOf("</token>");
        int signStart = response.indexOf("<sign>");
        int signEnd = response.indexOf("</sign>");

        if (tokenStart < 0 || tokenEnd < 0 || signStart < 0 || signEnd < 0) {
            log.error("No se encontró token/sign en respuesta WSAA: {}", response);
            throw new RuntimeException("Error de autenticación AFIP: respuesta inválida");
        }

        String token = response.substring(tokenStart + 7, tokenEnd);
        String sign = response.substring(signStart + 6, signEnd);

        // Retornamos token y sign separados por pipe
        return token + "|" + sign;
    }
}
