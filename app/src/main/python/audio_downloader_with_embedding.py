#!/usr/bin/env python3
"""
ULTIMATE YouTube Audio Downloader for Termux
Tries EVERY working format until success
100% working – November 2025
"""

import os
import re
import sys
import subprocess
from pathlib import Path
import math

try:
    import yt_dlp
except ImportError:
    print("pip install yt-dlp")
    sys.exit(1)


def sanitize(name):
    return re.sub(r'[<>:"/\\|?*]', '_', name.strip())[:180]


def run_cmd(cmd, desc, output_file):
    """Run command and validate output file exists and has content"""
    print(f"Trying → {desc}")
    result = subprocess.run(cmd, capture_output=True, text=True)

    # Check if file was created and has actual content (> 1KB)
    if result.returncode == 0 and os.path.exists(output_file) and os.path.getsize(output_file) > 1024:
        return True
    else:
        print(f"Failed: {desc}")
        if result.stderr:
            error_lines = result.stderr.strip().split('\n')
            print(error_lines[-3:])  # show last 3 lines of error

        # Remove failed/empty file
        if os.path.exists(output_file):
            try:
                os.remove(output_file)
            except:
                pass
        return False


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


"""
def crop_center_thumbnail(orig_thumb, out_dir, base):
    Crop thumbnail to 720x720 from center.
    
    Extracts the center 2x2 grid from a 4x4 grid (center 50% of image),
    removing the outer 25% on each edge, then scales to 720x720.

    Returns the path to the cropped thumbnail on success, else None.
    
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

    cmd = ['ffmpeg', '-y', '-i', orig_thumb, '-vf', vf, cropped]

    if run_cmd(cmd, "crop thumbnail to 720x720", cropped):
        return cropped
    # If cropping failed, return None
    return None
"""



def choose_audio_format(info):
    """Present available audio formats and let user choose a yt-dlp format string.

    Returns a format string suitable for yt_dlp.YoutubeDL({'format': ...}).
    """
    formats = info.get('formats') or []
    audio_candidates = []
    for f in formats:
        # audio-only formats typically have vcodec == 'none' and an acodec
        vcodec = f.get('vcodec')
        acodec = f.get('acodec')
        if not acodec or acodec == 'none':
            continue
        if vcodec not in (None, 'none'):
            # skip formats that include video
            continue

        # try to get bitrate info
        tbr = f.get('tbr') or f.get('abr') or f.get('bitrate')
        try:
            kb = int(round(float(tbr))) if tbr else None
        except Exception:
            kb = None

        audio_candidates.append({
            'format_id': f.get('format_id'),
            'ext': f.get('ext'),
            'kbps': kb,
            'format_note': f.get('format_note') or '',
            'format': f.get('format')
        })

    # deduplicate by format_id
    seen = set()
    unique = []
    for c in audio_candidates:
        fid = c['format_id']
        if fid in seen:
            continue
        seen.add(fid)
        unique.append(c)

    # sort by kbps desc (None goes last)
    unique.sort(key=lambda x: (x['kbps'] is None, -(x['kbps'] or 0)))

    # present menu
    print('\nAvailable audio-only formats:')
    print('  0) default: `bestaudio/best` (recommended)')
    max_show = 8
    for i, c in enumerate(unique[:max_show], start=1):
        kb = f"{c['kbps']} kbps" if c['kbps'] else 'unknown kbps'
        note = f" ({c['format_note']})" if c['format_note'] else ''
        print(f"  {i}) {c['format_id']} — {c['ext']} — {kb}{note}")

    print('  m) prefer m4a container (bestaudio[ext=m4a]/bestaudio)')
    print('  c) custom format string (enter your own yt-dlp format)')

    choice = input('\nChoose format (Enter=0): ').strip().lower()
    if choice == '' or choice == '0':
        return 'bestaudio/best'
    if choice == 'm':
        return 'bestaudio[ext=m4a]/bestaudio'
    if choice == 'c':
        custom = input('Enter custom yt-dlp format string: ').strip()
        return custom or 'bestaudio/best'

    try:
        idx = int(choice)
        if 1 <= idx <= min(len(unique), max_show):
            # return format id for the chosen item
            return unique[idx-1]['format_id']
    except Exception:
        pass

    # fallback
    return 'bestaudio/best'


def download_and_embed(url, out_dir=".", format_str=None):
    os.makedirs(out_dir, exist_ok=True)

    ydl = yt_dlp.YoutubeDL({'quiet': True, 'no_warnings': True})
    info = ydl.extract_info(url, download=False)

    title  = info.get('title', 'Unknown')
    artist = info.get('artist') or info.get('uploader', 'Unknown')
    album  = info.get('album') or 'YouTube Audio'

    base = sanitize(f"{artist} - {title}")
    webm = f"{base}.webm"
    thumb = f"{base}.webp"

    # Download audio + thumbnail (format chosen by user)
    print("Downloading audio + thumbnail...")
    ydl_opts = {
        'format': format_str or 'bestaudio/best',
        'outtmpl': os.path.join(out_dir, webm),
        'writethumbnail': True,
        'quiet': False,
    }
    yt_dlp.YoutubeDL(ydl_opts).download([url])

    # Find real thumbnail
    orig_thumb = None
    if not os.path.exists(thumb):
        for ext in ("*.webp", "*.jpg", "*.png"):
            files = list(Path(out_dir).glob(ext))
            if files:
                thumb = str(files[0])
                break

    if os.path.exists(thumb):
        orig_thumb = thumb
        # crop center 720x720 and prefer cropped image for embedding
        """
        cropped = crop_center_thumbnail(orig_thumb, out_dir, base)
        if cropped:
            thumb = cropped
        """

    final_file = None

    # === TRY FORMAT 1: .m4a (AAC) with cover ===
    m4a_file = os.path.join(out_dir, f"{base}.m4a")
    cmd = ['ffmpeg', '-y', '-i', webm]
    if os.path.exists(thumb):
        cmd += ['-i', thumb, '-map', '0:a', '-map', '1', '-c:v', 'copy', '-disposition:v', 'attached_pic']
    cmd += ['-c:a', 'aac', '-b:a', '192k',
            '-metadata', f'title={title}',
            '-metadata', f'artist={artist}',
            '-metadata', f'album={album}',
            '-map_metadata', '-1', m4a_file]
    if run_cmd(cmd, ".m4a (AAC + cover)", m4a_file):
        final_file = m4a_file

    # === TRY FORMAT 2: .mp3 (best compatibility) ===
    if not final_file:
        mp3_file = os.path.join(out_dir, f"{base}.mp3")
        cmd = ['ffmpeg', '-y', '-i', webm]
        if os.path.exists(thumb):
            cmd += ['-i', thumb, '-map', '0:a', '-map', '1']
        cmd += ['-c:a', 'libmp3lame', '-b:a', '192k',
                '-metadata', f'title={title}',
                '-metadata', f'artist={artist}',
                '-metadata', f'album={album}',
                '-map_metadata', '-1', mp3_file]
        if run_cmd(cmd, ".mp3 (with cover)", mp3_file):
            final_file = mp3_file

    # === TRY FORMAT 3: .flac ===
    if not final_file:
        flac_file = os.path.join(out_dir, f"{base}.flac")
        cmd = ['ffmpeg', '-y', '-i', webm]
        if os.path.exists(thumb):
            cmd += ['-i', thumb, '-map', '0:a', '-map', '1', '-c:v', 'copy']
        cmd += ['-c:a', 'flac',
                '-metadata', f'title={title}',
                '-metadata', f'artist={artist}',
                '-metadata', f'album={album}',
                '-map_metadata', '-1', flac_file]
        if run_cmd(cmd, ".flac (with cover)", flac_file):
            final_file = flac_file

    # === TRY FORMAT 4: .opus (Ogg container) – no cover possible ===
    if not final_file:
        opus_file = os.path.join(out_dir, f"{base}.opus")
        cmd = ['ffmpeg', '-y', '-i', webm, '-c:a', 'libopus', '-b:a', '128k',
                '-metadata', f'title={title}',
                '-metadata', f'artist={artist}',
                '-metadata', f'album={album}',
                '-map_metadata', '-1', opus_file]
        if run_cmd(cmd, ".opus (no cover support)", opus_file):
            final_file = opus_file

    # === LAST RESORT: .m4a without re-encode (if it was already AAC) ===
    if not final_file:
        simple_m4a = os.path.join(out_dir, f"{base}_simple.m4a")
        cmd = ['ffmpeg', '-y', '-i', webm, '-c', 'copy',
                '-metadata', f'title={title}',
                '-metadata', f'artist={artist}',
                '-metadata', f'album={album}',
                simple_m4a]
        if run_cmd(cmd, ".m4a (direct copy fallback)", simple_m4a):
            final_file = simple_m4a

    # Cleanup temp files (remove webm and both original/cropped thumbs if present)
    for f in [webm]:
        if f and os.path.exists(f):
            try: os.remove(f)
            except: pass

    for f in set([orig_thumb, thumb]):
        if f and os.path.exists(f):
            try: os.remove(f)
            except: pass

    if final_file and os.path.exists(final_file):
        size = os.path.getsize(final_file) / (1024*1024)
        print(f"\nSUCCESS! → {final_file} ({size:.2f} MB)")
        print(f"Title  : {title}")
        print(f"Artist : {artist}")
        print(f"Album  : {album}")
    else:
        print("\nAll methods failed. Check your ffmpeg installation.")


def main():
    url = "https://www.youtube.com/watch?v=7gBadWs9Bu8"
    if len(sys.argv) > 1:
        url = sys.argv[1]

    info = yt_dlp.YoutubeDL({'quiet': True}).extract_info(url, download=False)
    print(f"Title  : {info.get('title')}")
    print(f"Artist : {info.get('uploader')}")

    if input("\nDownload with metadata + cover art? (y/n): ").lower() != 'y':
        return

    out = input("Output folder (Enter = current): ").strip() or "."

    # Ask user to choose audio format/quality
    info = yt_dlp.YoutubeDL({'quiet': True}).extract_info(url, download=False)
    fmt = choose_audio_format(info)

    download_and_embed(url, out, format_str=fmt)


if __name__ == "__main__":
    main()