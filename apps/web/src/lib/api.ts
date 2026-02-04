import type { AgregarKioscoACadena, Cadena, CadenaCreate, CadenaMember, CadenaMemberCreate, Categoria, CategoriaCreate, Cliente, ClienteCreate, Comparativo, Comprobante, ConfigFiscal, ConfigFiscalCreate, ConfigImpresora, ConfigImpresoraCreate, ConfigPagos, ConfigPagosCreate, CuentaCorriente, EmitirFactura, GenerarOrdenDesdeSugerencias, HistorialPrecio, Insight, KioscoResumen, Lote, LoteCreate, MetodosPagoHabilitados, Movimiento, OrdenCompra, OrdenCompraCreate, Pago, PaymentStatus, PreferenciaResponse, Producto, ProductoAbc, ProductoCreate, ProductoMasVendido, ProductoProveedor, ProductoProveedorCreate, ProductoSinMovimiento, Proveedor, ProveedorCreate, ProyeccionVentas, QrInteroperableResponse, QrResponse, RankingKiosco, RecepcionOrden, RentabilidadCategoria, RentabilidadProducto, ReporteConsolidado, ResumenCaja, ResumenDashboard, StockConsolidado, SugerenciaCompra, Tendencia, TendenciaProducto, TicketPruebaResponse, TicketVentaResponse, ValidacionCuit, VencimientoResumen, Venta, VentaCreate, VentaDiaria, VentaPorHora, VentaRango, VerificacionAfip, VerificacionCertificado, VerificacionMp } from '@/types';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

function getAuthHeaders(): HeadersInit {
  if (typeof window === 'undefined') return {};
  const token = localStorage.getItem('kiosco_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function getHeaders(contentType = true): HeadersInit {
  const auth = getAuthHeaders();
  if (contentType) {
    return { ...auth, 'Content-Type': 'application/json' };
  }
  return auth;
}

// Wrapper for authenticated fetch - all API calls should use this
async function authFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const headers = options.headers instanceof Headers
    ? Object.fromEntries(options.headers.entries())
    : options.headers || {};

  return fetch(url, {
    ...options,
    headers: {
      ...getAuthHeaders(),
      ...headers,
    },
  });
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (response.status === 401 || response.status === 403) {
    // Redirect to login on auth errors (only in browser)
    if (typeof window !== 'undefined') {
      localStorage.removeItem('kiosco_token');
      localStorage.removeItem('kiosco_usuario');
      localStorage.removeItem('kiosco_kiosco');
      localStorage.removeItem('kiosco_rol');
      window.location.href = '/login';
    }
    throw new Error('Sesión expirada. Por favor, inicia sesión nuevamente.');
  }
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
    const response = await authFetch(`${API_BASE}/categorias`, { headers: getAuthHeaders() });
    return handleResponse<Categoria[]>(response);
  },

  obtener: async (id: string): Promise<Categoria> => {
    const response = await authFetch(`${API_BASE}/categorias/${id}`, { headers: getAuthHeaders() });
    return handleResponse<Categoria>(response);
  },

  crear: async (data: CategoriaCreate): Promise<Categoria> => {
    const response = await authFetch(`${API_BASE}/categorias`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Categoria>(response);
  },

  actualizar: async (id: string, data: CategoriaCreate): Promise<Categoria> => {
    const response = await authFetch(`${API_BASE}/categorias/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Categoria>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/categorias/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
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
    const response = await fetch(url, { headers: getAuthHeaders() });
    return handleResponse<Producto[]>(response);
  },

  obtener: async (id: string): Promise<Producto> => {
    const response = await authFetch(`${API_BASE}/productos/${id}`, { headers: getAuthHeaders() });
    return handleResponse<Producto>(response);
  },

  buscar: async (query: string): Promise<Producto[]> => {
    const response = await authFetch(`${API_BASE}/productos/buscar?q=${encodeURIComponent(query)}`, { headers: getAuthHeaders() });
    return handleResponse<Producto[]>(response);
  },

  buscarPorCodigoBarras: async (codigo: string): Promise<Producto> => {
    const response = await authFetch(`${API_BASE}/productos/barcode/${encodeURIComponent(codigo)}`, { headers: getAuthHeaders() });
    return handleResponse<Producto>(response);
  },

  favoritos: async (): Promise<Producto[]> => {
    const response = await authFetch(`${API_BASE}/productos/favoritos`, { headers: getAuthHeaders() });
    return handleResponse<Producto[]>(response);
  },

  stockBajo: async (): Promise<Producto[]> => {
    const response = await authFetch(`${API_BASE}/productos/stock-bajo`, { headers: getAuthHeaders() });
    return handleResponse<Producto[]>(response);
  },

  crear: async (data: ProductoCreate): Promise<Producto> => {
    const response = await authFetch(`${API_BASE}/productos`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Producto>(response);
  },

  actualizar: async (id: string, data: ProductoCreate): Promise<Producto> => {
    const response = await authFetch(`${API_BASE}/productos/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Producto>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/productos/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    return handleResponse<void>(response);
  },

  toggleFavorito: async (id: string, esFavorito: boolean): Promise<Producto> => {
    const response = await authFetch(`${API_BASE}/productos/${id}/favorito?esFavorito=${esFavorito}`, {
      method: 'PATCH',
      headers: getAuthHeaders(),
    });
    return handleResponse<Producto>(response);
  },
};

// Ventas API
export const ventasApi = {
  crear: async (data: VentaCreate): Promise<Venta> => {
    const response = await authFetch(`${API_BASE}/ventas`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Venta>(response);
  },

  obtener: async (id: string): Promise<Venta> => {
    const response = await authFetch(`${API_BASE}/ventas/${id}`);
    return handleResponse<Venta>(response);
  },

  obtenerHoy: async (): Promise<Venta[]> => {
    const response = await authFetch(`${API_BASE}/ventas/hoy`);
    return handleResponse<Venta[]>(response);
  },

  anular: async (id: string): Promise<Venta> => {
    const response = await authFetch(`${API_BASE}/ventas/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<Venta>(response);
  },

  obtenerProximoNumero: async (): Promise<{ proximoNumero: number }> => {
    const response = await authFetch(`${API_BASE}/ventas/ultimo-numero`);
    return handleResponse<{ proximoNumero: number }>(response);
  },
};

// Clientes API
export const clientesApi = {
  listar: async (): Promise<Cliente[]> => {
    const response = await authFetch(`${API_BASE}/clientes`);
    return handleResponse<Cliente[]>(response);
  },

  obtener: async (id: string): Promise<Cliente> => {
    const response = await authFetch(`${API_BASE}/clientes/${id}`);
    return handleResponse<Cliente>(response);
  },

  buscar: async (query: string): Promise<Cliente[]> => {
    const response = await authFetch(`${API_BASE}/clientes/buscar?q=${encodeURIComponent(query)}`);
    return handleResponse<Cliente[]>(response);
  },

  crear: async (data: ClienteCreate): Promise<Cliente> => {
    const response = await authFetch(`${API_BASE}/clientes`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Cliente>(response);
  },

  actualizar: async (id: string, data: ClienteCreate): Promise<Cliente> => {
    const response = await authFetch(`${API_BASE}/clientes/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Cliente>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/clientes/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// Cuenta Corriente API
export const cuentaCorrienteApi = {
  obtenerCuenta: async (clienteId: string): Promise<CuentaCorriente> => {
    const response = await authFetch(`${API_BASE}/clientes/${clienteId}/cuenta`);
    return handleResponse<CuentaCorriente>(response);
  },

  obtenerMovimientos: async (clienteId: string): Promise<Movimiento[]> => {
    const response = await authFetch(`${API_BASE}/clientes/${clienteId}/movimientos`);
    return handleResponse<Movimiento[]>(response);
  },

  registrarPago: async (clienteId: string, pago: Pago): Promise<Movimiento> => {
    const response = await authFetch(`${API_BASE}/clientes/${clienteId}/pago`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(pago),
    });
    return handleResponse<Movimiento>(response);
  },

  listarDeudores: async (): Promise<CuentaCorriente[]> => {
    const response = await authFetch(`${API_BASE}/cuenta-corriente/deudores`);
    return handleResponse<CuentaCorriente[]>(response);
  },

  verificarPuedeFiar: async (clienteId: string, monto: number): Promise<{ puede: boolean; saldoActual: number; limiteCredito: number; disponible: number }> => {
    const response = await authFetch(`${API_BASE}/clientes/${clienteId}/puede-fiar?monto=${monto}`);
    return handleResponse<{ puede: boolean; saldoActual: number; limiteCredito: number; disponible: number }>(response);
  },
};

// Configuración Fiscal API
export const configFiscalApi = {
  obtener: async (): Promise<ConfigFiscal | null> => {
    const response = await authFetch(`${API_BASE}/config/fiscal`);
    if (response.status === 204) {
      return null;
    }
    return handleResponse<ConfigFiscal>(response);
  },

  guardar: async (data: ConfigFiscalCreate): Promise<ConfigFiscal> => {
    const response = await authFetch(`${API_BASE}/config/fiscal`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<ConfigFiscal>(response);
  },

  subirCertificado: async (crt: File, key: File): Promise<ConfigFiscal> => {
    const formData = new FormData();
    formData.append('crt', crt);
    formData.append('key', key);

    const response = await authFetch(`${API_BASE}/config/fiscal/certificado`, {
      method: 'POST',
      body: formData,
    });
    return handleResponse<ConfigFiscal>(response);
  },

  verificarCertificado: async (): Promise<VerificacionCertificado> => {
    const response = await authFetch(`${API_BASE}/config/fiscal/certificado/verificar`);
    return handleResponse<VerificacionCertificado>(response);
  },

  verificarConexionAfip: async (): Promise<VerificacionAfip> => {
    const response = await authFetch(`${API_BASE}/config/fiscal/verificar`);
    return handleResponse<VerificacionAfip>(response);
  },

  validarCuit: async (cuit: string): Promise<ValidacionCuit> => {
    const response = await authFetch(`${API_BASE}/config/fiscal/validar-cuit?cuit=${encodeURIComponent(cuit)}`);
    return handleResponse<ValidacionCuit>(response);
  },
};

// Facturacion API
export const facturacionApi = {
  emitir: async (data: EmitirFactura): Promise<Comprobante> => {
    const response = await authFetch(`${API_BASE}/facturacion/emitir`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Comprobante>(response);
  },

  obtenerPorVenta: async (ventaId: string): Promise<Comprobante | null> => {
    const response = await authFetch(`${API_BASE}/facturacion/venta/${ventaId}`);
    if (response.status === 404) {
      return null;
    }
    return handleResponse<Comprobante>(response);
  },

  obtener: async (id: string): Promise<Comprobante> => {
    const response = await authFetch(`${API_BASE}/facturacion/${id}`);
    return handleResponse<Comprobante>(response);
  },

  listar: async (params?: { desde?: string; hasta?: string; tipo?: number }): Promise<Comprobante[]> => {
    const queryParams = new URLSearchParams();
    if (params?.desde) queryParams.append('desde', params.desde);
    if (params?.hasta) queryParams.append('hasta', params.hasta);
    if (params?.tipo) queryParams.append('tipo', params.tipo.toString());

    const url = queryParams.toString()
      ? `${API_BASE}/facturacion/comprobantes?${queryParams}`
      : `${API_BASE}/facturacion/comprobantes`;

    const response = await authFetch(url);
    return handleResponse<Comprobante[]>(response);
  },

  getUltimoNumero: async (tipoComprobante: number, puntoVenta: number): Promise<{ ultimoNumero: number; proximoNumero: number }> => {
    const response = await fetch(
      `${API_BASE}/facturacion/ultimo-numero?tipoComprobante=${tipoComprobante}&puntoVenta=${puntoVenta}`
    );
    return handleResponse<{ ultimoNumero: number; proximoNumero: number }>(response);
  },

  descargarPdf: async (id: string): Promise<Blob> => {
    const response = await authFetch(`${API_BASE}/facturacion/${id}/pdf`);
    if (!response.ok) {
      throw new Error('Error al descargar PDF');
    }
    return response.blob();
  },

  getPdfUrl: (id: string): string => {
    return `${API_BASE}/facturacion/${id}/pdf/preview`;
  },
};

// Configuración de Pagos API
export const configPagosApi = {
  obtener: async (): Promise<ConfigPagos> => {
    const response = await authFetch(`${API_BASE}/config/pagos`);
    return handleResponse<ConfigPagos>(response);
  },

  guardar: async (data: ConfigPagosCreate): Promise<ConfigPagos> => {
    const response = await authFetch(`${API_BASE}/config/pagos`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<ConfigPagos>(response);
  },

  actualizar: async (data: ConfigPagosCreate): Promise<ConfigPagos> => {
    const response = await authFetch(`${API_BASE}/config/pagos`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<ConfigPagos>(response);
  },

  actualizarMetodos: async (data: ConfigPagosCreate): Promise<ConfigPagos> => {
    const response = await authFetch(`${API_BASE}/config/pagos/metodos`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<ConfigPagos>(response);
  },

  obtenerMetodosHabilitados: async (): Promise<MetodosPagoHabilitados> => {
    const response = await authFetch(`${API_BASE}/config/pagos/metodos-habilitados`);
    return handleResponse<MetodosPagoHabilitados>(response);
  },

  verificarMercadoPago: async (): Promise<VerificacionMp> => {
    const response = await authFetch(`${API_BASE}/config/pagos/verificar-mp`);
    return handleResponse<VerificacionMp>(response);
  },
};

// Pagos API (payment operations)
export const pagosApi = {
  // Mercado Pago
  crearPreferencia: async (monto: number, descripcion: string, externalReference: string): Promise<PreferenciaResponse> => {
    const response = await authFetch(`${API_BASE}/pagos/mp/preferencia`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ monto, descripcion, externalReference }),
    });
    return handleResponse<PreferenciaResponse>(response);
  },

  crearQrMp: async (monto: number, descripcion: string): Promise<QrResponse> => {
    const response = await authFetch(`${API_BASE}/pagos/mp/qr`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ monto, descripcion }),
    });
    return handleResponse<QrResponse>(response);
  },

  verificarPago: async (paymentId: string): Promise<PaymentStatus> => {
    const response = await authFetch(`${API_BASE}/pagos/mp/status/${paymentId}`);
    return handleResponse<PaymentStatus>(response);
  },

  verificarPagoPorPreferencia: async (preferenceId: string): Promise<PaymentStatus> => {
    const response = await authFetch(`${API_BASE}/pagos/mp/status/preference/${preferenceId}`);
    return handleResponse<PaymentStatus>(response);
  },

  // QR Interoperable
  generarQr: async (monto: number, descripcion: string): Promise<QrInteroperableResponse> => {
    const response = await authFetch(`${API_BASE}/pagos/qr/generar`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ monto, descripcion }),
    });
    return handleResponse<QrInteroperableResponse>(response);
  },

  obtenerQrEstatico: async (): Promise<QrInteroperableResponse> => {
    const response = await authFetch(`${API_BASE}/pagos/qr/estatico`);
    return handleResponse<QrInteroperableResponse>(response);
  },
};

// Lotes API
export const lotesApi = {
  listarPorProducto: async (productoId: string): Promise<Lote[]> => {
    const response = await authFetch(`${API_BASE}/productos/${productoId}/lotes`);
    return handleResponse<Lote[]>(response);
  },

  obtener: async (id: string): Promise<Lote> => {
    const response = await authFetch(`${API_BASE}/lotes/${id}`);
    return handleResponse<Lote>(response);
  },

  crear: async (productoId: string, data: LoteCreate): Promise<Lote> => {
    const response = await authFetch(`${API_BASE}/productos/${productoId}/lotes`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Lote>(response);
  },

  actualizar: async (id: string, data: LoteCreate): Promise<Lote> => {
    const response = await authFetch(`${API_BASE}/lotes/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Lote>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/lotes/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// Vencimientos API
export const vencimientosApi = {
  proximosAVencer: async (dias: number = 7): Promise<Lote[]> => {
    const response = await authFetch(`${API_BASE}/vencimientos/proximos?dias=${dias}`);
    return handleResponse<Lote[]>(response);
  },

  vencidos: async (): Promise<Lote[]> => {
    const response = await authFetch(`${API_BASE}/vencimientos/vencidos`);
    return handleResponse<Lote[]>(response);
  },

  resumen: async (): Promise<VencimientoResumen> => {
    const response = await authFetch(`${API_BASE}/vencimientos/resumen`);
    return handleResponse<VencimientoResumen>(response);
  },
};

// Proveedores API
export const proveedoresApi = {
  listar: async (): Promise<Proveedor[]> => {
    const response = await authFetch(`${API_BASE}/proveedores`);
    return handleResponse<Proveedor[]>(response);
  },

  obtener: async (id: string): Promise<Proveedor> => {
    const response = await authFetch(`${API_BASE}/proveedores/${id}`);
    return handleResponse<Proveedor>(response);
  },

  buscar: async (query: string): Promise<Proveedor[]> => {
    const response = await authFetch(`${API_BASE}/proveedores/buscar?q=${encodeURIComponent(query)}`);
    return handleResponse<Proveedor[]>(response);
  },

  crear: async (data: ProveedorCreate): Promise<Proveedor> => {
    const response = await authFetch(`${API_BASE}/proveedores`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Proveedor>(response);
  },

  actualizar: async (id: string, data: ProveedorCreate): Promise<Proveedor> => {
    const response = await authFetch(`${API_BASE}/proveedores/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Proveedor>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/proveedores/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },

  listarProductos: async (proveedorId: string): Promise<ProductoProveedor[]> => {
    const response = await authFetch(`${API_BASE}/proveedores/${proveedorId}/productos`);
    return handleResponse<ProductoProveedor[]>(response);
  },
};

// Producto-Proveedor API
export const productoProveedorApi = {
  listarPorProducto: async (productoId: string): Promise<ProductoProveedor[]> => {
    const response = await authFetch(`${API_BASE}/productos/${productoId}/proveedores`);
    return handleResponse<ProductoProveedor[]>(response);
  },

  asociar: async (productoId: string, data: ProductoProveedorCreate): Promise<ProductoProveedor> => {
    const response = await authFetch(`${API_BASE}/productos/${productoId}/proveedores`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<ProductoProveedor>(response);
  },

  actualizar: async (id: string, data: ProductoProveedorCreate): Promise<ProductoProveedor> => {
    const response = await authFetch(`${API_BASE}/producto-proveedor/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<ProductoProveedor>(response);
  },

  actualizarPrecio: async (id: string, precio: number): Promise<ProductoProveedor> => {
    const response = await authFetch(`${API_BASE}/producto-proveedor/${id}/precio?precio=${precio}`, {
      method: 'PATCH',
    });
    return handleResponse<ProductoProveedor>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/producto-proveedor/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },

  historialPrecios: async (id: string): Promise<HistorialPrecio[]> => {
    const response = await authFetch(`${API_BASE}/producto-proveedor/${id}/historial-precios`);
    return handleResponse<HistorialPrecio[]>(response);
  },
};

// Ordenes de Compra API
export const ordenesCompraApi = {
  listar: async (params?: { estado?: string; proveedorId?: string }): Promise<OrdenCompra[]> => {
    const queryParams = new URLSearchParams();
    if (params?.estado) queryParams.append('estado', params.estado);
    if (params?.proveedorId) queryParams.append('proveedorId', params.proveedorId);

    const url = queryParams.toString()
      ? `${API_BASE}/ordenes-compra?${queryParams}`
      : `${API_BASE}/ordenes-compra`;

    const response = await authFetch(url);
    return handleResponse<OrdenCompra[]>(response);
  },

  obtener: async (id: string): Promise<OrdenCompra> => {
    const response = await authFetch(`${API_BASE}/ordenes-compra/${id}`);
    return handleResponse<OrdenCompra>(response);
  },

  crear: async (data: OrdenCompraCreate): Promise<OrdenCompra> => {
    const response = await authFetch(`${API_BASE}/ordenes-compra`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<OrdenCompra>(response);
  },

  actualizar: async (id: string, data: OrdenCompraCreate): Promise<OrdenCompra> => {
    const response = await authFetch(`${API_BASE}/ordenes-compra/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<OrdenCompra>(response);
  },

  enviar: async (id: string): Promise<OrdenCompra> => {
    const response = await authFetch(`${API_BASE}/ordenes-compra/${id}/enviar`, {
      method: 'POST',
    });
    return handleResponse<OrdenCompra>(response);
  },

  recibir: async (id: string, data: RecepcionOrden): Promise<OrdenCompra> => {
    const response = await authFetch(`${API_BASE}/ordenes-compra/${id}/recibir`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<OrdenCompra>(response);
  },

  cancelar: async (id: string): Promise<OrdenCompra> => {
    const response = await authFetch(`${API_BASE}/ordenes-compra/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<OrdenCompra>(response);
  },
};

// Sugerencias de Compra API
export const sugerenciasCompraApi = {
  obtener: async (params?: { tipo?: string; dias?: number }): Promise<SugerenciaCompra[]> => {
    const queryParams = new URLSearchParams();
    if (params?.tipo) queryParams.append('tipo', params.tipo);
    if (params?.dias) queryParams.append('dias', params.dias.toString());

    const url = queryParams.toString()
      ? `${API_BASE}/sugerencias-compra?${queryParams}`
      : `${API_BASE}/sugerencias-compra`;

    const response = await authFetch(url);
    return handleResponse<SugerenciaCompra[]>(response);
  },

  generarOrden: async (data: GenerarOrdenDesdeSugerencias): Promise<OrdenCompra> => {
    const response = await authFetch(`${API_BASE}/sugerencias-compra/generar-orden`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<OrdenCompra>(response);
  },
};

// Reportes API
export const reportesApi = {
  getDashboard: async (): Promise<ResumenDashboard> => {
    const response = await authFetch(`${API_BASE}/reportes/dashboard`);
    return handleResponse<ResumenDashboard>(response);
  },

  getVentaDiaria: async (fecha: string): Promise<VentaDiaria> => {
    const response = await authFetch(`${API_BASE}/reportes/ventas/diario?fecha=${fecha}`);
    return handleResponse<VentaDiaria>(response);
  },

  getVentasRango: async (desde: string, hasta: string): Promise<VentaRango> => {
    const response = await authFetch(`${API_BASE}/reportes/ventas/rango?desde=${desde}&hasta=${hasta}`);
    return handleResponse<VentaRango>(response);
  },

  getVentasPorHora: async (fecha: string): Promise<VentaPorHora[]> => {
    const response = await authFetch(`${API_BASE}/reportes/ventas/por-hora?fecha=${fecha}`);
    return handleResponse<VentaPorHora[]>(response);
  },

  getVentasPorMedioPago: async (desde: string, hasta: string): Promise<Record<string, number>> => {
    const response = await authFetch(`${API_BASE}/reportes/ventas/por-medio-pago?desde=${desde}&hasta=${hasta}`);
    return handleResponse<Record<string, number>>(response);
  },

  getProductosMasVendidos: async (desde: string, hasta: string, limit: number = 20): Promise<ProductoMasVendido[]> => {
    const response = await authFetch(`${API_BASE}/reportes/productos/mas-vendidos?desde=${desde}&hasta=${hasta}&limit=${limit}`);
    return handleResponse<ProductoMasVendido[]>(response);
  },

  getProductosSinMovimiento: async (dias: number = 30): Promise<ProductoSinMovimiento[]> => {
    const response = await authFetch(`${API_BASE}/reportes/productos/sin-movimiento?dias=${dias}`);
    return handleResponse<ProductoSinMovimiento[]>(response);
  },

  getResumenCaja: async (fecha: string): Promise<ResumenCaja> => {
    const response = await authFetch(`${API_BASE}/reportes/caja/resumen?fecha=${fecha}`);
    return handleResponse<ResumenCaja>(response);
  },

  exportarVentasCSV: async (desde: string, hasta: string): Promise<Blob> => {
    const response = await authFetch(`${API_BASE}/reportes/ventas/exportar?desde=${desde}&hasta=${hasta}`);
    if (!response.ok) {
      throw new Error('Error al exportar');
    }
    return response.blob();
  },

  exportarProductosMasVendidosCSV: async (desde: string, hasta: string, limit: number = 100): Promise<Blob> => {
    const response = await authFetch(`${API_BASE}/reportes/productos/mas-vendidos/exportar?desde=${desde}&hasta=${hasta}&limit=${limit}`);
    if (!response.ok) {
      throw new Error('Error al exportar');
    }
    return response.blob();
  },

  // Advanced Reports (Spec 016)
  getRentabilidadProductos: async (desde: string, hasta: string): Promise<RentabilidadProducto[]> => {
    const response = await authFetch(`${API_BASE}/reportes/rentabilidad/productos?desde=${desde}&hasta=${hasta}`);
    return handleResponse<RentabilidadProducto[]>(response);
  },

  getRentabilidadCategorias: async (desde: string, hasta: string): Promise<RentabilidadCategoria[]> => {
    const response = await authFetch(`${API_BASE}/reportes/rentabilidad/categorias?desde=${desde}&hasta=${hasta}`);
    return handleResponse<RentabilidadCategoria[]>(response);
  },

  getTendenciasVentas: async (meses: number = 6): Promise<Tendencia[]> => {
    const response = await authFetch(`${API_BASE}/reportes/tendencias/ventas?meses=${meses}`);
    return handleResponse<Tendencia[]>(response);
  },

  getTendenciaProducto: async (productoId: string, meses: number = 6): Promise<TendenciaProducto> => {
    const response = await authFetch(`${API_BASE}/reportes/tendencias/productos/${productoId}?meses=${meses}`);
    return handleResponse<TendenciaProducto>(response);
  },

  getComparativoPeriodos: async (
    periodo1Desde: string,
    periodo1Hasta: string,
    periodo2Desde: string,
    periodo2Hasta: string
  ): Promise<Comparativo> => {
    const params = new URLSearchParams({
      periodo1Desde,
      periodo1Hasta,
      periodo2Desde,
      periodo2Hasta,
    });
    const response = await authFetch(`${API_BASE}/reportes/comparativo/periodos?${params}`);
    return handleResponse<Comparativo>(response);
  },

  getAnalisisABC: async (desde: string, hasta: string): Promise<ProductoAbc[]> => {
    const response = await authFetch(`${API_BASE}/reportes/abc/productos?desde=${desde}&hasta=${hasta}`);
    return handleResponse<ProductoAbc[]>(response);
  },

  getProyeccionVentas: async (dias: number = 30): Promise<ProyeccionVentas> => {
    const response = await authFetch(`${API_BASE}/reportes/proyeccion/ventas?dias=${dias}`);
    return handleResponse<ProyeccionVentas>(response);
  },

  getInsights: async (): Promise<Insight[]> => {
    const response = await authFetch(`${API_BASE}/reportes/insights`);
    return handleResponse<Insight[]>(response);
  },
};

// Cadenas API (Multi-Kiosco)
export const cadenasApi = {
  listar: async (): Promise<Cadena[]> => {
    const response = await authFetch(`${API_BASE}/cadenas`);
    return handleResponse<Cadena[]>(response);
  },

  obtener: async (id: string): Promise<Cadena> => {
    const response = await authFetch(`${API_BASE}/cadenas/${id}`);
    return handleResponse<Cadena>(response);
  },

  crear: async (data: CadenaCreate): Promise<Cadena> => {
    const response = await authFetch(`${API_BASE}/cadenas`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Cadena>(response);
  },

  actualizar: async (id: string, data: CadenaCreate): Promise<Cadena> => {
    const response = await authFetch(`${API_BASE}/cadenas/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Cadena>(response);
  },

  // Kioscos management
  listarKioscos: async (cadenaId: string): Promise<KioscoResumen[]> => {
    const response = await authFetch(`${API_BASE}/cadenas/${cadenaId}/kioscos`);
    return handleResponse<KioscoResumen[]>(response);
  },

  agregarKiosco: async (cadenaId: string, data: AgregarKioscoACadena): Promise<void> => {
    const response = await authFetch(`${API_BASE}/cadenas/${cadenaId}/kioscos`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<void>(response);
  },

  quitarKiosco: async (cadenaId: string, kioscoId: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/cadenas/${cadenaId}/kioscos/${kioscoId}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },

  // Reports
  getReporteVentas: async (
    cadenaId: string,
    desde: string,
    hasta: string
  ): Promise<ReporteConsolidado> => {
    const response = await fetch(
      `${API_BASE}/cadenas/${cadenaId}/reportes/ventas?desde=${desde}&hasta=${hasta}`
    );
    return handleResponse<ReporteConsolidado>(response);
  },

  getReportePorKiosco: async (
    cadenaId: string,
    desde: string,
    hasta: string
  ): Promise<ReporteConsolidado> => {
    const response = await fetch(
      `${API_BASE}/cadenas/${cadenaId}/reportes/por-kiosco?desde=${desde}&hasta=${hasta}`
    );
    return handleResponse<ReporteConsolidado>(response);
  },

  getRanking: async (cadenaId: string): Promise<RankingKiosco[]> => {
    const response = await authFetch(`${API_BASE}/cadenas/${cadenaId}/reportes/ranking`);
    return handleResponse<RankingKiosco[]>(response);
  },

  getStockConsolidado: async (cadenaId: string): Promise<StockConsolidado[]> => {
    const response = await authFetch(`${API_BASE}/cadenas/${cadenaId}/stock`);
    return handleResponse<StockConsolidado[]>(response);
  },

  // Members management
  listarMembers: async (cadenaId: string): Promise<CadenaMember[]> => {
    const response = await authFetch(`${API_BASE}/cadenas/${cadenaId}/members`);
    return handleResponse<CadenaMember[]>(response);
  },

  agregarMember: async (cadenaId: string, data: CadenaMemberCreate): Promise<CadenaMember> => {
    const response = await authFetch(`${API_BASE}/cadenas/${cadenaId}/members`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<CadenaMember>(response);
  },

  quitarMember: async (cadenaId: string, memberId: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/cadenas/${cadenaId}/members/${memberId}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// Impresora (Printer) API
export const impresoraApi = {
  obtenerConfig: async (): Promise<ConfigImpresora> => {
    const response = await authFetch(`${API_BASE}/config/impresora`);
    return handleResponse<ConfigImpresora>(response);
  },

  guardarConfig: async (data: ConfigImpresoraCreate): Promise<ConfigImpresora> => {
    const response = await authFetch(`${API_BASE}/config/impresora`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<ConfigImpresora>(response);
  },

  imprimirPrueba: async (): Promise<TicketPruebaResponse> => {
    const response = await authFetch(`${API_BASE}/impresora/test`, {
      method: 'POST',
    });
    return handleResponse<TicketPruebaResponse>(response);
  },

  imprimirVenta: async (ventaId: string): Promise<TicketVentaResponse> => {
    const response = await authFetch(`${API_BASE}/impresora/imprimir/venta/${ventaId}`, {
      method: 'POST',
    });
    return handleResponse<TicketVentaResponse>(response);
  },

  obtenerTicketTexto: async (ventaId: string): Promise<{ ventaId: string; ticketText: string }> => {
    const response = await authFetch(`${API_BASE}/tickets/venta/${ventaId}`);
    return handleResponse<{ ventaId: string; ticketText: string }>(response);
  },

  obtenerTicketPdf: async (ventaId: string): Promise<Blob> => {
    const response = await authFetch(`${API_BASE}/tickets/venta/${ventaId}/pdf`);
    if (!response.ok) {
      throw new Error('Error generando PDF del ticket');
    }
    return response.blob();
  },

  obtenerTicketEscPos: async (ventaId: string): Promise<Uint8Array> => {
    const response = await authFetch(`${API_BASE}/tickets/venta/${ventaId}/escpos`);
    if (!response.ok) {
      throw new Error('Error generando ticket ESC/POS');
    }
    const buffer = await response.arrayBuffer();
    return new Uint8Array(buffer);
  },
};

// Admin Panel API (Spec 019)
import type { AdminDashboard, FeatureFlag, FeatureFlagCreate, FeatureFlagKiosco, KioscoAdmin, Plan, PlanCreate, Suscripcion, SuscripcionCreate, UsoMensual } from '@/types';

export const adminApi = {
  // Dashboard
  getDashboard: async (): Promise<AdminDashboard> => {
    const response = await authFetch(`${API_BASE}/admin/dashboard`);
    return handleResponse<AdminDashboard>(response);
  },

  // Kioscos
  listarKioscos: async (params?: { plan?: string; activo?: boolean; busqueda?: string }): Promise<KioscoAdmin[]> => {
    const searchParams = new URLSearchParams();
    if (params?.plan) searchParams.append('plan', params.plan);
    if (params?.activo !== undefined) searchParams.append('activo', String(params.activo));
    if (params?.busqueda) searchParams.append('busqueda', params.busqueda);
    const query = searchParams.toString();
    const response = await authFetch(`${API_BASE}/admin/kioscos${query ? `?${query}` : ''}`);
    return handleResponse<KioscoAdmin[]>(response);
  },

  obtenerKiosco: async (id: string): Promise<KioscoAdmin> => {
    const response = await authFetch(`${API_BASE}/admin/kioscos/${id}`);
    return handleResponse<KioscoAdmin>(response);
  },

  activarKiosco: async (id: string): Promise<KioscoAdmin> => {
    const response = await authFetch(`${API_BASE}/admin/kioscos/${id}/activar`, { method: 'PUT' });
    return handleResponse<KioscoAdmin>(response);
  },

  desactivarKiosco: async (id: string): Promise<KioscoAdmin> => {
    const response = await authFetch(`${API_BASE}/admin/kioscos/${id}/desactivar`, { method: 'PUT' });
    return handleResponse<KioscoAdmin>(response);
  },

  obtenerHistorialUso: async (kioscoId: string): Promise<UsoMensual[]> => {
    const response = await authFetch(`${API_BASE}/admin/kioscos/${kioscoId}/uso`);
    return handleResponse<UsoMensual[]>(response);
  },

  cambiarPlanKiosco: async (kioscoId: string, planId: string): Promise<Suscripcion> => {
    const response = await authFetch(`${API_BASE}/admin/kioscos/${kioscoId}/plan?planId=${planId}`, { method: 'PUT' });
    return handleResponse<Suscripcion>(response);
  },

  // Planes
  listarPlanes: async (): Promise<Plan[]> => {
    const response = await authFetch(`${API_BASE}/admin/planes`);
    return handleResponse<Plan[]>(response);
  },

  obtenerPlan: async (id: string): Promise<Plan> => {
    const response = await authFetch(`${API_BASE}/admin/planes/${id}`);
    return handleResponse<Plan>(response);
  },

  crearPlan: async (data: PlanCreate): Promise<Plan> => {
    const response = await authFetch(`${API_BASE}/admin/planes`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Plan>(response);
  },

  actualizarPlan: async (id: string, data: PlanCreate): Promise<Plan> => {
    const response = await authFetch(`${API_BASE}/admin/planes/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Plan>(response);
  },

  activarPlan: async (id: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/admin/planes/${id}/activar`, { method: 'PUT' });
    return handleResponse<void>(response);
  },

  desactivarPlan: async (id: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/admin/planes/${id}/desactivar`, { method: 'PUT' });
    return handleResponse<void>(response);
  },

  // Suscripciones
  listarSuscripciones: async (): Promise<Suscripcion[]> => {
    const response = await authFetch(`${API_BASE}/admin/suscripciones`);
    return handleResponse<Suscripcion[]>(response);
  },

  listarSuscripcionesActivas: async (): Promise<Suscripcion[]> => {
    const response = await authFetch(`${API_BASE}/admin/suscripciones/activas`);
    return handleResponse<Suscripcion[]>(response);
  },

  obtenerSuscripcion: async (id: string): Promise<Suscripcion> => {
    const response = await authFetch(`${API_BASE}/admin/suscripciones/${id}`);
    return handleResponse<Suscripcion>(response);
  },

  crearSuscripcion: async (data: SuscripcionCreate): Promise<Suscripcion> => {
    const response = await authFetch(`${API_BASE}/admin/suscripciones`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<Suscripcion>(response);
  },

  cancelarSuscripcion: async (id: string): Promise<Suscripcion> => {
    const response = await authFetch(`${API_BASE}/admin/suscripciones/${id}/cancelar`, { method: 'PUT' });
    return handleResponse<Suscripcion>(response);
  },

  // Feature Flags
  listarFeatures: async (): Promise<FeatureFlag[]> => {
    const response = await authFetch(`${API_BASE}/admin/features`);
    return handleResponse<FeatureFlag[]>(response);
  },

  obtenerFeature: async (id: string): Promise<FeatureFlag> => {
    const response = await authFetch(`${API_BASE}/admin/features/${id}`);
    return handleResponse<FeatureFlag>(response);
  },

  crearFeature: async (data: FeatureFlagCreate): Promise<FeatureFlag> => {
    const response = await authFetch(`${API_BASE}/admin/features`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<FeatureFlag>(response);
  },

  actualizarFeature: async (id: string, data: FeatureFlagCreate): Promise<FeatureFlag> => {
    const response = await authFetch(`${API_BASE}/admin/features/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return handleResponse<FeatureFlag>(response);
  },

  toggleFeature: async (key: string, enabled: boolean): Promise<FeatureFlag> => {
    const response = await authFetch(`${API_BASE}/admin/features/${key}/toggle?enabled=${enabled}`, { method: 'PUT' });
    return handleResponse<FeatureFlag>(response);
  },

  setFeatureForKiosco: async (key: string, kioscoId: string, enabled: boolean): Promise<FeatureFlagKiosco> => {
    const response = await authFetch(`${API_BASE}/admin/features/${key}/kiosco/${kioscoId}?enabled=${enabled}`, { method: 'PUT' });
    return handleResponse<FeatureFlagKiosco>(response);
  },

  removeFeatureOverride: async (key: string, kioscoId: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/admin/features/${key}/kiosco/${kioscoId}`, { method: 'DELETE' });
    return handleResponse<void>(response);
  },

  listarOverrides: async (featureFlagId: string): Promise<FeatureFlagKiosco[]> => {
    const response = await authFetch(`${API_BASE}/admin/features/${featureFlagId}/overrides`);
    return handleResponse<FeatureFlagKiosco[]>(response);
  },

  eliminarFeature: async (id: string): Promise<void> => {
    const response = await authFetch(`${API_BASE}/admin/features/${id}`, { method: 'DELETE' });
    return handleResponse<void>(response);
  },
};
