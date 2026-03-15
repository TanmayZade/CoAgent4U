"use client"

import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts"
import { ActivityDataPoint } from "@/lib/api/dashboard"

interface ActivityChartProps {
  data?: ActivityDataPoint[]
  isLoading?: boolean
}

export function ActivityChart({ data, isLoading }: ActivityChartProps) {
  if (isLoading) {
    return (
      <div className="p-6 bg-charcoal-light rounded-xl border border-border h-[264px] flex items-center justify-center">
        <div className="w-8 h-8 rounded-full border-2 border-accent border-r-transparent animate-spin" />
      </div>
    )
  }

  // Format the data for the chart, shortening date strings 
  const formattedData = data?.map(d => ({
    ...d,
    day: new Date(d.date).toLocaleDateString('en-US', { weekday: 'short' })
  })) || []

  return (
    <div className="p-6 bg-charcoal-light rounded-xl border border-border">
      <h3 className="text-lg font-semibold text-cream mb-4 font-[family-name:var(--font-display)]">
        Coordination Activity
      </h3>
      <div className="h-48">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={formattedData}>
            <defs>
              <linearGradient id="colorCompleted" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
              </linearGradient>
            </defs>
            <XAxis
              dataKey="day"
              stroke="#6B6966"
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              stroke="#6B6966"
              fontSize={12}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => `${value}`}
            />
            <Tooltip
              content={({ active, payload }) => {
                if (active && payload && payload.length) {
                  return (
                    <div className="bg-charcoal border border-border rounded-lg p-3 shadow-lg">
                      <p className="text-xs text-foreground-muted mb-1">
                        {payload[0].payload.day}
                      </p>
                      <p className="text-sm font-medium text-emerald-500">
                        {payload[0].payload.completed} completed
                      </p>
                      {payload[0].payload.rejected > 0 && (
                        <p className="text-sm font-medium text-rose-500">
                          {payload[0].payload.rejected} rejected
                        </p>
                      )}
                      {payload[0].payload.failed > 0 && (
                        <p className="text-sm font-medium text-red-500">
                          {payload[0].payload.failed} failed
                        </p>
                      )}
                    </div>
                  )
                }
                return null
              }}
            />
            <Area
              type="monotone"
              dataKey="completed"
              stroke="#10b981"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#colorCompleted)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
