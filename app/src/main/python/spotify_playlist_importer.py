# """
# Spotify Playlist Importer - Uses spotdl to fetch playlist data and find YouTube matches
# """
#
# from spotdl.types.playlist import Playlist
# from spotdl.types.song import Song
# import json
# import logging
# import re
# from concurrent.futures import ThreadPoolExecutor
#
# # Configure logging
# logging.basicConfig(level=logging.INFO)
# logger = logging.getLogger(__name__)
#
# def validate_spotify_url(url):
#     """
#     Validate if the URL is a valid Spotify playlist URL
#     """
#     try:
#         is_valid = "open.spotify.com/playlist" in url or "spotify:playlist:" in url
#
#         return json.dumps({
#             "isValid": is_valid,
#             "platform": "Spotify" if is_valid else None,
#             "playlistId": None  # complex to extract, not strictly needed for validation
#         })
#     except Exception as e:
#         logger.error(f"Spotify URL validation failed: {e}")
#         return json.dumps({
#             "isValid": False,
#             "platform": None,
#             "playlistId": None
#         })
#
# def get_youtube_url(song):
#     """
#     Find YouTube URL for a Spotify song
#     """
#     try:
#         # Link the song to a YouTube video
#         # This performs a search on YouTube
#         url = song.get_youtube_url()
#         return url
#     except Exception:
#         return None
#
# def extract_video_id(youtube_url):
#     """
#     Extract video ID from YouTube URL
#     """
#     if not youtube_url:
#         return None
#
#     # Standard YouTube video ID pattern
#     match = re.search(r'(?:v=|\/)([0-9A-Za-z_-]{11}).*', youtube_url)
#     if match:
#         return match.group(1)
#     return None
#
# def fetch_spotify_playlist(playlist_url):
#     """
#     Fetch playlist from Spotify and resolve to YouTube IDs
#     """
#     try:
#         if not ("open.spotify.com/playlist" in playlist_url or "spotify:playlist:" in playlist_url):
#              return json.dumps({
#                 "success": False,
#                 "error": "Invalid Spotify playlist URL"
#             })
#
#         logger.info(f"Fetching Spotify playlist: {playlist_url}")
#
#         # Load playlist metadata from Spotify
#         # This requires internet but no auth is handled by spotdl (clientless or default)
#         playlist = Playlist.from_url(playlist_url)
#
#         title = playlist.name
#         description = playlist.description
#         thumbnail_url = playlist.cover_url
#
#         logger.info(f"Found playlist: {title} with {len(playlist.songs)} songs. Resolving to YouTube...")
#
#         tracks = []
#
#         # Resolve songs in parallel to speed up
#         # Android's thread pool might be limited, but let's try 4 workers
#         with ThreadPoolExecutor(max_workers=4) as executor:
#             # First, map retrieval of YouTube URLs
#             future_to_song = {executor.submit(get_youtube_url, song): song for song in playlist.songs}
#
#             for i, future in enumerate(future_to_song):
#                 song = future_to_song[future]
#                 try:
#                     youtube_url = future.result()
#                     video_id = extract_video_id(youtube_url)
#
#                     if video_id:
#                         tracks.append({
#                             "videoId": video_id,
#                             "title": song.name,
#                             "artist": ", ".join(song.artists),
#                             "album": song.album_name,
#                             "thumbnail": song.cover_url,
#                             "duration": int(song.duration),
#                             "position": i
#                         })
#                 except Exception as e:
#                     logger.warning(f"Failed to resolve song {song.name}: {e}")
#
#         result = {
#             "success": True,
#             "playlistId": playlist.url, # Use URL as ID for spotify
#             "title": title,
#             "description": description,
#             "thumbnail": thumbnail_url,
#             "trackCount": len(tracks),
#             "tracks": tracks
#         }
#
#         return json.dumps(result)
#
#     except Exception as e:
#         logger.error(f"Failed to fetch Spotify playlist: {e}")
#         return json.dumps({
#             "success": False,
#             "error": str(e)
#         })

# src/main/python/spotify_playlist_importer.py
import json
import re
import requests
import base64
from urllib.parse import urlparse, parse_qs

# Use a public Piped instance (you can change this)
PIPED_API = "https://pipedapi.kavin.rocks"

# Spotify API credentials (replace with your own from Spotify Developer Dashboard)
SPOTIFY_CLIENT_ID = "49f66c7904a84a9b85ef496d65719366"
SPOTIFY_CLIENT_SECRET = "fc9e43c2cfd64b54854aaee5d90706eb"

def get_spotify_access_token():
    """Get access token using client credentials flow"""
    try:
        auth_url = "https://accounts.spotify.com/api/token"
        auth_data = {
            "grant_type": "client_credentials"
        }
        auth_headers = {
            "Authorization": f"Basic {base64.b64encode(f'{SPOTIFY_CLIENT_ID}:{SPOTIFY_CLIENT_SECRET}'.encode()).decode()}",
            "Content-Type": "application/x-www-form-urlencoded"
        }
        resp = requests.post(auth_url, data=auth_data, headers=auth_headers, timeout=10)
        if resp.status_code == 200:
            return resp.json()["access_token"]
        else:
            raise Exception(f"Failed to get token: {resp.status_code}")
    except Exception as e:
        raise Exception(f"Authentication failed: {e}")

def validate_spotify_url(url):
    """Check if URL is a valid Spotify playlist and extract ID"""
    patterns = [
        r"open\.spotify\.com/playlist/([a-zA-Z0-9]+)",
        r"spotify:playlist:([a-zA-Z0-9]+)",
        r"spotify\.com/playlist/([a-zA-Z0-9]+)"
    ]
    for pattern in patterns:
        match = re.search(pattern, url)
        if match:
            return {
                "isValid": True,
                "platform": "spotify",
                "playlistId": match.group(1)
            }
    return {"isValid": False, "platform": None, "playlistId": None}


def search_youtube_music(query):
    """Search YouTube Music via Piped (no login, no API key)"""
    try:
        url = f"{PIPED_API}/search"
        params = {
            "q": query + " audio",
            "filter": "videos"
        }
        headers = {
            "User-Agent": "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36"
        }
        resp = requests.get(url, params=params, headers=headers, timeout=15)
        data = resp.json()

        for item in data.get("items", []):
            if item.get("duration", 0) > 60:  # avoid shorts/trailers
                return {
                    "videoId": item["url"].split("v=")[-1],
                    "title": item["title"],
                    "artist": item["uploaderName"],
                    "duration": item["duration"],
                    "thumbnail": item["thumbnail"],
                    "streamUrl": f"{PIPED_API}/streams/{item['url'].split('v=')[-1]}"
                }
    except:
        pass
    return None


def fetch_spotify_playlist(url):
    """Main function called from Kotlin"""
    try:
        validation = validate_spotify_url(url)
        if not validation["isValid"]:
            return json.dumps({"success": False, "error": "Invalid Spotify URL"})

        playlist_id = validation["playlistId"]

        # Get access token
        access_token = get_spotify_access_token()

        # Step 1: Get playlist metadata + tracks
        api_url = f"https://api.spotify.com/v1/playlists/{playlist_id}"
        headers = {
            "Authorization": f"Bearer {access_token}",
            "User-Agent": "Mozilla/5.0 (compatible; SyncTax/1.0)"
        }

        resp = requests.get(api_url, headers=headers, timeout=20)
        if resp.status_code != 200:
            error_msg = f"Failed to fetch playlist: HTTP {resp.status_code} - {resp.text}"
            return json.dumps({"success": False, "error": error_msg})

        data = resp.json()

        playlist_name = data["name"]
        description = data.get("description", "")
        thumbnail = data["images"][0]["url"] if data["images"] else ""

        tracks = []
        items = data["tracks"]["items"]

        # Handle pagination
        next_url = data["tracks"]["next"]
        while next_url:
            resp = requests.get(next_url, headers=headers)
            if resp.status_code != 200:
                break
            page = resp.json()
            items.extend(page["items"])
            next_url = page["next"]

        # Extract tracks
        for idx, item in enumerate(items):
            track = item["track"]
            if not track or track.get("is_local"):
                continue

            name = track["name"]
            artists = ", ".join([a["name"] for a in track["artists"]])
            duration_sec = track["duration_ms"] // 1000

            # Search YouTube Music via Piped
            search_query = f"{artists} - {name}"
            yt_result = search_youtube_music(search_query)

            if yt_result:
                tracks.append({
                    "title": name,
                    "artist": artists,
                    "album": track["album"]["name"],
                    "duration": duration_sec,
                    "videoId": yt_result["videoId"],
                    "thumbnail": yt_result["thumbnail"],
                    "position": idx
                })

        return json.dumps({
            "success": True,
            "title": playlist_name,
            "description": description,
            "thumbnail": thumbnail,
            "tracks": tracks
        }, ensure_ascii=False)

    except Exception as e:
        return json.dumps({"success": False, "error": str(e)})