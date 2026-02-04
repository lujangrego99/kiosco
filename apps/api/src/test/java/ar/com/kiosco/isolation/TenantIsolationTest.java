package ar.com.kiosco.isolation;

import ar.com.kiosco.config.TenantSchemaManager;
import ar.com.kiosco.security.KioscoContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify multi-tenant isolation mechanisms.
 *
 * These tests validate:
 * 1. KioscoContext correctly stores and retrieves tenant information
 * 2. TenantSchemaManager generates correct and safe schema names
 * 3. Context switching works properly
 *
 * For full database-level isolation tests with actual schemas,
 * use TenantIsolationIntegrationTest with a PostgreSQL database.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Tenant Isolation Mechanism Tests")
class TenantIsolationTest {

    @Autowired
    private TenantSchemaManager tenantSchemaManager;

    // Test tenant IDs
    private UUID tenantAId;
    private UUID tenantBId;

    // Test user data
    private UUID userAId;
    private UUID userBId;
    private String userAEmail = "usera@test.com";
    private String userBEmail = "userb@test.com";

    @BeforeEach
    void setUp() {
        // Generate unique tenant IDs for this test run
        tenantAId = UUID.randomUUID();
        tenantBId = UUID.randomUUID();
        userAId = UUID.randomUUID();
        userBId = UUID.randomUUID();

        // Ensure clean state
        KioscoContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear context after each test
        KioscoContext.clear();
    }

    @Nested
    @DisplayName("KioscoContext Tests")
    class KioscoContextTests {

        @Test
        @DisplayName("setContext stores tenant and user information correctly")
        void setContext_stores_data_correctly() {
            KioscoContext.setContext(tenantAId, "owner", userAId, userAEmail);

            assertThat(KioscoContext.getCurrentKioscoId()).isEqualTo(tenantAId);
            assertThat(KioscoContext.getCurrentKioscoRole()).isEqualTo("owner");
            assertThat(KioscoContext.getCurrentUsuarioId()).isEqualTo(userAId);
            assertThat(KioscoContext.getCurrentUsuarioEmail()).isEqualTo(userAEmail);
        }

        @Test
        @DisplayName("clear() removes all context data")
        void clear_removes_context() {
            KioscoContext.setContext(tenantAId, "owner", userAId, userAEmail);
            KioscoContext.clear();

            assertThat(KioscoContext.getCurrentKioscoId()).isNull();
            assertThat(KioscoContext.getCurrentKioscoRole()).isNull();
            assertThat(KioscoContext.getCurrentUsuarioId()).isNull();
            assertThat(KioscoContext.getCurrentUsuarioEmail()).isNull();
        }

        @Test
        @DisplayName("Context switching between tenants works correctly")
        void context_switching_works() {
            // Set tenant A context
            KioscoContext.setContext(tenantAId, "owner", userAId, userAEmail);
            assertThat(KioscoContext.getCurrentKioscoId()).isEqualTo(tenantAId);

            // Switch to tenant B
            KioscoContext.setContext(tenantBId, "admin", userBId, userBEmail);
            assertThat(KioscoContext.getCurrentKioscoId()).isEqualTo(tenantBId);
            assertThat(KioscoContext.getCurrentKioscoRole()).isEqualTo("admin");

            // Switch back to tenant A
            KioscoContext.setContext(tenantAId, "owner", userAId, userAEmail);
            assertThat(KioscoContext.getCurrentKioscoId()).isEqualTo(tenantAId);
            assertThat(KioscoContext.getCurrentKioscoRole()).isEqualTo("owner");
        }

        @Test
        @DisplayName("getContext returns complete context data")
        void getContext_returns_complete_data() {
            KioscoContext.setContext(tenantAId, "cajero", userAId, userAEmail);

            KioscoContext.KioscoContextData data = KioscoContext.getContext();

            assertThat(data).isNotNull();
            assertThat(data.getKioscoId()).isEqualTo(tenantAId);
            assertThat(data.getKioscoRole()).isEqualTo("cajero");
            assertThat(data.getUsuarioId()).isEqualTo(userAId);
            assertThat(data.getUsuarioEmail()).isEqualTo(userAEmail);
        }

        @Test
        @DisplayName("isKioscoOwner returns true only for owner role")
        void isKioscoOwner_checks_role() {
            KioscoContext.setContext(tenantAId, "owner", userAId, userAEmail);
            assertTrue(KioscoContext.isKioscoOwner());

            KioscoContext.setContext(tenantAId, "admin", userAId, userAEmail);
            assertFalse(KioscoContext.isKioscoOwner());

            KioscoContext.setContext(tenantAId, "cajero", userAId, userAEmail);
            assertFalse(KioscoContext.isKioscoOwner());
        }

        @Test
        @DisplayName("isKioscoAdminOrOwner returns true for owner and admin")
        void isKioscoAdminOrOwner_checks_roles() {
            KioscoContext.setContext(tenantAId, "owner", userAId, userAEmail);
            assertTrue(KioscoContext.isKioscoAdminOrOwner());

            KioscoContext.setContext(tenantAId, "admin", userAId, userAEmail);
            assertTrue(KioscoContext.isKioscoAdminOrOwner());

            KioscoContext.setContext(tenantAId, "cajero", userAId, userAEmail);
            assertFalse(KioscoContext.isKioscoAdminOrOwner());
        }
    }

    @Nested
    @DisplayName("TenantSchemaManager Tests")
    class TenantSchemaManagerTests {

        @Test
        @DisplayName("getSchemaName generates correct format")
        void getSchemaName_correct_format() {
            UUID kioscoId = UUID.fromString("12345678-1234-1234-1234-123456789abc");
            String schemaName = tenantSchemaManager.getSchemaName(kioscoId);

            assertThat(schemaName).isEqualTo("kiosco_12345678");
        }

        @Test
        @DisplayName("Schema names are lowercase")
        void schema_names_are_lowercase() {
            UUID kioscoId = UUID.fromString("ABCDEF12-3456-7890-ABCD-EF1234567890");
            String schemaName = tenantSchemaManager.getSchemaName(kioscoId);

            assertThat(schemaName).isEqualTo("kiosco_abcdef12");
            assertThat(schemaName).isEqualTo(schemaName.toLowerCase());
        }

        @Test
        @DisplayName("Different UUIDs produce different schema names")
        void different_uuids_different_schemas() {
            String schemaA = tenantSchemaManager.getSchemaName(tenantAId);
            String schemaB = tenantSchemaManager.getSchemaName(tenantBId);

            assertThat(schemaA).isNotEqualTo(schemaB);
        }

        @Test
        @DisplayName("Same UUID always produces same schema name")
        void same_uuid_same_schema() {
            String schema1 = tenantSchemaManager.getSchemaName(tenantAId);
            String schema2 = tenantSchemaManager.getSchemaName(tenantAId);
            String schema3 = tenantSchemaManager.getSchemaName(tenantAId);

            assertThat(schema1).isEqualTo(schema2);
            assertThat(schema2).isEqualTo(schema3);
        }

        @Test
        @DisplayName("Schema name from String matches UUID version")
        void string_uuid_matches_uuid_version() {
            String fromUuid = tenantSchemaManager.getSchemaName(tenantAId);
            String fromString = tenantSchemaManager.getSchemaName(tenantAId.toString());

            assertThat(fromUuid).isEqualTo(fromString);
        }

        @Test
        @DisplayName("Schema name only contains safe characters")
        void schema_name_safe_characters() {
            // Test with many random UUIDs
            for (int i = 0; i < 100; i++) {
                UUID randomId = UUID.randomUUID();
                String schemaName = tenantSchemaManager.getSchemaName(randomId);

                // Should match pattern: kiosco_ followed by 8 lowercase hex chars
                assertThat(schemaName).matches("^kiosco_[a-f0-9]{8}$");

                // Should not contain SQL injection characters
                assertThat(schemaName).doesNotContain(";", "'", "\"", "--", "/*", "*/", "\\", " ");
            }
        }

        @Test
        @DisplayName("Null UUID throws NullPointerException")
        void null_uuid_throws_npe() {
            assertThrows(NullPointerException.class, () -> {
                tenantSchemaManager.getSchemaName((UUID) null);
            });
        }

        @Test
        @DisplayName("Null String throws NullPointerException")
        void null_string_throws_npe() {
            assertThrows(NullPointerException.class, () -> {
                tenantSchemaManager.getSchemaName((String) null);
            });
        }
    }

    @Nested
    @DisplayName("Tenant Isolation Logic")
    class TenantIsolationLogic {

        @Test
        @DisplayName("Two tenants get different schema names")
        void two_tenants_different_schemas() {
            String schemaA = tenantSchemaManager.getSchemaName(tenantAId);
            String schemaB = tenantSchemaManager.getSchemaName(tenantBId);

            // Schemas must be different for isolation to work
            assertThat(schemaA).isNotEqualTo(schemaB);

            // Both should follow the correct format
            assertThat(schemaA).startsWith("kiosco_");
            assertThat(schemaB).startsWith("kiosco_");
        }

        @Test
        @DisplayName("Context isolation - each thread can have its own context")
        void context_thread_isolation() throws InterruptedException {
            // Main thread sets context A
            KioscoContext.setContext(tenantAId, "owner", userAId, userAEmail);

            // Create a new thread that sets different context
            UUID[] threadTenantId = new UUID[1];
            Thread otherThread = new Thread(() -> {
                KioscoContext.setContext(tenantBId, "admin", userBId, userBEmail);
                threadTenantId[0] = KioscoContext.getCurrentKioscoId();
            });
            otherThread.start();
            otherThread.join();

            // Main thread should still have its original context
            assertThat(KioscoContext.getCurrentKioscoId()).isEqualTo(tenantAId);

            // Other thread had different context
            assertThat(threadTenantId[0]).isEqualTo(tenantBId);
        }

        @Test
        @DisplayName("Empty context returns null for all fields")
        void empty_context_returns_nulls() {
            // Don't set any context
            KioscoContext.clear();

            assertThat(KioscoContext.getCurrentKioscoId()).isNull();
            assertThat(KioscoContext.getCurrentKioscoRole()).isNull();
            assertThat(KioscoContext.getCurrentUsuarioId()).isNull();
            assertThat(KioscoContext.getCurrentUsuarioEmail()).isNull();
            assertThat(KioscoContext.getContext()).isNull();
        }
    }
}
