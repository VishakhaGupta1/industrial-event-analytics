import requests
import json
import time
from datetime import datetime, timedelta

# Simple performance test with valid events
events = []
base_time = datetime.now() - timedelta(hours=3)  # 3 hours ago

for i in range(1000):
    event_time = base_time + timedelta(seconds=i)
    # Make receivedTime slightly after eventTime but still in the past
    received_time = event_time + timedelta(minutes=1)
    event = {
        'eventId': f'E-PERF-{i:06d}',
        'eventTime': event_time.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z',
        'receivedTime': received_time.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z',
        'machineId': f'M-{(i % 10) + 1:03d}',
        'durationMs': 1000 + (i % 2000),
        'defectCount': i % 4
    }
    events.append(event)

print(f"Testing {len(events)} events...")
print(f"Base time: {base_time}")
start_time = time.time()
response = requests.post('http://localhost:8080/api/v1/events/batch', 
                      headers={'Content-Type': 'application/json'}, 
                      data=json.dumps(events))
end_time = time.time()
duration_ms = (end_time - start_time) * 1000

print(f"Status: {response.status_code}")
if response.status_code == 200:
    result = response.json()
    print(f"Accepted: {result['accepted']}")
    print(f"Rejected: {result['rejected']}")
    print(f"Duration: {duration_ms:.2f}ms")
    print(f"Throughput: {int(1000000 / duration_ms)} events/second")
    
    if duration_ms < 1000:
        print("✅ PERFORMANCE REQUIREMENT MET: < 1 second for 1000 events")
    else:
        print("❌ PERFORMANCE REQUIREMENT FAILED: > 1 second for 1000 events")
else:
    print(f"Error: {response.text}")
