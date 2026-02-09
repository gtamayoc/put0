import os
from PIL import Image

RES_DIR = r'c:\discolocal\UFPSO\SEMESTRE 13\PUT0\app\src\main\res'
EXTENSIONS = {'.png', '.jpg', '.jpeg'}

def convert_images():
    count = 0
    saved_space = 0
    
    print(f"Scanning directory: {RES_DIR}")
    
    for root, dirs, files in os.walk(RES_DIR):
        for file in files:
            # Skip 9-patch files
            if file.endswith('.9.png'):
                continue
            
            ext = os.path.splitext(file)[1].lower()
            if ext in EXTENSIONS:
                file_path = os.path.join(root, file)
                original_size = os.path.getsize(file_path)
                
                try:
                    img = Image.open(file_path)
                    webp_path = os.path.splitext(file_path)[0] + '.webp'
                    
                    # Handle transparency for PNG
                    if ext == '.png':
                        img = img.convert("RGBA")
                    
                    # Convert
                    img.save(webp_path, 'WEBP', quality=80)
                    new_size = os.path.getsize(webp_path)
                    
                    # Basic check: only replace if smaller or significantly better format
                    # For build size, smaller is key.
                    if new_size < original_size:
                        os.remove(file_path)
                        saved_space += (original_size - new_size)
                        count += 1
                        print(f"Converted: {file} ({original_size/1024:.1f}KB -> {new_size/1024:.1f}KB)")
                    else:
                        # If WebP is bigger (can happen for tiny simple icons), keep original
                        # But typically consistent format is better.
                        # For optimizing SIZE, keep simpler.
                        os.remove(webp_path)
                        print(f"Skipped (WebP larger): {file} ({original_size} vs {new_size})")
                        
                except Exception as e:
                    print(f"Error converting {file}: {e}")

    print(f"Total optimized: {count} images")
    print(f"Space saved: {saved_space / 1024 / 1024:.2f} MB")

if __name__ == '__main__':
    convert_images()
