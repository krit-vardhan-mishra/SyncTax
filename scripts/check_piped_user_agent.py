import urllib.request, urllib.parse, json
inst = 'https://piped.video'
query = 'Sunflower Post Malone'
url = f"{inst}/api/v1/search?q={urllib.parse.quote(query)}"
print('Requesting', url)
req = urllib.request.Request(url, headers={'User-Agent':'Mozilla/5.0','Accept':'application/json'})
try:
    with urllib.request.urlopen(req, timeout=10) as r:
        t = r.read().decode('utf-8')
        print('Len', len(t))
        print(t[:400])
        js = json.loads(t)
        print('Items', len(js))
except Exception as e:
    print('Error', e)
