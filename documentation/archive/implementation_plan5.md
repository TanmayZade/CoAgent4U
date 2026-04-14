# Phase 5 — Slack Integration & End-to-End Orchestration

## Goal
Wire the full Slack-to-domain flow: intent → agent → coordination → approval → notification. Complete the stub methods in [AgentCommandService](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/application/AgentCommandService.java#42-144), add Slack interactive buttons for approvals, and implement an interaction callback endpoint (PRD §6.4).

## User Review Required

> [!IMPORTANT]
> **Slack Bot Token**: The `SLACK_BOT_TOKEN` must be configured in [.env](file:///e:/CoAgent4U/.env) for real Slack API calls. All changes are testable via unit tests without a live Slack workspace.

---

## Proposed Changes

### Agent Module — Wire Stub Methods

#### [MODIFY] [AgentCommandService.java](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/application/AgentCommandService.java)
- **[notifySchedule(Agent agent)](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/application/AgentCommandService.java#124-127)**: Fetch upcoming events via `CalendarPort.getEvents()`, format as readable summary, send via `NotificationPort.sendMessage()`
- **[initiateCollaboration(Agent agent, ParsedIntent intent)](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/application/AgentCommandService.java#134-138)**: Resolve `targetUser` from intent params → look up target agent via `AgentPersistencePort.findByUsername()` → call `coordinationProtocol.initiate()` with matched IDs, time range, default 30-min duration

#### [MODIFY] [AgentPersistencePort.java](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/port/out/AgentPersistencePort.java)
- Add `findByUsername(String username)` — needed to resolve `@alice` → [AgentId](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/domain/Agent.java#53-57)

---

### Messaging Module — Interactive Approvals

#### [MODIFY] [SlackNotificationAdapter.java](file:///e:/CoAgent4U/integration/messaging-module/src/main/java/com/coagent4u/messaging/SlackNotificationAdapter.java)
- Add `sendApprovalRequest(SlackUserId, WorkspaceId, String proposalText, String approvalId)` — builds Block Kit message with `[Approve]` and `[Reject]` buttons, each carrying `approvalId` as action value

#### [NEW] [SlackInteractionHandler.java](file:///e:/CoAgent4U/integration/messaging-module/src/main/java/com/coagent4u/messaging/SlackInteractionHandler.java)
- `POST /slack/interactions` — receives Slack button clicks
- Verifies signature, parses `payload` JSON
- Extracts `action_id` ([approve](file:///e:/CoAgent4U/core/coordination-module/src/test/java/com/coagent4u/coordination/application/CoordinationServiceTest.java#111-143) or [reject](file:///e:/CoAgent4U/core/coordination-module/src/test/java/com/coagent4u/coordination/domain/CoordinationStateMachineTest.java#72-79)) and `value` (`approvalId`)
- Resolves Slack user → domain [UserId](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/domain/User.java#131-135)
- Calls `DecideApprovalUseCase.decide(approvalId, userId, APPROVED/REJECTED)`
- Responds with updated message (buttons replaced with decision text)

---

### Notification Port — Add Approval Method

#### [MODIFY] [NotificationPort.java](file:///e:/CoAgent4U/core/user-module/src/main/java/com/coagent4u/user/port/out/NotificationPort.java)
- Add `sendApprovalRequest(SlackUserId, WorkspaceId, String proposalText, String approvalId)` — contract for sending interactive approval messages

---

### Agent Capability — Wire ApprovalPortImpl

#### [MODIFY] [AgentApprovalPortImpl.java](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/capability/AgentApprovalPortImpl.java)
- Implement [requestApproval()](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/capability/AgentApprovalPortImpl.java#28-40) — resolve agent → user → Slack user, call `NotificationPort.sendApprovalRequest()` with interactive buttons

---

### Persistence — Add findByUsername

#### [MODIFY] Agent JPA adapter
- Implement `findByUsername()` query for resolving `@mentions` to agents

---

## Verification Plan

### Automated Tests
| Test | Verifies |
|---|---|
| `AgentCommandServiceTest` — [notifySchedule](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/application/AgentCommandService.java#124-127) | Events fetched, formatted, sent via NotificationPort |
| `AgentCommandServiceTest` — [initiateCollaboration](file:///e:/CoAgent4U/core/agent-module/src/main/java/com/coagent4u/agent/application/AgentCommandService.java#134-138) | Target resolved, `coordinationProtocol.initiate()` called |
| `SlackInteractionHandlerTest` | Button clicks → [decide()](file:///e:/CoAgent4U/core/approval-module/src/main/java/com/coagent4u/approval/application/ApprovalService.java#50-77) called with correct params |
| [SlackNotificationAdapterTest](file:///e:/CoAgent4U/integration/messaging-module/src/test/java/com/coagent4u/messaging/SlackNotificationAdapterTest.java#23-93) | Approval payload includes Block Kit buttons |

### Manual Verification
- Send `/CoAgent schedule meeting with @alice` via sandbox → verify coordination initiated in logs
- Click `[Approve]` button in Slack → verify approval state changes
