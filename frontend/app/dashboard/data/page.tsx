"use client"

import { Calendar, MessageSquare, Users, Shield, Database, Trash2, Download, Clock } from "lucide-react"
import { IntegrationCard } from "@/components/settings/integration-card"
import { PermissionMatrix } from "@/components/settings/permission-matrix"
import { Button } from "@/components/ui/button"
import { GlowCard } from "@/components/common/glow-card"

const integrations = [
  {
    name: "Google Calendar",
    description: "Sync your calendar for scheduling coordination",
    icon: <Calendar className="w-6 h-6" />,
    connected: true,
    lastSync: "2 minutes ago",
    permissions: [
      "View calendar events",
      "Create new events",
      "Modify existing events",
      "Access free/busy status",
    ],
  },
  {
    name: "Slack",
    description: "Connect Slack for meeting notifications and coordination",
    icon: <MessageSquare className="w-6 h-6" />,
    connected: true,
    lastSync: "5 minutes ago",
    permissions: [
      "Read messages in selected channels",
      "Send messages on your behalf",
      "Create private channels",
      "Add calendar integrations",
    ],
  },
  {
    name: "Microsoft Teams",
    description: "Integrate with Teams for enterprise coordination",
    icon: <Users className="w-6 h-6" />,
    connected: false,
    permissions: [],
  },
  {
    name: "Outlook Calendar",
    description: "Sync your Outlook calendar for scheduling",
    icon: <Calendar className="w-6 h-6" />,
    connected: false,
    permissions: [],
  },
]

const dataStats = [
  {
    label: "Calendar Events",
    value: "1,234",
    description: "Events synced",
    icon: <Calendar className="w-5 h-5" />,
  },
  {
    label: "Messages Processed",
    value: "8,521",
    description: "Slack messages analyzed",
    icon: <MessageSquare className="w-5 h-5" />,
  },
  {
    label: "Contacts",
    value: "342",
    description: "External contacts",
    icon: <Users className="w-5 h-5" />,
  },
  {
    label: "Data Retention",
    value: "90 days",
    description: "Automatic cleanup",
    icon: <Clock className="w-5 h-5" />,
  },
]

export default function DataPage() {
  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-cream font-[family-name:var(--font-display)]">
          Data & Permissions
        </h1>
        <p className="text-cream/70 mt-1">
          Manage your integrations, data access, and agent permissions
        </p>
      </div>

      {/* Data Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {dataStats.map((stat, index) => (
          <div
            key={stat.label}
            className="animate-[fadeInUp_0.4s_ease-out_forwards]"
            style={{ animationDelay: `${index * 100}ms`, opacity: 0 }}
          >
            <GlowCard className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-accent/10 flex items-center justify-center text-accent">
                  {stat.icon}
                </div>
                <div>
                  <p className="text-2xl font-bold text-cream">
                    {stat.value}
                  </p>
                  <p className="text-xs text-cream/50">
                    {stat.description}
                  </p>
                </div>
              </div>
            </GlowCard>
          </div>
        ))}
      </div>

      {/* Integrations */}
      <div>
        <h2 className="text-lg font-semibold text-cream mb-4 flex items-center gap-2">
          <Shield className="w-5 h-5 text-accent" />
          Connected Integrations
        </h2>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {integrations.map((integration, index) => (
            <div
              key={integration.name}
              className="animate-[fadeInUp_0.4s_ease-out_forwards]"
              style={{ animationDelay: `${200 + index * 100}ms`, opacity: 0 }}
            >
              <IntegrationCard
                {...integration}
                onConnect={() => {}}
                onDisconnect={() => {}}
                onSync={() => {}}
              />
            </div>
          ))}
        </div>
      </div>

      {/* Permission Matrix */}
      <div
        className="animate-[fadeInUp_0.4s_ease-out_forwards]"
        style={{ animationDelay: '600ms', opacity: 0 }}
      >
        <PermissionMatrix />
      </div>

      {/* Data Management */}
      <div
        className="animate-[fadeInUp_0.4s_ease-out_forwards]"
        style={{ animationDelay: '700ms', opacity: 0 }}
      >
        <GlowCard className="p-6">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-charcoal-lighter flex items-center justify-center">
                <Database className="w-6 h-6 text-cream/50" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-cream">
                  Data Management
                </h3>
                <p className="text-sm text-cream/50">
                  Export or delete your coordination data
                </p>
              </div>
            </div>
            <div className="flex gap-3">
              <Button variant="outline" className="border-charcoal-lighter">
                <Download className="w-4 h-4 mr-2" />
                Export Data
              </Button>
              <Button
                variant="outline"
                className="border-red-500/50 text-red-500 hover:bg-red-500/10"
              >
                <Trash2 className="w-4 h-4 mr-2" />
                Delete All Data
              </Button>
            </div>
          </div>

          <div className="mt-6 pt-6 border-t border-charcoal-lighter">
            <h4 className="text-sm font-medium text-cream mb-4">
              Data Retention Policy
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-4 rounded-lg bg-charcoal-light">
                <p className="text-sm font-medium text-cream">
                  Coordination Logs
                </p>
                <p className="text-xs text-cream/50 mt-1">
                  Retained for 90 days, then automatically deleted
                </p>
              </div>
              <div className="p-4 rounded-lg bg-charcoal-light">
                <p className="text-sm font-medium text-cream">
                  Calendar Sync Data
                </p>
                <p className="text-xs text-cream/50 mt-1">
                  Real-time sync, no historical storage
                </p>
              </div>
              <div className="p-4 rounded-lg bg-charcoal-light">
                <p className="text-sm font-medium text-cream">
                  Message Analysis
                </p>
                <p className="text-xs text-cream/50 mt-1">
                  Processed in real-time, metadata only stored
                </p>
              </div>
            </div>
          </div>
        </GlowCard>
      </div>
    </div>
  )
}
