"""
Audio converter and metadata embedder using yt-dlp's FFmpeg integration.
Converts WebM to OGG/Opus and embeds metadata.
"""
import os
import sys
import json
import subprocess
import shutil

def convert_and_embed_metadata(
    input_path,
    output_dir,
    title=None,
    artist=None,
    album=None,
    thumbnail_path=None,
    output_format="ogg"
):
    """
    Convert audio file to a format that supports metadata and embed tags.
    Uses yt-dlp's FFmpeg integration if available, falls back to Mutagen.
    
    Args:
        input_path: Path to the input audio file (WebM, etc.)
        output_dir: Directory to save the output file
        title: Song title
        artist: Artist name
        album: Album name
        thumbnail_path: Path to thumbnail/cover image (optional)
        output_format: Output format ('ogg', 'm4a', 'mp3')
        
    Returns:
        JSON string with result info
    """
    try:
        import yt_dlp
        from yt_dlp.postprocessor.ffmpeg import FFmpegPostProcessor
        
        if not os.path.exists(input_path):
            return json.dumps({
                "success": False,
                "message": f"Input file not found: {input_path}",
                "output_path": ""
            })
        
        # Get base filename
        base_name = os.path.splitext(os.path.basename(input_path))[0]
        output_path = os.path.join(output_dir, f"{base_name}.{output_format}")
        
        print(f"🐍 converter: Converting {input_path} to {output_path}", file=sys.stderr)
        
        # Check if FFmpeg is available through yt-dlp
        ffmpeg_available = False
        ffmpeg_location = None
        
        # yt-dlp includes FFmpeg detection
        try:
            pp = FFmpegPostProcessor()
            ffmpeg_available = pp.available
            if hasattr(pp, '_ffmpeg_location'):
                ffmpeg_location = pp._ffmpeg_location
            print(f"🐍 converter: FFmpeg available: {ffmpeg_available}", file=sys.stderr)
        except Exception as e:
            print(f"🐍 converter: FFmpeg check failed: {e}", file=sys.stderr)
        
        # If FFmpeg is available, use it for conversion
        if ffmpeg_available:
            print(f"🐍 converter: Using FFmpeg for conversion", file=sys.stderr)
            
            # Build FFmpeg command
            cmd = ['ffmpeg', '-y', '-i', input_path]
            
            # Add thumbnail if available
            if thumbnail_path and os.path.exists(thumbnail_path):
                cmd.extend(['-i', thumbnail_path])
                cmd.extend(['-map', '0:a', '-map', '1'])
                cmd.extend(['-c:v', 'copy', '-disposition:v', 'attached_pic'])
            
            # Audio codec settings
            if output_format == 'ogg':
                cmd.extend(['-c:a', 'libopus', '-b:a', '128k'])
            elif output_format == 'm4a':
                cmd.extend(['-c:a', 'aac', '-b:a', '192k'])
            elif output_format == 'mp3':
                cmd.extend(['-c:a', 'libmp3lame', '-b:a', '192k'])
            else:
                cmd.extend(['-c:a', 'copy'])
            
            # Add metadata
            if title:
                cmd.extend(['-metadata', f'title={title}'])
            if artist:
                cmd.extend(['-metadata', f'artist={artist}'])
            if album:
                cmd.extend(['-metadata', f'album={album}'])
            
            cmd.append(output_path)
            
            print(f"🐍 converter: FFmpeg command: {' '.join(cmd)}", file=sys.stderr)
            
            try:
                result = subprocess.run(cmd, capture_output=True, timeout=120)
                if result.returncode == 0 and os.path.exists(output_path):
                    print(f"✅ converter: FFmpeg conversion successful", file=sys.stderr)
                    return json.dumps({
                        "success": True,
                        "message": "Converted and embedded with FFmpeg",
                        "output_path": output_path
                    })
                else:
                    print(f"⚠️ converter: FFmpeg failed: {result.stderr.decode()}", file=sys.stderr)
            except Exception as e:
                print(f"⚠️ converter: FFmpeg execution failed: {e}", file=sys.stderr)
        
        # Fallback: Just rename/copy file and try Mutagen for metadata
        print(f"🐍 converter: Falling back to Mutagen for metadata", file=sys.stderr)
        
        # For WebM, we can't easily convert without FFmpeg
        # But we can try to use Mutagen on the original file
        try:
            from metadata_embedder import embed_metadata
            
            result = embed_metadata(
                input_path,
                title=title,
                artist=artist,
                album=album,
                thumbnail_path=thumbnail_path
            )
            
            if result.get('success'):
                return json.dumps({
                    "success": True,
                    "message": "Metadata embedded with Mutagen (no conversion)",
                    "output_path": input_path
                })
            else:
                return json.dumps({
                    "success": False,
                    "message": f"Mutagen failed: {result.get('message')}",
                    "output_path": input_path
                })
                
        except Exception as e:
            print(f"⚠️ converter: Mutagen fallback failed: {e}", file=sys.stderr)
            return json.dumps({
                "success": False,
                "message": f"Both FFmpeg and Mutagen failed: {e}",
                "output_path": input_path
            })
        
    except Exception as e:
        print(f"❌ converter: Error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return json.dumps({
            "success": False,
            "message": f"Error: {str(e)}",
            "output_path": ""
        })


def get_ffmpeg_info():
    """
    Get information about FFmpeg availability.
    
    Returns:
        JSON string with FFmpeg info
    """
    try:
        import yt_dlp
        from yt_dlp.postprocessor.ffmpeg import FFmpegPostProcessor
        
        pp = FFmpegPostProcessor()
        
        info = {
            "available": pp.available,
            "location": getattr(pp, '_ffmpeg_location', None),
            "version": None
        }
        
        # Try to get version
        if pp.available:
            try:
                result = subprocess.run(['ffmpeg', '-version'], capture_output=True, timeout=5)
                if result.returncode == 0:
                    version_line = result.stdout.decode().split('\n')[0]
                    info["version"] = version_line
            except:
                pass
        
        return json.dumps(info)
        
    except Exception as e:
        return json.dumps({
            "available": False,
            "error": str(e)
        })
