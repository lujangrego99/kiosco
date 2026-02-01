'use client';

import { useEffect, useState, useCallback } from 'react';
import { Store, Building2, Layers } from 'lucide-react';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  SelectSeparator,
} from '@/components/ui/select';
import { useCadenaStore } from '@/stores/cadena-store';
import { cadenasApi } from '@/lib/api';
import type { KioscoResumen } from '@/types';

interface KioscoSelectorProps {
  className?: string;
}

export function KioscoSelector({ className }: KioscoSelectorProps) {
  const {
    cadenas,
    cadenaActual,
    kioscoActual,
    vistaConsolidada,
    setCadenas,
    setCadenaActual,
    selectKiosco,
  } = useCadenaStore();

  const [loading, setLoading] = useState(true);
  const [kioscos, setKioscos] = useState<KioscoResumen[]>([]);

  const loadCadenas = useCallback(async () => {
    try {
      const data = await cadenasApi.listar();
      setCadenas(data);
    } catch (error) {
      console.error('Error loading cadenas:', error);
    } finally {
      setLoading(false);
    }
  }, [setCadenas]);

  const loadKioscos = useCallback(async (cadenaId: string) => {
    try {
      const data = await cadenasApi.listarKioscos(cadenaId);
      setKioscos(data);
      // Update cadena with kioscos
      if (cadenaActual) {
        setCadenaActual({ ...cadenaActual, kioscos: data });
      }
    } catch (error) {
      console.error('Error loading kioscos:', error);
    }
  }, [cadenaActual, setCadenaActual]);

  useEffect(() => {
    loadCadenas();
  }, [loadCadenas]);

  useEffect(() => {
    if (cadenaActual) {
      loadKioscos(cadenaActual.id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cadenaActual?.id, loadKioscos]);

  // If no cadenas, don't show anything
  if (!loading && cadenas.length === 0) {
    return null;
  }

  // If only one kiosco and no cadenas with multiple, don't show selector
  if (!loading && cadenas.length === 1 && kioscos.length <= 1) {
    return null;
  }

  const currentValue = vistaConsolidada
    ? 'todos'
    : kioscoActual?.id || 'todos';

  const currentLabel = vistaConsolidada
    ? 'Todos los kioscos'
    : kioscoActual?.nombre || 'Seleccionar';

  return (
    <div className={className}>
      <Select
        value={currentValue}
        onValueChange={(value) => selectKiosco(value as string | 'todos')}
      >
        <SelectTrigger className="w-[200px] bg-card">
          <div className="flex items-center gap-2">
            {vistaConsolidada ? (
              <Layers className="h-4 w-4 text-primary" />
            ) : kioscoActual?.esCasaCentral ? (
              <Building2 className="h-4 w-4 text-primary" />
            ) : (
              <Store className="h-4 w-4 text-muted-foreground" />
            )}
            <SelectValue placeholder="Seleccionar kiosco">
              {currentLabel}
            </SelectValue>
          </div>
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="todos">
            <div className="flex items-center gap-2">
              <Layers className="h-4 w-4 text-primary" />
              <span>Todos los kioscos</span>
            </div>
          </SelectItem>
          <SelectSeparator />
          {kioscos.map((kiosco) => (
            <SelectItem key={kiosco.id} value={kiosco.id}>
              <div className="flex items-center gap-2">
                {kiosco.esCasaCentral ? (
                  <Building2 className="h-4 w-4 text-primary" />
                ) : (
                  <Store className="h-4 w-4 text-muted-foreground" />
                )}
                <span>{kiosco.nombre}</span>
                {kiosco.esCasaCentral && (
                  <span className="text-xs text-muted-foreground">(Central)</span>
                )}
              </div>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
