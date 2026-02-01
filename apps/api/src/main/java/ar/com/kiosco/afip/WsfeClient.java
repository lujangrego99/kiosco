package ar.com.kiosco.afip;

import ar.com.kiosco.domain.AmbienteAfip;
import ar.com.kiosco.domain.TipoComprobante;
import ar.com.kiosco.dto.AfipResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class WsfeClient {

    private static final String WSFE_TESTING_URL = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
    private static final String WSFE_PRODUCTION_URL = "https://servicios1.afip.gov.ar/wsfev1/service.asmx";

    private static final DateTimeFormatter AFIP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public long getUltimoComprobante(String token, String sign, String cuit,
                                      int puntoVenta, int tipoComprobante,
                                      AmbienteAfip ambiente) throws Exception {
        log.info("Obteniendo último comprobante - PV: {}, Tipo: {}", puntoVenta, tipoComprobante);

        String request = buildFECompUltimoAutorizadoRequest(token, sign, cuit, puntoVenta, tipoComprobante);
        String response = callWsfe(request, "FECompUltimoAutorizado", ambiente);

        return parseUltimoComprobante(response);
    }

    public AfipResponseDTO solicitarCAE(String token, String sign, String cuit,
                                        FacturaRequest factura, AmbienteAfip ambiente) throws Exception {
        log.info("Solicitando CAE para factura - PV: {}, Tipo: {}, Número: {}",
                factura.puntoVenta(), factura.tipoComprobante(), factura.numero());

        String request = buildFECAESolicitarRequest(token, sign, cuit, factura);
        String response = callWsfe(request, "FECAESolicitar", ambiente);

        return parseCAEResponse(response);
    }

    private String buildFECompUltimoAutorizadoRequest(String token, String sign, String cuit,
                                                       int puntoVenta, int tipoComprobante) {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                           xmlns:ar="http://ar.gov.afip.dif.FEV1/">
                <soap:Body>
                    <ar:FECompUltimoAutorizado>
                        <ar:Auth>
                            <ar:Token>%s</ar:Token>
                            <ar:Sign>%s</ar:Sign>
                            <ar:Cuit>%s</ar:Cuit>
                        </ar:Auth>
                        <ar:PtoVta>%d</ar:PtoVta>
                        <ar:CbteTipo>%d</ar:CbteTipo>
                    </ar:FECompUltimoAutorizado>
                </soap:Body>
            </soap:Envelope>
            """.formatted(token, sign, cuit.replace("-", ""), puntoVenta, tipoComprobante);
    }

    private String buildFECAESolicitarRequest(String token, String sign, String cuit,
                                               FacturaRequest factura) {
        String fechaEmision = factura.fechaEmision().format(AFIP_DATE_FORMAT);
        String cuitReceptor = factura.cuitReceptor() != null
                ? factura.cuitReceptor().replace("-", "")
                : "0";
        int docTipo = factura.cuitReceptor() != null ? 80 : 99; // 80=CUIT, 99=Consumidor Final

        StringBuilder ivaBuilder = new StringBuilder();
        if (factura.importeIva() != null && factura.importeIva().compareTo(BigDecimal.ZERO) > 0) {
            ivaBuilder.append("""
                        <ar:Iva>
                            <ar:AlicIva>
                                <ar:Id>5</ar:Id>
                                <ar:BaseImp>%s</ar:BaseImp>
                                <ar:Importe>%s</ar:Importe>
                            </ar:AlicIva>
                        </ar:Iva>
                """.formatted(
                    formatImporte(factura.importeNeto()),
                    formatImporte(factura.importeIva())
            ));
        }

        return """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                           xmlns:ar="http://ar.gov.afip.dif.FEV1/">
                <soap:Body>
                    <ar:FECAESolicitar>
                        <ar:Auth>
                            <ar:Token>%s</ar:Token>
                            <ar:Sign>%s</ar:Sign>
                            <ar:Cuit>%s</ar:Cuit>
                        </ar:Auth>
                        <ar:FeCAEReq>
                            <ar:FeCabReq>
                                <ar:CantReg>1</ar:CantReg>
                                <ar:PtoVta>%d</ar:PtoVta>
                                <ar:CbteTipo>%d</ar:CbteTipo>
                            </ar:FeCabReq>
                            <ar:FeDetReq>
                                <ar:FECAEDetRequest>
                                    <ar:Concepto>1</ar:Concepto>
                                    <ar:DocTipo>%d</ar:DocTipo>
                                    <ar:DocNro>%s</ar:DocNro>
                                    <ar:CbteDesde>%d</ar:CbteDesde>
                                    <ar:CbteHasta>%d</ar:CbteHasta>
                                    <ar:CbteFch>%s</ar:CbteFch>
                                    <ar:ImpTotal>%s</ar:ImpTotal>
                                    <ar:ImpTotConc>0</ar:ImpTotConc>
                                    <ar:ImpNeto>%s</ar:ImpNeto>
                                    <ar:ImpOpEx>0</ar:ImpOpEx>
                                    <ar:ImpTrib>0</ar:ImpTrib>
                                    <ar:ImpIVA>%s</ar:ImpIVA>
                                    <ar:MonId>PES</ar:MonId>
                                    <ar:MonCotiz>1</ar:MonCotiz>
                                    %s
                                </ar:FECAEDetRequest>
                            </ar:FeDetReq>
                        </ar:FeCAEReq>
                    </ar:FECAESolicitar>
                </soap:Body>
            </soap:Envelope>
            """.formatted(
                token, sign, cuit.replace("-", ""),
                factura.puntoVenta(), factura.tipoComprobante(),
                docTipo, cuitReceptor,
                factura.numero(), factura.numero(),
                fechaEmision,
                formatImporte(factura.importeTotal()),
                formatImporte(factura.importeNeto() != null ? factura.importeNeto() : factura.importeTotal()),
                formatImporte(factura.importeIva() != null ? factura.importeIva() : BigDecimal.ZERO),
                ivaBuilder.toString()
        );
    }

    private String formatImporte(BigDecimal importe) {
        if (importe == null) return "0";
        return importe.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String callWsfe(String request, String action, AmbienteAfip ambiente) throws Exception {
        String wsfeUrl = ambiente == AmbienteAfip.PRODUCTION ? WSFE_PRODUCTION_URL : WSFE_TESTING_URL;

        URL url = URI.create(wsfeUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "http://ar.gov.afip.dif.FEV1/" + action);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            if (responseCode >= 400) {
                log.error("Error WSFE - Código: {}, Respuesta: {}", responseCode, response);
                throw new RuntimeException("Error en comunicación con AFIP: " + responseCode);
            }

            return response.toString();
        }
    }

    private long parseUltimoComprobante(String response) throws Exception {
        Document doc = parseXml(response);
        NodeList cbteNroNodes = doc.getElementsByTagName("CbteNro");
        if (cbteNroNodes.getLength() > 0) {
            return Long.parseLong(cbteNroNodes.item(0).getTextContent());
        }

        // Buscar errores
        checkErrors(doc);
        return 0;
    }

    private AfipResponseDTO parseCAEResponse(String response) throws Exception {
        Document doc = parseXml(response);
        AfipResponseDTO result = new AfipResponseDTO();

        // Buscar resultado
        NodeList resultadoNodes = doc.getElementsByTagName("Resultado");
        if (resultadoNodes.getLength() > 0) {
            String resultado = resultadoNodes.item(0).getTextContent();
            result.setAprobado("A".equalsIgnoreCase(resultado));
        }

        // Buscar CAE
        NodeList caeNodes = doc.getElementsByTagName("CAE");
        if (caeNodes.getLength() > 0) {
            result.setCae(caeNodes.item(0).getTextContent());
        }

        // Buscar vencimiento CAE
        NodeList caeVtoNodes = doc.getElementsByTagName("CAEFchVto");
        if (caeVtoNodes.getLength() > 0) {
            String fechaVto = caeVtoNodes.item(0).getTextContent();
            result.setCaeVencimiento(LocalDate.parse(fechaVto, AFIP_DATE_FORMAT));
        }

        // Buscar número de comprobante
        NodeList cbteDesdeNodes = doc.getElementsByTagName("CbteDesde");
        if (cbteDesdeNodes.getLength() > 0) {
            result.setNumeroComprobante(Long.parseLong(cbteDesdeNodes.item(0).getTextContent()));
        }

        // Buscar observaciones
        NodeList obsNodes = doc.getElementsByTagName("Obs");
        for (int i = 0; i < obsNodes.getLength(); i++) {
            Element obs = (Element) obsNodes.item(i);
            NodeList msgNodes = obs.getElementsByTagName("Msg");
            if (msgNodes.getLength() > 0) {
                result.addObservacion(msgNodes.item(0).getTextContent());
            }
        }

        // Buscar errores
        NodeList errorNodes = doc.getElementsByTagName("Err");
        for (int i = 0; i < errorNodes.getLength(); i++) {
            Element err = (Element) errorNodes.item(i);
            NodeList msgNodes = err.getElementsByTagName("Msg");
            if (msgNodes.getLength() > 0) {
                result.addError(msgNodes.item(0).getTextContent());
                result.setAprobado(false);
            }
        }

        return result;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private void checkErrors(Document doc) {
        NodeList errorNodes = doc.getElementsByTagName("Err");
        if (errorNodes.getLength() > 0) {
            Element err = (Element) errorNodes.item(0);
            NodeList msgNodes = err.getElementsByTagName("Msg");
            if (msgNodes.getLength() > 0) {
                throw new RuntimeException("Error AFIP: " + msgNodes.item(0).getTextContent());
            }
        }
    }

    public record FacturaRequest(
            int puntoVenta,
            int tipoComprobante,
            long numero,
            LocalDate fechaEmision,
            String cuitReceptor,
            int condicionIvaReceptor,
            BigDecimal importeNeto,
            BigDecimal importeIva,
            BigDecimal importeTotal
    ) {}
}
