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
      <div className="p-6 bg-card/40 backdrop-blur-xl rounded-2xl border border-border/40 h-[264px] flex items-center justify-center">
        <div className="w-8 h-8 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
      </div>
    )
  }

  // Format the data for the chart, shortening date strings 
  const formattedData = data?.map(d => ({
    ...d,
    day: new Date(d.day).toLocaleDateString('en-US', { weekday: 'short' })
  })) || []

  return (
    <div className="p-6 bg-card/40 backdrop-blur-xl rounded-2xl border border-border/40 shadow-sm">
      <h3 className="text-lg font-semibold text-foreground mb-4 tracking-tight">
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
              <linearGradient id="colorRejected" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#f59e0b" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="colorFailed" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
              </linearGradient>
            </defs>
            <XAxis
              dataKey="day"
              stroke="currentColor"
              className="text-foreground/40"
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              stroke="currentColor"
              className="text-foreground/40"
              fontSize={12}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => `${value}`}
            />
            <Tooltip
              content={({ active, payload }) => {
                if (active && payload && payload.length) {
                  return (
                    <div className="bg-card/95 backdrop-blur-md border border-border/40 rounded-xl p-3 shadow-xl">
                      <p className="text-xs text-foreground/40 mb-1 font-medium">
                        {payload[0].payload.day}
                      </p>
                      <p className="text-sm font-semibold text-emerald-500">
                        {payload[0].payload.completed} completed
                      </p>
                      {payload[0].payload.rejected > 0 && (
                        <p className="text-sm font-semibold text-amber-500">
                          {payload[0].payload.rejected} rejected
                        </p>
                      )}
                      {payload[0].payload.failed > 0 && (
                        <p className="text-sm font-semibold text-rose-500">
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
              stackId="1"
              stroke="#10b981"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#colorCompleted)"
            />
            <Area
              type="monotone"
              dataKey="rejected"
              stackId="1"
              stroke="#f59e0b"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#colorRejected)"
            />
            <Area
              type="monotone"
              dataKey="failed"
              stackId="1"
              stroke="#ef4444"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#colorFailed)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
