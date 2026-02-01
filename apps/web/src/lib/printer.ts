/**
 * Printer utilities for Web Bluetooth and Web USB APIs
 * These APIs allow direct printing to thermal printers from the browser
 */

// Web Bluetooth Printer class
export class BluetoothPrinter {
  private device: BluetoothDevice | null = null
  private server: BluetoothRemoteGATTServer | null = null
  private characteristic: BluetoothRemoteGATTCharacteristic | null = null

  // Common thermal printer service and characteristic UUIDs
  private static readonly PRINTER_SERVICE = '000018f0-0000-1000-8000-00805f9b34fb'
  private static readonly PRINTER_CHARACTERISTIC = '00002af1-0000-1000-8000-00805f9b34fb'

  async connect(): Promise<void> {
    if (!navigator.bluetooth) {
      throw new Error('Web Bluetooth no esta soportado en este navegador')
    }

    try {
      // Request device with thermal printer service
      this.device = await navigator.bluetooth.requestDevice({
        acceptAllDevices: true,
        optionalServices: [BluetoothPrinter.PRINTER_SERVICE],
      })

      if (!this.device.gatt) {
        throw new Error('El dispositivo no soporta GATT')
      }

      // Connect to GATT server
      this.server = await this.device.gatt.connect()

      // Get the printer service
      const service = await this.server.getPrimaryService(BluetoothPrinter.PRINTER_SERVICE)

      // Get the write characteristic
      this.characteristic = await service.getCharacteristic(BluetoothPrinter.PRINTER_CHARACTERISTIC)
    } catch (error) {
      this.cleanup()
      if (error instanceof Error) {
        if (error.name === 'NotFoundError') {
          throw new Error('No se encontro una impresora Bluetooth compatible')
        }
        throw error
      }
      throw new Error('Error al conectar con la impresora Bluetooth')
    }
  }

  async print(data: Uint8Array): Promise<void> {
    if (!this.characteristic) {
      throw new Error('Impresora no conectada')
    }

    try {
      // Split data into chunks (BLE has MTU limits, typically 20 bytes)
      const chunkSize = 20
      for (let i = 0; i < data.length; i += chunkSize) {
        const chunk = data.slice(i, Math.min(i + chunkSize, data.length))
        await this.characteristic.writeValueWithoutResponse(chunk)
        // Small delay between chunks
        await new Promise(resolve => setTimeout(resolve, 20))
      }
    } catch (error) {
      throw new Error('Error al enviar datos a la impresora')
    }
  }

  async disconnect(): Promise<void> {
    this.cleanup()
  }

  isConnected(): boolean {
    return this.device?.gatt?.connected || false
  }

  private cleanup() {
    if (this.server?.connected) {
      this.server.disconnect()
    }
    this.characteristic = null
    this.server = null
    this.device = null
  }
}

// Web USB Printer class
export class UsbPrinter {
  private device: USBDevice | null = null
  private endpointOut: USBEndpoint | null = null

  // Common thermal printer vendor IDs
  private static readonly VENDOR_IDS = [
    0x0416, // Winbond Electronics
    0x0483, // STMicroelectronics
    0x0525, // Netchip Technology
    0x067B, // Prolific Technology
    0x1504, // XIAMEN REGO ELECTRONIC
    0x0FE6, // ICS Advent
    0x20D1, // Simtech
    0x0DD4, // Custom Engineering
    0x1FC9, // NXP
    0x04B8, // Seiko Epson
    0x1A86, // QinHeng Electronics (CH340)
  ]

  async connect(): Promise<void> {
    if (!navigator.usb) {
      throw new Error('Web USB no esta soportado en este navegador')
    }

    try {
      // Request device - filter by common printer vendors
      const filters = UsbPrinter.VENDOR_IDS.map(vendorId => ({ vendorId }))

      this.device = await navigator.usb.requestDevice({ filters })

      await this.device.open()

      // Select configuration (usually the first one)
      if (this.device.configuration === null) {
        await this.device.selectConfiguration(1)
      }

      // Find a bulk OUT endpoint
      const iface = this.device.configuration?.interfaces[0]
      if (!iface) {
        throw new Error('No se encontro interfaz USB')
      }

      await this.device.claimInterface(iface.interfaceNumber)

      // Find bulk OUT endpoint
      for (const endpoint of iface.alternate.endpoints) {
        if (endpoint.direction === 'out' && endpoint.type === 'bulk') {
          this.endpointOut = endpoint
          break
        }
      }

      if (!this.endpointOut) {
        throw new Error('No se encontro endpoint de salida')
      }
    } catch (error) {
      this.cleanup()
      if (error instanceof Error) {
        if (error.name === 'NotFoundError') {
          throw new Error('No se encontro una impresora USB compatible')
        }
        throw error
      }
      throw new Error('Error al conectar con la impresora USB')
    }
  }

  async print(data: Uint8Array): Promise<void> {
    if (!this.device || !this.endpointOut) {
      throw new Error('Impresora no conectada')
    }

    try {
      // Create a new ArrayBuffer from the Uint8Array to ensure compatibility
      const buffer = new ArrayBuffer(data.length)
      new Uint8Array(buffer).set(data)
      await this.device.transferOut(this.endpointOut.endpointNumber, buffer)
    } catch (error) {
      throw new Error('Error al enviar datos a la impresora')
    }
  }

  async disconnect(): Promise<void> {
    this.cleanup()
  }

  isConnected(): boolean {
    return this.device?.opened || false
  }

  private async cleanup() {
    if (this.device?.opened) {
      try {
        await this.device.close()
      } catch {
        // Ignore errors during cleanup
      }
    }
    this.endpointOut = null
    this.device = null
  }
}

// Network Printer class (for TCP/IP printers)
// Note: This requires a backend proxy since browsers can't open raw TCP sockets
export class NetworkPrinter {
  private address: string
  private port: number

  constructor(address: string, port: number = 9100) {
    this.address = address
    this.port = port
  }

  // Network printing must go through the backend API
  async print(data: Uint8Array): Promise<void> {
    // The backend handles the actual TCP connection
    // This is a placeholder for the API call
    throw new Error('La impresion por red debe realizarse a traves del servidor')
  }

  isConnected(): boolean {
    // Network printers are stateless from the frontend perspective
    return true
  }
}

// Printer factory based on connection type
export type PrinterType = 'USB' | 'BLUETOOTH' | 'RED' | 'NINGUNA'

export function createPrinter(type: PrinterType, options?: { address?: string; port?: number }) {
  switch (type) {
    case 'USB':
      return new UsbPrinter()
    case 'BLUETOOTH':
      return new BluetoothPrinter()
    case 'RED':
      return new NetworkPrinter(options?.address || '', options?.port || 9100)
    default:
      return null
  }
}

// Check browser support for printing APIs
export function checkPrinterSupport() {
  return {
    bluetooth: typeof navigator !== 'undefined' && 'bluetooth' in navigator,
    usb: typeof navigator !== 'undefined' && 'usb' in navigator,
    network: true, // Always supported via backend
  }
}
