"use client"

import { useState } from "react"
import { motion } from "framer-motion"
import { Check, X, AlertTriangle, Info } from "lucide-react"
import { cn } from "@/lib/utils"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"

interface Permission {
  id: string
  name: string
  description: string
  category: string
  yourAgent: boolean
  theirAgent: boolean
  risk: "low" | "medium" | "high"
}

const defaultPermissions: Permission[] = [
  {
    id: "read-calendar",
    name: "Read Calendar",
    description: "View your calendar events and availability",
    category: "Calendar",
    yourAgent: true,
    theirAgent: true,
    risk: "low",
  },
  {
    id: "create-events",
    name: "Create Events",
    description: "Create new calendar events on your behalf",
    category: "Calendar",
    yourAgent: true,
    theirAgent: false,
    risk: "medium",
  },
  {
    id: "modify-events",
    name: "Modify Events",
    description: "Edit or delete existing calendar events",
    category: "Calendar",
    yourAgent: true,
    theirAgent: false,
    risk: "high",
  },
  {
    id: "read-messages",
    name: "Read Messages",
    description: "View Slack messages in allowed channels",
    category: "Slack",
    yourAgent: true,
    theirAgent: false,
    risk: "medium",
  },
  {
    id: "send-messages",
    name: "Send Messages",
    description: "Send messages on your behalf",
    category: "Slack",
    yourAgent: true,
    theirAgent: false,
    risk: "medium",
  },
  {
    id: "view-contacts",
    name: "View Contacts",
    description: "Access your contact list and information",
    category: "Contacts",
    yourAgent: true,
    theirAgent: true,
    risk: "low",
  },
  {
    id: "share-availability",
    name: "Share Availability",
    description: "Share your free/busy status with external agents",
    category: "Privacy",
    yourAgent: true,
    theirAgent: true,
    risk: "low",
  },
  {
    id: "auto-accept",
    name: "Auto-Accept Invites",
    description: "Automatically accept meeting invitations that match criteria",
    category: "Automation",
    yourAgent: true,
    theirAgent: false,
    risk: "high",
  },
]

export function PermissionMatrix() {
  const [permissions, setPermissions] = useState(defaultPermissions)

  const togglePermission = (id: string, agent: "yourAgent" | "theirAgent") => {
    setPermissions((prev) =>
      prev.map((p) =>
        p.id === id
          ? { ...p, [agent]: !p[agent] }
          : p
      )
    )
  }

  const categories = [...new Set(permissions.map((p) => p.category))]

  const getRiskColor = (risk: string) => {
    switch (risk) {
      case "low":
        return "text-emerald"
      case "medium":
        return "text-amber"
      case "high":
        return "text-destructive"
      default:
        return "text-muted-foreground"
    }
  }

  return (
    <div className="glass-card rounded-xl overflow-hidden">
      <div className="p-6 border-b border-border">
        <h3 className="text-lg font-semibold text-foreground">
          Agent Permissions Matrix
        </h3>
        <p className="text-sm text-muted-foreground mt-1">
          Control what your agent and external agents can access
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border">
              <th className="text-left p-4 text-sm font-medium text-muted-foreground">
                Permission
              </th>
              <th className="text-center p-4 text-sm font-medium text-primary w-32">
                Your Agent
              </th>
              <th className="text-center p-4 text-sm font-medium text-secondary w-32">
                Their Agent
              </th>
              <th className="text-center p-4 text-sm font-medium text-muted-foreground w-24">
                Risk
              </th>
            </tr>
          </thead>
          <tbody>
            {categories.map((category) => (
              <>
                <tr key={category} className="bg-muted/30">
                  <td
                    colSpan={4}
                    className="p-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider"
                  >
                    {category}
                  </td>
                </tr>
                {permissions
                  .filter((p) => p.category === category)
                  .map((permission) => (
                    <motion.tr
                      key={permission.id}
                      className="border-b border-border/50 hover:bg-muted/20 transition-colors"
                      whileHover={{ backgroundColor: "rgba(15, 30, 53, 0.5)" }}
                    >
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          <span className="text-sm text-foreground">
                            {permission.name}
                          </span>
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger>
                                <Info className="w-3.5 h-3.5 text-muted-foreground" />
                              </TooltipTrigger>
                              <TooltipContent>
                                <p className="max-w-xs">
                                  {permission.description}
                                </p>
                              </TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                        </div>
                      </td>
                      <td className="p-4 text-center">
                        <button
                          onClick={() =>
                            togglePermission(permission.id, "yourAgent")
                          }
                          className={cn(
                            "w-8 h-8 rounded-lg flex items-center justify-center mx-auto transition-all",
                            permission.yourAgent
                              ? "bg-primary/20 text-primary"
                              : "bg-muted text-muted-foreground hover:bg-muted/80"
                          )}
                        >
                          {permission.yourAgent ? (
                            <Check className="w-4 h-4" />
                          ) : (
                            <X className="w-4 h-4" />
                          )}
                        </button>
                      </td>
                      <td className="p-4 text-center">
                        <button
                          onClick={() =>
                            togglePermission(permission.id, "theirAgent")
                          }
                          className={cn(
                            "w-8 h-8 rounded-lg flex items-center justify-center mx-auto transition-all",
                            permission.theirAgent
                              ? "bg-secondary/20 text-secondary"
                              : "bg-muted text-muted-foreground hover:bg-muted/80"
                          )}
                        >
                          {permission.theirAgent ? (
                            <Check className="w-4 h-4" />
                          ) : (
                            <X className="w-4 h-4" />
                          )}
                        </button>
                      </td>
                      <td className="p-4 text-center">
                        <span
                          className={cn(
                            "flex items-center justify-center gap-1 text-xs",
                            getRiskColor(permission.risk)
                          )}
                        >
                          {permission.risk === "high" && (
                            <AlertTriangle className="w-3 h-3" />
                          )}
                          {permission.risk.charAt(0).toUpperCase() +
                            permission.risk.slice(1)}
                        </span>
                      </td>
                    </motion.tr>
                  ))}
              </>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
