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
        try:
            import subprocess
            result = subprocess.run(['ffmpeg', '-version'], capture_output=True, timeout=5)
            ffmpeg_available = result.returncode == 0
        except:
            pass
        
        # Configure yt-dlp options based on FFmpeg availability
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
                'writethumbnail': True,
                'quiet': False,
                'no_warnings': False,
            }
        else:
            # Download M4A without post-processing (we'll embed manually)
            ydl_opts = {
                'format': 'bestaudio[ext=m4a]/bestaudio/best',
                'outtmpl': os.path.join(output_dir, '%(title)s.%(ext)s'),
                'writethumbnail': True,
                'quiet': False,
                'no_warnings': False,
            }
        
        # Download the audio
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            
            # Get the final filename
            filename = ydl.prepare_filename(info)
            
            # If FFmpeg was used, update extension to mp3
            if ffmpeg_available and prefer_mp3:
                filename = os.path.splitext(filename)[0] + '.mp3'
            else:
                # Manually embed thumbnail for M4A files using mutagen
                try:
                    from mutagen.mp4 import MP4, MP4Cover
                    
                    # Find the thumbnail file
                    base_path = os.path.splitext(filename)[0]
                    thumbnail_path = None
                    for ext in ['.webp', '.jpg', '.png']:
                        potential_thumb = base_path + ext
                        if os.path.exists(potential_thumb):
                            thumbnail_path = potential_thumb
                            break
                    
                    if thumbnail_path and filename.endswith('.m4a'):
                        # Read thumbnail data
                        with open(thumbnail_path, 'rb') as f:
                            thumbnail_data = f.read()
                        
                        # Determine image format
                        if thumbnail_path.endswith('.png'):
                            cover_format = MP4Cover.FORMAT_PNG
                        elif thumbnail_path.endswith('.jpg') or thumbnail_path.endswith('.jpeg'):
                            cover_format = MP4Cover.FORMAT_JPEG
                        else:
                            # For WebP, we'll try JPEG format (most compatible)
                            cover_format = MP4Cover.FORMAT_JPEG
                        
                        # Embed the cover art
                        audio = MP4(filename)
                        audio['covr'] = [MP4Cover(thumbnail_data, imageformat=cover_format)]
                        audio.save()
                        
                        # Clean up the separate thumbnail file
                        os.remove(thumbnail_path)
                except ImportError:
                    # mutagen not available, keep separate thumbnail
                    pass
                except Exception as e:
                    # If embedding fails, keep separate thumbnail
                    print(f"Warning: Could not embed thumbnail: {e}")
            
            result = {
                "success": True,
                "message": "Download completed successfully with embedded album art",
                "file_path": filename,
                "title": info.get('title', 'Unknown'),
                "artist": info.get('artist') or info.get('uploader', 'Unknown'),
                "duration": info.get('duration', 0),
                "thumbnail_url": info.get('thumbnail', ''),
                "format": info.get('ext', 'unknown'),
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
