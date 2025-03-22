from flask import Flask, request
import os

app = Flask(__name__)
UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route('/')
def index():
    return 'Server is running!'

@app.route('/upload', methods=['POST'])
def upload():
    image = request.files['image']
    meta = request.files['metadata']

    image.save(os.path.join(UPLOAD_FOLDER, image.filename))
    meta.save(os.path.join(UPLOAD_FOLDER, meta.filename))

    print(f'✅ Got: {image.filename}, {meta.filename}')
    return 'OK'

if __name__ == '__main__':
    app.run()
