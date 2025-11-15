import urllib.request
import urllib.parse
import json

query = "Sunflower Post Malone"
instance = "https://piped.video"
url = f"{instance}/api/v1/search?q={urllib.parse.quote(query)}"
print("Requesting:", url)
with urllib.request.urlopen(url, timeout=10) as resp:
    text = resp.read().decode('utf-8')
    print("HTTP OK")
    try:
        data = json.loads(text)
        print(f"Got {len(data)} results")
        for item in data[:5]:
            print(item.get('title'))
    except Exception as e:
        print("Failed to parse JSON", e)
        print(text[:1000])
