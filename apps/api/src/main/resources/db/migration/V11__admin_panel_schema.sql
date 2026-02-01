-- V11: Admin Panel (SaaS management)
-- Tables for subscription plans, usage tracking, superadmins, and feature flags

-- Planes de suscripcion
CREATE TABLE planes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    precio_mensual DECIMAL(10,2),
    precio_anual DECIMAL(10,2),
    max_productos INT,
    max_usuarios INT,
    max_ventas_mes INT,
    tiene_facturacion BOOLEAN DEFAULT false,
    tiene_reportes_avanzados BOOLEAN DEFAULT false,
    tiene_multi_kiosco BOOLEAN DEFAULT false,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Suscripciones
CREATE TABLE suscripciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kiosco_id UUID NOT NULL REFERENCES kioscos(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES planes(id),
    estado VARCHAR(20) DEFAULT 'ACTIVA',  -- ACTIVA, CANCELADA, VENCIDA, TRIAL
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    periodo VARCHAR(10),  -- MENSUAL, ANUAL
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Uso mensual (para billing y limites)
CREATE TABLE uso_mensual (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kiosco_id UUID NOT NULL REFERENCES kioscos(id) ON DELETE CASCADE,
    mes DATE NOT NULL,  -- primer dia del mes
    cantidad_ventas INT DEFAULT 0,
    cantidad_productos INT DEFAULT 0,
    cantidad_usuarios INT DEFAULT 0,
    monto_total_ventas DECIMAL(14,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(kiosco_id, mes)
);

-- Superadmins (sistema)
CREATE TABLE superadmins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(usuario_id)
);

-- Feature flags
CREATE TABLE feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(100) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    habilitado_global BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Feature flags por kiosco (override del global)
CREATE TABLE feature_flags_kiosco (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_flag_id UUID NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    kiosco_id UUID NOT NULL REFERENCES kioscos(id) ON DELETE CASCADE,
    habilitado BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(feature_flag_id, kiosco_id)
);

-- Indexes
CREATE INDEX idx_suscripciones_kiosco ON suscripciones(kiosco_id);
CREATE INDEX idx_suscripciones_plan ON suscripciones(plan_id);
CREATE INDEX idx_suscripciones_estado ON suscripciones(estado);
CREATE INDEX idx_uso_mensual_kiosco ON uso_mensual(kiosco_id);
CREATE INDEX idx_uso_mensual_mes ON uso_mensual(mes);
CREATE INDEX idx_superadmins_usuario ON superadmins(usuario_id);
CREATE INDEX idx_feature_flags_key ON feature_flags(key);
CREATE INDEX idx_feature_flags_kiosco_flag ON feature_flags_kiosco(feature_flag_id);
CREATE INDEX idx_feature_flags_kiosco_kiosco ON feature_flags_kiosco(kiosco_id);

-- Insert default plans
INSERT INTO planes (nombre, descripcion, precio_mensual, precio_anual, max_productos, max_usuarios, max_ventas_mes, tiene_facturacion, tiene_reportes_avanzados, tiene_multi_kiosco)
VALUES
    ('free', 'Plan gratuito con funciones basicas', 0, 0, 100, 1, 500, false, false, false),
    ('basic', 'Plan basico para kioscos pequeños', 4999, 49990, 500, 3, 2000, true, false, false),
    ('pro', 'Plan profesional con todas las funciones', 9999, 99990, NULL, 10, NULL, true, true, true);

-- Insert default feature flags
INSERT INTO feature_flags (key, nombre, descripcion, habilitado_global)
VALUES
    ('nueva_facturacion', 'Nueva Facturacion AFIP', 'Sistema de facturacion AFIP renovado', false),
    ('reportes_ia', 'Reportes con IA', 'Insights automaticos usando IA', false),
    ('modo_oscuro', 'Modo Oscuro', 'Tema oscuro en la interfaz', true),
    ('notificaciones_push', 'Notificaciones Push', 'Notificaciones en tiempo real', false),
    ('integracion_whatsapp', 'Integracion WhatsApp', 'Envio de tickets por WhatsApp', true);
