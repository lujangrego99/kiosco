package ar.com.kiosco.service;

import ar.com.kiosco.security.KioscoContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificadoService {

    @Value("${kiosco.certificados.path:./data/certificados}")
    private String certificadosBasePath;

    /**
     * Guarda el certificado .crt y la clave privada .key de forma segura.
     *
     * @param crt El archivo .crt
     * @param key El archivo .key
     * @return Un record con los paths donde se guardaron los archivos
     */
    public CertificadoPaths guardarCertificado(MultipartFile crt, MultipartFile key) throws IOException {
        UUID kioscoId = KioscoContext.getCurrentKioscoId();
        String kioscoPrefix = kioscoId != null ? kioscoId.toString().substring(0, 8) : "default";

        // Crear directorio si no existe
        Path kioscoDir = Paths.get(certificadosBasePath, kioscoPrefix);
        Files.createDirectories(kioscoDir);

        // Generar nombres únicos para los archivos
        String timestamp = String.valueOf(System.currentTimeMillis());
        String crtFileName = "certificado_" + timestamp + ".crt";
        String keyFileName = "clave_" + timestamp + ".key";

        Path crtPath = kioscoDir.resolve(crtFileName);
        Path keyPath = kioscoDir.resolve(keyFileName);

        // Guardar archivos
        Files.copy(crt.getInputStream(), crtPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(key.getInputStream(), keyPath, StandardCopyOption.REPLACE_EXISTING);

        // Configurar permisos restrictivos para la clave privada
        try {
            keyPath.toFile().setReadable(false, false);
            keyPath.toFile().setReadable(true, true);
            keyPath.toFile().setWritable(false, false);
            keyPath.toFile().setWritable(true, true);
        } catch (Exception e) {
            log.warn("No se pudieron configurar permisos restrictivos en {}", keyPath);
        }

        log.info("Certificados guardados para kiosco {}: crt={}, key={}", kioscoPrefix, crtPath, keyPath);

        return new CertificadoPaths(crtPath.toString(), keyPath.toString());
    }

    /**
     * Verifica que el certificado sea válido y legible.
     *
     * @param certificadoPath Path al archivo .crt
     * @return true si el certificado es válido
     */
    public boolean verificarCertificado(String certificadoPath) {
        if (certificadoPath == null || certificadoPath.isBlank()) {
            return false;
        }

        try {
            X509Certificate cert = leerCertificado(certificadoPath);
            cert.checkValidity();
            return true;
        } catch (Exception e) {
            log.error("Error verificando certificado: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene la fecha de vencimiento del certificado.
     *
     * @param certificadoPath Path al archivo .crt
     * @return La fecha de vencimiento o null si no se puede leer
     */
    public LocalDate getVencimiento(String certificadoPath) {
        if (certificadoPath == null || certificadoPath.isBlank()) {
            return null;
        }

        try {
            X509Certificate cert = leerCertificado(certificadoPath);
            return cert.getNotAfter().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (Exception e) {
            log.error("Error obteniendo vencimiento del certificado: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene información del certificado.
     *
     * @param certificadoPath Path al archivo .crt
     * @return Info del certificado
     */
    public CertificadoInfo getInfo(String certificadoPath) {
        if (certificadoPath == null || certificadoPath.isBlank()) {
            return null;
        }

        try {
            X509Certificate cert = leerCertificado(certificadoPath);
            return new CertificadoInfo(
                    cert.getSubjectX500Principal().getName(),
                    cert.getIssuerX500Principal().getName(),
                    cert.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    cert.getSerialNumber().toString()
            );
        } catch (Exception e) {
            log.error("Error obteniendo info del certificado: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Elimina los archivos de certificado.
     */
    public void eliminarCertificado(String certificadoPath, String clavePrivadaPath) {
        try {
            if (certificadoPath != null) {
                Files.deleteIfExists(Paths.get(certificadoPath));
            }
            if (clavePrivadaPath != null) {
                Files.deleteIfExists(Paths.get(clavePrivadaPath));
            }
        } catch (IOException e) {
            log.error("Error eliminando certificados: {}", e.getMessage());
        }
    }

    private X509Certificate leerCertificado(String path) throws CertificateException, IOException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(path)) {
            return (X509Certificate) factory.generateCertificate(fis);
        }
    }

    public record CertificadoPaths(String certificadoPath, String clavePrivadaPath) {}

    public record CertificadoInfo(
            String subject,
            String issuer,
            LocalDate validFrom,
            LocalDate validTo,
            String serialNumber
    ) {}
}
