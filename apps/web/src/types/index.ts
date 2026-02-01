export interface Categoria {
  id: string;
  nombre: string;
  descripcion?: string;
  color?: string;
  orden: number;
  activo: boolean;
}

export interface CategoriaCreate {
  nombre: string;
  descripcion?: string;
  color?: string;
  orden?: number;
}

export interface Producto {
  id: string;
  codigo?: string;
  codigoBarras?: string;
  nombre: string;
  descripcion?: string;
  categoria?: Categoria;
  precioCosto: number;
  precioVenta: number;
  margen?: number;
  stockActual: number;
  stockMinimo: number;
  stockBajo: boolean;
  esFavorito: boolean;
  activo: boolean;
}

export interface ProductoCreate {
  codigo?: string;
  codigoBarras?: string;
  nombre: string;
  descripcion?: string;
  categoriaId?: string;
  precioCosto?: number;
  precioVenta: number;
  stockActual?: number;
  stockMinimo?: number;
  esFavorito?: boolean;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message?: string;
  errors?: Record<string, string>;
}

// Venta types
export type MedioPago = 'EFECTIVO' | 'MERCADOPAGO' | 'TRANSFERENCIA' | 'FIADO';
export type EstadoVenta = 'COMPLETADA' | 'ANULADA';

export interface VentaItem {
  id: string;
  productoId: string;
  productoNombre: string;
  productoCodigo?: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Venta {
  id: string;
  numero: number;
  fecha: string;
  subtotal: number;
  descuento: number;
  total: number;
  medioPago: MedioPago;
  montoRecibido?: number;
  vuelto?: number;
  estado: EstadoVenta;
  clienteId?: string;
  clienteNombre?: string;
  esFiado?: boolean;
  items: VentaItem[];
}

export interface VentaItemCreate {
  productoId: string;
  cantidad: number;
}

export interface VentaCreate {
  items: VentaItemCreate[];
  medioPago: MedioPago;
  descuento?: number;
  montoRecibido?: number;
  clienteId?: string;
}

// Cart types (frontend only)
export interface CartItem {
  producto: Producto;
  cantidad: number;
}

// Cliente types
export type TipoDocumento = 'DNI' | 'CUIT' | 'OTRO';

export interface Cliente {
  id: string;
  nombre: string;
  documento?: string;
  tipoDocumento?: TipoDocumento;
  telefono?: string;
  email?: string;
  direccion?: string;
  notas?: string;
  activo: boolean;
}

export interface ClienteCreate {
  nombre: string;
  documento?: string;
  tipoDocumento?: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  notas?: string;
}

// Cuenta corriente types
export type TipoMovimiento = 'CARGO' | 'PAGO' | 'AJUSTE';

export interface CuentaCorriente {
  clienteId: string;
  clienteNombre: string;
  saldo: number;
  limiteCredito: number;
  disponible: number;
  ultimaActualizacion?: string;
}

export interface Movimiento {
  id: string;
  tipo: TipoMovimiento;
  monto: number;
  saldoAnterior: number;
  saldoNuevo: number;
  referenciaId?: string;
  descripcion?: string;
  fecha: string;
}

export interface Pago {
  monto: number;
  medioPago?: string;
  descripcion?: string;
}

// Configuración Fiscal types
export type CondicionIva = 'RESPONSABLE_INSCRIPTO' | 'MONOTRIBUTO' | 'EXENTO' | 'CONSUMIDOR_FINAL';
export type AmbienteAfip = 'TESTING' | 'PRODUCTION';
export type EstadoFiscal = 'SIN_CONFIGURAR' | 'CERTIFICADO_VENCIDO' | 'CERTIFICADO_POR_VENCER' | 'CONFIGURADO';

export interface ConfigFiscal {
  id: string;
  cuit: string;
  razonSocial: string;
  condicionIva: CondicionIva;
  condicionIvaDescripcion: string;
  domicilioFiscal: string;
  inicioActividades?: string;
  puntoVenta: number;
  ambiente: AmbienteAfip;
  certificadoVencimiento?: string;
  certificadoConfigurado: boolean;
  certificadoVencido: boolean;
  certificadoPorVencer: boolean;
  estado: EstadoFiscal;
}

export interface ConfigFiscalCreate {
  cuit: string;
  razonSocial: string;
  condicionIva: CondicionIva;
  domicilioFiscal: string;
  inicioActividades?: string;
  puntoVenta: number;
  ambiente?: AmbienteAfip;
}

export interface VerificacionCertificado {
  valido: boolean;
  mensaje: string;
  vencimiento?: string;
  info?: {
    subject: string;
    issuer: string;
    validFrom: string;
    validTo: string;
    serialNumber: string;
  };
}

export interface VerificacionAfip {
  conectado: boolean;
  estado: string;
  mensaje: string;
}

export interface ValidacionCuit {
  valido: boolean;
  cuitFormateado: string;
  mensaje: string;
}

// Facturacion types
export type ResultadoAfip = 'APROBADO' | 'RECHAZADO' | 'PARCIAL';

export interface Comprobante {
  id: string;
  ventaId?: string;
  ventaNumero?: number;
  clienteId?: string;
  clienteNombre?: string;
  clienteCuit?: string;
  tipoComprobante: number;
  tipoComprobanteDescripcion: string;
  tipoComprobanteLetra: string;
  puntoVenta: number;
  numero: number;
  numeroCompleto: string;
  cuitEmisor: string;
  razonSocialEmisor: string;
  condicionIvaEmisor: string;
  cuitReceptor?: string;
  condicionIvaReceptor?: string;
  importeNeto?: number;
  importeIva?: number;
  importeTotal: number;
  cae?: string;
  caeVencimiento?: string;
  caeVigente: boolean;
  resultado?: ResultadoAfip;
  aprobado: boolean;
  observaciones?: string;
  fechaEmision: string;
  createdAt: string;
}

export interface EmitirFactura {
  ventaId: string;
  clienteId?: string;
  cuitReceptor?: string;
  condicionIvaReceptor?: string;
}
