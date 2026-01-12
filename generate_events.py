import json
from datetime import datetime, timedelta

# Generate 1000 events
events = []
base_time = datetime.now() - timedelta(hours=5)  # 5 hours ago

for i in range(1000):
    event = {
        "eventId": f"E-{i:06d}",
        "eventTime": (base_time + timedelta(seconds=i)).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z",
        "receivedTime": datetime.now().strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z",
        "machineId": f"M-{(i % 10) + 1:03d}",
        "durationMs": 1000 + (i % 1000),
        "defectCount": i % 5
    }
    events.append(event)

# Save to file
with open('events_1000.json', 'w') as f:
    json.dump(events, f)

print("Generated 1000 events in events_1000.json")
