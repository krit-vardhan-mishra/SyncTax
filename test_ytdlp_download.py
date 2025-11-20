"""
Test script for yt-dlp audio download functionality
Downloads a short audio clip to verify the implementation works
Run this from project root: python test_ytdlp_download.py
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
    print("Testing yt-dlp Audio Download")
    print("=" * 60)
    
    # Test URL - a short Creative Commons licensed audio
    # Using a short YouTube video for testing
    test_url = "https://www.youtube.com/watch?v=jNQXAC9IVRw"  # "Me at the zoo" - first YouTube video (short)
    
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
        print(f"  File Path: {result.get('file_path')}")
        
        # Check if file exists
        file_path = result.get('file_path')
        if os.path.exists(file_path):
            file_size = os.path.getsize(file_path)
            print(f"  File Size: {file_size / 1024:.2f} KB")
            print(f"\n✓ File successfully downloaded!")
        else:
            print(f"\n✗ Warning: File not found at expected path")
    else:
        print(f"\n✗ Download failed")
    
    print("=" * 60)

if __name__ == "__main__":
    main()
