"use client"

import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts"

const data = [
  { day: "Mon", coordinations: 2 },
  { day: "Tue", coordinations: 4 },
  { day: "Wed", coordinations: 3 },
  { day: "Thu", coordinations: 5 },
  { day: "Fri", coordinations: 4 },
  { day: "Sat", coordinations: 1 },
  { day: "Sun", coordinations: 3 },
]

export function ActivityChart() {
  return (
    <div className="p-6 bg-charcoal-light rounded-xl border border-border">
      <h3 className="text-lg font-semibold text-cream mb-4 font-[family-name:var(--font-display)]">
        Coordination Activity
      </h3>
      <div className="h-48">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data}>
            <defs>
              <linearGradient id="colorCoord" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#E85A2C" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#E85A2C" stopOpacity={0} />
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
                      <p className="text-xs text-foreground-muted">
                        {payload[0].payload.day}
                      </p>
                      <p className="text-sm font-medium text-accent">
                        {payload[0].value} coordinations
                      </p>
                    </div>
                  )
                }
                return null
              }}
            />
            <Area
              type="monotone"
              dataKey="coordinations"
              stroke="#E85A2C"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#colorCoord)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
