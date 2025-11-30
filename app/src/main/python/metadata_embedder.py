"""
Metadata embedder module for embedding metadata into audio files using Mutagen.
Supports WebM (Opus/Vorbis), OGG, M4A, and other formats.
"""
import os
import sys
import base64

def embed_metadata(file_path, title=None, artist=None, album=None, thumbnail_path=None):
    """
    Embed metadata and optional cover art into an audio file.
    
    Args:
        file_path: Path to the audio file (WebM, OGG, M4A, etc.)
        title: Song title
        artist: Artist name
        album: Album name
        thumbnail_path: Path to thumbnail/cover image (optional)
        
    Returns:
        dict with 'success' (bool) and 'message' (str)
    """
    try:
        import mutagen
        
        if not os.path.exists(file_path):
            return {"success": False, "message": f"File not found: {file_path}"}
        
        print(f"🐍 metadata_embedder: Loading file: {file_path}", file=sys.stderr)
        
        # Determine file type by extension
        ext = os.path.splitext(file_path)[1].lower()
        
        # Handle M4A/MP4 files
        if ext in ['.m4a', '.mp4', '.aac']:
            return embed_m4a_metadata(file_path, title, artist, album, thumbnail_path)
        
        # Handle OGG/WebM files
        if ext in ['.ogg', '.opus']:
            return embed_ogg_metadata(file_path, title, artist, album, thumbnail_path)
        
        # Handle WebM (try as OGG, but likely won't work)
        if ext == '.webm':
            print(f"⚠️ metadata_embedder: WebM format has limited metadata support", file=sys.stderr)
            return embed_ogg_metadata(file_path, title, artist, album, thumbnail_path)
        
        # Handle MP3 files
        if ext == '.mp3':
            return embed_mp3_metadata(file_path, title, artist, album, thumbnail_path)
        
        # Try generic approach
        return embed_generic_metadata(file_path, title, artist, album, thumbnail_path)
        
    except ImportError as e:
        return {"success": False, "message": f"Mutagen not available: {e}"}
    except Exception as e:
        print(f"❌ metadata_embedder: Error: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return {"success": False, "message": f"Error: {str(e)}"}


def embed_m4a_metadata(file_path, title, artist, album, thumbnail_path):
    """Embed metadata into M4A/MP4 files."""
    try:
        from mutagen.mp4 import MP4, MP4Cover
        
        print(f"🐍 metadata_embedder: Loading M4A file...", file=sys.stderr)
        audio = MP4(file_path)
        
        tags_set = []
        
        if title:
            audio['\xa9nam'] = [title]
            tags_set.append(f"TITLE={title}")
            
        if artist:
            audio['\xa9ART'] = [artist]
            tags_set.append(f"ARTIST={artist}")
            
        if album:
            audio['\xa9alb'] = [album]
            tags_set.append(f"ALBUM={album}")
        
        print(f"🐍 metadata_embedder: M4A tags set: {', '.join(tags_set)}", file=sys.stderr)
        
        # Embed cover art
        cover_embedded = False
        if thumbnail_path and os.path.exists(thumbnail_path):
            try:
                print(f"🐍 metadata_embedder: Embedding cover art from: {thumbnail_path}", file=sys.stderr)
                
                with open(thumbnail_path, 'rb') as f:
                    cover_data = f.read()
                
                # Determine format
                if thumbnail_path.lower().endswith('.png'):
                    cover_format = MP4Cover.FORMAT_PNG
                else:
                    cover_format = MP4Cover.FORMAT_JPEG
                
                audio['covr'] = [MP4Cover(cover_data, imageformat=cover_format)]
                cover_embedded = True
                print(f"🐍 metadata_embedder: Cover art embedded ({len(cover_data)} bytes)", file=sys.stderr)
                
            except Exception as e:
                print(f"⚠️ metadata_embedder: Cover art failed: {e}", file=sys.stderr)
        
        audio.save()
        
        message = "M4A metadata embedded successfully"
        if cover_embedded:
            message += " with cover art"
        
        print(f"✅ metadata_embedder: {message}", file=sys.stderr)
        return {"success": True, "message": message}
        
    except Exception as e:
        print(f"❌ metadata_embedder: M4A embedding failed: {e}", file=sys.stderr)
        return {"success": False, "message": f"M4A error: {str(e)}"}


def embed_mp3_metadata(file_path, title, artist, album, thumbnail_path):
    """Embed metadata into MP3 files using ID3 tags."""
    try:
        from mutagen.id3 import ID3, TIT2, TPE1, TALB, APIC
        from mutagen.mp3 import MP3
        
        print(f"🐍 metadata_embedder: Loading MP3 file...", file=sys.stderr)
        
        # Try to load existing ID3 tags or create new
        try:
            audio = ID3(file_path)
        except:
            audio = ID3()
        
        tags_set = []
        
        if title:
            audio['TIT2'] = TIT2(encoding=3, text=title)
            tags_set.append(f"TITLE={title}")
            
        if artist:
            audio['TPE1'] = TPE1(encoding=3, text=artist)
            tags_set.append(f"ARTIST={artist}")
            
        if album:
            audio['TALB'] = TALB(encoding=3, text=album)
            tags_set.append(f"ALBUM={album}")
        
        print(f"🐍 metadata_embedder: MP3 tags set: {', '.join(tags_set)}", file=sys.stderr)
        
        # Embed cover art
        cover_embedded = False
        if thumbnail_path and os.path.exists(thumbnail_path):
            try:
                print(f"🐍 metadata_embedder: Embedding cover art from: {thumbnail_path}", file=sys.stderr)
                
                with open(thumbnail_path, 'rb') as f:
                    cover_data = f.read()
                
                # Determine MIME type
                mime_type = "image/jpeg"
                if thumbnail_path.lower().endswith('.png'):
                    mime_type = "image/png"
                
                audio['APIC'] = APIC(
                    encoding=3,
                    mime=mime_type,
                    type=3,  # Cover (front)
                    desc='Cover',
                    data=cover_data
                )
                cover_embedded = True
                print(f"🐍 metadata_embedder: Cover art embedded ({len(cover_data)} bytes)", file=sys.stderr)
                
            except Exception as e:
                print(f"⚠️ metadata_embedder: Cover art failed: {e}", file=sys.stderr)
        
        audio.save(file_path)
        
        message = "MP3 metadata embedded successfully"
        if cover_embedded:
            message += " with cover art"
        
        print(f"✅ metadata_embedder: {message}", file=sys.stderr)
        return {"success": True, "message": message}
        
    except Exception as e:
        print(f"❌ metadata_embedder: MP3 embedding failed: {e}", file=sys.stderr)
        return {"success": False, "message": f"MP3 error: {str(e)}"}


def embed_ogg_metadata(file_path, title, artist, album, thumbnail_path):
    """Embed metadata into OGG/Opus files using Vorbis comments."""
    try:
        from mutagen.oggopus import OggOpus
        from mutagen.oggvorbis import OggVorbis
        from mutagen.flac import Picture
        import mutagen
        
        print(f"🐍 metadata_embedder: Loading OGG file...", file=sys.stderr)
        
        # Try to load the audio file
        audio = None
        file_type = None
        
        # First try automatic detection
        try:
            audio = mutagen.File(file_path)
            if audio is not None:
                file_type = type(audio).__name__
        except:
            pass
        
        # If auto-detection failed, try specific loaders
        if audio is None:
            try:
                audio = OggOpus(file_path)
                file_type = "OggOpus"
            except:
                pass
        
        if audio is None:
            try:
                audio = OggVorbis(file_path)
                file_type = "OggVorbis"
            except:
                pass
        
        if audio is None:
            return {"success": False, "message": "Could not load OGG file"}
        
        print(f"🐍 metadata_embedder: Loaded as {file_type}", file=sys.stderr)
        
        tags_set = []
        
        if title:
            audio["TITLE"] = [title]
            tags_set.append(f"TITLE={title}")
            
        if artist:
            audio["ARTIST"] = [artist]
            tags_set.append(f"ARTIST={artist}")
            
        if album:
            audio["ALBUM"] = [album]
            tags_set.append(f"ALBUM={album}")
        
        print(f"🐍 metadata_embedder: OGG tags set: {', '.join(tags_set)}", file=sys.stderr)
        
        # Embed cover art
        cover_embedded = False
        if thumbnail_path and os.path.exists(thumbnail_path):
            try:
                print(f"🐍 metadata_embedder: Embedding cover art...", file=sys.stderr)
                
                with open(thumbnail_path, 'rb') as f:
                    cover_data = f.read()
                
                # Determine MIME type
                mime_type = "image/jpeg"
                if thumbnail_path.lower().endswith('.png'):
                    mime_type = "image/png"
                
                # Create Picture object
                picture = Picture()
                picture.type = 3  # Front cover
                picture.mime = mime_type
                picture.desc = "Cover"
                picture.data = cover_data
                
                # Encode as base64 for METADATA_BLOCK_PICTURE
                picture_data = picture.write()
                encoded_data = base64.b64encode(picture_data).decode('ascii')
                
                audio["METADATA_BLOCK_PICTURE"] = [encoded_data]
                cover_embedded = True
                print(f"🐍 metadata_embedder: Cover art embedded ({len(cover_data)} bytes)", file=sys.stderr)
                
            except Exception as e:
                print(f"⚠️ metadata_embedder: Cover art failed: {e}", file=sys.stderr)
        
        audio.save()
        
        message = f"{file_type} metadata embedded successfully"
        if cover_embedded:
            message += " with cover art"
        
        print(f"✅ metadata_embedder: {message}", file=sys.stderr)
        return {"success": True, "message": message}
        
    except Exception as e:
        print(f"❌ metadata_embedder: OGG embedding failed: {e}", file=sys.stderr)
        return {"success": False, "message": f"OGG error: {str(e)}"}


def embed_generic_metadata(file_path, title, artist, album, thumbnail_path):
    """Try to embed metadata using automatic format detection."""
    try:
        import mutagen
        
        print(f"🐍 metadata_embedder: Trying generic approach...", file=sys.stderr)
        
        audio = mutagen.File(file_path)
        if audio is None:
            return {"success": False, "message": "Could not load audio file"}
        
        file_type = type(audio).__name__
        print(f"🐍 metadata_embedder: Detected as {file_type}", file=sys.stderr)
        
        # Try setting tags based on what the format supports
        try:
            if title:
                audio["TITLE"] = [title]
            if artist:
                audio["ARTIST"] = [artist]
            if album:
                audio["ALBUM"] = [album]
            
            audio.save()
            return {"success": True, "message": f"Generic metadata embedded ({file_type})"}
        except:
            pass
        
        return {"success": False, "message": f"Format {file_type} not supported for metadata"}
        
    except Exception as e:
        print(f"❌ metadata_embedder: Generic embedding failed: {e}", file=sys.stderr)
        return {"success": False, "message": f"Error: {str(e)}"}


def check_metadata(file_path):
    """
    Check what metadata is embedded in an audio file.
    
    Args:
        file_path: Path to the audio file
        
    Returns:
        dict with metadata info
    """
    try:
        import mutagen
        
        if not os.path.exists(file_path):
            return {"success": False, "message": f"File not found: {file_path}"}
        
        audio = mutagen.File(file_path)
        if audio is None:
            return {"success": False, "message": "Could not load audio file"}
        
        result = {
            "success": True,
            "file_type": type(audio).__name__,
            "tags": {}
        }
        
        for key in audio.keys():
            if key == "METADATA_BLOCK_PICTURE" or key == "covr" or key == "APIC":
                result["tags"][str(key)] = "[Cover art present]"
            else:
                result["tags"][str(key)] = str(audio[key])
        
        return result
        
    except Exception as e:
        return {"success": False, "message": f"Error: {str(e)}"}
