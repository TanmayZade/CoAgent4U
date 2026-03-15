"use client"

import { useState, useEffect } from "react"
import { useUser } from "@/components/providers/user-provider"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Slack, Copy, Check } from "lucide-react"
import { toast } from "sonner"

export function SlackInstallGuard() {
  const { user, loading } = useUser()
  const [isOpen, setIsOpen] = useState(false)
  const [isCopied, setIsCopied] = useState(false)

  useEffect(() => {
    if (!loading && user && !user.isSlackAppInstalled) {
      setIsOpen(true)
    } else {
      setIsOpen(false)
    }
  }, [user, loading])

  const handleCopyLink = () => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "https://api.coagent4u.com"
    const installUrl = `${apiUrl}/auth/slack/install/start`
    
    navigator.clipboard.writeText(installUrl)
    setIsCopied(true)
    toast.success("Admin installation link copied to clipboard")
    setTimeout(() => setIsCopied(false), 2000)
  }

  const handleAddtoSlack = () => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "https://api.coagent4u.com"
    window.location.href = `${apiUrl}/auth/slack/install/start`
  }

  if (loading || !user || user.isSlackAppInstalled) return null

  return (
    <Dialog open={isOpen} onOpenChange={() => {}}>
      <DialogContent className="sm:max-w-md bg-charcoal-light border-border text-cream" showCloseButton={false}>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-xl mb-2">
            <Slack className="w-6 h-6 text-[#4A154B]" />
            Slack App Not Installed
          </DialogTitle>
          <DialogDescription className="text-foreground-secondary text-base">
            CoAgent4U needs to be installed in your Slack workspace to coordinate meetings.
          </DialogDescription>
        </DialogHeader>
        
        <div className="flex flex-col gap-4 py-4">
          <Button 
            onClick={handleAddtoSlack}
            className="w-full bg-[#4A154B] hover:bg-[#611f64] text-white py-6 flex items-center justify-center gap-2 text-base font-medium"
          >
            <Slack className="w-5 h-5" />
            Add CoAgent4U to Slack
          </Button>
          
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t border-border" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-charcoal-light px-2 text-foreground-muted">
                Or share with admin
              </span>
            </div>
          </div>
          
          <Button 
            variant="outline"
            onClick={handleCopyLink}
            className="w-full border-border text-foreground-secondary hover:text-cream hover:bg-charcoal-lighter py-6 flex items-center justify-center gap-2 transition-all"
          >
            {isCopied ? <Check className="w-4 h-4 text-green-500" /> : <Copy className="w-4 h-4" />}
            Copy link for admin
          </Button>
        </div>
        
        <p className="text-xs text-foreground-muted text-center italic">
          Only organization admins can install Slack apps. If you aren't one, please share the link above.
        </p>
      </DialogContent>
    </Dialog>
  )
}
