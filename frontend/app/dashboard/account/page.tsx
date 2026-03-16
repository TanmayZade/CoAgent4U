"use client"

import { useState } from "react"
import { useUser } from "../layout"
import { accountAPI } from "@/lib/api/account"
import { authAPI } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Download, Trash2, ShieldCheck, AlertTriangle } from "lucide-react"

export default function AccountPage() {
  const { user, isLoading } = useUser()
  const [isDeleting, setIsDeleting] = useState(false)

  const handleExport = () => {
    if (!user) return
    const url = accountAPI.exportDataUrl(user.username)
    window.open(url, '_blank')
  }

  const handleDelete = async () => {
    if (!user) return
    const confirmDelete = window.confirm(
      "Are you absolutely sure you want to delete your CoAgent4U account? This action is irreversible and will delete all your coordination history, agent activity logs, and agent settings."
    )
    if (!confirmDelete) return

    setIsDeleting(true)
    try {
      await accountAPI.deleteAccount(user.username)
      // On success, logout and redirect to signin
      await authAPI.logout()
      window.location.href = "/signin"
    } catch (error) {
      console.error("Failed to delete account:", error)
      alert("Failed to delete account. Please try again later.")
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Account & Data</h1>
        <p className="text-foreground/60 text-sm mt-1">
          Control your identity, data ownership, and account lifecycle.
        </p>
      </div>

      {/* Profile Card */}
      <div className="rounded-xl border border-border/50 bg-card p-6 shadow-sm">
        <h3 className="text-lg font-semibold tracking-tight mb-6">Profile Information</h3>
        <div className="flex flex-col sm:flex-row gap-6 items-start">
          <div className="w-20 h-20 rounded-full bg-primary/10 border border-primary/20 flex flex-shrink-0 items-center justify-center text-primary text-2xl font-semibold overflow-hidden">
            {isLoading ? (
              <div className="w-full h-full bg-muted animate-pulse" />
            ) : user?.slack_avatar_url ? (
              <img src={user.slack_avatar_url} alt={user.slack_name} className="w-full h-full object-cover" />
            ) : (
              (user?.slack_name || user?.username || "?")?.substring(0, 2).toUpperCase()
            )}
          </div>
          <div className="flex-1 space-y-4 w-full">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-1">
                <p className="text-xs text-foreground/50 uppercase tracking-wider font-semibold">Display Name / Identity</p>
                <p className="font-medium">{isLoading ? "..." : user?.slack_name}</p>
              </div>
              <div className="space-y-1">
                <p className="text-xs text-foreground/50 uppercase tracking-wider font-semibold">Workspace</p>
                <p className="font-medium">{isLoading ? "..." : user?.slack_workspace}</p>
              </div>
              <div className="space-y-1">
                <p className="text-xs text-foreground/50 uppercase tracking-wider font-semibold">Account ID (Username)</p>
                <p className="font-medium font-mono text-sm">{isLoading ? "..." : user?.username}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Data Transparency Block */}
      <div className="rounded-xl border border-blue-500/30 bg-blue-500/5 p-6 shadow-sm relative overflow-hidden">
        <div className="absolute left-0 top-0 bottom-0 w-1 bg-blue-500" />
        <h3 className="text-lg font-semibold tracking-tight mb-4 flex items-center gap-2 text-blue-500">
          <ShieldCheck className="w-5 h-5" />
          Data Transparency Guarantee
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-12">
          <div>
            <h4 className="font-semibold text-foreground mb-3 text-sm">What we store (and you own):</h4>
            <ul className="space-y-2 text-sm text-foreground/70 list-disc list-inside">
              <li>Your linked Slack identity (Name, Workspace ID)</li>
              <li>Google Calendar OAuth token (Encrypted at rest AES-256)</li>
              <li>Coordination lifecycle logs (Retained for 90 days)</li>
              <li>Agent action audit trail (Retained for 90 days)</li>
            </ul>
          </div>
          <div>
            <h4 className="font-semibold text-foreground mb-3 text-sm">What we explicitly DO NOT store:</h4>
            <ul className="space-y-2 text-sm text-foreground/70 list-disc list-inside">
              <li>Calendar event contents besides coordinations</li>
              <li>Raw Slack message contents or DM history</li>
              <li>Personal files, contacts, or generic preferences</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Data Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="rounded-xl border border-border/50 bg-card p-6 shadow-sm flex flex-col items-start">
          <h3 className="text-lg font-semibold tracking-tight mb-2">GDPR Data Export</h3>
          <p className="text-sm text-foreground/60 mb-6 flex-1">
            Download a machine-readable JSON archive containing your profile, active configurations, full coordination history, and the complete agent activity log.
          </p>
          <Button onClick={handleExport} className="w-full sm:w-auto mt-auto gap-2">
            <Download className="w-4 h-4" />
            Export My Data
          </Button>
        </div>

        {/* Danger Zone */}
        <div className="rounded-xl border border-rose-500/30 bg-rose-500/5 p-6 shadow-sm flex flex-col items-start relative overflow-hidden">
          <div className="absolute left-0 top-0 bottom-0 w-1 bg-rose-500" />
          <h3 className="text-lg font-semibold tracking-tight mb-2 text-rose-500 flex items-center gap-2">
            <AlertTriangle className="w-5 h-5" />
            Danger Zone
          </h3>
          <p className="text-sm text-foreground/60 mb-6 flex-1">
            Permanently delete your account. This will immediately revoke all connected OAuth tokens and schedule your data for hard deletion within 30 days.
          </p>
          <Button 
            variant="destructive" 
            onClick={handleDelete} 
            disabled={isDeleting || isLoading}
            className="w-full sm:w-auto mt-auto gap-2"
          >
            {isDeleting ? (
              <div className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
            ) : (
              <Trash2 className="w-4 h-4" />
            )}
            Delete My Account
          </Button>
        </div>
      </div>
    </div>
  )
}
