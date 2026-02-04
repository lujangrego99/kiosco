package ar.com.kiosco.config;

import ar.com.kiosco.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Attribute Converter for encrypting/decrypting String fields.
 *
 * Usage:
 * @Convert(converter = EncryptedStringConverter.class)
 * private String email;
 *
 * This converter transparently encrypts data when saving to DB
 * and decrypts when loading from DB.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService service) {
        EncryptedStringConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (encryptionService == null) {
            return attribute;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (encryptionService == null) {
            return dbData;
        }
        return encryptionService.decrypt(dbData);
    }
}
