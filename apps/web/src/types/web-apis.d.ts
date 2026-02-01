// Web Bluetooth API Type Declarations
// These APIs are experimental and not included in standard lib.dom.d.ts

declare global {
  interface BluetoothDevice {
    readonly id: string
    readonly name?: string
    readonly gatt?: BluetoothRemoteGATTServer
    watchAdvertisements(): Promise<void>
    unwatchAdvertisements(): void
    readonly watchingAdvertisements: boolean
  }

  interface BluetoothRemoteGATTServer {
    readonly device: BluetoothDevice
    readonly connected: boolean
    connect(): Promise<BluetoothRemoteGATTServer>
    disconnect(): void
    getPrimaryService(service: string): Promise<BluetoothRemoteGATTService>
    getPrimaryServices(service?: string): Promise<BluetoothRemoteGATTService[]>
  }

  interface BluetoothRemoteGATTService {
    readonly device: BluetoothDevice
    readonly uuid: string
    readonly isPrimary: boolean
    getCharacteristic(characteristic: string): Promise<BluetoothRemoteGATTCharacteristic>
    getCharacteristics(characteristic?: string): Promise<BluetoothRemoteGATTCharacteristic[]>
  }

  interface BluetoothRemoteGATTCharacteristic {
    readonly service?: BluetoothRemoteGATTService
    readonly uuid: string
    readonly properties: BluetoothCharacteristicProperties
    readonly value?: DataView
    getDescriptor(descriptor: string): Promise<BluetoothRemoteGATTDescriptor>
    getDescriptors(descriptor?: string): Promise<BluetoothRemoteGATTDescriptor[]>
    readValue(): Promise<DataView>
    writeValue(value: BufferSource): Promise<void>
    writeValueWithResponse(value: BufferSource): Promise<void>
    writeValueWithoutResponse(value: BufferSource): Promise<void>
    startNotifications(): Promise<BluetoothRemoteGATTCharacteristic>
    stopNotifications(): Promise<BluetoothRemoteGATTCharacteristic>
  }

  interface BluetoothCharacteristicProperties {
    readonly broadcast: boolean
    readonly read: boolean
    readonly writeWithoutResponse: boolean
    readonly write: boolean
    readonly notify: boolean
    readonly indicate: boolean
    readonly authenticatedSignedWrites: boolean
    readonly reliableWrite: boolean
    readonly writableAuxiliaries: boolean
  }

  interface BluetoothRemoteGATTDescriptor {
    readonly characteristic?: BluetoothRemoteGATTCharacteristic
    readonly uuid: string
    readonly value?: DataView
    readValue(): Promise<DataView>
    writeValue(value: BufferSource): Promise<void>
  }

  interface RequestDeviceOptions {
    filters?: BluetoothLEScanFilter[]
    optionalServices?: string[]
    acceptAllDevices?: boolean
  }

  interface BluetoothLEScanFilter {
    services?: string[]
    name?: string
    namePrefix?: string
    manufacturerData?: Map<number, DataView>
    serviceData?: Map<string, DataView>
  }

  interface Bluetooth {
    getAvailability(): Promise<boolean>
    requestDevice(options?: RequestDeviceOptions): Promise<BluetoothDevice>
  }

  // Web USB API Type Declarations

  interface USBDevice {
    readonly configuration: USBConfiguration | null
    readonly configurations: USBConfiguration[]
    readonly deviceClass: number
    readonly deviceProtocol: number
    readonly deviceSubclass: number
    readonly deviceVersionMajor: number
    readonly deviceVersionMinor: number
    readonly deviceVersionSubminor: number
    readonly manufacturerName?: string
    readonly opened: boolean
    readonly productId: number
    readonly productName?: string
    readonly serialNumber?: string
    readonly usbVersionMajor: number
    readonly usbVersionMinor: number
    readonly usbVersionSubminor: number
    readonly vendorId: number
    claimInterface(interfaceNumber: number): Promise<void>
    clearHalt(direction: USBDirection, endpointNumber: number): Promise<void>
    close(): Promise<void>
    controlTransferIn(setup: USBControlTransferParameters, length: number): Promise<USBInTransferResult>
    controlTransferOut(setup: USBControlTransferParameters, data?: BufferSource): Promise<USBOutTransferResult>
    isochronousTransferIn(endpointNumber: number, packetLengths: number[]): Promise<USBIsochronousInTransferResult>
    isochronousTransferOut(endpointNumber: number, data: BufferSource, packetLengths: number[]): Promise<USBIsochronousOutTransferResult>
    open(): Promise<void>
    releaseInterface(interfaceNumber: number): Promise<void>
    reset(): Promise<void>
    selectAlternateInterface(interfaceNumber: number, alternateSetting: number): Promise<void>
    selectConfiguration(configurationValue: number): Promise<void>
    transferIn(endpointNumber: number, length: number): Promise<USBInTransferResult>
    transferOut(endpointNumber: number, data: BufferSource): Promise<USBOutTransferResult>
  }

  interface USBConfiguration {
    readonly configurationName?: string
    readonly configurationValue: number
    readonly interfaces: USBInterface[]
  }

  interface USBInterface {
    readonly alternate: USBAlternateInterface
    readonly alternates: USBAlternateInterface[]
    readonly claimed: boolean
    readonly interfaceNumber: number
  }

  interface USBAlternateInterface {
    readonly alternateSetting: number
    readonly endpoints: USBEndpoint[]
    readonly interfaceClass: number
    readonly interfaceName?: string
    readonly interfaceProtocol: number
    readonly interfaceSubclass: number
  }

  interface USBEndpoint {
    readonly direction: USBDirection
    readonly endpointNumber: number
    readonly packetSize: number
    readonly type: USBEndpointType
  }

  type USBDirection = 'in' | 'out'
  type USBEndpointType = 'bulk' | 'interrupt' | 'isochronous'

  interface USBControlTransferParameters {
    requestType: USBRequestType
    recipient: USBRecipient
    request: number
    value: number
    index: number
  }

  type USBRequestType = 'standard' | 'class' | 'vendor'
  type USBRecipient = 'device' | 'interface' | 'endpoint' | 'other'

  interface USBInTransferResult {
    readonly data?: DataView
    readonly status: USBTransferStatus
  }

  interface USBOutTransferResult {
    readonly bytesWritten: number
    readonly status: USBTransferStatus
  }

  interface USBIsochronousInTransferResult {
    readonly data?: DataView
    readonly packets: USBIsochronousInTransferPacket[]
  }

  interface USBIsochronousOutTransferResult {
    readonly packets: USBIsochronousOutTransferPacket[]
  }

  interface USBIsochronousInTransferPacket {
    readonly data?: DataView
    readonly status: USBTransferStatus
  }

  interface USBIsochronousOutTransferPacket {
    readonly bytesWritten: number
    readonly status: USBTransferStatus
  }

  type USBTransferStatus = 'ok' | 'stall' | 'babble'

  interface USBDeviceFilter {
    vendorId?: number
    productId?: number
    classCode?: number
    subclassCode?: number
    protocolCode?: number
    serialNumber?: string
  }

  interface USBDeviceRequestOptions {
    filters: USBDeviceFilter[]
  }

  interface USB {
    getDevices(): Promise<USBDevice[]>
    requestDevice(options?: USBDeviceRequestOptions): Promise<USBDevice>
  }

  // Extend Navigator interface
  interface Navigator {
    bluetooth?: Bluetooth
    usb?: USB
  }
}

export {}
