import hmac
import hashlib
import time
import requests
import json
import sys

# Change these to match your .env or application.properties
SLACK_SIGNING_SECRET = "gMy4gslsG6Jq++FidPymUXYm7s8eL/Ygw882sJVFRvI="
BASE_URL = "http://localhost:8080"

def get_slack_headers(body, secret):
    timestamp = str(int(time.time()))
    base_string = f"v0:{timestamp}:{body}"
    signature = "v0=" + hmac.new(
        secret.encode('utf-8'),
        base_string.encode('utf-8'),
        hashlib.sha256
    ).hexdigest()
    
    return {
        "X-Slack-Request-Timestamp": timestamp,
        "X-Slack-Signature": signature,
        "Content-Type": "application/json"
    }

def send_event(text, user_id="U_ALICE", team_id="T_LOCAL"):
    url = f"{BASE_URL}/slack/events"
    payload = {
        "type": "event_callback",
        "team_id": team_id,
        "event_id": f"evt_{int(time.time())}",
        "event": {
            "type": "app_mention",
            "user": user_id,
            "text": text,
            "ts": str(time.time())
        }
    }
    body = json.dumps(payload)
    headers = get_slack_headers(body, SLACK_SIGNING_SECRET)
    
    print(f"--- Sending Event: {text} ---")
    resp = requests.post(url, data=body, headers=headers)
    print(f"Status: {resp.status_code}")
    print(f"Response: {resp.text}\n")

def send_interaction(action_id, value, user_id="U_ALICE", team_id="T_LOCAL"):
    url = f"{BASE_URL}/slack/interactions"
    payload = {
        "type": "block_actions",
        "user": {"id": user_id},
        "team": {"id": team_id},
        "actions": [
            {
                "action_id": action_id,
                "value": value
            }
        ]
    }
    # Slack sends interactions as form-encoded payload={json}
    payload_json = json.dumps(payload)
    body = f"payload={payload_json}"
    headers = get_slack_headers(body, SLACK_SIGNING_SECRET)
    headers["Content-Type"] = "application/x-www-form-urlencoded"
    
    print(f"--- Sending Interaction: {action_id} (value={value}) ---")
    resp = requests.post(url, data=body, headers=headers)
    print(f"Status: {resp.status_code}")
    print(f"Response: {resp.text}\n")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python simulate_slack.py [event|interaction] ...")
        sys.exit(1)
        
    cmd = sys.argv[1]
    if cmd == "event":
        # usage: python simulate_slack.py event [user_id] "@CoAgent do something"
        # Since text usually contains spaces and is grouped, let's treat arg 2 as user_id if we want, or default to U_ALICE
        # To make it simple: 
        # python simulate_slack.py event "text" -> U_ALICE
        # python simulate_slack.py event U_BOB "text" -> U_BOB
        
        if len(sys.argv) >= 4 and not sys.argv[2].startswith("@"):
            user_id = sys.argv[2]
            text = " ".join(sys.argv[3:])
        else:
            user_id = "U_ALICE"
            text = " ".join(sys.argv[2:]) if len(sys.argv) > 2 else "@CoAgent view schedule"
            
        send_event(text, user_id=user_id)
        
    elif cmd == "interaction":
        # usage: python simulate_slack.py interaction [approve_action|reject_action] [approvalId] [user_id(optional)]
        if len(sys.argv) < 4:
            print("Usage: python simulate_slack.py interaction [approve_action|reject_action] [approvalId] [user_id]")
            sys.exit(1)
            
        action = sys.argv[2]
        val = sys.argv[3]
        user_id = sys.argv[4] if len(sys.argv) > 4 else "U_ALICE"
        
        send_interaction(action, val, user_id=user_id)

