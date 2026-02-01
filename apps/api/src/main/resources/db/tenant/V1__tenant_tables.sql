-- V1__tenant_tables.sql
-- Template for tenant schema tables (per-kiosco data)
-- This template is executed by TenantSchemaManager when creating a new tenant schema

-- Categorias
CREATE TABLE categorias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color VARCHAR(7),
    orden INT DEFAULT 0,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Productos
CREATE TABLE productos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50),
    codigo_barras VARCHAR(50),
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    categoria_id UUID REFERENCES categorias(id),

    precio_costo DECIMAL(12,2) DEFAULT 0,
    precio_venta DECIMAL(12,2) NOT NULL,

    stock_actual DECIMAL(10,2) DEFAULT 0,
    stock_minimo DECIMAL(10,2) DEFAULT 0,

    es_favorito BOOLEAN DEFAULT false,
    activo BOOLEAN DEFAULT true,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Clientes
CREATE TABLE clientes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(200) NOT NULL,
    documento VARCHAR(20),
    tipo_documento VARCHAR(10),
    telefono VARCHAR(50),
    email VARCHAR(200),
    direccion TEXT,
    notas TEXT,
    activo BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Cuenta corriente
CREATE TABLE cuenta_corriente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    saldo DECIMAL(12,2) DEFAULT 0,
    limite_credito DECIMAL(12,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Ventas
CREATE TABLE ventas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero INT NOT NULL,
    fecha TIMESTAMP DEFAULT NOW(),

    cliente_id UUID REFERENCES clientes(id),
    es_fiado BOOLEAN DEFAULT false,

    subtotal DECIMAL(12,2) NOT NULL,
    descuento DECIMAL(12,2) DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,

    medio_pago VARCHAR(20) NOT NULL,
    monto_recibido DECIMAL(12,2),
    vuelto DECIMAL(12,2),

    estado VARCHAR(20) DEFAULT 'COMPLETADA',

    created_at TIMESTAMP DEFAULT NOW()
);

-- Detalle de ventas
CREATE TABLE venta_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venta_id UUID REFERENCES ventas(id) ON DELETE CASCADE,
    producto_id UUID REFERENCES productos(id),

    cantidad DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,

    producto_nombre VARCHAR(200) NOT NULL,
    producto_codigo VARCHAR(50)
);

-- Movimientos de cuenta corriente
CREATE TABLE cuenta_movimientos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    tipo VARCHAR(20) NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    saldo_anterior DECIMAL(12,2) NOT NULL,
    saldo_nuevo DECIMAL(12,2) NOT NULL,
    referencia_id UUID,
    descripcion TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Indices
CREATE INDEX idx_productos_codigo ON productos(codigo);
CREATE INDEX idx_productos_codigo_barras ON productos(codigo_barras);
CREATE INDEX idx_productos_nombre ON productos(nombre);
CREATE INDEX idx_productos_categoria ON productos(categoria_id);
CREATE INDEX idx_productos_favorito ON productos(es_favorito) WHERE es_favorito = true;
CREATE INDEX idx_categorias_activo ON categorias(activo) WHERE activo = true;
CREATE INDEX idx_clientes_documento ON clientes(documento);
CREATE INDEX idx_clientes_nombre ON clientes(nombre);
CREATE INDEX idx_clientes_activo ON clientes(activo) WHERE activo = true;
CREATE INDEX idx_cuenta_corriente_cliente ON cuenta_corriente(cliente_id);
CREATE INDEX idx_ventas_fecha ON ventas(fecha);
CREATE INDEX idx_ventas_estado ON ventas(estado);
CREATE INDEX idx_ventas_numero ON ventas(numero);
CREATE INDEX idx_ventas_cliente ON ventas(cliente_id);
CREATE INDEX idx_ventas_fiado ON ventas(es_fiado) WHERE es_fiado = true;
CREATE INDEX idx_venta_items_venta ON venta_items(venta_id);
CREATE INDEX idx_venta_items_producto ON venta_items(producto_id);
CREATE INDEX idx_cuenta_movimientos_cliente ON cuenta_movimientos(cliente_id);

-- Secuencia para numeros de venta
CREATE SEQUENCE IF NOT EXISTS ventas_numero_seq START WITH 1 INCREMENT BY 1;
