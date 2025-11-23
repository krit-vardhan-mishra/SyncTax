"""
Audio downloader using yt-dlp
Downloads audio from YouTube and other platforms with album art embedding
"""
import json
import os
from typing import Dict, Any


def download_audio(url: str, output_dir: str, prefer_mp3: bool = False, format_id: str = None) -> str:
    """
    Download audio from a URL using yt-dlp with embedded album art
    
    Args:
        url: Video/audio URL to download
        output_dir: Directory to save the downloaded file
        prefer_mp3: If True and FFmpeg is available, convert to MP3. Otherwise use M4A with embedded art.
        format_id: Specific format ID to download. If None, uses best audio format.
        
    Returns:
        JSON string with download result
    """
    try:
        import sys
        print(f"🎵 Python: Starting download_audio", file=sys.stderr)
        print(f"🎵 Python: URL: {url}", file=sys.stderr)
        print(f"🎵 Python: Output directory: {output_dir}", file=sys.stderr)
        
        import yt_dlp
        
        # Ensure output directory exists
        os.makedirs(output_dir, exist_ok=True)
        print(f"🎵 Python: Output directory created/verified", file=sys.stderr)
        
        # Check if FFmpeg is available
        ffmpeg_available = False
        ffmpeg_path = None
        
        print(f"🎵 Python: Checking for FFmpeg...", file=sys.stderr)
        
        # Try system PATH first
        try:
            import subprocess
            result = subprocess.run(['ffmpeg', '-version'], capture_output=True, timeout=5)
            ffmpeg_available = result.returncode == 0
            if ffmpeg_available:
                ffmpeg_path = 'ffmpeg'
                print(f"🎵 Python: FFmpeg found in PATH", file=sys.stderr)
        except Exception as e:
            print(f"🎵 Python: FFmpeg not in PATH: {e}", file=sys.stderr)
        
        # If not in PATH, try local tools directory (Android uses 'ffmpeg' not 'ffmpeg.exe')
        if not ffmpeg_available:
            # Get the project root directory (5 levels up from this file)
            current_file = os.path.abspath(__file__)
            project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(current_file)))))
            # Try both with and without .exe extension for cross-platform compatibility
            for ffmpeg_name in ['ffmpeg', 'ffmpeg.exe']:
                local_ffmpeg = os.path.join(project_root, 'tools', 'ffmpeg-8.0.1-essentials_build', 'bin', ffmpeg_name)
                print(f"🎵 Python: Checking local FFmpeg at: {local_ffmpeg}", file=sys.stderr)
                if os.path.exists(local_ffmpeg):
                    try:
                        result = subprocess.run([local_ffmpeg, '-version'], capture_output=True, timeout=5)
                        if result.returncode == 0:
                            ffmpeg_available = True
                            ffmpeg_path = local_ffmpeg
                            print(f"🎵 Python: FFmpeg found locally at {local_ffmpeg}", file=sys.stderr)
                            break
                    except Exception as e:
                        print(f"🎵 Python: Local FFmpeg check failed: {e}", file=sys.stderr)
        
        print(f"🎵 Python: FFmpeg available: {ffmpeg_available}", file=sys.stderr)
        
        # List of YouTube clients to try in order (from most compatible to least)
        clients_to_try = ['android', 'web', 'tv', 'ios', 'mweb']
        
        last_error = None
        
        for client in clients_to_try:
            try:
                print(f"🎵 Python: Trying YouTube client: {client}", file=sys.stderr)
                
                # Configure yt-dlp options for this client
                ydl_opts = {
                    'quiet': False,
                    'no_warnings': False,
                    'nocheckcertificate': True,
                    'prefer_free_formats': True,
                    'postprocessors': [],
                    'outtmpl': os.path.join(output_dir, '%(title)s.%(ext)s'),
                    'extractor_args': {
                        'youtube': {
                            'skip': ['hls', 'dash'],
                            'player_client': [client],
                        }
                    },
                }
                
                # Set format based on whether format_id is specified
                if format_id:
                    ydl_opts['format'] = format_id
                    print(f"🎵 Python: Using specified format: {format_id}", file=sys.stderr)
                else:
                    # Select best audio format - prefer audio-only, fallback to smallest video+audio for audio extraction
                    # YouTube audio formats: 140 (m4a 128k), 139 (m4a 48k), 141 (m4a 256k), 251 (webm opus), 250 (webm opus), 249 (webm opus)
                    # If audio-only not available, use smallest video format (18) for audio extraction
                    ydl_opts['format'] = 'bestaudio/best[height<=360]'
                    print(f"🎵 Python: Using audio format selection (prefer audio-only, fallback to video with audio)", file=sys.stderr)
                
                # Add post-processing to extract audio if FFmpeg is available
                if ffmpeg_available and ffmpeg_path:
                    ydl_opts['ffmpeg_location'] = ffmpeg_path
                    # Add audio extraction post-processor
                    ydl_opts['postprocessors'] = [{
                        'key': 'FFmpegExtractAudio',
                        'preferredcodec': 'm4a',
                        'preferredquality': '192',
                    }]
                    print(f"🎵 Python: FFmpeg available - will extract audio from video if needed", file=sys.stderr)
                else:
                    # Without FFmpeg, we still need to extract audio from video formats
                    # yt-dlp can do basic audio extraction without FFmpeg for some formats
                    print(f"🎵 Python: FFmpeg not available - will download and extract audio using yt-dlp", file=sys.stderr)
                
                print(f"🎵 Python: Starting yt-dlp download with client {client}...", file=sys.stderr)
                
                with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                    info = ydl.extract_info(url, download=True)
                    
                    print(f"🎵 Python: Download completed with client {client}", file=sys.stderr)
                    print(f"🎵 Python: Title: {info.get('title', 'Unknown')}", file=sys.stderr)
                    
                    # Get the final filename (already includes output directory from outtmpl)
                    filename = ydl.prepare_filename(info)
                    
                    # Use the actual extension from the downloaded file
                    actual_ext = info.get('ext', 'mp4')
                    filename = os.path.splitext(filename)[0] + '.' + actual_ext
                    
                    # yt-dlp may remove the [ID] part during conversion, so also try without it
                    base_filename = os.path.splitext(filename)[0]
                    if '[' in base_filename and ']' in base_filename:
                        # Remove the [ID] part
                        title_part = base_filename.split(' [')[0]
                        filename_no_id = title_part + os.path.splitext(filename)[1]
                        filename_no_id_full = os.path.join(output_dir, filename_no_id)
                        if os.path.exists(filename_no_id_full):
                            filename = filename_no_id_full
                    
                    print(f"🎵 Python: Final file path: {filename}", file=sys.stderr)
                    print(f"🎵 Python: File exists: {os.path.exists(filename)}", file=sys.stderr)
                    if os.path.exists(filename):
                        file_size_mb = os.path.getsize(filename) / (1024 * 1024)
                        print(f"🎵 Python: File size: {file_size_mb:.2f}MB", file=sys.stderr)
                    
                    result = {
                        "success": True,
                        "message": f"Download completed successfully using {client} client",
                        "file_path": filename,
                        "format": info.get('ext', 'unknown'),
                        "ffmpeg_available": ffmpeg_available,
                        "client_used": client,
                    }
                    
                    print(f"🎵 Python: ✅ Success with client {client}!", file=sys.stderr)
                    return json.dumps(result)
                    
            except Exception as e:
                error_msg = str(e)
                print(f"🎵 Python: ❌ Client {client} failed: {error_msg}", file=sys.stderr)
                last_error = error_msg
                continue
        
        # If we get here, all clients failed
        print(f"🎵 Python: ❌ All clients failed, last error: {last_error}", file=sys.stderr)
        return json.dumps({
            "success": False,
            "message": f"Download failed with all clients. Last error: {last_error}",
            "file_path": "",
        })
            
    except ImportError as e:
        import sys
        print(f"🎵 Python: ❌ ImportError: {str(e)}", file=sys.stderr)
        return json.dumps({
            "success": False,
            "message": f"yt-dlp not installed: {str(e)}",
            "file_path": "",
        })
    except Exception as e:
        import sys
        import traceback
        print(f"🎵 Python: ❌ Exception: {str(e)}", file=sys.stderr)
        print(f"🎵 Python: Stack trace:", file=sys.stderr)
        traceback.print_exc(file=sys.stderr)
        return json.dumps({
            "success": False,
            "message": f"Download failed: {str(e)}",
            "file_path": "",
        })


def get_video_info(url: str) -> str:
    """
    Get video information including all available formats from all clients
    
    Args:
        url: Video/audio URL
        
    Returns:
        JSON string with video information and formats
    """
    try:
        import yt_dlp
        
        # List of YouTube clients to try in order
        clients_to_try = ['android', 'web', 'tv', 'ios', 'mweb']
        
        all_formats = []
        successful_client = None
        video_info = {}
        
        for client in clients_to_try:
            try:
                ydl_opts = {
                    'quiet': True,
                    'no_warnings': True,
                    'extractor_args': {
                        'youtube': {
                            'player_client': [client],
                        }
                    },
                }
                
                with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                    info = ydl.extract_info(url, download=False)
                    
                    # Store video info from the first successful client
                    if not successful_client:
                        successful_client = client
                        video_info = {
                            "success": True,
                            "title": info.get('title', 'Unknown'),
                            "artist": info.get('artist') or info.get('uploader', 'Unknown'),
                            "duration": info.get('duration', 0),
                            "thumbnail_url": info.get('thumbnail', ''),
                            "description": info.get('description', ''),
                            "client_used": client,
                        }
                    
                    # Extract format information and add client info
                    if 'formats' in info:
                        for fmt in info['formats']:
                            # Only include formats that have audio
                            acodec = fmt.get('acodec', 'none')
                            vcodec = fmt.get('vcodec', 'none')
                            format_id = fmt.get('format_id', '')
                            
                            # Skip storyboards and image-only formats
                            if 'sb' in format_id or fmt.get('ext') == 'mhtml':
                                continue
                            
                            # Include formats that have audio:
                            # 1. Pure audio formats (acodec != none, vcodec == none)
                            # 2. Combined video+audio formats that can be converted (like format 18)
                            # 3. Known audio format IDs: 140, 139, 141, 249, 250, 251, 171, 172
                            has_audio = acodec != 'none' and acodec != ''
                            
                            if has_audio:
                                # Determine if it's audio-only or combined
                                is_audio_only = (
                                    vcodec == 'none' or 
                                    fmt.get('resolution') == 'audio only' or
                                    format_id in ['140', '139', '141', '249', '250', '251', '171', '172']
                                )
                                
                                # Extract comprehensive format information
                                format_info = {
                                    'format_id': format_id,
                                    'container': fmt.get('ext', ''),  # Map 'ext' to 'container' for Format.kt
                                    'ext': fmt.get('ext', ''),
                                    'resolution': fmt.get('resolution', 'audio only' if is_audio_only else 'combined'),
                                    'fps': str(fmt.get('fps', '')) if fmt.get('fps') else None,
                                    'vcodec': vcodec,
                                    'acodec': acodec,
                                    'encoding': fmt.get('encoding', ''),
                                    'filesize': fmt.get('filesize', 0) or fmt.get('filesize_approx', 0) or 0,
                                    'format_note': fmt.get('format_note', '') or fmt.get('quality', '') or ('audio only' if is_audio_only else 'combined'),
                                    'asr': str(fmt.get('asr', '')) if fmt.get('asr') else None,  # Audio sample rate
                                    'url': fmt.get('url', ''),
                                    'lang': fmt.get('language', '') or fmt.get('audio_lang', ''),  # Audio language
                                    'tbr': str(fmt.get('tbr', '')) if fmt.get('tbr') else str(fmt.get('abr', '')) if fmt.get('abr') else None,  # Total/audio bitrate
                                    'abr': fmt.get('abr'),  # Keep for backward compatibility
                                    'client': client,
                                    'audio_only': is_audio_only,
                                }
                                all_formats.append(format_info)
                    
            except Exception as e:
                # Continue to next client
                continue
        
        if successful_client:
            video_info["formats"] = all_formats
            return json.dumps(video_info, default=str)
        else:
            return json.dumps({
                "success": False,
                "message": f"Failed to get video info with all clients",
            })
            
    except Exception as e:
        return json.dumps({
            "success": False,
            "message": f"Failed to get video info: {str(e)}",
        })
