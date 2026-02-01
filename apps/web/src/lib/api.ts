import type { Categoria, CategoriaCreate, Cliente, ClienteCreate, Comprobante, ConfigFiscal, ConfigFiscalCreate, ConfigPagos, ConfigPagosCreate, CuentaCorriente, EmitirFactura, GenerarOrdenDesdeSugerencias, HistorialPrecio, Lote, LoteCreate, MetodosPagoHabilitados, Movimiento, OrdenCompra, OrdenCompraCreate, Pago, PaymentStatus, PreferenciaResponse, Producto, ProductoCreate, ProductoMasVendido, ProductoProveedor, ProductoProveedorCreate, ProductoSinMovimiento, Proveedor, ProveedorCreate, QrInteroperableResponse, QrResponse, RecepcionOrden, ResumenCaja, ResumenDashboard, SugerenciaCompra, ValidacionCuit, VencimientoResumen, Venta, VentaCreate, VentaDiaria, VentaPorHora, VentaRango, VerificacionAfip, VerificacionCertificado, VerificacionMp } from '@/types';

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

// Clientes API
export const clientesApi = {
  listar: async (): Promise<Cliente[]> => {
    const response = await fetch(`${API_BASE}/clientes`);
    return handleResponse<Cliente[]>(response);
  },

  obtener: async (id: string): Promise<Cliente> => {
    const response = await fetch(`${API_BASE}/clientes/${id}`);
    return handleResponse<Cliente>(response);
  },

  buscar: async (query: string): Promise<Cliente[]> => {
    const response = await fetch(`${API_BASE}/clientes/buscar?q=${encodeURIComponent(query)}`);
    return handleResponse<Cliente[]>(response);
  },

  crear: async (data: ClienteCreate): Promise<Cliente> => {
    const response = await fetch(`${API_BASE}/clientes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Cliente>(response);
  },

  actualizar: async (id: string, data: ClienteCreate): Promise<Cliente> => {
    const response = await fetch(`${API_BASE}/clientes/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Cliente>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/clientes/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// Cuenta Corriente API
export const cuentaCorrienteApi = {
  obtenerCuenta: async (clienteId: string): Promise<CuentaCorriente> => {
    const response = await fetch(`${API_BASE}/clientes/${clienteId}/cuenta`);
    return handleResponse<CuentaCorriente>(response);
  },

  obtenerMovimientos: async (clienteId: string): Promise<Movimiento[]> => {
    const response = await fetch(`${API_BASE}/clientes/${clienteId}/movimientos`);
    return handleResponse<Movimiento[]>(response);
  },

  registrarPago: async (clienteId: string, pago: Pago): Promise<Movimiento> => {
    const response = await fetch(`${API_BASE}/clientes/${clienteId}/pago`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(pago),
    });
    return handleResponse<Movimiento>(response);
  },

  listarDeudores: async (): Promise<CuentaCorriente[]> => {
    const response = await fetch(`${API_BASE}/cuenta-corriente/deudores`);
    return handleResponse<CuentaCorriente[]>(response);
  },

  verificarPuedeFiar: async (clienteId: string, monto: number): Promise<{ puede: boolean; saldoActual: number; limiteCredito: number; disponible: number }> => {
    const response = await fetch(`${API_BASE}/clientes/${clienteId}/puede-fiar?monto=${monto}`);
    return handleResponse<{ puede: boolean; saldoActual: number; limiteCredito: number; disponible: number }>(response);
  },
};

// Configuración Fiscal API
export const configFiscalApi = {
  obtener: async (): Promise<ConfigFiscal | null> => {
    const response = await fetch(`${API_BASE}/config/fiscal`);
    if (response.status === 204) {
      return null;
    }
    return handleResponse<ConfigFiscal>(response);
  },

  guardar: async (data: ConfigFiscalCreate): Promise<ConfigFiscal> => {
    const response = await fetch(`${API_BASE}/config/fiscal`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<ConfigFiscal>(response);
  },

  subirCertificado: async (crt: File, key: File): Promise<ConfigFiscal> => {
    const formData = new FormData();
    formData.append('crt', crt);
    formData.append('key', key);

    const response = await fetch(`${API_BASE}/config/fiscal/certificado`, {
      method: 'POST',
      body: formData,
    });
    return handleResponse<ConfigFiscal>(response);
  },

  verificarCertificado: async (): Promise<VerificacionCertificado> => {
    const response = await fetch(`${API_BASE}/config/fiscal/certificado/verificar`);
    return handleResponse<VerificacionCertificado>(response);
  },

  verificarConexionAfip: async (): Promise<VerificacionAfip> => {
    const response = await fetch(`${API_BASE}/config/fiscal/verificar`);
    return handleResponse<VerificacionAfip>(response);
  },

  validarCuit: async (cuit: string): Promise<ValidacionCuit> => {
    const response = await fetch(`${API_BASE}/config/fiscal/validar-cuit?cuit=${encodeURIComponent(cuit)}`);
    return handleResponse<ValidacionCuit>(response);
  },
};

// Facturacion API
export const facturacionApi = {
  emitir: async (data: EmitirFactura): Promise<Comprobante> => {
    const response = await fetch(`${API_BASE}/facturacion/emitir`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Comprobante>(response);
  },

  obtenerPorVenta: async (ventaId: string): Promise<Comprobante | null> => {
    const response = await fetch(`${API_BASE}/facturacion/venta/${ventaId}`);
    if (response.status === 404) {
      return null;
    }
    return handleResponse<Comprobante>(response);
  },

  obtener: async (id: string): Promise<Comprobante> => {
    const response = await fetch(`${API_BASE}/facturacion/${id}`);
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

    const response = await fetch(url);
    return handleResponse<Comprobante[]>(response);
  },

  getUltimoNumero: async (tipoComprobante: number, puntoVenta: number): Promise<{ ultimoNumero: number; proximoNumero: number }> => {
    const response = await fetch(
      `${API_BASE}/facturacion/ultimo-numero?tipoComprobante=${tipoComprobante}&puntoVenta=${puntoVenta}`
    );
    return handleResponse<{ ultimoNumero: number; proximoNumero: number }>(response);
  },

  descargarPdf: async (id: string): Promise<Blob> => {
    const response = await fetch(`${API_BASE}/facturacion/${id}/pdf`);
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
    const response = await fetch(`${API_BASE}/config/pagos`);
    return handleResponse<ConfigPagos>(response);
  },

  guardar: async (data: ConfigPagosCreate): Promise<ConfigPagos> => {
    const response = await fetch(`${API_BASE}/config/pagos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<ConfigPagos>(response);
  },

  actualizar: async (data: ConfigPagosCreate): Promise<ConfigPagos> => {
    const response = await fetch(`${API_BASE}/config/pagos`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<ConfigPagos>(response);
  },

  actualizarMetodos: async (data: ConfigPagosCreate): Promise<ConfigPagos> => {
    const response = await fetch(`${API_BASE}/config/pagos/metodos`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<ConfigPagos>(response);
  },

  obtenerMetodosHabilitados: async (): Promise<MetodosPagoHabilitados> => {
    const response = await fetch(`${API_BASE}/config/pagos/metodos-habilitados`);
    return handleResponse<MetodosPagoHabilitados>(response);
  },

  verificarMercadoPago: async (): Promise<VerificacionMp> => {
    const response = await fetch(`${API_BASE}/config/pagos/verificar-mp`);
    return handleResponse<VerificacionMp>(response);
  },
};

// Pagos API (payment operations)
export const pagosApi = {
  // Mercado Pago
  crearPreferencia: async (monto: number, descripcion: string, externalReference: string): Promise<PreferenciaResponse> => {
    const response = await fetch(`${API_BASE}/pagos/mp/preferencia`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ monto, descripcion, externalReference }),
    });
    return handleResponse<PreferenciaResponse>(response);
  },

  crearQrMp: async (monto: number, descripcion: string): Promise<QrResponse> => {
    const response = await fetch(`${API_BASE}/pagos/mp/qr`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ monto, descripcion }),
    });
    return handleResponse<QrResponse>(response);
  },

  verificarPago: async (paymentId: string): Promise<PaymentStatus> => {
    const response = await fetch(`${API_BASE}/pagos/mp/status/${paymentId}`);
    return handleResponse<PaymentStatus>(response);
  },

  verificarPagoPorPreferencia: async (preferenceId: string): Promise<PaymentStatus> => {
    const response = await fetch(`${API_BASE}/pagos/mp/status/preference/${preferenceId}`);
    return handleResponse<PaymentStatus>(response);
  },

  // QR Interoperable
  generarQr: async (monto: number, descripcion: string): Promise<QrInteroperableResponse> => {
    const response = await fetch(`${API_BASE}/pagos/qr/generar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ monto, descripcion }),
    });
    return handleResponse<QrInteroperableResponse>(response);
  },

  obtenerQrEstatico: async (): Promise<QrInteroperableResponse> => {
    const response = await fetch(`${API_BASE}/pagos/qr/estatico`);
    return handleResponse<QrInteroperableResponse>(response);
  },
};

// Lotes API
export const lotesApi = {
  listarPorProducto: async (productoId: string): Promise<Lote[]> => {
    const response = await fetch(`${API_BASE}/productos/${productoId}/lotes`);
    return handleResponse<Lote[]>(response);
  },

  obtener: async (id: string): Promise<Lote> => {
    const response = await fetch(`${API_BASE}/lotes/${id}`);
    return handleResponse<Lote>(response);
  },

  crear: async (productoId: string, data: LoteCreate): Promise<Lote> => {
    const response = await fetch(`${API_BASE}/productos/${productoId}/lotes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Lote>(response);
  },

  actualizar: async (id: string, data: LoteCreate): Promise<Lote> => {
    const response = await fetch(`${API_BASE}/lotes/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Lote>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/lotes/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },
};

// Vencimientos API
export const vencimientosApi = {
  proximosAVencer: async (dias: number = 7): Promise<Lote[]> => {
    const response = await fetch(`${API_BASE}/vencimientos/proximos?dias=${dias}`);
    return handleResponse<Lote[]>(response);
  },

  vencidos: async (): Promise<Lote[]> => {
    const response = await fetch(`${API_BASE}/vencimientos/vencidos`);
    return handleResponse<Lote[]>(response);
  },

  resumen: async (): Promise<VencimientoResumen> => {
    const response = await fetch(`${API_BASE}/vencimientos/resumen`);
    return handleResponse<VencimientoResumen>(response);
  },
};

// Proveedores API
export const proveedoresApi = {
  listar: async (): Promise<Proveedor[]> => {
    const response = await fetch(`${API_BASE}/proveedores`);
    return handleResponse<Proveedor[]>(response);
  },

  obtener: async (id: string): Promise<Proveedor> => {
    const response = await fetch(`${API_BASE}/proveedores/${id}`);
    return handleResponse<Proveedor>(response);
  },

  buscar: async (query: string): Promise<Proveedor[]> => {
    const response = await fetch(`${API_BASE}/proveedores/buscar?q=${encodeURIComponent(query)}`);
    return handleResponse<Proveedor[]>(response);
  },

  crear: async (data: ProveedorCreate): Promise<Proveedor> => {
    const response = await fetch(`${API_BASE}/proveedores`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Proveedor>(response);
  },

  actualizar: async (id: string, data: ProveedorCreate): Promise<Proveedor> => {
    const response = await fetch(`${API_BASE}/proveedores/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<Proveedor>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/proveedores/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },

  listarProductos: async (proveedorId: string): Promise<ProductoProveedor[]> => {
    const response = await fetch(`${API_BASE}/proveedores/${proveedorId}/productos`);
    return handleResponse<ProductoProveedor[]>(response);
  },
};

// Producto-Proveedor API
export const productoProveedorApi = {
  listarPorProducto: async (productoId: string): Promise<ProductoProveedor[]> => {
    const response = await fetch(`${API_BASE}/productos/${productoId}/proveedores`);
    return handleResponse<ProductoProveedor[]>(response);
  },

  asociar: async (productoId: string, data: ProductoProveedorCreate): Promise<ProductoProveedor> => {
    const response = await fetch(`${API_BASE}/productos/${productoId}/proveedores`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<ProductoProveedor>(response);
  },

  actualizar: async (id: string, data: ProductoProveedorCreate): Promise<ProductoProveedor> => {
    const response = await fetch(`${API_BASE}/producto-proveedor/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<ProductoProveedor>(response);
  },

  actualizarPrecio: async (id: string, precio: number): Promise<ProductoProveedor> => {
    const response = await fetch(`${API_BASE}/producto-proveedor/${id}/precio?precio=${precio}`, {
      method: 'PATCH',
    });
    return handleResponse<ProductoProveedor>(response);
  },

  eliminar: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE}/producto-proveedor/${id}`, {
      method: 'DELETE',
    });
    return handleResponse<void>(response);
  },

  historialPrecios: async (id: string): Promise<HistorialPrecio[]> => {
    const response = await fetch(`${API_BASE}/producto-proveedor/${id}/historial-precios`);
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

    const response = await fetch(url);
    return handleResponse<OrdenCompra[]>(response);
  },

  obtener: async (id: string): Promise<OrdenCompra> => {
    const response = await fetch(`${API_BASE}/ordenes-compra/${id}`);
    return handleResponse<OrdenCompra>(response);
  },

  crear: async (data: OrdenCompraCreate): Promise<OrdenCompra> => {
    const response = await fetch(`${API_BASE}/ordenes-compra`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<OrdenCompra>(response);
  },

  actualizar: async (id: string, data: OrdenCompraCreate): Promise<OrdenCompra> => {
    const response = await fetch(`${API_BASE}/ordenes-compra/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<OrdenCompra>(response);
  },

  enviar: async (id: string): Promise<OrdenCompra> => {
    const response = await fetch(`${API_BASE}/ordenes-compra/${id}/enviar`, {
      method: 'POST',
    });
    return handleResponse<OrdenCompra>(response);
  },

  recibir: async (id: string, data: RecepcionOrden): Promise<OrdenCompra> => {
    const response = await fetch(`${API_BASE}/ordenes-compra/${id}/recibir`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<OrdenCompra>(response);
  },

  cancelar: async (id: string): Promise<OrdenCompra> => {
    const response = await fetch(`${API_BASE}/ordenes-compra/${id}`, {
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

    const response = await fetch(url);
    return handleResponse<SugerenciaCompra[]>(response);
  },

  generarOrden: async (data: GenerarOrdenDesdeSugerencias): Promise<OrdenCompra> => {
    const response = await fetch(`${API_BASE}/sugerencias-compra/generar-orden`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<OrdenCompra>(response);
  },
};

// Reportes API
export const reportesApi = {
  getDashboard: async (): Promise<ResumenDashboard> => {
    const response = await fetch(`${API_BASE}/reportes/dashboard`);
    return handleResponse<ResumenDashboard>(response);
  },

  getVentaDiaria: async (fecha: string): Promise<VentaDiaria> => {
    const response = await fetch(`${API_BASE}/reportes/ventas/diario?fecha=${fecha}`);
    return handleResponse<VentaDiaria>(response);
  },

  getVentasRango: async (desde: string, hasta: string): Promise<VentaRango> => {
    const response = await fetch(`${API_BASE}/reportes/ventas/rango?desde=${desde}&hasta=${hasta}`);
    return handleResponse<VentaRango>(response);
  },

  getVentasPorHora: async (fecha: string): Promise<VentaPorHora[]> => {
    const response = await fetch(`${API_BASE}/reportes/ventas/por-hora?fecha=${fecha}`);
    return handleResponse<VentaPorHora[]>(response);
  },

  getVentasPorMedioPago: async (desde: string, hasta: string): Promise<Record<string, number>> => {
    const response = await fetch(`${API_BASE}/reportes/ventas/por-medio-pago?desde=${desde}&hasta=${hasta}`);
    return handleResponse<Record<string, number>>(response);
  },

  getProductosMasVendidos: async (desde: string, hasta: string, limit: number = 20): Promise<ProductoMasVendido[]> => {
    const response = await fetch(`${API_BASE}/reportes/productos/mas-vendidos?desde=${desde}&hasta=${hasta}&limit=${limit}`);
    return handleResponse<ProductoMasVendido[]>(response);
  },

  getProductosSinMovimiento: async (dias: number = 30): Promise<ProductoSinMovimiento[]> => {
    const response = await fetch(`${API_BASE}/reportes/productos/sin-movimiento?dias=${dias}`);
    return handleResponse<ProductoSinMovimiento[]>(response);
  },

  getResumenCaja: async (fecha: string): Promise<ResumenCaja> => {
    const response = await fetch(`${API_BASE}/reportes/caja/resumen?fecha=${fecha}`);
    return handleResponse<ResumenCaja>(response);
  },

  exportarVentasCSV: async (desde: string, hasta: string): Promise<Blob> => {
    const response = await fetch(`${API_BASE}/reportes/ventas/exportar?desde=${desde}&hasta=${hasta}`);
    if (!response.ok) {
      throw new Error('Error al exportar');
    }
    return response.blob();
  },

  exportarProductosMasVendidosCSV: async (desde: string, hasta: string, limit: number = 100): Promise<Blob> => {
    const response = await fetch(`${API_BASE}/reportes/productos/mas-vendidos/exportar?desde=${desde}&hasta=${hasta}&limit=${limit}`);
    if (!response.ok) {
      throw new Error('Error al exportar');
    }
    return response.blob();
  },
};
