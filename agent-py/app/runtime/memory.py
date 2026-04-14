"""Redis-backed conversational memory for the agent runtime.

Stores the last N message exchanges so the LLM has multi-turn context
for referencing previous tool calls and approvals.
"""
import json
import logging
from typing import List, Dict, Any

from redis.asyncio import Redis, ConnectionPool

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


class ConversationMemory:
    """Manages short-term conversation context for agents."""

    def __init__(self):
        """Initialize connection pool without testing connection yet."""
        self.pool = ConnectionPool.from_url(
            settings.REDIS_URL,
            decode_responses=True,
            health_check_interval=30
        )
        self.redis = Redis(connection_pool=self.pool)

    async def connect(self):
        """Verify connection to Redis (called during app startup)."""
        try:
            await self.redis.ping()
            logger.info(f"[Memory] Connected to Redis at {settings.REDIS_URL}")
        except Exception as e:
            logger.error(f"[Memory] Failed to connect to Redis: {e}")
            raise

    async def close(self):
        """Close connection pool."""
        await self.redis.aclose()
        await self.pool.disconnect()

    def _key(self, agent_id: str) -> str:
        return f"agent:{agent_id}:conversation"

    async def get_history(self, agent_id: str) -> List[Dict[str, str]]:
        """Retrieve recent conversation history formatted for LiteLLM.
        
        Returns:
            List of dicts: [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
        """
        try:
            raw_exchanges = await self.redis.lrange(self._key(agent_id), 0, -1)
            history = []
            for item in raw_exchanges:
                exchange = json.loads(item)
                history.append({"role": "user", "content": exchange["user"]})
                history.append({"role": "assistant", "content": exchange["assistant"]})
            return history
        except Exception as e:
            logger.warning(f"[Memory] Failed to get history for {agent_id}: {e}")
            return []

    async def add_exchange(self, agent_id: str, user_msg: str, assistant_msg: str) -> None:
        """Add a back-and-forth exchange to the history and enforce limits."""
        try:
            key = self._key(agent_id)
            exchange = {
                "user": user_msg,
                "assistant": assistant_msg,
            }
            # Add to the RIGHT (end) of the list
            await self.redis.rpush(key, json.dumps(exchange))
            
            # Trim the list from the LEFT to keep only the MAX EXCHANGES
            # If MAX=20, keep indices -20 to -1
            await self.redis.ltrim(key, -settings.MEMORY_MAX_EXCHANGES, -1)
            
            # Reset TTL so inactive sessions expire
            await self.redis.expire(key, settings.MEMORY_TTL_SECONDS)
        except Exception as e:
            logger.warning(f"[Memory] Failed to save exchange for {agent_id}: {e}")

    async def clear(self, agent_id: str) -> None:
        """Clear conversation history for an agent."""
        await self.redis.delete(self._key(agent_id))
