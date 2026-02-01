import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Cadena, KioscoResumen } from '@/types';

interface CadenaStore {
  cadenas: Cadena[];
  cadenaActual: Cadena | null;
  kioscoActual: KioscoResumen | null;
  vistaConsolidada: boolean;

  // Actions
  setCadenas: (cadenas: Cadena[]) => void;
  setCadenaActual: (cadena: Cadena | null) => void;
  setKioscoActual: (kiosco: KioscoResumen | null) => void;
  setVistaConsolidada: (consolidada: boolean) => void;
  selectKiosco: (kioscoId: string | 'todos') => void;
  clear: () => void;
}

export const useCadenaStore = create<CadenaStore>()(
  persist(
    (set, get) => ({
      cadenas: [],
      cadenaActual: null,
      kioscoActual: null,
      vistaConsolidada: false,

      setCadenas: (cadenas: Cadena[]) => {
        set({ cadenas });
        // If we have cadenas and no current one selected, select the first
        if (cadenas.length > 0 && !get().cadenaActual) {
          set({ cadenaActual: cadenas[0] });
        }
      },

      setCadenaActual: (cadena: Cadena | null) => {
        set({
          cadenaActual: cadena,
          kioscoActual: null,
          vistaConsolidada: false,
        });
      },

      setKioscoActual: (kiosco: KioscoResumen | null) => {
        set({
          kioscoActual: kiosco,
          vistaConsolidada: kiosco === null,
        });
      },

      setVistaConsolidada: (consolidada: boolean) => {
        set({
          vistaConsolidada: consolidada,
          kioscoActual: consolidada ? null : get().kioscoActual,
        });
      },

      selectKiosco: (kioscoId: string | 'todos') => {
        const cadena = get().cadenaActual;
        if (!cadena || !cadena.kioscos) return;

        if (kioscoId === 'todos') {
          set({ kioscoActual: null, vistaConsolidada: true });
        } else {
          const kiosco = cadena.kioscos.find((k) => k.id === kioscoId);
          if (kiosco) {
            set({ kioscoActual: kiosco, vistaConsolidada: false });
          }
        }
      },

      clear: () => {
        set({
          cadenas: [],
          cadenaActual: null,
          kioscoActual: null,
          vistaConsolidada: false,
        });
      },
    }),
    {
      name: 'cadena-storage',
      partialize: (state) => ({
        cadenaActual: state.cadenaActual,
        kioscoActual: state.kioscoActual,
        vistaConsolidada: state.vistaConsolidada,
      }),
    }
  )
);
