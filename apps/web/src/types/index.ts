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
export type MedioPago = 'EFECTIVO' | 'MERCADOPAGO' | 'TRANSFERENCIA';
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
