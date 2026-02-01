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
