"use client"

import { useState } from "react"
import { motion } from "framer-motion"
import { Check, ExternalLink, RefreshCw, Settings, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import { cn } from "@/lib/utils"

interface IntegrationCardProps {
  name: string
  description: string
  icon: React.ReactNode
  connected: boolean
  lastSync?: string
  permissions?: string[]
  onConnect?: () => void
  onDisconnect?: () => void
  onSync?: () => void
}

export function IntegrationCard({
  name,
  description,
  icon,
  connected,
  lastSync,
  permissions = [],
  onConnect,
  onDisconnect,
  onSync,
}: IntegrationCardProps) {
  const [isExpanded, setIsExpanded] = useState(false)
  const [isSyncing, setIsSyncing] = useState(false)

  const handleSync = async () => {
    setIsSyncing(true)
    onSync?.()
    setTimeout(() => setIsSyncing(false), 2000)
  }

  return (
    <motion.div
      layout
      className={cn(
        "glass-card rounded-xl overflow-hidden transition-all duration-300",
        connected && "glow-border"
      )}
    >
      <div className="p-6">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-4">
            <div
              className={cn(
                "w-12 h-12 rounded-xl flex items-center justify-center",
                connected
                  ? "bg-primary/20 text-primary"
                  : "bg-muted text-muted-foreground"
              )}
            >
              {icon}
            </div>
            <div>
              <h3 className="font-semibold text-foreground flex items-center gap-2">
                {name}
                {connected && (
                  <span className="flex items-center gap-1 text-xs text-emerald px-2 py-0.5 rounded-full bg-emerald/10">
                    <Check className="w-3 h-3" />
                    Connected
                  </span>
                )}
              </h3>
              <p className="text-sm text-muted-foreground">{description}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {connected ? (
              <>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleSync}
                  disabled={isSyncing}
                  className="text-muted-foreground hover:text-primary"
                >
                  <RefreshCw
                    className={cn("w-4 h-4", isSyncing && "animate-spin")}
                  />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setIsExpanded(!isExpanded)}
                  className="text-muted-foreground hover:text-primary"
                >
                  <Settings className="w-4 h-4" />
                </Button>
              </>
            ) : (
              <Button
                onClick={onConnect}
                className="bg-primary text-primary-foreground hover:bg-primary/90"
              >
                Connect
                <ExternalLink className="w-4 h-4 ml-2" />
              </Button>
            )}
          </div>
        </div>

        {connected && lastSync && (
          <p className="text-xs text-muted-foreground mt-3">
            Last synced: {lastSync}
          </p>
        )}

        {isExpanded && connected && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            className="mt-6 pt-6 border-t border-border"
          >
            <h4 className="text-sm font-medium text-foreground mb-4">
              Permissions
            </h4>
            <div className="space-y-3">
              {permissions.map((permission, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between py-2"
                >
                  <span className="text-sm text-muted-foreground">
                    {permission}
                  </span>
                  <Switch defaultChecked />
                </div>
              ))}
            </div>

            <div className="mt-6 pt-4 border-t border-border">
              <Button
                variant="destructive"
                onClick={onDisconnect}
                className="w-full"
              >
                <X className="w-4 h-4 mr-2" />
                Disconnect {name}
              </Button>
            </div>
          </motion.div>
        )}
      </div>
    </motion.div>
  )
}
