"use client"

import { useUser } from "../layout"
import { Button } from "@/components/ui/button"
import { Slack, Calendar, CheckCircle2, ShieldAlert, Loader2 } from "lucide-react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { integrationAPI } from "@/lib/api"
import { toast } from "sonner"

export default function IntegrationsPage() {
  const { user, isLoading } = useUser()
  const queryClient = useQueryClient()

  const { data: googleStatus, isLoading: isGoogleLoading } = useQuery({
    queryKey: ["google-status"],
    queryFn: integrationAPI.googleStatus,
    retry: false,
  })

  const disconnectMutation = useMutation({
    mutationFn: integrationAPI.googleDisconnect,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["google-status"] })
      toast.success("Google Calendar disconnected successfully")
    },
    onError: () => {
      toast.error("Failed to disconnect Google Calendar")
    }
  })

  const handleConnect = () => {
    window.location.href = integrationAPI.googleAuthorize('/dashboard/integrations')
  }

  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Integrations</h1>
        <p className="text-foreground/60 text-sm mt-1">
          Manage the services your agent can access and interact with.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Slack Card */}
        <div className="rounded-xl border border-border/50 bg-card p-6 shadow-sm relative overflow-hidden">
          <div className="flex items-start justify-between mb-6 relative z-10">
            <div className="w-12 h-12 rounded-xl bg-[#4A154B]/10 flex items-center justify-center border border-[#4A154B]/20">
              <Slack className="w-6 h-6 text-[#4A154B] dark:text-[#E01E5A]" />
            </div>
            <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 text-xs font-semibold">
              <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              Connected
            </div>
          </div>
          
          <div className="relative z-10">
            <h3 className="text-lg font-semibold tracking-tight text-foreground mb-1">Slack</h3>
            <p className="text-sm text-foreground/60 mb-6">
              Primary communication channel. Required for agent human-in-the-loop approvals.
            </p>
            
            {isLoading ? (
              <div className="h-20 animate-pulse bg-muted/50 rounded-lg" />
            ) : (
              <div className="space-y-3 p-4 bg-muted/20 border border-border/50 rounded-lg">
                <div className="flex justify-between items-center text-sm">
                  <span className="text-foreground/60">Workspace</span>
                  <span className="font-medium">{user?.slack_workspace}</span>
                </div>
                <div className="flex justify-between items-center text-sm">
                  <span className="text-foreground/60">Identity</span>
                  <span className="font-medium font-mono">{user?.slack_name}</span>
                </div>
              </div>
            )}
            
            <p className="text-xs text-foreground/40 mt-4 flex items-center gap-1.5">
              <ShieldAlert className="w-3.5 h-3.5" />
              Cannot be disconnected (Core identity provider)
            </p>
          </div>
        </div>

        {/* Google Calendar Card */}
        <div className="rounded-xl border border-border/50 bg-card p-6 shadow-sm relative overflow-hidden">
          <div className="flex items-start justify-between mb-6 relative z-10">
            <div className="w-12 h-12 rounded-xl bg-blue-500/10 flex items-center justify-center border border-blue-500/20">
              <Calendar className="w-6 h-6 text-blue-500" />
            </div>
            {isGoogleLoading ? (
               <div className="w-24 h-6 bg-muted/50 animate-pulse rounded-full" />
            ) : googleStatus?.connected ? (
              <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 text-xs font-semibold">
                <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                Connected
              </div>
            ) : (
              <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-muted border border-border/50 text-foreground/60 text-xs font-semibold">
                Not Connected
              </div>
            )}
          </div>
          
          <div className="relative z-10">
            <h3 className="text-lg font-semibold tracking-tight text-foreground mb-1">Google Calendar & Tasks</h3>
            <p className="text-sm text-foreground/60 mb-6">
              Required for checking your availability, scheduling meetings, and managing to-do lists.
            </p>
            
            {isGoogleLoading ? (
              <div className="h-24 animate-pulse bg-muted/50 rounded-lg" />
            ) : googleStatus?.connected ? (
              <div className="space-y-4">
                <div className="space-y-3 p-4 bg-muted/20 border border-border/50 rounded-lg">
                  <div className="flex justify-between items-center text-sm">
                    <span className="text-foreground/60">Account</span>
                    <span className="font-medium truncate max-w-[150px]">{user?.username}</span>
                  </div>
                  <div className="flex justify-between items-center text-sm">
                    <span className="text-foreground/60">Sync Status</span>
                    <span className="font-medium text-emerald-500 flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Healthy
                    </span>
                  </div>
                </div>
                
                <Button 
                  variant="outline" 
                  className="w-full border-rose-500/50 text-rose-500 hover:bg-rose-500/10"
                  onClick={() => disconnectMutation.mutate()}
                  disabled={disconnectMutation.isPending}
                >
                  {disconnectMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : null}
                  Disconnect Calendar & Tasks
                </Button>
              </div>
            ) : (
              <div className="space-y-4">
                 <div className="flex items-center justify-center p-4 border border-dashed border-border/60 rounded-lg bg-muted/10 min-h-[92px]">
                   <p className="text-sm text-foreground/50 text-center">
                     Connect your calendar to enable unified coordination.
                   </p>
                 </div>
                <Button 
                  className="w-full bg-blue-600 hover:bg-blue-700 text-white"
                  onClick={handleConnect}
                >
                  Connect Google Calendar & Tasks
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Permissions Matrix */}
      <div className="rounded-xl border border-border/50 bg-card overflow-hidden shadow-sm">
        <div className="p-6 border-b border-border/50">
          <h3 className="text-lg font-semibold tracking-tight">Agent Access Matrix</h3>
          <p className="text-foreground/60 text-sm mt-1">What your agent is explicitly allowed to do on these services.</p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-muted/30 text-foreground/60 text-xs uppercase font-medium">
              <tr>
                <th className="px-6 py-4">Resource</th>
                <th className="px-6 py-4">Access Level</th>
                <th className="px-6 py-4">Used For</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/50">
              <tr className="hover:bg-muted/20 transition-colors">
                <td className="px-6 py-4 font-medium flex items-center gap-2"><Calendar className="w-4 h-4 text-blue-500" /> Calendar Events</td>
                <td className="px-6 py-4"><span className="px-2 py-0.5 rounded text-[10px] font-semibold tracking-wider border bg-emerald-500/10 text-emerald-500 border-emerald-500/20">Read / Write</span></td>
                <td className="px-6 py-4 text-foreground/70">Creating confirmed meetings</td>
              </tr>
              <tr className="hover:bg-muted/20 transition-colors">
                <td className="px-6 py-4 font-medium flex items-center gap-2"><Calendar className="w-4 h-4 text-blue-500" /> Free / Busy Status</td>
                <td className="px-6 py-4"><span className="px-2 py-0.5 rounded text-[10px] font-semibold tracking-wider border bg-blue-500/10 text-blue-500 border-blue-500/20">Read Only</span></td>
                <td className="px-6 py-4 text-foreground/70">Checking availability across time zones</td>
              </tr>
              <tr className="hover:bg-muted/20 transition-colors">
                <td className="px-6 py-4 font-medium flex items-center gap-2"><CheckCircle2 className="w-4 h-4 text-blue-500" /> Google Tasks</td>
                <td className="px-6 py-4"><span className="px-2 py-0.5 rounded text-[10px] font-semibold tracking-wider border bg-emerald-500/10 text-emerald-500 border-emerald-500/20">Read / Write</span></td>
                <td className="px-6 py-4 text-foreground/70">Creating and managing to-do lists and tasks</td>
              </tr>
              <tr className="hover:bg-muted/20 transition-colors">
                <td className="px-6 py-4 font-medium flex items-center gap-2"><Slack className="w-4 h-4 text-[#E01E5A]" /> Slack Profile</td>
                <td className="px-6 py-4"><span className="px-2 py-0.5 rounded text-[10px] font-semibold tracking-wider border bg-blue-500/10 text-blue-500 border-blue-500/20">Read Only</span></td>
                <td className="px-6 py-4 text-foreground/70">Identifying you and your timezone</td>
              </tr>
              <tr className="hover:bg-muted/20 transition-colors">
                <td className="px-6 py-4 font-medium flex items-center gap-2"><Slack className="w-4 h-4 text-[#E01E5A]" /> Slack DMs</td>
                <td className="px-6 py-4"><span className="px-2 py-0.5 rounded text-[10px] font-semibold tracking-wider border bg-amber-500/10 text-amber-500 border-amber-500/20">Write Only</span></td>
                <td className="px-6 py-4 text-foreground/70">Sending human-in-the-loop approvals</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
