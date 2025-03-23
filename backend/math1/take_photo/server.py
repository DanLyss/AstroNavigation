from flask import Flask, request, send_from_directory, jsonify
import os
from datetime import datetime
import requests

app = Flask(__name__)

UPLOAD_FOLDER = "uploads"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# ✅ Читаем из переменных окружения
TELEGRAM_BOT_TOKEN = os.environ.get("TELEGRAM_BOT_TOKEN")
TELEGRAM_CHAT_ID = os.environ.get("TELEGRAM_CHAT_ID")

# Сохраняем, что уже отправили
import hashlib
SENT_HASHES = set()

def get_file_hash(path):
    with open(path, "rb") as f:
        return hashlib.sha1(f.read()).hexdigest()

def send_to_telegram(image_path, json_path):
    meta_hash = get_file_hash(json_path)

    if meta_hash in SENT_HASHES:
        print("📵 Already sent hash:", meta_hash)
        return

    url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/sendDocument"
    with open(image_path, "rb") as photo:
        r = requests.post(url, data={"chat_id": TELEGRAM_CHAT_ID}, files={"document": photo})
        print("PNG (doc):", r.status_code, r.text)

    url2 = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/sendDocument"
    with open(json_path, "rb") as meta:
        r2 = requests.post(url2, data={"chat_id": TELEGRAM_CHAT_ID}, files={"document": meta})
        print("JSON :", r2.status_code, r2.text)

    SENT_HASHES.add(meta_hash)


@app.route("/")
def home():
    return "🛰 Astro server is alive!"

@app.route("/upload", methods=["POST"])
def upload():
    image = request.files.get("image")
    metadata = request.files.get("metadata")

    if not image or not metadata:
        return "❌ Missing image or metadata", 400

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    image_filename = f"{timestamp}_photo.jpg"
    metadata_filename = f"{timestamp}_meta.json"

    image_path = os.path.join(UPLOAD_FOLDER, image_filename)
    meta_path = os.path.join(UPLOAD_FOLDER, metadata_filename)

    image.save(image_path)
    metadata.save(meta_path)

    print(f"✅ Saved: {image_filename}, {metadata_filename}")
    send_to_telegram(image_path, meta_path)

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
