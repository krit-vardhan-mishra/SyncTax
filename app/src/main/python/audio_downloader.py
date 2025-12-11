"""
Audio downloader using yt-dlp
Downloads audio from YouTube and other platforms with album art embedding
"""
import os
import sys
import json
import subprocess
import math
from typing import Dict, Any


def map_format_id_to_selector(format_id: str) -> str:
    """
    Maps format IDs to format selectors to bypass YouTube restrictions.
    YouTube blocks specific format ID downloads but allows format selectors.
    
    Format ID examples:
    - 251: Opus ~160kbps (webm)
    - 250: Opus ~70kbps (webm)
    - 249: Opus ~50kbps (webm)
    - 140: AAC 128kbps (m4a)
    - 139: AAC 48kbps (m4a)
    
    Args:
        format_id: The specific format ID selected by the user
        
    Returns:
        Format selector string that YouTube accepts
    """
    # Opus formats (webm container)
    if format_id in ['251', '250', '249', '600']:
        return 'bestaudio[ext=webm]/bestaudio'
    
    # AAC formats (m4a container)
    elif format_id in ['140', '139', '141']:
        return 'bestaudio[ext=m4a]/bestaudio'
    
    # Generic audio formats
    elif format_id in ['ba', 'bestaudio']:
        return 'bestaudio'
    
    # If specific format ID, try it but fallback to bestaudio
    else:
        return f'{format_id}/bestaudio'


def _get_image_size(image_path):
    """Return (width, height) of image using ffprobe, or None on failure."""
    try:
        proc = subprocess.run([
            'ffprobe', '-v', 'error', '-select_streams', 'v:0',
            '-show_entries', 'stream=width,height', '-of', 'csv=p=0:s=x', image_path
        ], capture_output=True, text=True)
        out = proc.stdout.strip()
        if not out:
            return None
        parts = out.split('x')
        if len(parts) != 2:
            return None
        return int(parts[0]), int(parts[1])
    except Exception:
        return None


def crop_center_thumbnail(orig_thumb, out_dir, base):
    """Crop thumbnail to 720x720 from center.
    
    Extracts the center 2x2 grid from a 4x4 grid (center 50% of image),
    removing the outer 25% on each edge, then scales to 720x720.

    Returns the path to the cropped thumbnail on success, else None.
    """
    if not orig_thumb or not os.path.exists(orig_thumb):
        return None

    ext = os.path.splitext(orig_thumb)[1]
    cropped = os.path.join(out_dir, f"{base}_thumb_720x720{ext}")

    # Crop center 50% of image (2x2 from 4x4 grid):
    # - New width = iw/2 (50% of original width)
    # - New height = ih/2 (50% of original height)
    # - X offset = iw/4 (start at 25% from left)
    # - Y offset = ih/4 (start at 25% from top)
    # Then scale to 720x720
    vf = "crop=iw/2:ih/2:iw/4:ih/4,scale=720:720"

    print(f"🎨 Python: Cropping thumbnail with filter: {vf}", file=sys.stderr)
    cmd = ['ffmpeg', '-y', '-i', orig_thumb, '-vf', vf, cropped]

    # Simple run_cmd equivalent
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
        if result.returncode == 0 and os.path.exists(cropped) and os.path.getsize(cropped) > 1024:
            return cropped
        else:
            if os.path.exists(cropped):
                try:
                    os.remove(cropped)
                except:
                    pass
            return None
    except Exception:
        if os.path.exists(cropped):
            try:
                os.remove(cropped)
            except:
                pass
        return None


def download_audio(url: str, output_dir: str, prefer_mp3: bool = False, format_id: str = None, po_token_data: str = None) -> str:
    """
    Download audio from a URL using yt-dlp with embedded album art and PO token support
    
    Args:
        url: Video/audio URL to download
        output_dir: Directory to save the downloaded file
        prefer_mp3: If True and FFmpeg is available, convert to MP3. Otherwise use M4A with embedded art.
        format_id: Specific format ID to download (optional)
        po_token_data: JSON string containing PO token data (optional)
        
    Returns:
        JSON string with download result
    """
    # Parse PO token data (JSON format: {"visitor_data": "...", "android": "...", "web": "...", "ios": "..."})
    po_tokens = {}
    visitor_data = None
    
    if po_token_data:
        try:
            token_dict = json.loads(po_token_data)
            visitor_data = token_dict.get('visitor_data')
            po_tokens = {
                'android': token_dict.get('android'),
                'web': token_dict.get('web'),
                'ios': token_dict.get('ios'),
                'tv': token_dict.get('tv'),
                'mweb': token_dict.get('mweb')
            }
            print(f"🎵 Python: Loaded PO tokens for {len([t for t in po_tokens.values() if t])} clients", file=sys.stderr)
        except json.JSONDecodeError as e:
            print(f"🎵 Python: Failed to parse PO token data: {e}", file=sys.stderr)
            # Fallback: treat as single token
            po_tokens = {}
    
    # List of YouTube clients to try in order (from most compatible to least)
    clients_to_try = ['android', 'web', 'tv', 'ios', 'mweb']
    
    last_error = None
    
    for client in clients_to_try:
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
                'nocheckcertificate': True,
                'prefer_free_formats': True,
            }
            
            if ffmpeg_available and ffmpeg_path:
                base_opts['ffmpeg_location'] = ffmpeg_path
            
            # Configure format selection
            if format_id:
                format_spec = format_id
            elif ffmpeg_available and prefer_mp3:
                format_spec = 'bestaudio/best'
            else:
                format_spec = 'bestaudio[ext=m4a]/bestaudio/best'
            
            # Configure yt-dlp options for this client
            ydl_opts = {
                'format': format_spec,
                'outtmpl': os.path.join(output_dir, '%(title)s.%(ext)s'),
                'extractor_args': {
                    'youtube': {
                        'skip': ['hls', 'dash'],
                        'player_client': [client],
                    }
                },
                **base_opts
            }
            
            # Add visitor_data if available
            if visitor_data:
                ydl_opts['extractor_args']['youtube']['visitor_data'] = visitor_data
                print(f"🎵 Python: Using visitor_data for {client} client", file=sys.stderr)
            
            # Add PO Token for this specific client if available
            if po_tokens.get(client):
                ydl_opts['extractor_args']['youtube']['po_token'] = [f'{client}+{po_tokens[client]}']
                print(f"🎵 Python: Using PO Token for {client} client", file=sys.stderr)
            elif po_token_data and not po_tokens:  # Fallback for old format
                ydl_opts['extractor_args']['youtube']['po_token'] = [f'{client}+{po_token_data}']
                print(f"🎵 Python: Using fallback PO Token for {client} client", file=sys.stderr)
            
            # Configure postprocessors based on format and FFmpeg availability
            if ffmpeg_available and prefer_mp3:
                # Full conversion with embedded thumbnail using FFmpeg
                ydl_opts['writethumbnail'] = True  # Download thumbnail for embedding
                ydl_opts['postprocessors'] = [
                    {
                        'key': 'FFmpegExtractAudio',
                        'preferredcodec': 'mp3',
                        'preferredquality': '320',
                    },
                    {
                        'key': 'EmbedThumbnail',
                        'already_have_thumbnail': False,
                    },
                    {
                        'key': 'FFmpegMetadata',
                        'add_metadata': True,
                    },
                ]
            elif ffmpeg_available:
                # Download M4A with embedded thumbnail - M4A supports embedded thumbnails better than opus
                ydl_opts['writethumbnail'] = True  # Download thumbnail for embedding
                ydl_opts['postprocessors'] = [
                    {
                        'key': 'FFmpegExtractAudio',
                        'preferredcodec': 'm4a',
                        'preferredquality': '320',
                    },
                    {
                        'key': 'EmbedThumbnail',
                        'already_have_thumbnail': False,
                    },
                    {
                        'key': 'FFmpegMetadata',
                        'add_metadata': True,
                    },
                ]
            else:
                # FFmpeg NOT available - download native M4A format and add metadata via Mutagen
                print(f"🎵 Python: FFmpeg not available, downloading native M4A format", file=sys.stderr)
                ydl_opts['format'] = 'bestaudio[ext=m4a]/bestaudio'
                ydl_opts['writethumbnail'] = True  # Download thumbnail separately
                ydl_opts['postprocessors'] = []  # No FFmpeg postprocessors
            
            # Download the audio
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=True)
                
                # Get the final filename
                filename = ydl.prepare_filename(info)
                actual_ext = info.get('ext', 'm4a')
                
                # Update extension based on format
                if ffmpeg_available and prefer_mp3:
                    filename = os.path.splitext(filename)[0] + '.mp3'
                elif ffmpeg_available:
                    filename = os.path.splitext(filename)[0] + '.m4a'
                else:
                    # Keep original extension when FFmpeg not available
                    filename = os.path.splitext(filename)[0] + '.' + actual_ext

                # Crop thumbnail to 720x720 if it exists
                thumb_base = os.path.splitext(filename)[0]
                orig_thumb = None
                for ext in ['.jpg', '.webp', '.png', '.jpeg', '.jpg.webp']:
                    potential_thumb = thumb_base + ext
                    if os.path.exists(potential_thumb):
                        orig_thumb = potential_thumb
                        break

                cropped_thumb = None
                if orig_thumb and ffmpeg_available:
                    print(f"🎵 Python: Cropping thumbnail to 720x720...", file=sys.stderr)
                    base_name = os.path.splitext(os.path.basename(filename))[0]
                    cropped_thumb = crop_center_thumbnail(orig_thumb, output_dir, base_name)
                    if cropped_thumb:
                        print(f"🎵 Python: Thumbnail cropped successfully: {cropped_thumb}", file=sys.stderr)
                        # Replace original thumbnail with cropped one for embedding
                        try:
                            os.remove(orig_thumb)
                            # Rename cropped to original name so EmbedThumbnail uses it
                            os.rename(cropped_thumb, orig_thumb)
                            cropped_thumb = orig_thumb
                        except Exception as e:
                            print(f"🎵 Python: Failed to replace thumbnail: {e}", file=sys.stderr)
                    else:
                        print(f"🎵 Python: Thumbnail cropping failed, using original", file=sys.stderr)
                
                # If FFmpeg is NOT available, try to embed metadata using Mutagen
                metadata_embedded = False
                if not ffmpeg_available:
                    print(f"🎵 Python: Attempting to embed metadata with Mutagen...", file=sys.stderr)
                    try:
                        from mutagen.mp4 import MP4, MP4Cover
                        
                        if os.path.exists(filename) and actual_ext in ['m4a', 'mp4']:
                            audio = MP4(filename)
                            
                            # Set metadata tags for M4A
                            title = info.get('title', 'Unknown')
                            artist = info.get('artist') or info.get('uploader', 'Unknown')
                            album = info.get('album', 'YouTube Audio')
                            
                            audio['\xa9nam'] = [title]  # Title
                            audio['\xa9ART'] = [artist]  # Artist
                            audio['\xa9alb'] = [album]   # Album
                            
                            print(f"🎵 Python: M4A tags set - Title: {title}, Artist: {artist}, Album: {album}", file=sys.stderr)
                            
                            # Try to embed thumbnail
                            thumb_base = os.path.splitext(filename)[0]
                            thumb_extensions = ['.jpg', '.webp', '.png', '.jpeg']
                            thumb_file = None
                            for ext in thumb_extensions:
                                potential_thumb = thumb_base + ext
                                if os.path.exists(potential_thumb):
                                    thumb_file = potential_thumb
                                    break
                            
                            if thumb_file:
                                print(f"🎵 Python: Found thumbnail: {thumb_file}", file=sys.stderr)
                                try:
                                    with open(thumb_file, 'rb') as f:
                                        thumb_data = f.read()
                                    
                                    # Determine format
                                    if thumb_file.endswith('.png'):
                                        cover_format = MP4Cover.FORMAT_PNG
                                    else:
                                        cover_format = MP4Cover.FORMAT_JPEG
                                    
                                    audio['covr'] = [MP4Cover(thumb_data, imageformat=cover_format)]
                                    print(f"🎵 Python: Cover art embedded ({len(thumb_data)} bytes)", file=sys.stderr)
                                    
                                    # Clean up thumbnail file
                                    os.remove(thumb_file)
                                except Exception as e:
                                    print(f"🎵 Python: Cover embedding failed: {e}", file=sys.stderr)
                            
                            audio.save()
                            metadata_embedded = True
                            print(f"✅ Python: M4A metadata saved successfully", file=sys.stderr)
                            
                    except ImportError:
                        print(f"🎵 Python: Mutagen MP4 module not available", file=sys.stderr)
                    except Exception as e:
                        print(f"🎵 Python: Mutagen metadata embedding failed: {e}", file=sys.stderr)
                
                # Clean up any leftover thumbnail files regardless of success
                thumb_base = os.path.splitext(filename)[0]
                for ext in ['.jpg', '.webp', '.png', '.jpeg', '.jpg.webp']:
                    potential_thumb = thumb_base + ext
                    if os.path.exists(potential_thumb):
                        try:
                            os.remove(potential_thumb)
                            print(f"🧹 Python: Cleaned up thumbnail: {potential_thumb}", file=sys.stderr)
                        except Exception as e:
                            print(f"🧹 Python: Failed to cleanup thumbnail {potential_thumb}: {e}", file=sys.stderr)
                
                result = {
                    "success": True,
                    "message": f"Download completed successfully using {client} client" + 
                              (" with embedded metadata" if (ffmpeg_available or metadata_embedded) else " (metadata not embedded)"),
                    "file_path": filename,
                    "title": info.get('title', 'Unknown'),
                    "artist": info.get('artist') or info.get('uploader', 'Unknown'),
                    "duration": info.get('duration', 0),
                    "thumbnail_url": info.get('thumbnail', ''),
                    "format": actual_ext if not ffmpeg_available else ('mp3' if prefer_mp3 else 'm4a'),
                    "ffmpeg_available": ffmpeg_available,
                    "metadata_embedded": ffmpeg_available or metadata_embedded,
                    "client_used": client,
                }
                
                return json.dumps(result)
                
        except Exception as e:
            error_message = str(e)
            print(f"🎵 Python: Failed with {client} client: {error_message}", file=sys.stderr)
            last_error = error_message
            
            # Continue to next client if this one failed
            continue
    
    # If all clients failed, return error
    return json.dumps({
        "success": False,
        "message": f"Download failed with all clients. Last error: {last_error}",
        "file_path": "",
    })

def get_video_info(url: str, po_token_data: str = None) -> str:
    """
    Get video information without downloading with PO token support
    
    Args:
        url: Video/audio URL
        po_token_data: JSON string containing PO token data (optional)
        
    Returns:
        JSON string with video information
    """
    # Parse PO token data (same as download_audio)
    po_tokens = {}
    visitor_data = None
    
    if po_token_data:
        try:
            token_dict = json.loads(po_token_data)
            visitor_data = token_dict.get('visitor_data')
            po_tokens = {
                'android': token_dict.get('android'),
                'web': token_dict.get('web'),
                'ios': token_dict.get('ios'),
                'tv': token_dict.get('tv'),
                'mweb': token_dict.get('mweb')
            }
        except json.JSONDecodeError:
            # Fallback: treat as single token for current client
            pass
    
    # List of YouTube clients to try
    clients_to_try = ['android', 'web', 'tv', 'ios', 'mweb']
    
    all_formats = []
    video_info = {}
    successful_client = None
    
    for client in clients_to_try:
        try:
            import yt_dlp
            
            ydl_opts = {
                'quiet': True,
                'no_warnings': True,
                'extract_flat': False,
                'extractor_args': {
                    'youtube': {
                        'skip': ['hls', 'dash'],
                        'player_client': [client],
                    }
                },
            }
            
            # Add visitor_data if available
            if visitor_data:
                ydl_opts['extractor_args']['youtube']['visitor_data'] = visitor_data
            
            # Add PO Token for this specific client if available
            if po_tokens.get(client):
                ydl_opts['extractor_args']['youtube']['po_token'] = [f'{client}+{po_tokens[client]}']
            elif po_token_data and not po_tokens:  # Fallback for old format
                ydl_opts['extractor_args']['youtube']['po_token'] = [f'{client}+{po_token_data}']
            
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(url, download=False)
                
                # Collect format information
                formats = []
                if 'formats' in info:
                    for fmt in info['formats']:
                        if fmt.get('acodec') != 'none':  # Audio formats only
                            formats.append({
                                'format_id': fmt.get('format_id', ''),
                                'ext': fmt.get('ext', ''),
                                'abr': fmt.get('abr', 0),
                                'filesize': fmt.get('filesize', 0),
                                'format_note': fmt.get('format_note', ''),
                                'quality': fmt.get('quality', 0),
                            })
                
                video_info = {
                    "success": True,
                    "title": info.get('title', 'Unknown'),
                    "artist": info.get('artist') or info.get('uploader', 'Unknown'),
                    "album": info.get('album', 'YouTube Audio'),
                    "duration": info.get('duration', 0),
                    "thumbnail_url": info.get('thumbnail', ''),
                    "description": info.get('description', ''),
                    "view_count": info.get('view_count', 0),
                    "upload_date": info.get('upload_date', ''),
                    "formats": formats,
                    "client_used": client,
                }
                
                successful_client = client
                break  # Success, no need to try other clients
                
        except Exception as e:
            error_message = str(e)
            print(f"🎵 Python: get_video_info failed with {client} client: {error_message}", file=sys.stderr)
            continue
    
    if successful_client:
        return json.dumps(video_info)
    else:
        return json.dumps({
            "success": False,
            "message": f"Failed to get video info with all clients. Last error: {error_message}",
        })