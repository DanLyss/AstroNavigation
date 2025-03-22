from flask import Flask, request, send_from_directory, jsonify
import os
from datetime import datetime

app = Flask(__name__)

UPLOAD_FOLDER = "uploads"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route("/")
def home():
    return "📡 Astro server is live!"

@app.route("/upload", methods=["POST"])
def upload():
    image = request.files.get("image")
    metadata = request.files.get("metadata")

    if not image or not metadata:
        return "❌ Missing files", 400

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    image_filename = f"{timestamp}_photo.jpg"
    metadata_filename = f"{timestamp}_meta.json"

    image.save(os.path.join(UPLOAD_FOLDER, image_filename))
    metadata.save(os.path.join(UPLOAD_FOLDER, metadata_filename))

    print(f"✅ Saved: {image_filename}, {metadata_filename}")
    return "Upload OK", 200

@app.route("/files/<filename>")
def get_file(filename):
    return send_from_directory(UPLOAD_FOLDER, filename)

@app.route("/list")
def list_files():
    files = sorted(os.listdir(UPLOAD_FOLDER), reverse=True)
    return jsonify(files)

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
