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
  controlaVencimiento?: boolean;
  diasAlertaVencimiento?: number;
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
  controlaVencimiento?: boolean;
  diasAlertaVencimiento?: number;
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

// Payment configuration types
export type EstadoPagos = 'BASICO' | 'PARCIAL' | 'COMPLETO';

export interface ConfigPagos {
  id?: string;
  mpConfigurado: boolean;
  mpPublicKey?: string;
  mpUserId?: string;
  qrAlias?: string;
  qrCbu?: string;
  qrConfigurado: boolean;
  aceptaEfectivo: boolean;
  aceptaDebito: boolean;
  aceptaCredito: boolean;
  aceptaMercadopago: boolean;
  aceptaQr: boolean;
  aceptaTransferencia: boolean;
  estado: EstadoPagos;
}

export interface ConfigPagosCreate {
  mpAccessToken?: string;
  mpPublicKey?: string;
  qrAlias?: string;
  qrCbu?: string;
  aceptaEfectivo?: boolean;
  aceptaDebito?: boolean;
  aceptaCredito?: boolean;
  aceptaMercadopago?: boolean;
  aceptaQr?: boolean;
  aceptaTransferencia?: boolean;
}

export interface MetodosPagoHabilitados {
  efectivo: boolean;
  debito: boolean;
  credito: boolean;
  mercadopago: boolean;
  qr: boolean;
  transferencia: boolean;
  fiado: boolean;
}

export interface VerificacionMp {
  valido: boolean;
  estado: string;
  mensaje: string;
}

export interface PreferenciaResponse {
  preferenceId: string;
  initPoint: string;
  sandboxInitPoint: string;
}

export interface QrResponse {
  preferenceId?: string;
  qrContent: string;
  monto: number;
}

export interface QrInteroperableResponse {
  qrContent: string;
  qrImageBase64: string;
  alias: string;
  monto: number;
  descripcion?: string;
}

export interface PaymentStatus {
  paymentId?: string;
  status: string;
  statusDetail?: string;
  amount?: number;
  externalReference?: string;
}

// Lotes y Vencimientos types
export type EstadoLote = 'OK' | 'PROXIMO' | 'VENCIDO';

export interface Lote {
  id: string;
  productoId: string;
  productoNombre: string;
  codigoLote?: string;
  cantidad: number;
  cantidadDisponible: number;
  fechaVencimiento: string;
  fechaIngreso?: string;
  costoUnitario?: number;
  notas?: string;
  diasParaVencer: number;
  estado: EstadoLote;
}

export interface LoteCreate {
  codigoLote?: string;
  cantidad: number;
  fechaVencimiento: string;
  costoUnitario?: number;
  notas?: string;
}

export interface VencimientoResumen {
  proximosAVencer: number;
  vencidos: number;
  totalLotesActivos: number;
}

// Proveedor types
export interface Proveedor {
  id: string;
  nombre: string;
  cuit?: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  contacto?: string;
  diasEntrega?: number;
  notas?: string;
  activo: boolean;
}

export interface ProveedorCreate {
  nombre: string;
  cuit?: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  contacto?: string;
  diasEntrega?: number;
  notas?: string;
}

// Producto-Proveedor types
export interface ProductoProveedor {
  id: string;
  productoId: string;
  productoNombre?: string;
  productoCodigo?: string;
  proveedorId: string;
  proveedorNombre?: string;
  codigoProveedor?: string;
  precioCompra?: number;
  ultimoPrecio?: number;
  fechaUltimoPrecio?: string;
  esPrincipal: boolean;
}

export interface ProductoProveedorCreate {
  proveedorId: string;
  codigoProveedor?: string;
  precioCompra?: number;
  esPrincipal?: boolean;
}

export interface HistorialPrecio {
  id: string;
  precio: number;
  fecha: string;
}

// Orden de Compra types
export type EstadoOrdenCompra = 'BORRADOR' | 'ENVIADA' | 'RECIBIDA' | 'CANCELADA';

export interface OrdenCompraItem {
  id: string;
  productoId: string;
  productoNombre?: string;
  productoCodigo?: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  cantidadRecibida: number;
}

export interface OrdenCompra {
  id: string;
  numero: number;
  proveedorId: string;
  proveedorNombre?: string;
  estado: EstadoOrdenCompra;
  fechaEmision: string;
  fechaEntregaEsperada?: string;
  fechaRecepcion?: string;
  subtotal: number;
  total: number;
  notas?: string;
  items?: OrdenCompraItem[];
  cantidadItems: number;
}

export interface OrdenCompraItemCreate {
  productoId: string;
  cantidad: number;
  precioUnitario: number;
}

export interface OrdenCompraCreate {
  proveedorId: string;
  fechaEntregaEsperada?: string;
  notas?: string;
  items: OrdenCompraItemCreate[];
}

export interface RecepcionItem {
  itemId: string;
  cantidadRecibida: number;
}

export interface RecepcionOrden {
  items: RecepcionItem[];
}

// Sugerencias de Compra types
export interface SugerenciaCompra {
  productoId: string;
  productoNombre: string;
  productoCodigo?: string;
  stockActual: number;
  stockMinimo: number;
  promedioVentasDiarias?: number;
  cantidadSugerida: number;
  proveedorSugeridoId?: string;
  proveedorSugeridoNombre?: string;
  precioEstimado?: number;
  motivoSugerencia: 'STOCK_BAJO' | 'VENTAS_ALTAS' | 'VENCIMIENTO_PROXIMO';
}

export interface GenerarOrdenDesdeSugerencias {
  proveedorId: string;
  productoIds: string[];
  notas?: string;
}

// Reportes types
export interface ResumenDashboard {
  ventasHoy: number;
  cantidadVentasHoy: number;
  ventasMes: number;
  cantidadVentasMes: number;
  ticketPromedio: number;
  productosVendidosHoy: number;
  productosStockBajo: number;
  productosProximosVencer: number;
}

export interface VentaDiaria {
  fecha: string;
  cantidadVentas: number;
  totalVentas: number;
  ticketPromedio: number;
  porMedioPago: Record<string, number>;
}

export interface VentaRango {
  desde: string;
  hasta: string;
  totalVentas: number;
  montoTotal: number;
  ticketPromedio: number;
  porMedioPago: Record<string, number>;
  porDia: VentaDiaria[];
}

export interface VentaPorHora {
  hora: number;
  cantidadVentas: number;
  total: number;
}

export interface ProductoMasVendido {
  productoId: string;
  nombre: string;
  categoria: string;
  cantidadVendida: number;
  montoTotal: number;
  margenTotal: number;
}

export interface ProductoSinMovimiento {
  productoId: string;
  nombre: string;
  categoria: string;
  stockActual: number;
  ultimaVenta: string | null;
  diasSinMovimiento: number;
}

export interface ResumenCaja {
  fecha: string;
  saldoInicial: number;
  ingresos: number;
  egresos: number;
  ventasEfectivo: number;
  ventasDigital: number;
  saldoFinal: number;
  saldoTeorico: number;
  diferencia: number;
}

// Advanced Reports types (Spec 016)
export interface RentabilidadProducto {
  productoId: string;
  nombre: string;
  categoria: string;
  cantidadVendida: number;
  ingresos: number;
  costos: number;
  margenBruto: number;
  margenPorcentaje: number;
  rentabilidadPorUnidad: number;
}

export interface RentabilidadCategoria {
  categoriaId: string | null;
  nombre: string;
  cantidadProductos: number;
  cantidadVendida: number;
  ingresos: number;
  costos: number;
  margenBruto: number;
  margenPorcentaje: number;
}

export interface Tendencia {
  periodo: string;
  ventas: number;
  cantidadVentas: number;
  variacion: number;
  variacionPorcentaje: number;
}

export interface TendenciaPeriodo {
  periodo: string;
  cantidadVendida: number;
  ingresos: number;
}

export interface TendenciaProducto {
  productoId: string;
  nombre: string;
  categoria: string;
  periodos: TendenciaPeriodo[];
  tendenciaGeneral: number;
}

export interface ComparativoItem {
  concepto: string;
  periodo1: number;
  periodo2: number;
  diferencia: number;
  variacionPorcentaje: number;
}

export interface Comparativo {
  periodo1Desde: string;
  periodo1Hasta: string;
  periodo2Desde: string;
  periodo2Hasta: string;
  items: ComparativoItem[];
}

export interface ProductoAbc {
  productoId: string;
  nombre: string;
  categoria: string;
  ventas: number;
  porcentajeVentas: number;
  porcentajeAcumulado: number;
  clasificacion: 'A' | 'B' | 'C';
}

export interface ProyeccionDia {
  fecha: string;
  ventaProyectada: number;
  esProyeccion: boolean;
}

export interface ProyeccionVentas {
  fechaDesde: string;
  fechaHasta: string;
  diasProyectados: number;
  ventasProyectadas: number;
  promedioHistorico: number;
  proyeccionDiaria: ProyeccionDia[];
}

export type InsightTipo = 'SUCCESS' | 'WARNING' | 'INFO' | 'DANGER';

export interface Insight {
  tipo: InsightTipo;
  icono: string;
  titulo: string;
  descripcion: string;
  accion: string | null;
}

// Cadena (Multi-Kiosco) types
export interface KioscoResumen {
  id: string;
  nombre: string;
  slug: string;
  esCasaCentral: boolean;
  ventasHoy: number;
  ventasMes: number;
  activo: boolean;
}

export interface Cadena {
  id: string;
  nombre: string;
  ownerId: string;
  ownerNombre: string;
  kioscos?: KioscoResumen[];
  totalKioscos: number;
  createdAt: string;
}

export interface CadenaCreate {
  nombre: string;
}

export type RolCadena = 'OWNER' | 'ADMIN' | 'VIEWER';

export interface CadenaMember {
  id: string;
  cadenaId: string;
  usuarioId: string;
  usuarioNombre: string;
  usuarioEmail: string;
  rol: RolCadena;
  puedeVerTodos: boolean;
  kioscosPermitidos: string[] | null;
  createdAt: string;
}

export interface CadenaMemberCreate {
  usuarioId: string;
  rol: RolCadena;
  puedeVerTodos?: boolean;
  kioscosPermitidos?: string[];
}

export interface AgregarKioscoACadena {
  kioscoId: string;
  esCasaCentral?: boolean;
}

export interface VentaPorKiosco {
  kioscoId: string;
  kioscoNombre: string;
  ventas: number;
  cantidad: number;
  porcentajeDelTotal: number;
}

export interface ReporteConsolidado {
  desde: string;
  hasta: string;
  ventasTotal: number;
  cantidadVentas: number;
  ticketPromedio: number;
  porKiosco: VentaPorKiosco[];
}

export interface RankingKiosco {
  posicion: number;
  kioscoId: string;
  kioscoNombre: string;
  ventas: number;
  variacionVsMesAnterior: number;
}

export interface StockPorKiosco {
  kioscoId: string;
  kioscoNombre: string;
  stock: number;
}

export interface StockConsolidado {
  productoId: string;
  productoNombre: string;
  productoCodigo: string;
  stockTotal: number;
  stockPorKiosco: StockPorKiosco[];
}
