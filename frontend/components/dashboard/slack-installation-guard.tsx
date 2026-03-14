"use client"

import { useState } from "react"
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog"
import { Slack } from "lucide-react"
import { authAPI } from "@/lib/api"

interface SlackInstallationGuardProps {
  isOpen: boolean
  onDismiss: () => void
}

export function SlackInstallationGuard({ isOpen, onDismiss }: SlackInstallationGuardProps) {
  const [isLoading, setIsLoading] = useState(false)

  const handleInstall = async () => {
    setIsLoading(true)
    try {
      // Redirect to Slack installation endpoint
      window.location.href = authAPI.slackInstallStart()
    } catch (err) {
      console.error("Failed to initiate Slack installation:", err)
      setIsLoading(false)
    }
  }

  return (
    <AlertDialog open={isOpen} onOpenChange={onDismiss}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <div className="flex items-center gap-3 mb-2">
            <div className="w-12 h-12 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center">
              <Slack className="w-6 h-6 text-primary" />
            </div>
            <AlertDialogTitle>Install CoAgent4U on your Slack workspace</AlertDialogTitle>
          </div>
          <AlertDialogDescription>
            To enable your agent to coordinate with others on Slack, please install the CoAgent4U app on your workspace. This will allow seamless integration and communication.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <div className="flex gap-3 mt-6">
          <AlertDialogCancel onClick={onDismiss}>
            Maybe later
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleInstall}
            disabled={isLoading}
            className="gap-2"
          >
            {isLoading ? (
              <>
                <div className="w-4 h-4 border-2 border-background/30 border-t-background rounded-full animate-spin" />
                Installing...
              </>
            ) : (
              <>
                <Slack className="w-4 h-4" />
                Install CoAgent4U
              </>
            )}
          </AlertDialogAction>
        </div>
      </AlertDialogContent>
    </AlertDialog>
  )
}
