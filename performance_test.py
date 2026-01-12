import requests
import json
import time
import random
from datetime import datetime, timedelta

# Configuration
BASE_URL = "http://localhost:8080"
BATCH_SIZE = 1000

def generate_events(batch_size):
    """Generate realistic test events"""
    events = []
    base_time = datetime.now() - timedelta(hours=1)
    
    for i in range(batch_size):
        event = {
            "eventId": f"E-{i:06d}",
            "eventTime": (base_time + timedelta(seconds=i)).isoformat().replace('+00:00', '') + "Z",
            "receivedTime": datetime.now().isoformat().replace('+00:00', '') + "Z",
            "machineId": f"M-{random.randint(1, 10):03d}",
            "durationMs": random.randint(500, 5000),
            "defectCount": random.choice([0, 0, 0, 1, 2])  # Remove -1 to avoid unknown defects
        }
        events.append(event)
    
    return events

def run_performance_test():
    """Execute performance test"""
    print(f"Starting performance test: {BATCH_SIZE} events")
    print("-" * 60)
    
    # Generate test data
    events = generate_events(BATCH_SIZE)
    
    # Measure API call time
    start_time = time.time()
    
    try:
        response = requests.post(
            f"{BASE_URL}/api/v1/events/batch",
            headers={"Content-Type": "application/json"},
            data=json.dumps(events)
        )
        
        end_time = time.time()
        duration = (end_time - start_time) * 1000  # Convert to milliseconds
        
        if response.status_code == 200:
            result = response.json()
            print(f"✅ SUCCESS: {duration:.2f}ms")
            print(f"   Accepted: {result['accepted']}")
            print(f"   Deduped: {result['deduped']}")
            print(f"   Updated: {result['updated']}")
            print(f"   Rejected: {result['rejected']}")
            
            # Performance requirement check
            if duration < 1000:
                print("✅ PERFORMANCE REQUIREMENT MET: < 1 second for 1000 events")
            else:
                print("❌ PERFORMANCE REQUIREMENT FAILED: > 1 second for 1000 events")
                
            print(f"   Throughput: {(BATCH_SIZE * 1000) / duration:.0f} events/second")
        else:
            print(f"❌ FAILED with status {response.status_code}")
            print(f"   Response: {response.text}")
            
    except Exception as e:
        print(f"❌ ERROR: {e}")

if __name__ == "__main__":
    run_performance_test()
