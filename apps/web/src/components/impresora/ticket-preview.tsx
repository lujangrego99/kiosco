"use client"

import { cn } from '@/lib/utils'

interface TicketPreviewProps {
  ticketText: string
  className?: string
}

export function TicketPreview({ ticketText, className }: TicketPreviewProps) {
  return (
    <div
      className={cn(
        "bg-white text-black font-mono text-xs p-4 rounded-lg border shadow-inner overflow-x-auto",
        "dark:bg-gray-100",
        className
      )}
    >
      <pre className="whitespace-pre-wrap break-words leading-tight">
        {ticketText}
      </pre>
    </div>
  )
}
