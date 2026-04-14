"use client"

import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"
import { TextPlugin } from "gsap/TextPlugin"
import { Calendar, CheckCircle2 } from "lucide-react"
import { useEffect, useRef, useState } from "react"
import {
  SlackWindow,
  SlackMessage,
  SlackAttachment,
} from "./slack-ui"

// ── Slot pill helper ─────────────────────────────────────────────────────────

function SlotPillsReadOnly() {
  const slots = [
    "06:00 pm – 07:00 pm",
    "06:30 pm – 07:30 pm",
    "07:00 pm – 08:00 pm",
    "07:30 pm – 08:30 pm",
  ]
  return (
    <div className="grid grid-cols-2 gap-1.5 mt-2">
      {slots.map((t, i) => (
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

export function AgentPreview() {
  const sectionRef = useRef<HTMLDivElement>(null)
  const stickyRef = useRef<HTMLDivElement>(null)
  const cardRef = useRef<HTMLDivElement>(null)
  const userMessageRef = useRef<HTMLDivElement>(null)
  const userTextRef = useRef<HTMLSpanElement>(null)
  const cursorRef = useRef<HTMLSpanElement>(null)
  const agentResponseRef = useRef<HTMLDivElement>(null)
  const proposedSlotsRef = useRef<HTMLDivElement>(null)
  const approvalRequestRef = useRef<HTMLDivElement>(null)
  const approveButtonRef = useRef<HTMLButtonElement>(null)
  const meetingConfirmedRef = useRef<HTMLDivElement>(null)
  const [approveClicked, setApproveClicked] = useState(false)

  useEffect(() => {
    if (typeof window === "undefined") return
    gsap.registerPlugin(ScrollTrigger, TextPlugin)

    const ctx = gsap.context(() => {
      // Card starts at 0.80 — grows to 1.0 as hero section exits
      gsap.set(cardRef.current, { scale: 0.80, transformOrigin: "center center" })

      // Hide all messages initially
      gsap.set(userMessageRef.current, { opacity: 0, y: 24 })
      gsap.set(agentResponseRef.current, { opacity: 0, y: 24 })
      gsap.set(proposedSlotsRef.current, { opacity: 0, y: 24 })
      gsap.set(approvalRequestRef.current, { opacity: 0, y: 24 })
      gsap.set(meetingConfirmedRef.current, { opacity: 0, y: 24 })
      gsap.set(cursorRef.current, { opacity: 0 })
      if (userTextRef.current) userTextRef.current.textContent = ""

      const USER_MSG = "@CoAgent4U schedule meeting with @Sarah Friday afternoon"

      // ── Zoom-out: card grows 0.80 → 1.0 as hero exits the viewport ──
      const heroEl = document.querySelector("[data-section='hero']") as HTMLElement
      if (heroEl) {
        gsap.to(cardRef.current, {
          scale: 1,
          ease: "none",
          scrollTrigger: {
            trigger: heroEl,
            start: "bottom 80%",
            end: "bottom top",
            scrub: true,
          },
        })
      }

      // ── Master timeline scrubbed to the pinned scroll section ──
      const tl = gsap.timeline({ paused: true })

      tl.to(userMessageRef.current, { opacity: 1, y: 0, duration: 0.4, ease: "power2.out" }, 0)
      tl.to(cursorRef.current, { opacity: 1, duration: 0.1 }, 0.4)
      tl.to(
        userTextRef.current,
        { text: { value: USER_MSG, delimiter: "" }, duration: 1.1, ease: "none" },
        0.5
      )
      tl.to(cursorRef.current, { opacity: 0, duration: 0.1 }, 1.6)
      tl.to(agentResponseRef.current, { opacity: 1, y: 0, duration: 0.4, ease: "power2.out" }, 1.8)
      tl.to(proposedSlotsRef.current, { opacity: 1, y: 0, duration: 0.4, ease: "power2.out" }, 2.3)
      tl.to(approvalRequestRef.current, { opacity: 1, y: 0, duration: 0.4, ease: "power2.out" }, 2.8)
      tl.to(approveButtonRef.current, { scale: 0.92, duration: 0.15 }, 3.5)
      tl.to(approveButtonRef.current, {
        scale: 1,
        duration: 0.15,
        onComplete: () => setApproveClicked(true),
      }, 3.65)
      tl.to(meetingConfirmedRef.current, { opacity: 1, y: 0, duration: 0.4, ease: "power2.out" }, 4.1)

      tl.timeScale(0.4)

      ScrollTrigger.create({
        trigger: sectionRef.current,
        start: "top 60%",
        animation: tl,
        toggleActions: "play none none none",
      })
    })

    return () => ctx.revert()
  }, [])

  return (
    <div ref={sectionRef} className="relative h-screen flex flex-col justify-center">
      <div ref={stickyRef} className="w-full py-6">
        <div className="mx-auto max-w-[65rem] px-6">
          <div className="max-w-[68rem] mx-auto">
            <SlackWindow ref={cardRef} channel="CoAgent4U" height="auto">
              {/* Slack-style thread content */}
              <div className="flex-1 space-y-1 transition-colors py-2">
                
                {/* User message */}
                <SlackMessage
                  ref={userMessageRef}
                  sender="Tanmay Zade"
                  time="4:24 PM"
                  initials="TZ"
                >
                  <p className="text-zinc-700 text-sm mt-0.5 break-words">
                    <span ref={userTextRef} />
                    <span
                      ref={cursorRef}
                      className="inline-block w-[2px] h-[14px] bg-zinc-800 align-middle ml-px"
                    />
                  </p>
                </SlackMessage>

                {/* Agent response */}
                <SlackMessage
                  ref={agentResponseRef}
                  sender="CoAgent4U"
                  time="4:24 PM"
                  isApp
                >
                  <p className="text-zinc-700 text-sm mt-0.5">
                    Coordinating with Sarah&apos;s agent. Common availability found: 2:00 PM – 5:00 PM. Awaiting Sarah&apos;s approval before confirming.
                  </p>
                  <div className="mt-2 flex items-center gap-2 text-xs text-emerald-600">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    Agent-to-Agent coordination in progress
                  </div>
                </SlackMessage>

                {/* Proposed slots */}
                <SlackMessage
                  ref={proposedSlotsRef}
                  sender="CoAgent4U"
                  time="4:24 PM"
                  isApp
                >
                  <div className="md:max-w-[50%]">
                    <SlackAttachment color="blue" emoji="📅" header="Proposed Slots">
                      <div className="text-xs text-zinc-600">
                        <p className="mb-2">I&apos;ve proposed these available time slots to <span className="text-blue-600">@Sarah</span>. Waiting for their selection...</p>
                        <div className="flex items-center gap-1 mb-1">
                          <span>🗓️</span><span className="font-semibold text-zinc-800">Fri, Mar 21, 2026</span>
                        </div>
                        <SlotPillsReadOnly />
                      </div>
                    </SlackAttachment>
                  </div>
                </SlackMessage>

                {/* Meeting approval request */}
                <SlackMessage
                  ref={approvalRequestRef}
                  sender="CoAgent4U"
                  time="4:25 PM"
                  isApp
                >
                  <SlackAttachment color="amber" emoji="📋" header="Meeting Approval Request">
                    <p className="text-zinc-600 text-sm mb-2">
                      <span className="text-blue-600">@Sarah</span> selected a meeting slot.
                    </p>
                    <div className="flex items-center gap-2 text-zinc-500 text-xs mb-1">
                      <Calendar className="w-3.5 h-3.5" />
                      <span>Fri, 14 Mar 2026</span>
                    </div>
                    <div className="flex items-center gap-2 text-zinc-500 text-xs mb-3">
                      <span>🕐</span>
                      <span>03:00 PM – 04:00 PM</span>
                    </div>
                    <p className="text-zinc-500 text-xs mb-3">Approve or reject this meeting time.</p>
                    <div className="flex items-center gap-2">
                      <button
                        ref={approveButtonRef}
                        className={`flex items-center gap-1.5 px-3 py-1.5 text-white text-xs font-medium rounded transition-colors ${approveClicked ? "bg-green-700" : "bg-green-600 hover:bg-green-500"
                          }`}
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        {approveClicked ? "Approved" : "Approve"}
                      </button>
                      <button
                        className={`flex items-center gap-1.5 px-3 py-1.5 text-white text-xs font-medium rounded transition-colors ${approveClicked ? "opacity-40 cursor-not-allowed bg-zinc-500" : "bg-red-600 hover:bg-red-500"
                          }`}
                        disabled={approveClicked}
                      >
                        <span>✕</span>
                        Reject
                      </button>
                    </div>
                  </SlackAttachment>
                </SlackMessage>

                {/* Meeting confirmed */}
                <SlackMessage
                  ref={meetingConfirmedRef}
                  sender="CoAgent4U"
                  time="4:26 PM"
                  isApp
                >
                  <SlackAttachment color="green" emoji="✓" header="Meeting Confirmed">
                    <p className="text-zinc-500 text-xs mb-1">Participants:</p>
                    <ul className="text-xs mb-2 space-y-0.5">
                      <li className="flex items-center gap-2 text-zinc-600">
                        <span className="w-1.5 h-1.5 rounded-full bg-zinc-400" />
                        <span className="text-blue-600">@Tanmay Zade</span>
                      </li>
                      <li className="flex items-center gap-2 text-zinc-600">
                        <span className="w-1.5 h-1.5 rounded-full bg-zinc-400" />
                        <span className="text-blue-600">@Sarah</span>
                      </li>
                    </ul>
                    <div className="flex items-center gap-2 text-zinc-500 text-xs mb-1">
                      <Calendar className="w-3.5 h-3.5" />
                      <span>Fri, 14 Mar 2026</span>
                    </div>
                    <div className="flex items-center gap-2 text-zinc-500 text-xs">
                      <span>🕐</span>
                      <span>03:00 PM – 04:00 PM</span>
                    </div>
                  </SlackAttachment>
                </SlackMessage>

              </div>
            </SlackWindow>
          </div>
        </div>
      </div>
    </div>
  )
}
