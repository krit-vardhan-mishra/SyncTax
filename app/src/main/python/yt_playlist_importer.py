"""
YouTube Playlist Importer - Uses ytmusicapi to fetch playlist data
This module provides playlist fetching functionality for YouTube and YouTube Music playlists
"""

from ytmusicapi import YTMusic
import json
import logging
import re

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def extract_playlist_id(playlist_url):
    """
    Extract playlist ID from YouTube/YouTube Music URL
    
    Args:
        playlist_url: Full YouTube/YouTube Music playlist URL
        
    Returns:
        Playlist ID string or None if not found
    """
    try:
        # Handle various YouTube URL formats
        # https://www.youtube.com/playlist?list=PLxxxxxx
        # https://youtube.com/playlist?list=PLxxxxxx
        # https://music.youtube.com/playlist?list=PLxxxxxx
        # https://www.youtube.com/watch?v=xxx&list=PLxxxxxx
        
        if "list=" in playlist_url:
            # Extract the list parameter
            match = re.search(r'list=([a-zA-Z0-9_-]+)', playlist_url)
            if match:
                return match.group(1)
        
        return None
    except Exception as e:
        logger.error(f"Failed to extract playlist ID: {e}")
        return None


def fetch_playlist(playlist_url):
    """
    Fetch playlist data from YouTube/YouTube Music
    
    Args:
        playlist_url: Full YouTube/YouTube Music playlist URL
        
    Returns:
        JSON string containing playlist data or error
    """
    try:
        # Extract playlist ID
        playlist_id = extract_playlist_id(playlist_url)
        
        if not playlist_id:
            return json.dumps({
                "success": False,
                "error": "Invalid YouTube playlist link. URL must contain 'list=' parameter."
            })
        
        logger.info(f"Fetching playlist: {playlist_id}")
        
        # Initialize YTMusic client
        yt = YTMusic()
        
        # Fetch playlist data
        playlist_data = yt.get_playlist(playlist_id, limit=None)
        
        if not playlist_data:
            return json.dumps({
                "success": False,
                "error": "Could not fetch playlist. It may be private or deleted."
            })
        
        # Extract playlist info
        title = playlist_data.get("title", "Unknown Playlist")
        description = playlist_data.get("description", "")
        
        # Get thumbnail
        thumbnails = playlist_data.get("thumbnails", [])
        thumbnail_url = thumbnails[-1].get("url", "") if thumbnails else ""
        
        # Process tracks
        tracks = []
        for idx, track in enumerate(playlist_data.get("tracks", [])):
            # Skip unavailable tracks
            if not track.get("videoId"):
                continue
                
            # Extract artist names
            artists = track.get("artists", [])
            artist_name = ", ".join(a.get("name", "") for a in artists if a.get("name"))
            if not artist_name:
                artist_name = "Unknown Artist"
            
            # Get track thumbnail
            track_thumbnails = track.get("thumbnails", [])
            track_thumbnail = track_thumbnails[-1].get("url", "") if track_thumbnails else ""
            
            # Get album info
            album = track.get("album")
            album_name = album.get("name", "") if album else ""
            
            # Duration handling - can be None, string "3:45", or seconds integer
            duration_seconds = None
            duration_val = track.get("duration_seconds")
            if duration_val is not None:
                duration_seconds = int(duration_val)
            else:
                # Try parsing duration string
                duration_str = track.get("duration", "")
                if duration_str and ":" in duration_str:
                    parts = duration_str.split(":")
                    try:
                        if len(parts) == 2:
                            duration_seconds = int(parts[0]) * 60 + int(parts[1])
                        elif len(parts) == 3:
                            duration_seconds = int(parts[0]) * 3600 + int(parts[1]) * 60 + int(parts[2])
                    except ValueError:
                        pass
            
            track_info = {
                "videoId": track.get("videoId", ""),
                "title": track.get("title", "Unknown Title"),
                "artist": artist_name,
                "album": album_name,
                "thumbnail": track_thumbnail,
                "duration": duration_seconds,
                "position": idx
            }
            tracks.append(track_info)
        
        result = {
            "success": True,
            "playlistId": playlist_id,
            "title": title,
            "description": description,
            "thumbnail": thumbnail_url,
            "trackCount": len(tracks),
            "tracks": tracks
        }
        
        logger.info(f"Successfully fetched playlist '{title}' with {len(tracks)} tracks")
        return json.dumps(result)
        
    except Exception as e:
        error_msg = str(e)
        logger.error(f"Failed to fetch playlist: {error_msg}")
        
        # Provide more helpful error messages
        if "404" in error_msg or "not found" in error_msg.lower():
            error_msg = "Playlist not found. It may be private, deleted, or the URL is incorrect."
        elif "private" in error_msg.lower():
            error_msg = "This playlist is private. Only public playlists can be imported."
        
        return json.dumps({
            "success": False,
            "error": error_msg
        })


def validate_playlist_url(url):
    """
    Validate if the URL is a valid YouTube/YouTube Music playlist URL
    
    Args:
        url: URL string to validate
        
    Returns:
        JSON string with validation result
    """
    try:
        is_valid = False
        platform = None
        
        # Check for YouTube Music
        if "music.youtube.com" in url and "list=" in url:
            is_valid = True
            platform = "YouTube Music"
        # Check for regular YouTube
        elif ("youtube.com" in url or "youtu.be" in url) and "list=" in url:
            is_valid = True
            platform = "YouTube"
        
        playlist_id = extract_playlist_id(url) if is_valid else None
        
        return json.dumps({
            "isValid": is_valid,
            "platform": platform,
            "playlistId": playlist_id
        })
        
    except Exception as e:
        logger.error(f"URL validation failed: {e}")
        return json.dumps({
            "isValid": False,
            "platform": None,
            "playlistId": None
        })


# For testing in Python environment
if __name__ == "__main__":
    # Test with a sample public playlist
    test_url = "https://www.youtube.com/playlist?list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf"
    print(f"Testing with URL: {test_url}")
    
    validation = validate_playlist_url(test_url)
    print(f"Validation result: {validation}")
    
    result = fetch_playlist(test_url)
    print(f"Fetch result: {result[:500]}...")  # Print first 500 chars
