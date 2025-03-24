import cv2
import numpy as np

# Глобальные переменные
dots = []  # Список для хранения точек
image = None  # Изображение
original_image = None  # Исходное изображение


# Функция обработки кликов
def click_event(event, x, y, flags, param):
    global dots, image

    if event == cv2.EVENT_LBUTTONDOWN:
        if not dots:
            base_x, base_y = x, y  # Первая точка становится началом координат

            # Рисуем координатные оси
            height, width = image.shape[:2]
            cv2.line(image, (0, base_y), (width, base_y), (255, 0, 0), 1)  # Ось X (вправо)
            cv2.line(image, (base_x, 0), (base_x, height), (0, 255, 255), 1)  # Ось Y (вверх)

        dots.append((x, y))

        # Рисуем точку и её номер
        cv2.circle(image, (x, y), 5, (0, 0, 255), -1)
        cv2.putText(image, str(len(dots)), (x + 5, y - 5),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
        cv2.imshow("Image", image)


# Основная функция
def main(image_path):
    global image, original_image

    image = cv2.imread(image_path)
    original_image = image.copy()

    if image is None:
        print(f"Не удалось загрузить изображение по пути: {image_path}")
        return []

    cv2.imshow("Image", image)
    cv2.setMouseCallback("Image", click_event)

    print("🖱️ Кликни по точкам. Нажми 's' чтобы сохранить и выйти.")
    while True:
        key = cv2.waitKey(1) & 0xFF
        if key == ord('s'):
            break

    if not dots:
        print("⚠️ Точки не выбраны.")
        return []

    # Вычисление относительных координат (ось Y направлена вверх!)
    base_x, base_y = dots[0]
    relative_dots = [(x - base_x, base_y - y) for x, y in dots]

    print("📌 Относительные координаты точек (X вправо, Y вверх):")
    for i, (x, y) in enumerate(relative_dots):
        print(f"Точка {i + 1}: ({x}, {y})")

    # Сохраняем изображение с метками
    output_path = image_path
    cv2.imwrite(output_path, image)
    print(f"💾 Изображение с метками сохранено: {output_path}")
    cv2.destroyAllWindows()
    return relative_dots


if __name__ == "__main__":
    image_path = r"C:\\Users\\Dan\\Downloads\\im3_1.png"  # Укажи путь к изображению
    coords = main(image_path)
    print("✅ Список координат сохранён:", coords)
