"use client"

import { ArrowRight, Bot, CheckCircle2, Plug, User } from "lucide-react"
import { useEffect, useRef } from "react"
import gsap from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"

if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger)
}

export function CoordinationSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const headerRef = useRef<HTMLDivElement>(null)
  const flowRef = useRef<HTMLDivElement>(null)
  const stepsRef = useRef<HTMLDivElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      if (!sectionRef.current) return;

      // Header animation
      gsap.fromTo(
        headerRef.current,
        { opacity: 0, y: 40 },
        { 
          opacity: 1, 
          y: 0, 
          duration: 0.8, 
          ease: "power3.out",
          scrollTrigger: {
            trigger: headerRef.current,
            start: "top 85%",
            toggleActions: "play none none reverse",
          }
        }
      )

      // Unified sequential timeline for grid items and arrows
      const tl = gsap.timeline({ paused: true });

      // Elevate the entire container automatically during the step sequence
      if (containerRef.current) {
        tl.call(() => {
          containerRef.current?.classList.add("is-elevated")
        }, undefined, 0)
      }

      // Loop logically from 1 to 9 to strictly sequence Card -> Arrow -> Card -> Arrow
      for (let i = 1; i <= 9; i++) {
        const item = flowRef.current?.querySelector(`.flow-item[data-index="${i}"]`);
        const arrow = flowRef.current?.querySelector(`.flow-arrow[data-index="${i}"]`);

        // 1. Reveal the card
        if (item) {
          tl.fromTo(
            item,
            { opacity: 0, y: 30, scale: 0.95 },
            { 
              opacity: 1, 
              y: 0, 
              scale: 1,
              duration: 0.8, // Fluid auto-play speed
              ease: "back.out(1.2)",
            }
          );
        }

        // 2. Draw the corresponding arrow
        if (arrow) {
          const dir = arrow.getAttribute("data-dir") || "right";
          
          let fromVars: Record<string, any> = { opacity: 0 };
          let toVars: Record<string, any> = { 
            opacity: 1, 
            duration: 0.5, // Fluid auto-play speed
            ease: "power2.inOut",
          };

          if (dir === "right") {
            fromVars.scaleX = 0;
            fromVars.transformOrigin = "left center";
            toVars.scaleX = 1;
          } else if (dir === "left") {
            fromVars.scaleX = 0;
            fromVars.transformOrigin = "right center";
            toVars.scaleX = 1;
          } else if (dir === "down") {
            fromVars.scaleY = 0;
            fromVars.transformOrigin = "top center";
            toVars.scaleY = 1;
          }

          tl.fromTo(arrow, fromVars, toVars);
        }
      }

      // Return the container to its flat resting state after the sequence finishes
      if (containerRef.current) {
        tl.call(() => {
          containerRef.current?.classList.remove("is-elevated")
        })
      }

      // Auto-play timeline cleanly when scrolled into view. No pinning, no scrubbing, no rewinding!
      ScrollTrigger.create({
        trigger: sectionRef.current,
        start: "top 60%", // Start early enough so it's playing when user sees it
        animation: tl,
        toggleActions: "play none none none", // Fire once globally!
      });

    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section ref={sectionRef} id="architecture" className="py-24 lg:py-32 bg-muted/30">
      <div className="mx-auto max-w-7xl px-6">
        {/* Section header */}
        <div ref={headerRef} className="max-w-2xl mx-auto text-center mb-8">
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3">
            MCP + A2A Architecture
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
            How MCP tools and A2A coordination work together
          </h2>
          <p className="text-muted-foreground text-lg">
            Your personal agent uses MCP to access your tools and A2A to collaborate with other agents — all orchestrated by an LLM planner.
          </p>
        </div>

        {/* Coordination flow visual */}
        <div className="max-w-[75rem] mx-auto">
          <div ref={containerRef} className="rounded-xl border border-border/60 bg-card p-6 lg:p-8 transition-all duration-500 hover:scale-[1.02] hover:shadow-[0_20px_40px_rgba(0,0,0,0.08)] hover:border-foreground/20 [&.is-elevated]:scale-[1.02] [&.is-elevated]:shadow-[0_20px_40px_rgba(0,0,0,0.08)] [&.is-elevated]:border-foreground/20">
            {/* Mental model text */}
            <p className="text-center text-xs text-muted-foreground mb-6">
              User Message → LLM Planner → MCP Tool Calls → A2A Negotiation → Human Approval → Execution
            </p>

            {/* Flow diagram - 9 Steps Grid */}
            <div ref={flowRef} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-y-6 lg:gap-y-8 gap-x-12 relative max-w-5xl mx-auto">
              
              {/* Step 1 */}
              <div className="flow-item flex flex-col p-3 rounded-lg border border-border/50 bg-background/50 hover:bg-muted/50 transition-colors relative isolate" data-index="1">
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex items-center justify-center w-6 h-6 rounded-full bg-background text-foreground border shadow-sm font-bold text-[10px]">1</div>
                  <User className="w-4 h-4 text-blue-500" />
                </div>
                <h4 className="font-semibold text-foreground text-[13px] mb-1">User Message</h4>
                <p className="text-[11px] text-muted-foreground leading-relaxed">User sends a natural language request via Slack or direct HTTP (e.g. &quot;schedule a meeting with Sarah&quot;).</p>
                {/* Connector Arrow -> Right */}
                <div className="flow-arrow hidden lg:block absolute top-1/2 -right-12 w-12 h-[2px] bg-foreground z-[-1] -translate-y-1/2" data-index="1" data-dir="right">
                   <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-[2px] w-2 h-2 border-t-2 border-r-2 border-foreground rotate-45" />
                </div>
              </div>

              {/* Step 2 */}
              <div className="flow-item flex flex-col p-3 rounded-lg border border-border/50 bg-background/50 hover:bg-muted/50 transition-colors relative isolate" data-index="2">
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex items-center justify-center w-6 h-6 rounded-full bg-background text-foreground border shadow-sm font-bold text-[10px]">2</div>
                  <Bot className="w-4 h-4 text-purple-500" />
                </div>
                <h4 className="font-semibold text-foreground text-[13px] mb-1">LLM Plans Tool Calls</h4>
                <p className="text-[11px] text-muted-foreground leading-relaxed">Groq LLM receives 32 MCP tool schemas and decides which tools to call based on user intent.</p>
                {/* Connector Arrow -> Right */}
                <div className="flow-arrow hidden lg:block absolute top-1/2 -right-12 w-12 h-[2px] bg-foreground z-[-1] -translate-y-1/2" data-index="2" data-dir="right">
                   <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-[2px] w-2 h-2 border-t-2 border-r-2 border-foreground rotate-45" />
                </div>
              </div>

              {/* Step 3 */}
              <div className="flow-item flex flex-col p-3 rounded-lg border border-border/50 bg-background/50 hover:bg-muted/50 transition-colors relative isolate" data-index="3">
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex items-center justify-center w-6 h-6 rounded-full bg-background text-foreground border shadow-sm font-bold text-[10px]">3</div>
                  <Plug className="w-4 h-4 text-emerald-500" />
                </div>
                <h4 className="font-semibold text-foreground text-[13px] mb-1">MCP Tool Execution</h4>
                <p className="text-[11px] text-muted-foreground leading-relaxed">Agent executes planned tools via MCP clients — reading calendar, checking availability, creating events.</p>
                {/* Connector Arrow -> Down */}
                <div className="flow-arrow hidden lg:block absolute -bottom-8 left-1/2 w-[2px] h-8 bg-foreground z-[-1] -translate-x-1/2" data-index="3" data-dir="down">
                   <div className="absolute bottom-0 left-1/2 -translate-x-1/2 translate-y-[2px] w-2 h-2 border-b-2 border-r-2 border-foreground rotate-45" />
                </div>
              </div>

              {/* Step 6 */}
              <div className="flow-item flex flex-col p-3 rounded-lg border border-border/50 bg-background/50 hover:bg-muted/50 transition-colors relative isolate" data-index="6">
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex items-center justify-center w-6 h-6 rounded-full bg-background text-foreground border shadow-sm font-bold text-[10px]">6</div>
                  <User className="w-4 h-4 text-blue-500" />
                </div>
                <h4 className="font-semibold text-foreground text-[13px] mb-1">Invitee Selection</h4>
                <p className="text-[11px] text-muted-foreground leading-relaxed">Invitee&apos;s agent presents available slots via Slack. The invitee selects a preferred time.</p>
                {/* Connector Arrow -> Down */}
                <div className="flow-arrow hidden lg:block absolute -bottom-8 left-1/2 w-[2px] h-8 bg-foreground z-[-1] -translate-x-1/2" data-index="6" data-dir="down">
                   <div className="absolute bottom-0 left-1/2 -translate-x-1/2 translate-y-[2px] w-2 h-2 border-b-2 border-r-2 border-foreground rotate-45" />
                </div>
              </div>

              {/* Step 5 */}
              <div className="flow-item flex flex-col p-3 rounded-lg border border-border/50 bg-background/50 hover:bg-muted/50 transition-colors relative isolate" data-index="5">
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex items-center justify-center w-6 h-6 rounded-full bg-background text-foreground border shadow-sm font-bold text-[10px]">5</div>
                  <div className="w-5 h-5 rounded bg-blue-600 flex items-center justify-center"><span className="text-[9px] font-bold text-white">A2A</span></div>
                </div>
                <h4 className="font-semibold text-foreground text-[13px] mb-1">A2A Slot Proposal</h4>
                <p className="text-[11px] text-muted-foreground leading-relaxed">Agent A sends proposed slots to Agent B via A2A protocol. Both agents&apos; availability is compared.</p>
                {/* Connector Arrow -> Left */}
                <div className="flow-arrow hidden lg:block absolute top-1/2 -left-12 w-12 h-[2px] bg-foreground z-[-1] -translate-y-1/2" data-index="5" data-dir="left">
                   <div className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-[2px] w-2 h-2 border-b-2 border-l-2 border-foreground rotate-45" />
                </div>
              </div>

              {/* Step 4 */}
              <div className="flow-item flex flex-col p-3 rounded-lg border border-border/50 bg-background/50 hover:bg-muted/50 transition-colors relative isolate" data-index="4">
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex items-center justify-center w-6 h-6 rounded-full bg-background text-foreground border shadow-sm font-bold text-[10px]">4</div>
                  <div className="w-5 h-5 rounded bg-blue-600 flex items-center justify-center"><span className="text-[9px] font-bold text-white">A2A</span></div>
                </div>
                <h4 className="font-semibold text-foreground text-[13px] mb-1">A2A Initiation</h4>
                <p className="text-[11px] text-muted-foreground leading-relaxed">Agent initiates A2A session with invitee&apos;s agent to negotiate meeting times across both calendars.</p>
                {/* Connector Arrow -> Left */}
                <div className="flow-arrow hidden lg:block absolute top-1/2 -left-12 w-12 h-[2px] bg-foreground z-[-1] -translate-y-1/2" data-index="4" data-dir="left">
                   <div className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-[2px] w-2 h-2 border-b-2 border-l-2 border-foreground rotate-45" />
                </div>
              </div>

              {/* Step 7 */}
              <div className="flow-item flex flex-col p-3 rounded-lg border border-border/50 bg-background/50 hover:bg-muted/50 transition-colors relative isolate" data-index="7">
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex items-center justify-center w-6 h-6 rounded-full bg-background text-foreground border shadow-sm font-bold text-[10px]">7</div>
                  <User className="w-4 h-4 text-amber-500" />
                </div>
                <h4 className="font-semibold text-foreground text-[13px] mb-1">Human Approval</h4>
                <p className="text-[11px] text-muted-foreground leading-relaxed">Requester reviews the selected slot and explicitly approves or rejects the meeting via Slack.</p>
                {/* Connector Arrow -> Right */}
                <div className="flow-arrow hidden lg:block absolute top-1/2 -right-12 w-12 h-[2px] bg-foreground z-[-1] -translate-y-1/2" data-index="7" data-dir="right">
                   <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-[2px] w-2 h-2 border-t-2 border-r-2 border-foreground rotate-45" />
                </div>
              </div>

              {/* Step 8 */}
              <div className="flow-item flex flex-col p-3 rounded-lg border border-border/50 bg-background/50 hover:bg-muted/50 transition-colors relative isolate" data-index="8">
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex items-center justify-center w-6 h-6 rounded-full bg-background text-foreground border shadow-sm font-bold text-[10px]">8</div>
                  <Plug className="w-4 h-4 text-emerald-500" />
                </div>
                <h4 className="font-semibold text-foreground text-[13px] mb-1">MCP Calendar Create</h4>
                <p className="text-[11px] text-muted-foreground leading-relaxed">Agent calls MCP calendar tools to create the event on both Google Calendars via OAuth.</p>
                {/* Connector Arrow -> Right */}
                <div className="flow-arrow hidden lg:block absolute top-1/2 -right-12 w-12 h-[2px] bg-foreground z-[-1] -translate-y-1/2" data-index="8" data-dir="right">
                   <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-[2px] w-2 h-2 border-t-2 border-r-2 border-foreground rotate-45" />
                </div>
              </div>

              {/* Step 9 */}
              <div className="flow-item flex flex-col p-3 rounded-lg border border-border/50 bg-background/50 hover:bg-muted/50 transition-colors border-green-500/30 bg-green-500/5 relative isolate" data-index="9">
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex items-center justify-center w-6 h-6 rounded-full bg-green-500/20 text-green-700 font-bold text-[10px]">9</div>
                  <CheckCircle2 className="w-4 h-4 text-green-600" />
                </div>
                <h4 className="font-semibold text-foreground text-[13px] mb-1">Confirmed & Synced</h4>
                <p className="text-[11px] text-muted-foreground leading-relaxed">Meeting confirmed. Both agents sync calendars via MCP. LLM summarizes outcome to both users.</p>
              </div>
            </div>

            {/* Description Removed since steps are detailed in the grid above */}
          </div>
        </div>
      </div>
    </section>
  )
}
