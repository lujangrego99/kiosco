import type { Categoria, CategoriaCreate, Producto, ProductoCreate, Venta, VentaCreate } from '@/types';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error de conexión' }));
    throw new Error(error.message || `Error ${response.status}`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json();
}

// Categorias API
export const categoriasApi = {
  listar: async (): Promise<Categoria[]> => {
    const response = await fetch(`${API_BASE}/categorias`);
    return handleResponse<Categoria[]>(response);
  },

  obtener: async (id: string): Promise<Categoria> => {
    const response = await fetch(`${API_BASE}/categorias/${id}`);
    return handleResponse<Categoria>(response);
  },

  crear: async (data: CategoriaCreate): Promise<Categoria> => {
    const response = await fetch(`${API_BASE}/categorias`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Categoria>(response);
  },

  actualizar: async (id: string, data: CategoriaCreate): Promise<Categoria> => {
    const response = await fetch(`${API_BASE}/categorias/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Categoria>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/categorias/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// Productos API
export const productosApi = {
  listar: async (categoriaId?: string): Promise<Producto[]> => {
    const url = categoriaId
      ? `${API_BASE}/productos?categoriaId=${categoriaId}`
      : `${API_BASE}/productos`;
    const response = await fetch(url);
    return handleResponse<Producto[]>(response);
  },

  obtener: async (id: string): Promise<Producto> => {
    const response = await fetch(`${API_BASE}/productos/${id}`);
    return handleResponse<Producto>(response);
  },

  buscar: async (query: string): Promise<Producto[]> => {
    const response = await fetch(`${API_BASE}/productos/buscar?q=${encodeURIComponent(query)}`);
    return handleResponse<Producto[]>(response);
  },

  buscarPorCodigoBarras: async (codigo: string): Promise<Producto> => {
    const response = await fetch(`${API_BASE}/productos/barcode/${encodeURIComponent(codigo)}`);
    return handleResponse<Producto>(response);
  },

  favoritos: async (): Promise<Producto[]> => {
    const response = await fetch(`${API_BASE}/productos/favoritos`);
    return handleResponse<Producto[]>(response);
  },

  stockBajo: async (): Promise<Producto[]> => {
    const response = await fetch(`${API_BASE}/productos/stock-bajo`);
    return handleResponse<Producto[]>(response);
  },

  crear: async (data: ProductoCreate): Promise<Producto> => {
    const response = await fetch(`${API_BASE}/productos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Producto>(response);
  },

  actualizar: async (id: string, data: ProductoCreate): Promise<Producto> => {
    const response = await fetch(`${API_BASE}/productos/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Producto>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/productos/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },

  toggleFavorito: async (id: string, esFavorito: boolean): Promise<Producto> => {
    const response = await fetch(`${API_BASE}/productos/${id}/favorito?esFavorito=${esFavorito}`, {
      method: 'PATCH',
    });
    return handleResponse<Producto>(response);
  },
};

// Ventas API
export const ventasApi = {
  crear: async (data: VentaCreate): Promise<Venta> => {
    const response = await fetch(`${API_BASE}/ventas`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Venta>(response);
  },

  obtener: async (id: string): Promise<Venta> => {
    const response = await fetch(`${API_BASE}/ventas/${id}`);
    return handleResponse<Venta>(response);
  },

  obtenerHoy: async (): Promise<Venta[]> => {
    const response = await fetch(`${API_BASE}/ventas/hoy`);
    return handleResponse<Venta[]>(response);
  },

  anular: async (id: string): Promise<Venta> => {
    const response = await fetch(`${API_BASE}/ventas/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<Venta>(response);
  },

  obtenerProximoNumero: async (): Promise<{ proximoNumero: number }> => {
    const response = await fetch(`${API_BASE}/ventas/ultimo-numero`);
    return handleResponse<{ proximoNumero: number }>(response);
  },
};
