"""
Test downloading "Baby" by Justin Bieber with embedded album art
"""
import os
import sys
import json

# Add the python module directory to path
python_module_dir = os.path.join("app", "src", "main", "python")
sys.path.insert(0, python_module_dir)

# Import the audio downloader
import audio_downloader

def main():
    print("=" * 60)
    print("Testing yt-dlp: Baby by Justin Bieber")
    print("=" * 60)
    
    # Search for the song
    test_url = "https://www.youtube.com/watch?v=kffacxfA7G4"  # Baby - Justin Bieber
    
    # Output directory
    output_dir = os.path.join("assets", "songs")
    
    print(f"\nTest URL: {test_url}")
    print(f"Output directory: {output_dir}")
    print("\nStarting download...\n")
    
    # Download the audio
    result_json = audio_downloader.download_audio(test_url, output_dir)
    result = json.loads(result_json)
    
    print("\n" + "=" * 60)
    print("Download Result:")
    print("=" * 60)
    print(f"Success: {result.get('success')}")
    print(f"Message: {result.get('message')}")
    
    if result.get('success'):
        print(f"\nFile Details:")
        print(f"  Title: {result.get('title')}")
        print(f"  Artist: {result.get('artist')}")
        print(f"  Duration: {result.get('duration')} seconds")
        print(f"  Format: {result.get('format')}")
        print(f"  File Path: {result.get('file_path')}")
        
        # Check if file exists
        file_path = result.get('file_path')
        if os.path.exists(file_path):
            file_size = os.path.getsize(file_path)
            print(f"  File Size: {file_size / (1024*1024):.2f} MB")
            
            # Check for separate thumbnail files
            base_path = os.path.splitext(file_path)[0]
            thumbnail_extensions = ['.webp', '.jpg', '.png']
            separate_thumbnails = []
            for ext in thumbnail_extensions:
                thumb_path = base_path + ext
                if os.path.exists(thumb_path):
                    separate_thumbnails.append(thumb_path)
            
            if separate_thumbnails:
                print(f"\n⚠ Warning: Separate thumbnail files found:")
                for thumb in separate_thumbnails:
                    print(f"    - {thumb}")
            else:
                print(f"\n✓ No separate thumbnail files - album art is embedded!")
            
            print(f"\n✓ File successfully downloaded!")
        else:
            print(f"\n✗ Warning: File not found at expected path")
    else:
        print(f"\n✗ Download failed")
    
    print("=" * 60)

if __name__ == "__main__":
    main()
