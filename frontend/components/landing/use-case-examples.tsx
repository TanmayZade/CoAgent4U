"use client"

import { Calendar, CheckCircle2 } from "lucide-react"
import { useEffect, useRef, useState } from "react"
import {
  SlackAttachment,
  SlackColor,
  SlackMessage as SlackMsgComp,
  SlackWindow,
} from "./slack-ui"

// ── Types ────────────────────────────────────────────────────────────────────

type SlackMessageData = {
  from: "user" | "agent"
  userName?: string
  userInitials?: string
  time: string
  text?: string
  attachment?: {
    color: SlackColor
    header: string
    emoji: string
    body: React.ReactNode
  }
}

// ── Slot pill helpers ─────────────────────────────────────────────────────────

const SLOTS = [
  "06:00 pm – 07:00 pm",
  "06:30 pm – 07:30 pm",
  "07:00 pm – 08:00 pm",
  "07:30 pm – 08:30 pm",
]

function SlotPillsReadOnly() {
  return (
    <div className="grid grid-cols-2 gap-1.5 mt-2">
      {SLOTS.map((t, i) => (
        <button key={i} disabled className="px-2 py-1.5 text-[11px] bg-zinc-100 border border-zinc-300 rounded text-zinc-800 font-medium whitespace-nowrap cursor-default">
          {t}
        </button>
      ))}
      <button disabled className="px-2 py-1.5 text-[11px] bg-zinc-100 border border-zinc-300 rounded text-zinc-800 font-medium cursor-default">
        + 1 more
      </button>
    </div>
  )
}

function SlotPillsClickable() {
  return (
    <div className="grid grid-cols-2 gap-1.5 mt-2">
      {SLOTS.map((t, i) => (
        <button key={i} className="px-2 py-1.5 text-[11px] bg-zinc-100 border border-zinc-300 rounded hover:bg-zinc-200 transition-colors text-zinc-800 font-medium whitespace-nowrap">
          {t}
        </button>
      ))}
      <button className="px-2 py-1.5 text-[11px] bg-zinc-100 border border-zinc-300 rounded hover:bg-zinc-200 transition-colors text-zinc-800 font-medium">
        + 1 more
      </button>
    </div>
  )
}

// ── Data ─────────────────────────────────────────────────────────────────────

const viewScheduleThread: SlackMessageData[] = [
  { from: "user", time: "11:33 AM", text: "show my schedule" },
  {
    from: "agent", time: "11:33 AM",
    attachment: {
      color: "blue", emoji: "📅", header: "Your upcoming schedule (next 7 days):",
      body: (
        <ul className="mt-1 space-y-1 text-xs text-zinc-700">
          {[
            { name: "Team Standup", date: "Mon, Mar 20", time: "09:00 am – 09:30 am" },
            { name: "Design Review", date: "Mon, Mar 20", time: "10:30 am – 11:30 am" },
            { name: "Lunch with Alex", date: "Mon, Mar 20", time: "01:00 pm – 02:00 pm" },
            { name: "Sprint Planning", date: "Mon, Mar 20", time: "03:00 pm – 05:00 pm" },
            { name: "Team Lunch", date: "Tue, Mar 21", time: "12:00 pm – 01:00 pm" },
          ].map((e, i) => (
            <li key={i} className="flex flex-col">
              <span className="font-semibold text-zinc-900">• {e.name}</span>
              <span className="pl-3 text-zinc-500">{e.date}, {e.time}</span>
            </li>
          ))}
          <li><button className="text-blue-600 mt-1 hover:underline text-xs">See less</button></li>
        </ul>
      ),
    },
  },
]

const addEventThread: SlackMessageData[] = [
  { from: "user", time: "2:10 PM", text: "@CoAgent4U add team lunch next Tuesday at 12 pm" },
  { from: "agent", time: "2:10 PM", text: "Checking your calendar for Tuesday at 12:00 PM…" },
  {
    from: "agent", time: "2:10 PM",
    text: "No conflicts found. Here's the proposed event:",
    attachment: {
      color: "amber", emoji: "📋", header: "Event Approval Request",
      body: (
        <div className="mt-1 text-xs text-zinc-600 space-y-1">
          <div className="flex items-center gap-2"><Calendar className="w-3.5 h-3.5 text-zinc-400" /><span>Tue, Mar 21, 2026</span></div>
          <div className="flex items-center gap-2"><span>🕐</span><span>12:00 pm – 01:00 pm</span></div>
          <div className="flex items-center gap-2"><span>📌</span><span className="font-semibold text-zinc-800">Team Lunch</span></div>
          <p className="text-zinc-500 mt-2">Approve or reject this event.</p>
          <div className="flex items-center gap-2 mt-3">
            <button className="flex items-center gap-1 px-3 py-1.5 bg-green-600 hover:bg-green-500 text-white text-xs font-medium rounded transition-colors">
              <CheckCircle2 className="w-3.5 h-3.5" /> Approve
            </button>
            <button className="flex items-center gap-1 px-3 py-1.5 bg-red-600 hover:bg-red-500 text-white text-xs font-medium rounded transition-colors">
              ✕ Reject
            </button>
          </div>
        </div>
      ),
    },
  },
  {
    from: "agent", time: "2:11 PM",
    attachment: {
      color: "green", emoji: "✅", header: "Event Created Successfully!",
      body: (
        <div className="mt-1 text-xs text-zinc-600 space-y-1">
          <div className="flex items-center gap-2"><span>📌</span><span className="font-semibold text-zinc-800">Team Lunch</span></div>
          <div className="flex items-center gap-2"><Calendar className="w-3.5 h-3.5 text-zinc-400" /><span>Tue, Mar 21, 2026</span></div>
          <div className="flex items-center gap-2"><span>🕐</span><span>12:00 pm – 01:00 pm</span></div>
        </div>
      ),
    },
  },
]

const requesterThread: SlackMessageData[] = [
  {
    from: "user",
    userName: "Tanmay Zade", userInitials: "TZ",
    time: "4:24 PM",
    text: "@CoAgent4U schedule meeting with @Sarah Friday evening",
  },
  {
    from: "agent", time: "4:24 PM",
    text: "Coordinating with Sarah's agent. Common availability found: 6:00 PM – 8:00 PM. Awaiting Sarah's selection.",
  },
  {
    from: "agent", time: "4:24 PM",
    attachment: {
      color: "blue", emoji: "📅", header: "Proposed Slots",
      body: (
        <div className="text-xs text-zinc-600">
          <p className="mb-2">I&apos;ve proposed these available time slots to <span className="text-blue-600">@Sarah</span>. Waiting for their selection...</p>
          <div className="flex items-center gap-1 mb-1">
            <span>🗓️</span><span className="font-semibold text-zinc-800">Fri, Mar 21, 2026</span>
          </div>
          <SlotPillsReadOnly />
          <button className="text-blue-600 mt-2 hover:underline text-xs">See less</button>
        </div>
      ),
    },
  },
  {
    from: "agent", time: "4:35 PM",
    attachment: {
      color: "amber", emoji: "📋", header: "Meeting Approval Request",
      body: (
        <div className="text-xs text-zinc-600 space-y-1">
          <p><span className="text-blue-600">@Sarah</span> selected a meeting slot.</p>
          <div className="flex items-center gap-2"><Calendar className="w-3.5 h-3.5 text-zinc-400" /><span>Fri, Mar 21, 2026</span></div>
          <div className="flex items-center gap-2"><span>🕐</span><span>06:30 pm – 07:30 pm</span></div>
          <p className="text-zinc-500 mt-1">Approve or reject this meeting time.</p>
          <div className="flex items-center gap-2 mt-3">
            <button className="flex items-center gap-1 px-3 py-1.5 bg-green-600 hover:bg-green-500 text-white text-xs font-medium rounded transition-colors">
              <CheckCircle2 className="w-3.5 h-3.5" /> Approve
            </button>
            <button className="flex items-center gap-1 px-3 py-1.5 bg-red-600 hover:bg-red-500 text-white text-xs font-medium rounded transition-colors">
              ✕ Reject
            </button>
          </div>
        </div>
      ),
    },
  },
  {
    from: "agent", time: "4:35 PM",
    attachment: {
      color: "green", emoji: "✓", header: "Meeting Confirmed",
      body: (
        <div className="text-xs text-zinc-600 space-y-1">
          <p className="text-zinc-500">Participants:</p>
          <ul className="space-y-0.5">
            <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 rounded-full bg-zinc-400 shrink-0" /><span className="text-blue-600">@Tanmay Zade</span></li>
            <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 rounded-full bg-zinc-400 shrink-0" /><span className="text-blue-600">@Sarah</span></li>
          </ul>
          <div className="flex items-center gap-2 mt-1"><Calendar className="w-3.5 h-3.5 text-zinc-400" /><span>Fri, Mar 21, 2026</span></div>
          <div className="flex items-center gap-2"><span>🕐</span><span>06:30 pm – 07:30 pm</span></div>
        </div>
      ),
    },
  },
]

const inviteeThread: SlackMessageData[] = [
  {
    from: "agent", time: "4:24 PM",
    text: "<@Tanmay Zade> invited you to a meeting.",
    attachment: {
      color: "blue", emoji: "📅", header: "Available Time Slots",
      body: (
        <div className="text-xs text-zinc-600">
          <p className="mb-2">Please select a suitable time slot:</p>
          <div className="flex items-center gap-1 mb-1">
            <span>🗓️</span><span className="font-semibold text-zinc-800">Fri, Mar 21, 2026</span>
          </div>
          <SlotPillsClickable />
          <button className="mt-3 flex items-center gap-1 px-3 py-1.5 bg-red-600 hover:bg-red-500 text-white text-xs font-medium rounded transition-colors">
            Reject Meeting
          </button>
          <button className="text-blue-600 mt-2 hover:underline text-xs block">See less</button>
        </div>
      ),
    },
  },
  {
    from: "agent", time: "4:35 PM",
    attachment: {
      color: "blue", emoji: "🕐", header: "Selected Time Slot",
      body: (
        <div className="text-xs text-zinc-600 space-y-1">
          <div className="flex items-center gap-1 mb-1"><span>🗓️</span><span className="font-semibold text-zinc-800">Fri, Mar 21, 2026</span></div>
          <div className="flex items-center gap-2"><span>🕐</span><span>06:30 pm – 07:30 pm</span></div>
          <p className="text-zinc-400 mt-1 text-[11px]">Selected at Fri, 20 Mar 2026 at 4:35 pm</p>
          <p className="text-zinc-600 mt-1">⏳ Waiting for approval...</p>
        </div>
      ),
    },
  },
  {
    from: "agent", time: "4:38 PM",
    attachment: {
      color: "green", emoji: "✅", header: "Meeting Confirmed!",
      body: (
        <div className="text-xs text-zinc-600 space-y-1">
          <p><span className="text-blue-600">@Tanmay Zade</span> approved the meeting.</p>
          <div className="flex items-center gap-2 mt-1"><Calendar className="w-3.5 h-3.5 text-zinc-400" /><span>Fri, Mar 21, 2026</span></div>
          <div className="flex items-center gap-2"><span>🕐</span><span>06:30 pm – 07:30 pm</span></div>
          <p className="text-zinc-500 mt-1">Both calendars have been updated. ✓</p>
        </div>
      ),
    },
  },
]

// ── Use case config ───────────────────────────────────────────────────────────

const useCases = [
  { id: "view", label: "Show Schedule", thread: viewScheduleThread },
  { id: "add", label: "Add Event", thread: addEventThread },
  { id: "coordinate", label: "Coordinate", thread: null },
]

// ── Components ────────────────────────────────────────────────────────────────

function SlackThread({
  messages,
  userName = "Tanmay Zade",
  userInitials = "TZ",
}: {
  messages: SlackMessageData[]
  userName?: string
  userInitials?: string
}) {
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = scrollRef.current
    if (!el) return

    const handleWheel = (e: WheelEvent) => {
      const { scrollTop, scrollHeight, clientHeight } = el
      const canScrollUp = scrollTop > 0
      const canScrollDown = scrollTop + clientHeight < scrollHeight

      if ((e.deltaY < 0 && canScrollUp) || (e.deltaY > 0 && canScrollDown)) {
        el.scrollTop += e.deltaY
        e.preventDefault()
        e.stopPropagation()
      }
    }

    el.addEventListener("wheel", handleWheel, { passive: false })
    return () => el.removeEventListener("wheel", handleWheel)
  }, [])

  return (
    <div
      ref={scrollRef}
      className="flex-1 space-y-1 py-4 lg:py-6"
      style={{ overflowY: 'auto', overscrollBehavior: 'contain' }}
    >
      {messages.map((msg, i) => {
        const prevMsg = messages[i - 1]
        const showHeader = !prevMsg || prevMsg.from !== msg.from || (prevMsg.from === msg.from && prevMsg.time !== msg.time)

        return (
          <SlackMsgComp
            key={i}
            sender={msg.from === "user" ? (msg.userName ?? userName) : "CoAgent4U"}
            time={msg.time}
            isApp={msg.from === "agent"}
            initials={msg.from === "user" ? (msg.userInitials ?? userInitials) : undefined}
            showHeader={showHeader}
          >
            {msg.text && <p className="text-zinc-700 text-sm mt-0.5">{msg.text}</p>}
            {msg.attachment && (
              <SlackAttachment
                color={msg.attachment.color}
                emoji={msg.attachment.emoji}
                header={msg.attachment.header}
              >
                {msg.attachment.body}
              </SlackAttachment>
            )}
          </SlackMsgComp>
        )
      })}
    </div>
  )
}

// ── Main Section ──────────────────────────────────────────────────────────────

export function UseCaseExamples() {
  const [activeCase, setActiveCase] = useState("view")
  const [coordinateView, setCoordinateView] = useState<"requester" | "invitee">("requester")

  const active = useCases.find((uc) => uc.id === activeCase)!

  return (
    <section id="use-cases" className="py-24 lg:py-32 text-left">
      <div className="mx-auto max-w-7xl px-6">
        {/* Header */}
        <div className="max-w-2xl mx-auto text-center mb-4">
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-2">
            Real Use Case Examples
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-2">
            See it in action
          </h2>
          <p className="text-muted-foreground text-lg">
            Simple commands, powerful coordination. Walk through complete flows to see how agents work.
          </p>
        </div>

        {/* Tab selector */}
        <div className="flex flex-wrap justify-center gap-2 mb-4">
          {useCases.map((uc) => (
            <button
              key={uc.id}
              onClick={() => setActiveCase(uc.id)}
              className={`px-5 py-2.5 rounded-full text-sm font-medium transition-all duration-300 ${activeCase === uc.id
                ? "bg-foreground text-background shadow-lg"
                : "bg-muted text-muted-foreground hover:text-foreground hover:bg-muted/80"
                }`}
            >
              {uc.label}
            </button>
          ))}
        </div>

        <div className="max-w-[800px] mx-auto">
          {activeCase === "coordinate" && (
            <div className="flex justify-center mb-4">
              <div className="inline-flex items-center bg-muted rounded-full p-1 gap-1">
                {([
                  { id: "requester", label: "👤 Requester's View" },
                  { id: "invitee", label: "👤 Invitee's View" },
                ] as const).map((v) => (
                  <button
                    key={v.id}
                    onClick={() => setCoordinateView(v.id)}
                    className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all duration-200 ${coordinateView === v.id
                        ? "bg-background text-foreground shadow-sm"
                        : "text-muted-foreground hover:text-foreground"
                      }`}
                  >
                    {v.label}
                  </button>
                ))}
              </div>
            </div>
          )}

          <SlackWindow
            height="580px"
            channel={
              activeCase === "coordinate"
                ? `# coagent4u (${coordinateView === "requester" ? "Requester" : "Invitee"})`
                : "# coagent4u-demo"
            }
          >
            <SlackThread
              key={`${activeCase}-${coordinateView}`}
              messages={
                activeCase === "coordinate"
                  ? (coordinateView === "requester" ? requesterThread : inviteeThread)
                  : active.thread!
              }
              userName={coordinateView === "invitee" ? "Sarah" : "Tanmay Zade"}
              userInitials={coordinateView === "invitee" ? "SR" : "TZ"}
            />
          </SlackWindow>
        </div>
      </div>
    </section>
  )
}
