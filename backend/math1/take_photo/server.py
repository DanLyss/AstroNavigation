from flask import Flask, request
import os

app = Flask(__name__)

# Папка для сохранения загруженных файлов
UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route('/')
def index():
    return '🚀 AstroPhoto server is running!'

@app.route('/upload', methods=['POST'])
def upload():
    try:
        image = request.files['image']
        metadata = request.files['metadata']

        image_path = os.path.join(UPLOAD_FOLDER, image.filename)
        metadata_path = os.path.join(UPLOAD_FOLDER, metadata.filename)

        image.save(image_path)
        metadata.save(metadata_path)

        print(f'✅ Получено: {image.filename}, {metadata.filename}')
        return 'OK', 200

    except Exception as e:
        print(f'❌ Ошибка при загрузке: {e}')
        return 'Ошибка сервера', 500

if __name__ == '__main__':
    # Для Render — берём порт из окружения
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)
