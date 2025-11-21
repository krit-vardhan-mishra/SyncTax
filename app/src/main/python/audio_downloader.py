"""
Audio downloader using yt-dlp
Downloads audio from YouTube and other platforms with album art embedding
"""
import json
import os
from typing import Dict, Any


def download_audio(url: str, output_dir: str, prefer_mp3: bool = False) -> str:
    """
    Download audio from a URL using yt-dlp with embedded album art
    
    Args:
        url: Video/audio URL to download
        output_dir: Directory to save the downloaded file
        prefer_mp3: If True and FFmpeg is available, convert to MP3. Otherwise use M4A with embedded art.
        
    Returns:
        JSON string with download result
    """
    try:
        import yt_dlp
        
        # Ensure output directory exists
        os.makedirs(output_dir, exist_ok=True)
        
        # Check if FFmpeg is available
        ffmpeg_available = False
        ffmpeg_path = None
        
        # Try system PATH first
        try:
            import subprocess
            result = subprocess.run(['ffmpeg', '-version'], capture_output=True, timeout=5)
            ffmpeg_available = result.returncode == 0
            if ffmpeg_available:
                ffmpeg_path = 'ffmpeg'
        except:
            pass
        
        # If not in PATH, try local tools directory
        if not ffmpeg_available:
            # Get the project root directory (5 levels up from this file)
            current_file = os.path.abspath(__file__)
            project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(current_file)))))
            local_ffmpeg = os.path.join(project_root, 'tools', 'ffmpeg-8.0.1-essentials_build', 'bin', 'ffmpeg.exe')
            if os.path.exists(local_ffmpeg):
                try:
                    result = subprocess.run([local_ffmpeg, '-version'], capture_output=True, timeout=5)
                    if result.returncode == 0:
                        ffmpeg_available = True
                        ffmpeg_path = local_ffmpeg
                except:
                    pass
        
        # Configure yt-dlp options based on FFmpeg availability
        base_opts = {
            'quiet': False,
            'no_warnings': False,
        }
        
        if ffmpeg_available and ffmpeg_path:
            base_opts['ffmpeg_location'] = ffmpeg_path
        
        if ffmpeg_available and prefer_mp3:
            # Full conversion with embedded thumbnail using FFmpeg
            ydl_opts = {
                'format': 'bestaudio/best',
                'outtmpl': os.path.join(output_dir, '%(title)s.%(ext)s'),
                'postprocessors': [
                    {
                        'key': 'FFmpegExtractAudio',
                        'preferredcodec': 'mp3',
                        'preferredquality': '320',
                    },
                    {
                        'key': 'EmbedThumbnail',
                    },
                    {
                        'key': 'FFmpegMetadata',
                        'add_metadata': True,
                    },
                ],
                **base_opts
            }
        else:
            # Download M4A with embedded thumbnail using FFmpeg
            ydl_opts = {
                'format': 'bestaudio[ext=m4a]/bestaudio/best',
                'outtmpl': os.path.join(output_dir, '%(title)s.%(ext)s'),
                'postprocessors': [
                    {
                        'key': 'FFmpegExtractAudio',
                        'preferredcodec': 'm4a',
                        'preferredquality': '320',
                    },
                    {
                        'key': 'EmbedThumbnail',
                    },
                    {
                        'key': 'FFmpegMetadata',
                        'add_metadata': True,
                    },
                ],
                **base_opts
            }
        
        # Download the audio
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            
            # Get the final filename
            filename = ydl.prepare_filename(info)
            
            # If FFmpeg was used, update extension to mp3
            if ffmpeg_available and prefer_mp3:
                filename = os.path.splitext(filename)[0] + '.mp3'
            
            result = {
                "success": True,
                "message": "Download completed successfully with embedded album art",
                "file_path": filename,
                "title": info.get('title', 'Unknown'),
                "artist": info.get('artist') or info.get('uploader', 'Unknown'),
                "duration": info.get('duration', 0),
                "thumbnail_url": info.get('thumbnail', ''),
                "format": 'mp3' if (ffmpeg_available and prefer_mp3) else info.get('ext', 'unknown'),
                "ffmpeg_available": ffmpeg_available,
            }
            
            return json.dumps(result)
            
    except ImportError as e:
        return json.dumps({
            "success": False,
            "message": f"yt-dlp not installed: {str(e)}",
            "file_path": "",
        })
    except Exception as e:
        return json.dumps({
            "success": False,
            "message": f"Download failed: {str(e)}",
            "file_path": "",
        })


def get_video_info(url: str) -> str:
    """
    Get video information without downloading
    
    Args:
        url: Video/audio URL
        
    Returns:
        JSON string with video information
    """
    try:
        import yt_dlp
        
        ydl_opts = {
            'quiet': True,
            'no_warnings': True,
        }
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            
            result = {
                "success": True,
                "title": info.get('title', 'Unknown'),
                "artist": info.get('artist') or info.get('uploader', 'Unknown'),
                "duration": info.get('duration', 0),
                "thumbnail_url": info.get('thumbnail', ''),
                "description": info.get('description', ''),
            }
            
            return json.dumps(result)
            
    except Exception as e:
        return json.dumps({
            "success": False,
            "message": f"Failed to get video info: {str(e)}",
        })
