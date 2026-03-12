import { CoordinationTable } from "@/components/coordination/coordination-table"

export default function CoordinationsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground font-[family-name:var(--font-display)]">
          Coordinations
        </h1>
        <p className="text-foreground-secondary mt-1">
          View and manage all your agent coordinations
        </p>
      </div>
      
      <CoordinationTable />
    </div>
  )
}
