"""
Spotify Playlist Importer - Uses spotdl to fetch playlist data and find YouTube matches
"""

from spotdl.types.playlist import Playlist
from spotdl.types.song import Song
import json
import logging
import re
from concurrent.futures import ThreadPoolExecutor

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def validate_spotify_url(url):
    """
    Validate if the URL is a valid Spotify playlist URL
    """
    try:
        is_valid = "open.spotify.com/playlist" in url or "spotify:playlist:" in url
        
        return json.dumps({
            "isValid": is_valid,
            "platform": "Spotify" if is_valid else None,
            "playlistId": None  # complex to extract, not strictly needed for validation
        })
    except Exception as e:
        logger.error(f"Spotify URL validation failed: {e}")
        return json.dumps({
            "isValid": False,
            "platform": None,
            "playlistId": None
        })

def get_youtube_url(song):
    """
    Find YouTube URL for a Spotify song
    """
    try:
        # Link the song to a YouTube video
        # This performs a search on YouTube
        url = song.get_youtube_url()
        return url
    except Exception:
        return None

def extract_video_id(youtube_url):
    """
    Extract video ID from YouTube URL
    """
    if not youtube_url:
        return None
    
    # Standard YouTube video ID pattern
    match = re.search(r'(?:v=|\/)([0-9A-Za-z_-]{11}).*', youtube_url)
    if match:
        return match.group(1)
    return None

def fetch_spotify_playlist(playlist_url):
    """
    Fetch playlist from Spotify and resolve to YouTube IDs
    """
    try:
        if not ("open.spotify.com/playlist" in playlist_url or "spotify:playlist:" in playlist_url):
             return json.dumps({
                "success": False,
                "error": "Invalid Spotify playlist URL"
            })
            
        logger.info(f"Fetching Spotify playlist: {playlist_url}")
        
        # Load playlist metadata from Spotify
        # This requires internet but no auth is handled by spotdl (clientless or default)
        playlist = Playlist.from_url(playlist_url)
        
        title = playlist.name
        description = playlist.description
        thumbnail_url = playlist.cover_url
        
        logger.info(f"Found playlist: {title} with {len(playlist.songs)} songs. Resolving to YouTube...")
        
        tracks = []
        
        # Resolve songs in parallel to speed up
        # Android's thread pool might be limited, but let's try 4 workers
        with ThreadPoolExecutor(max_workers=4) as executor:
            # First, map retrieval of YouTube URLs
            future_to_song = {executor.submit(get_youtube_url, song): song for song in playlist.songs}
            
            for i, future in enumerate(future_to_song):
                song = future_to_song[future]
                try:
                    youtube_url = future.result()
                    video_id = extract_video_id(youtube_url)
                    
                    if video_id:
                        tracks.append({
                            "videoId": video_id,
                            "title": song.name,
                            "artist": ", ".join(song.artists),
                            "album": song.album_name,
                            "thumbnail": song.cover_url,
                            "duration": int(song.duration),
                            "position": i
                        })
                except Exception as e:
                    logger.warning(f"Failed to resolve song {song.name}: {e}")
                    
        result = {
            "success": True,
            "playlistId": playlist.url, # Use URL as ID for spotify
            "title": title,
            "description": description,
            "thumbnail": thumbnail_url,
            "trackCount": len(tracks),
            "tracks": tracks
        }
        
        return json.dumps(result)
        
    except Exception as e:
        logger.error(f"Failed to fetch Spotify playlist: {e}")
        return json.dumps({
            "success": False,
            "error": str(e)
        })
