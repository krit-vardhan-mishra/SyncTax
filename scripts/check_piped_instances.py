import urllib.request, urllib.parse, json
instances = ['https://piped.video','https://piped.kavin.rocks','https://piped.moomoo.me','https://yewtu.cafe','https://yewtu.eu','https://piped.kavin.rocks']
query = 'Sunflower Post Malone'
for inst in instances:
    url = f"{inst}/api/v1/search?q={urllib.parse.quote(query)}"
    print('Trying', url)
    try:
        req = urllib.request.Request(url, headers={'User-Agent':'Mozilla/5.0','Accept':'application/json'})
        with urllib.request.urlopen(req, timeout=6) as r:
            t = r.read().decode('utf-8')
            data = json.loads(t)
            print('OK:', inst, 'items=', len(data))
            for i in data[:3]:
                print('-', i.get('title'))
    except Exception as e:
        print('Failed', inst, repr(e))
    print()

from os import getenv
# If we have YOUTUBE_API_KEY, try the official YouTube Data API fallback
api_key = getenv('YOUTUBE_API_KEY')
if api_key:
    print('Trying YouTube Data API fallback')
    q = urllib.parse.quote('Sunflower Post Malone')
    url = f'https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=5&q={q}&key={api_key}'
    try:
        req = urllib.request.Request(url, headers={'User-Agent':'Mozilla/5.0','Accept':'application/json'})
        with urllib.request.urlopen(req, timeout=6) as r:
            t = r.read().decode('utf-8')
            j = json.loads(t)
            items = j.get('items', [])
            print('YouTube API items =', len(items))
            for it in items:
                print('-', it['snippet']['title'])
    except Exception as e:
        print('YouTube API failed', e)
