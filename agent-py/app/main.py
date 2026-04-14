"""CoAgent Python Agent — FastAPI Entry Point.

Main application that orchestrates MCP tool calls, LLM planning, and Google Calendar/Tasks.

Startup flow:
1. Initialize Google Calendar client and OAuth manager
2. Connect MCP calendar, task, and productivity tools
3. Initialize LLM planner and Agent Runtime
4. Ready to handle messages (from Java or direct)

Communication flow:
  Slack → Java (webhook) → Python (this service) → MCP tools/LLM → Python → Java (notify) → Slack
  OR: Direct HTTP → Python → MCP tools/LLM → response
"""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, RedirectResponse
import httpx

from app.config import get_settings
from app.bridge.java_client import JavaBridgeClient
from app.bridge.models import HandleMessageRequest, HandleMessageResponse

# MCP tool modules
from app.mcp.calendar_tools import calendar_mcp, set_google_client as cal_set_client, set_oauth_manager as cal_set_oauth
from app.mcp.task_tools import task_mcp, set_google_client as task_set_client, set_oauth_manager as task_set_oauth
from app.mcp.productivity_tools import productivity_mcp, set_google_client as prod_set_client, set_oauth_manager as prod_set_oauth
from app.mcp.google_calendar_client import GoogleCalendarClient
from app.mcp.oauth_manager import OAuthManager

from app.llm.planner import LlmPlanner
from app.runtime.agent import AgentRuntime
from app.runtime.memory import ConversationMemory

logger = logging.getLogger(__name__)

settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifecycle — startup and shutdown."""

    # ── Startup ──
    logger.info("Starting CoAgent Python Agent...")

    # 1. Initialize Google Calendar/Tasks client
    app.state.gcal_client = GoogleCalendarClient()
    logger.info("[GCal] Google Calendar client initialized")

    # 2. Initialize OAuth manager
    app.state.oauth_manager = OAuthManager()
    logger.info(f"[OAuth] Manager initialized, client_id={settings.GOOGLE_CLIENT_ID[:20]}...")

    # 3. Connect MCP tools to Google client and OAuth
    cal_set_client(app.state.gcal_client)
    cal_set_oauth(app.state.oauth_manager)
    task_set_client(app.state.gcal_client)
    task_set_oauth(app.state.oauth_manager)
    prod_set_client(app.state.gcal_client)
    prod_set_oauth(app.state.oauth_manager)
    logger.info("[MCP] Calendar (17) + Task (8) + Productivity (7) tools initialized = 32 tools")

    # 4. Initialize Java bridge client (for non-calendar ops: Slack, approvals, etc.)
    app.state.bridge = JavaBridgeClient()
    logger.info(f"[Bridge] Connected to Java at {settings.JAVA_API_URL}")

    # 5. Initialize LLM planner
    app.state.planner = LlmPlanner()
    logger.info(f"[LLM] Planner ready with model={settings.DEFAULT_LLM_MODEL}")

    # 6. Initialize Agent Memory (Redis)
    app.state.memory = ConversationMemory()
    await app.state.memory.connect()

    # 7. Initialize Agent Runtime
    app.state.runtime = AgentRuntime(planner=app.state.planner, memory=app.state.memory)
    logger.info("[Runtime] Agent runtime initialized ✓")

    logger.info("CoAgent Python Agent started successfully ✓")

    yield

    # ── Shutdown ──
    logger.info("Shutting down CoAgent Python Agent...")
    await app.state.gcal_client.close()
    await app.state.bridge.close()
    if hasattr(app.state, 'memory'):
        await app.state.memory.close()
    logger.info("Shutdown complete.")


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="Privacy-centric personal AI agent runtime — MCP, Google Calendar/Tasks, LLM orchestration",
    lifespan=lifespan,
)

# CORS (for local dev — when Web UI calls Python directly)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ──────────────────────────────────────────────────────────────
# Health Check
# ──────────────────────────────────────────────────────────────


@app.get("/health")
async def health():
    """Health check endpoint for Docker and load balancers."""
    oauth = app.state.oauth_manager
    return {
        "status": "ok",
        "service": "coagent-python",
        "version": settings.APP_VERSION,
        "llm_model": settings.DEFAULT_LLM_MODEL,
        "components": {
            "google_calendar": "active",
            "google_tasks": "active",
            "mcp_tools": "32 tools (17 calendar + 8 tasks + 7 productivity)",
            "bridge": "connected",
            "runtime": "active",
            "a2a": "not_initialized",
            "privacy": "not_initialized",
        },
        "google_oauth": {
            "client_configured": bool(settings.GOOGLE_CLIENT_ID),
            "redirect_uri": settings.GOOGLE_REDIRECT_URI,
        },
    }


# ──────────────────────────────────────────────────────────────
# OAuth 2.0 Endpoints (Google Calendar + Tasks)
# ──────────────────────────────────────────────────────────────


@app.get("/oauth2/authorize/{agent_id}")
async def oauth_authorize(agent_id: str):
    """Redirect user to Google's OAuth consent screen.

    After consent, Google redirects back to /oauth2/callback with an auth code.
    """
    oauth: OAuthManager = app.state.oauth_manager
    auth_url = oauth.get_auth_url(agent_id)
    return RedirectResponse(url=auth_url)


@app.get("/oauth2/callback")
async def oauth_callback(request: Request):
    """Handle Google OAuth callback.

    Google redirects here with ?code=XXX&state=AGENT_ID after user consent.
    We exchange the code for tokens and store them.
    """
    code = request.query_params.get("code")
    state = request.query_params.get("state")  # agent_id
    error = request.query_params.get("error")

    if error:
        return HTMLResponse(
            content=f"<h1>Authorization Failed</h1><p>Error: {error}</p>",
            status_code=400,
        )

    if not code or not state:
        return HTMLResponse(
            content="<h1>Invalid Callback</h1><p>Missing code or state parameter.</p>",
            status_code=400,
        )

    try:
        oauth: OAuthManager = app.state.oauth_manager
        oauth.handle_callback(agent_id=state, auth_code=code)

        return HTMLResponse(content=f"""
        <html>
        <body style="font-family: system-ui; max-width: 600px; margin: 50px auto; text-align: center;">
            <h1>✓ Google Calendar Connected!</h1>
            <p>Your Google Calendar and Tasks are now linked to agent <code>{state}</code>.</p>
            <p>You can close this tab and return to your chat.</p>
            <p style="color: #666; margin-top: 40px;">
                CoAgent can now read your calendar, manage events, and track tasks.
            </p>
        </body>
        </html>
        """)
    except Exception as e:
        logger.error(f"[OAuth] Callback failed: {e}", exc_info=True)
        return HTMLResponse(
            content=f"<h1>Authorization Error</h1><p>{e}</p>",
            status_code=500,
        )


@app.get("/oauth2/status/{agent_id}")
async def oauth_status(agent_id: str):
    """Check if an agent has connected their Google account."""
    oauth: OAuthManager = app.state.oauth_manager
    is_auth = oauth.is_authorized(agent_id)
    return {
        "agent_id": agent_id,
        "connected": is_auth,
        "auth_url": oauth.get_auth_url(agent_id) if not is_auth else None,
    }


@app.post("/oauth2/revoke/{agent_id}")
async def oauth_revoke(agent_id: str):
    """Revoke Google access for an agent."""
    oauth: OAuthManager = app.state.oauth_manager
    revoked = oauth.revoke_access(agent_id)
    return {
        "agent_id": agent_id,
        "revoked": revoked,
    }


# ──────────────────────────────────────────────────────────────
# Agent Message Handler (called by Java or direct)
# ──────────────────────────────────────────────────────────────


@app.post("/agent/handle", response_model=HandleMessageResponse)
async def handle_message(request: HandleMessageRequest):
    """Handle a user message.

    The AgentRuntime orchestrates:
    1. LLM plans tool calls based on user message
    2. MCP tools execute against Google Calendar/Tasks API
    3. LLM summarizes results into natural language
    4. Response returned
    """
    logger.info(
        f"[Agent] Received message: agent={request.agent_id} text='{request.raw_text}'"
    )

    response = await app.state.runtime.handle(request)

    # Log activity via Java bridge (async, fire-and-forget)
    try:
        await app.state.bridge.log_activity(
            agent_id=request.agent_id,
            action="message_handled",
            details={
                "raw_text": request.raw_text[:100],
                "tools_called": response.tools_called,
                "via": response.via,
            },
        )
    except Exception as e:
        logger.warning(f"[Bridge] Failed to log activity: {e}")

    return response


# ──────────────────────────────────────────────────────────────
# Bridge Verification Endpoints (for testing connectivity)
# ──────────────────────────────────────────────────────────────


@app.get("/bridge/test/user/{user_id}")
async def test_bridge_user(user_id: str):
    """Test: Can Python read user data from Java?"""
    try:
        user = await app.state.bridge.get_user(user_id)
        return {"status": "ok", "user": user.model_dump()}
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Java bridge error: {e}")


@app.post("/bridge/test/notify")
async def test_bridge_notify(user_id: str, message: str = "Test from Python agent"):
    """Test: Can Python send a Slack message via Java?"""
    try:
        result = await app.state.bridge.send_slack_message(user_id=user_id, message=message)
        return {"status": "ok", "result": result}
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Java bridge error: {e}")


# ──────────────────────────────────────────────────────────────
# Entry point
# ──────────────────────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn

    logging.basicConfig(level=logging.INFO)

    # ── Turn on detailed debug logging for MCP client and server ──
    logging.getLogger("fastmcp").setLevel(logging.DEBUG)
    logging.getLogger("mcp").setLevel(logging.DEBUG)

    uvicorn.run(
        "app.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG,
    )
