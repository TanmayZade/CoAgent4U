import { cn } from "@/lib/utils"

export type CoordinationState =
  | "INITIATED"
  | "CHECKING_AVAILABILITY_A"
  | "CHECKING_AVAILABILITY_B"
  | "MATCHING"
  | "PROPOSAL_GENERATED"
  | "AWAITING_APPROVAL_A"
  | "AWAITING_APPROVAL_B"
  | "APPROVED_BY_BOTH"
  | "CREATING_EVENT_A"
  | "CREATING_EVENT_B"
  | "COMPLETED"
  | "REJECTED"
  | "FAILED"

interface StatusChipProps {
  state: CoordinationState
  className?: string
}

export function StatusChip({ state, className }: StatusChipProps) {
  let colorClass = ""
  let labelClass = ""
  let isAnimated = false
  let showCheck = false

  switch (state) {
    case "INITIATED":
    case "CHECKING_AVAILABILITY_A":
    case "CHECKING_AVAILABILITY_B":
    case "MATCHING":
      colorClass = "bg-blue-500/10 border-blue-500/20 text-blue-500 font-medium"
      labelClass = "text-blue-500"
      isAnimated = state !== "INITIATED"
      break
    case "PROPOSAL_GENERATED":
    case "AWAITING_APPROVAL_A":
    case "AWAITING_APPROVAL_B":
      colorClass = "bg-amber-500/20 border-amber-500/30 text-amber-500 font-semibold"
      labelClass = "text-amber-500"
      isAnimated = state.startsWith("AWAITING_APPROVAL")
      break
    case "APPROVED_BY_BOTH":
    case "CREATING_EVENT_A":
    case "CREATING_EVENT_B":
      colorClass = "bg-emerald-500/10 border-emerald-500/20 text-emerald-500 font-medium"
      labelClass = "text-emerald-500"
      isAnimated = state !== "APPROVED_BY_BOTH"
      break
    case "COMPLETED":
      colorClass = "bg-emerald-500 border-emerald-600 text-white font-semibold"
      labelClass = "text-white"
      showCheck = true
      break
    case "REJECTED":
      colorClass = "bg-rose-500 border-rose-600 text-white font-semibold"
      labelClass = "text-white"
      break
    case "FAILED":
      colorClass = "bg-red-500 border-red-600 text-white font-semibold"
      labelClass = "text-white"
      break
  }

  return (
    <div
      className={cn(
        "inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full border text-[11px] leading-tight select-none shadow-sm",
        colorClass,
        className
      )}
    >
      {isAnimated && (
        <span className="relative flex h-1.5 w-1.5 shrink-0">
          <span className={cn("absolute inline-flex h-full w-full rounded-full opacity-75 animate-ping", labelClass.replace('text-', 'bg-'))} />
          <span className={cn("relative inline-flex rounded-full h-1.5 w-1.5", labelClass.replace('text-', 'bg-'))} />
        </span>
      )}
      <span>{state}</span>
      {showCheck && (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 20 20"
          fill="currentColor"
          className="w-3 h-3 ml-0.5 shrink-0"
        >
          <path
            fillRule="evenodd"
            d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z"
            clipRule="evenodd"
          />
        </svg>
      )}
    </div>
  )
}
