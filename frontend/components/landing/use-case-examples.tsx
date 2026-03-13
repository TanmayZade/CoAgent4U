"use client"

import { useState } from "react"
import { Bot, Calendar, CheckCircle2, Clock, AlertTriangle, Users, ArrowRight, ChevronRight } from "lucide-react"
import { motion } from "framer-motion"
import { useScrollAnimation, fadeSlideUpVariants, staggerContainerVariants, itemVariants } from "@/hooks/use-framer-animations"

const useCases = [
  {
    id: "view",
    command: "@CoAgent4U show my schedule today",
    label: "View Schedule",
    type: "simple",
  },
  {
    id: "add",
    command: "@CoAgent4U add team lunch next Tuesday at noon",
    label: "Add Event",
    type: "flow",
  },
  {
    id: "coordinate",
    command: "@CoAgent4U schedule meeting with @Sarah Friday evening",
    label: "Coordinate",
    type: "flow",
  },
]

const viewScheduleData = {
  response: "Here's your schedule for today:",
  calendar: [
    { time: "9:00 AM", event: "Team Standup", duration: "30m" },
    { time: "10:30 AM", event: "Design Review", duration: "1h" },
    { time: "1:00 PM", event: "Lunch with Alex", duration: "1h" },
    { time: "3:00 PM", event: "Sprint Planning", duration: "2h" },
  ],
}

const addEventSteps = [
  {
    step: 1,
    title: "Conflict Check",
    agentResponse: "Checking your calendar for Tuesday, March 17 at 12:00 PM...",
    content: {
      type: "checking",
      message: "Analyzing your schedule for conflicts...",
    },
  },
  {
    step: 2,
    title: "No Conflicts Found",
    agentResponse: "No conflicts found. Here's the proposed event:",
    content: {
      type: "proposal",
      proposal: {
        title: "Team Lunch",
        date: "Tuesday, March 17",
        time: "12:00 PM",
        duration: "1 hour",
        location: "TBD",
      },
    },
  },
  {
    step: 3,
    title: "Awaiting Approval",
    agentResponse: "Would you like me to add this event to your calendar?",
    content: {
      type: "approval",
      proposal: {
        title: "Team Lunch",
        date: "Tuesday, March 17",
        time: "12:00 PM",
        duration: "1 hour",
      },
    },
  },
  {
    step: 4,
    title: "Event Created",
    agentResponse: "Done! 'Team Lunch' has been added to your calendar.",
    content: {
      type: "confirmed",
      event: {
        title: "Team Lunch",
        date: "Tuesday, March 17",
        time: "12:00 PM",
        duration: "1 hour",
      },
    },
  },
]

const coordinateSteps = [
  {
    step: 1,
    title: "Initiate Coordination",
    agentResponse: "Initiating agent-to-agent coordination with Sarah's agent...",
    content: {
      type: "initiating",
      message: "Connecting to Sarah's personal agent via A2A protocol...",
    },
  },
  {
    step: 2,
    title: "Exchange Availability",
    agentResponse: "Exchanging availability windows with Sarah's agent...",
    content: {
      type: "availability",
      userA: { name: "You", slots: "6:00 PM - 9:00 PM" },
      userB: { name: "Sarah", slots: "5:00 PM - 8:00 PM" },
    },
  },
  {
    step: 3,
    title: "Match Found",
    agentResponse: "Common availability window found. Generating slot options...",
    content: {
      type: "match",
      window: "6:00 PM - 8:00 PM",
      slots: [
        { time: "6:00 PM - 7:00 PM", label: "Early Evening" },
        { time: "6:30 PM - 7:30 PM", label: "Mid Evening" },
        { time: "7:00 PM - 8:00 PM", label: "Late Evening" },
      ],
    },
  },
  {
    step: 4,
    title: "Invitee Selection",
    agentResponse: "Slot options sent to Sarah. Waiting for Sarah to select her preferred time...",
    content: {
      type: "invitee_selection",
      status: "Sarah is reviewing the options",
      slots: [
        { time: "6:00 PM - 7:00 PM", label: "Early Evening" },
        { time: "6:30 PM - 7:30 PM", label: "Mid Evening" },
        { time: "7:00 PM - 8:00 PM", label: "Late Evening" },
      ],
    },
  },
  {
    step: 5,
    title: "Sarah Selected",
    agentResponse: "Sarah selected 6:30 PM - 7:30 PM. Please confirm to finalize the meeting.",
    content: {
      type: "requester_confirm",
      selectedSlot: "6:30 PM - 7:30 PM",
      selectedBy: "Sarah",
      proposal: {
        title: "Meeting with Sarah",
        date: "Friday, March 20",
        time: "6:30 PM - 7:30 PM",
        duration: "1 hour",
      },
    },
  },
  {
    step: 6,
    title: "Meeting Confirmed",
    agentResponse: "Meeting confirmed! Both calendars have been updated.",
    content: {
      type: "confirmed",
      event: {
        title: "Meeting with Sarah",
        date: "Friday, March 20",
        time: "6:30 PM - 7:30 PM",
        participants: ["You", "Sarah"],
      },
    },
  },
]

export function UseCaseExamples() {
  const [activeCase, setActiveCase] = useState("view")
  const [currentStep, setCurrentStep] = useState(1)
  const { ref: sectionRef, isInView } = useScrollAnimation()

  const currentCase = useCases.find((uc) => uc.id === activeCase)
  
  const getSteps = () => {
    if (activeCase === "add") return addEventSteps
    if (activeCase === "coordinate") return coordinateSteps
    return []
  }

  const steps = getSteps()
  const currentStepData = steps.find((s) => s.step === currentStep)
  const maxSteps = steps.length

  const handleCaseChange = (caseId: string) => {
    setActiveCase(caseId)
    setCurrentStep(1)
  }

  const nextStep = () => {
    if (currentStep < maxSteps) {
      setCurrentStep(currentStep + 1)
    }
  }

  const prevStep = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1)
    }
  }

  return (
    <section ref={sectionRef} id="use-cases" className="py-24 lg:py-32">
      <div className="mx-auto max-w-6xl px-6">
        {/* Section header */}
        <motion.div 
          className="max-w-2xl mx-auto text-center mb-16"
          variants={fadeSlideUpVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          <p className="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-3">
            Real Use Case Examples
          </p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight text-foreground mb-4">
            See it in action
          </h2>
          <p className="text-muted-foreground text-lg">
            Simple commands, powerful coordination. Walk through complete flows to see how agents work.
          </p>
        </motion.div>

        {/* Use case selector */}
        <motion.div 
          className="flex flex-wrap justify-center gap-2 mb-10"
          variants={itemVariants}
          initial="hidden"
          animate={isInView ? "visible" : "hidden"}
        >
          {useCases.map((uc) => (
            <button
              key={uc.id}
              onClick={() => handleCaseChange(uc.id)}
              className={`px-5 py-2.5 rounded-full text-sm font-medium transition-all duration-300 transform hover:scale-105 ${
                activeCase === uc.id
                  ? "bg-foreground text-background shadow-lg"
                  : "bg-muted text-muted-foreground hover:text-foreground hover:bg-muted/80"
              }`}
            >
              {uc.label}
            </button>
          ))}
        </motion.div>

        {/* Use case display */}
        <div className="max-w-3xl mx-auto">
          <motion.div 
            className="rounded-2xl border border-border/60 bg-card overflow-hidden shadow-lg shadow-black/[0.02]"
            variants={itemVariants}
            initial="hidden"
            animate={isInView ? "visible" : "hidden"}
            transition={{ delay: 0.2 }}
            whileHover={{ boxShadow: "0 20px 40px rgba(0, 0, 0, 0.1)" }}
          >
            {/* Header */}
            <div className="px-5 py-3 bg-muted/30 border-b border-border/40 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Bot className="w-4 h-4 text-foreground" />
                <span className="text-sm font-medium text-foreground">CoAgent4U</span>
              </div>
              {currentCase?.type === "flow" && (
                <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  <span>Step {currentStep} of {maxSteps}</span>
                </div>
              )}
            </div>

            {/* Content */}
            <motion.div 
              className="p-6 space-y-5"
              key={`${activeCase}-${currentStep}`}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3 }}
            >
              {/* Command */}
              <div className="rounded-xl bg-muted/50 p-4 transition-all duration-300 hover:bg-muted/70">
                <p className="text-xs text-muted-foreground mb-1.5">Your command:</p>
                <p className="font-mono text-sm text-foreground">{currentCase?.command}</p>
              </div>

              {/* View Schedule - Simple Display */}
              {activeCase === "view" && (
                <>
                  <div className="rounded-xl border border-primary/20 bg-primary/[0.02] p-4">
                    <p className="text-xs text-muted-foreground font-medium mb-1.5">Agent response:</p>
                    <p className="text-sm text-foreground">{viewScheduleData.response}</p>
                  </div>
                  <div className="rounded-xl border border-border/60 p-4">
                    <div className="flex items-center gap-2 mb-4">
                      <Calendar className="w-4 h-4 text-muted-foreground" />
                      <span className="text-sm font-medium text-foreground">Today&apos;s Schedule</span>
                    </div>
                    <div className="space-y-2">
                      {viewScheduleData.calendar.map((item, i) => (
                        <div key={i} className="flex items-center justify-between py-2 px-3 bg-muted/30 rounded-lg">
                          <div className="flex items-center gap-3">
                            <span className="text-xs font-mono text-muted-foreground w-16">{item.time}</span>
                            <span className="text-sm text-foreground">{item.event}</span>
                          </div>
                          <span className="text-xs text-muted-foreground">{item.duration}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 pt-2">
                    <CheckCircle2 className="w-4 h-4 text-green-500" />
                    <span className="text-xs text-muted-foreground">Schedule retrieved successfully</span>
                  </div>
                </>
              )}

              {/* Add Event Flow */}
              {activeCase === "add" && currentStepData && (
                <>
                  {/* Step Progress */}
                  <div className="flex items-center gap-1 overflow-x-auto pb-2">
                    {addEventSteps.map((s, i) => (
                      <div key={s.step} className="flex items-center">
                        <button
                          onClick={() => setCurrentStep(s.step)}
                          className={`px-2 py-1 text-xs rounded-md whitespace-nowrap transition-colors ${
                            currentStep === s.step
                              ? "bg-foreground text-background"
                              : currentStep > s.step
                              ? "bg-green-500/10 text-green-600"
                              : "bg-muted text-muted-foreground"
                          }`}
                        >
                          {s.title}
                        </button>
                        {i < addEventSteps.length - 1 && (
                          <ChevronRight className="w-3 h-3 text-muted-foreground/50 mx-0.5 shrink-0" />
                        )}
                      </div>
                    ))}
                  </div>

                  {/* Agent Response */}
                  <div className="rounded-xl border border-primary/20 bg-primary/[0.02] p-4">
                    <p className="text-xs text-muted-foreground font-medium mb-1.5">Agent response:</p>
                    <p className="text-sm text-foreground">{currentStepData.agentResponse}</p>
                  </div>

                  {/* Step Content */}
                  {currentStepData.content.type === "checking" && (
                    <div className="rounded-xl border border-border/60 p-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center">
                          <Clock className="w-4 h-4 text-muted-foreground animate-pulse" />
                        </div>
                        <div>
                          <p className="text-sm text-foreground">Scanning calendar...</p>
                          <p className="text-xs text-muted-foreground">{currentStepData.content.message}</p>
                        </div>
                      </div>
                    </div>
                  )}

                  {currentStepData.content.type === "proposal" && currentStepData.content.proposal && (
                    <div className="rounded-xl border border-border/60 p-4">
                      <div className="flex items-center justify-between mb-4">
                        <span className="text-sm font-medium text-foreground">Proposed Event</span>
                        <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600">
                          No Conflicts
                        </span>
                      </div>
                      <div className="space-y-2 text-sm">
                        {Object.entries(currentStepData.content.proposal).map(([key, value]) => (
                          <div key={key} className="flex justify-between">
                            <span className="text-muted-foreground capitalize">{key}</span>
                            <span className="text-foreground">{value}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {currentStepData.content.type === "approval" && currentStepData.content.proposal && (
                    <div className="rounded-xl border border-border/60 p-4">
                      <div className="flex items-center justify-between mb-4">
                        <span className="text-sm font-medium text-foreground">Confirm Event</span>
                        <span className="text-xs px-2 py-1 rounded-full bg-yellow-500/10 text-yellow-600">
                          Pending Approval
                        </span>
                      </div>
                      <div className="space-y-2 text-sm mb-4">
                        {Object.entries(currentStepData.content.proposal).map(([key, value]) => (
                          <div key={key} className="flex justify-between">
                            <span className="text-muted-foreground capitalize">{key}</span>
                            <span className="text-foreground">{value}</span>
                          </div>
                        ))}
                      </div>
                      <div className="flex gap-2">
                        <button 
                          onClick={nextStep}
                          className="flex-1 py-2 text-sm font-medium bg-foreground text-background rounded-lg hover:bg-foreground/90 transition-colors"
                        >
                          Approve
                        </button>
                        <button className="flex-1 py-2 text-sm font-medium border border-border rounded-lg hover:bg-muted transition-colors">
                          Decline
                        </button>
                      </div>
                    </div>
                  )}

                  {currentStepData.content.type === "confirmed" && currentStepData.content.event && (
                    <div className="rounded-xl border border-green-500/30 bg-green-500/5 p-4">
                      <div className="flex items-center gap-3 mb-4">
                        <CheckCircle2 className="w-5 h-5 text-green-500" />
                        <span className="text-sm font-medium text-foreground">Event Added Successfully</span>
                      </div>
                      <div className="space-y-2 text-sm">
                        {Object.entries(currentStepData.content.event).map(([key, value]) => (
                          <div key={key} className="flex justify-between">
                            <span className="text-muted-foreground capitalize">{key}</span>
                            <span className="text-foreground">{value}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Navigation */}
                  <div className="flex items-center justify-between pt-2">
                    <button
                      onClick={prevStep}
                      disabled={currentStep === 1}
                      className="px-3 py-1.5 text-xs font-medium border border-border rounded-lg hover:bg-muted transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Previous
                    </button>
                    <div className="flex items-center gap-2">
                      {currentStep < maxSteps && currentStepData.content.type !== "approval" && (
                        <button
                          onClick={nextStep}
                          className="px-3 py-1.5 text-xs font-medium bg-foreground text-background rounded-lg hover:bg-foreground/90 transition-colors flex items-center gap-1"
                        >
                          Next Step <ArrowRight className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  </div>
                </>
              )}

              {/* Coordinate Flow */}
              {activeCase === "coordinate" && currentStepData && (
                <>
                  {/* Step Progress */}
                  <div className="flex items-center gap-1 overflow-x-auto pb-2">
                    {coordinateSteps.map((s, i) => (
                      <div key={s.step} className="flex items-center">
                        <button
                          onClick={() => setCurrentStep(s.step)}
                          className={`px-2 py-1 text-xs rounded-md whitespace-nowrap transition-colors ${
                            currentStep === s.step
                              ? "bg-foreground text-background"
                              : currentStep > s.step
                              ? "bg-green-500/10 text-green-600"
                              : "bg-muted text-muted-foreground"
                          }`}
                        >
                          {s.title}
                        </button>
                        {i < coordinateSteps.length - 1 && (
                          <ChevronRight className="w-3 h-3 text-muted-foreground/50 mx-0.5 shrink-0" />
                        )}
                      </div>
                    ))}
                  </div>

                  {/* Agent Response */}
                  <div className="rounded-xl border border-primary/20 bg-primary/[0.02] p-4">
                    <p className="text-xs text-muted-foreground font-medium mb-1.5">Agent response:</p>
                    <p className="text-sm text-foreground">{currentStepData.agentResponse}</p>
                  </div>

                  {/* Step Content */}
                  {currentStepData.content.type === "initiating" && (
                    <div className="rounded-xl border border-border/60 p-4">
                      <div className="flex items-center gap-4">
                        <div className="flex items-center gap-2">
                          <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                            <Bot className="w-5 h-5 text-foreground" />
                          </div>
                          <span className="text-xs text-muted-foreground">Your Agent</span>
                        </div>
                        <div className="flex-1 h-0.5 bg-muted relative">
                          <div className="absolute inset-0 bg-primary/50 animate-pulse" style={{ width: '50%' }} />
                        </div>
                        <div className="flex items-center gap-2">
                          <div className="w-10 h-10 rounded-full bg-muted flex items-center justify-center">
                            <Bot className="w-5 h-5 text-muted-foreground" />
                          </div>
                          <span className="text-xs text-muted-foreground">Sarah&apos;s Agent</span>
                        </div>
                      </div>
                      <p className="text-xs text-muted-foreground mt-4 text-center">{currentStepData.content.message}</p>
                    </div>
                  )}

                  {currentStepData.content.type === "availability" && (
                    <div className="rounded-xl border border-border/60 p-4 space-y-4">
                      <div className="text-sm font-medium text-foreground mb-2">Availability Exchange</div>
                      <div className="grid grid-cols-2 gap-4">
                        <div className="p-3 bg-primary/5 rounded-lg border border-primary/20">
                          <div className="flex items-center gap-2 mb-2">
                            <div className="w-6 h-6 rounded-full bg-primary/20 flex items-center justify-center">
                              <span className="text-xs font-medium text-foreground">Y</span>
                            </div>
                            <span className="text-sm font-medium text-foreground">{currentStepData.content.userA?.name}</span>
                          </div>
                          <p className="text-xs text-muted-foreground">Friday evening:</p>
                          <p className="text-sm text-foreground font-mono">{currentStepData.content.userA?.slots}</p>
                        </div>
                        <div className="p-3 bg-muted/50 rounded-lg border border-border/60">
                          <div className="flex items-center gap-2 mb-2">
                            <div className="w-6 h-6 rounded-full bg-muted flex items-center justify-center">
                              <span className="text-xs font-medium text-muted-foreground">S</span>
                            </div>
                            <span className="text-sm font-medium text-foreground">{currentStepData.content.userB?.name}</span>
                          </div>
                          <p className="text-xs text-muted-foreground">Friday evening:</p>
                          <p className="text-sm text-foreground font-mono">{currentStepData.content.userB?.slots}</p>
                        </div>
                      </div>
                    </div>
                  )}

                  {currentStepData.content.type === "match" && (
                    <div className="rounded-xl border border-border/60 p-4 space-y-4">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-foreground">Common Window Found</span>
                        <span className="text-xs px-2 py-1 rounded-full bg-green-500/10 text-green-600">
                          Match!
                        </span>
                      </div>
                      <div className="flex items-center gap-3 p-3 bg-green-500/5 rounded-lg border border-green-500/20">
                        <CheckCircle2 className="w-5 h-5 text-green-500" />
                        <div>
                          <p className="text-sm font-medium text-foreground">Overlap: {currentStepData.content.window}</p>
                          <p className="text-xs text-muted-foreground">2 hour common availability window</p>
                        </div>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground mb-2">Generated slot options:</p>
                        <div className="space-y-2">
                          {currentStepData.content.slots?.map((slot, i) => (
                            <div key={i} className="flex items-center justify-between p-2 bg-muted/30 rounded-lg">
                              <span className="text-sm font-mono text-foreground">{slot.time}</span>
                              <span className="text-xs text-muted-foreground">{slot.label}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}

                  {currentStepData.content.type === "invitee_selection" && (
                    <div className="rounded-xl border border-border/60 p-4 space-y-4">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-foreground">Awaiting Sarah&apos;s Selection</span>
                        <span className="text-xs px-2 py-1 rounded-full bg-blue-500/10 text-blue-600">
                          {currentStepData.content.status}
                        </span>
                      </div>
                      <div className="p-3 bg-muted/30 rounded-lg border border-border/40">
                        <p className="text-xs text-muted-foreground mb-3">Sarah sees these options:</p>
                        <div className="space-y-2">
                          {currentStepData.content.slots?.map((slot, i) => (
                            <div key={i} className="flex items-center justify-between p-2 bg-background rounded-lg border border-border/60">
                              <span className="text-sm font-mono text-foreground">{slot.time}</span>
                              <button className="px-2 py-1 text-xs bg-muted rounded hover:bg-muted/80 transition-colors">
                                Select
                              </button>
                            </div>
                          ))}
                        </div>
                      </div>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Clock className="w-3.5 h-3.5" />
                        <span>Sarah will select her preferred slot, then you&apos;ll confirm</span>
                      </div>
                    </div>
                  )}

                  {currentStepData.content.type === "requester_confirm" && (
                    <div className="rounded-xl border border-border/60 p-4 space-y-4">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-foreground">Sarah Made Her Selection</span>
                        <span className="text-xs px-2 py-1 rounded-full bg-yellow-500/10 text-yellow-600">
                          Your Approval Needed
                        </span>
                      </div>
                      <div className="p-3 bg-green-500/5 rounded-lg border border-green-500/20">
                        <div className="flex items-center gap-2 mb-2">
                          <CheckCircle2 className="w-4 h-4 text-green-500" />
                          <span className="text-sm text-foreground">Sarah selected: <span className="font-medium">{currentStepData.content.selectedSlot}</span></span>
                        </div>
                      </div>
                      <div className="space-y-2 text-sm">
                        {currentStepData.content.proposal && Object.entries(currentStepData.content.proposal).map(([key, value]) => (
                          <div key={key} className="flex justify-between">
                            <span className="text-muted-foreground capitalize">{key}</span>
                            <span className="text-foreground">{value}</span>
                          </div>
                        ))}
                      </div>
                      <div className="flex gap-2 pt-2">
                        <button 
                          onClick={nextStep}
                          className="flex-1 py-2 text-sm font-medium bg-foreground text-background rounded-lg hover:bg-foreground/90 transition-colors"
                        >
                          Confirm Meeting
                        </button>
                        <button className="flex-1 py-2 text-sm font-medium border border-border rounded-lg hover:bg-muted transition-colors">
                          Suggest Different Time
                        </button>
                      </div>
                    </div>
                  )}

                  {currentStepData.content.type === "confirmed" && (
                    <div className="rounded-xl border border-green-500/30 bg-green-500/5 p-4 space-y-4">
                      <div className="flex items-center gap-3">
                        <CheckCircle2 className="w-6 h-6 text-green-500" />
                        <div>
                          <p className="text-sm font-medium text-foreground">Meeting Confirmed!</p>
                          <p className="text-xs text-muted-foreground">Both calendars have been updated</p>
                        </div>
                      </div>
                      <div className="space-y-2 text-sm">
                        {currentStepData.content.event && (
                          <>
                            <div className="flex justify-between">
                              <span className="text-muted-foreground">Title</span>
                              <span className="text-foreground">{currentStepData.content.event.title}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-muted-foreground">Date</span>
                              <span className="text-foreground">{currentStepData.content.event.date}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-muted-foreground">Time</span>
                              <span className="text-foreground">{currentStepData.content.event.time}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-muted-foreground">Participants</span>
                              <span className="text-foreground">{currentStepData.content.event.participants?.join(", ")}</span>
                            </div>
                          </>
                        )}
                      </div>
                      <div className="flex items-center gap-2 pt-2 text-xs text-green-600">
                        <Users className="w-3.5 h-3.5" />
                        <span>Event synced to both you and Sarah&apos;s calendars</span>
                      </div>
                    </div>
                  )}

                  {/* Navigation */}
                  <div className="flex items-center justify-between pt-2">
                    <button
                      onClick={prevStep}
                      disabled={currentStep === 1}
                      className="px-3 py-1.5 text-xs font-medium border border-border rounded-lg hover:bg-muted transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Previous
                    </button>
                    <div className="flex items-center gap-2">
                      {currentStep < maxSteps && currentStepData.content.type !== "requester_confirm" && (
                        <button
                          onClick={nextStep}
                          className="px-3 py-1.5 text-xs font-medium bg-foreground text-background rounded-lg hover:bg-foreground/90 transition-colors flex items-center gap-1"
                        >
                          Next Step <ArrowRight className="w-3 h-3" />
                        </button>
                      )}
                    </div>
                  </div>
                </>
              )}
            </motion.div>
          </motion.div>
        </div>
      </div>
    </section>
  )
}
