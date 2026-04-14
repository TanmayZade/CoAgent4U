"use client"

import React, { forwardRef } from "react"
import Image from "next/image"

// ── Components ────────────────────────────────────────────────────────────────

/**
 * Renders a Slack-style "APP" badge.
 */
export const SlackBadge = () => (
  <span className="bg-zinc-200 text-zinc-600 text-[10px] px-1.5 py-0.5 rounded font-medium ml-1">
    APP
  </span>
)

/**
 * Renders a Slack user or app avatar.
 */
export const SlackAvatar = forwardRef<
  HTMLDivElement,
  { initials?: string; isApp?: boolean; className?: string }
>(({ initials, isApp, className = "" }, ref) => {
  if (isApp) {
    return (
      <div
        ref={ref}
        className={`w-9 h-9 rounded-md bg-white border border-zinc-200 flex items-center justify-center shrink-0 ${className}`}
      >
        <Image
          src="/images/logo-light.png"
          alt="CoAgent4U"
          width={20}
          height={20}
          className="object-contain"
        />
      </div>
    )
  }
  return (
    <div
      ref={ref}
      className={`w-9 h-9 rounded-md bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center text-white text-sm font-semibold shrink-0 ${className}`}
    >
      {initials}
    </div>
  )
})
SlackAvatar.displayName = "SlackAvatar"

/**
 * Standard colors for Slack attachments.
 */
export type SlackColor = "amber" | "green" | "blue" | "red"

const borderColor: Record<SlackColor, string> = {
  amber: "border-amber-500",
  green: "border-green-500",
  blue: "border-blue-500",
  red: "border-red-500",
}

/**
 * Renders a Slack attachment card with a colored left border.
 */
export const SlackAttachment = forwardRef<
  HTMLDivElement,
  {
    color: SlackColor
    emoji: string
    header: string
    children: React.ReactNode
    className?: string
  }
>(({ color, emoji, header, children, className = "" }, ref) => {
  return (
    <div
      ref={ref}
      className={`mt-2 border-l-4 ${borderColor[color]} bg-zinc-50/50 rounded-r-lg p-3 transition-colors ${className}`}
    >
      <div className="flex items-center gap-2 mb-1">
        <span className="text-base">{emoji}</span>
        <h4 className="text-zinc-900 font-semibold text-sm">{header}</h4>
      </div>
      {children}
    </div>
  )
})
SlackAttachment.displayName = "SlackAttachment"

/**
 * Renders a single Slack message row.
 */
export const SlackMessage = forwardRef<
  HTMLDivElement,
  {
    sender: string
    time: string
    isApp?: boolean
    initials?: string
    avatarRef?: React.Ref<HTMLDivElement>
    showHeader?: boolean
    children: React.ReactNode
    className?: string
  }
>(({ sender, time, isApp, initials, avatarRef, showHeader = true, children, className = "" }, ref) => {
  return (
    <div
      ref={ref}
      className={`flex items-start gap-4 px-4 py-2 rounded-lg hover:bg-zinc-100/60 transition-colors ${className}`}
    >
      <div className="w-9 h-9 shrink-0">
        {showHeader ? (
          <SlackAvatar initials={initials} isApp={isApp} ref={avatarRef} />
        ) : (
          <div className="w-9 h-full" />
        )}
      </div>
      <div className="flex-1 min-w-0">
        {showHeader && (
          <div className="flex items-baseline gap-2">
            <span className="text-zinc-900 font-bold text-sm leading-none">{sender}</span>
            {isApp && <SlackBadge />}
            <span className="text-zinc-500 text-xs leading-none">{time}</span>
          </div>
        )}
        <div className="mt-0.5">
          {children}
        </div>
      </div>
    </div>
  )
})
SlackMessage.displayName = "SlackMessage"

/**
 * Renders a Slack window frame with "dots" and a header.
 */
export const SlackWindow = forwardRef<
  HTMLDivElement,
  {
    channel: string
    children: React.ReactNode
    height?: string
    className?: string
  }
>(({ channel, children, height = "520px", className = "" }, ref) => {
  return (
    <div
      ref={ref}
      className={`rounded-2xl border border-border/80 bg-white shadow-2xl shadow-black/[0.08] flex flex-col overflow-hidden ${className}`}
      style={{ height }}
    >
      {/* Window chrome */}
      <div className="flex-none flex items-center justify-between px-5 py-3 border-b border-border/60 bg-muted/30 rounded-t-2xl">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5">
            <div className="w-3 h-3 rounded-full bg-red-400" />
            <div className="w-3 h-3 rounded-full bg-yellow-400" />
            <div className="w-3 h-3 rounded-full bg-green-400" />
          </div>
          <span className="text-sm font-medium text-foreground ml-2">{channel}</span>
        </div>
        <div className="flex items-center gap-2 text-xs font-medium text-green-600">
          <div className="w-2 h-2 rounded-full bg-green-500" />
          Connected
        </div>
      </div>
      {/* Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {children}
      </div>
    </div>
  )
})
SlackWindow.displayName = "SlackWindow"
