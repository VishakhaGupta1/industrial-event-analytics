import requests
import json
import time
from datetime import datetime, timedelta

# Test single event to see rejection reason
event = {
    'eventId': 'E-DEBUG-1',
    'eventTime': '2026-01-12T10:00:00.000Z',
    'receivedTime': '2026-01-12T10:01:00.000Z',
    'machineId': 'M-DEBUG',
    'durationMs': 1000,
    'defectCount': 0
}

print("Testing single event...")
response = requests.post('http://localhost:8080/api/v1/events/batch', 
                      headers={'Content-Type': 'application/json'}, 
                      data=json.dumps([event]))
print(f"Status: {response.status_code}")
if response.status_code == 200:
    result = response.json()
    print(f"Accepted: {result['accepted']}")
    print(f"Rejected: {result['rejected']}")
    if result['rejections']:
        for rejection in result['rejections']:
            print(f"Rejection: {rejection['eventId']} - {rejection['reason']}")
else:
    print(f"Error: {response.text}")
