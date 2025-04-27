import json

def format_ra_dec(ra_dec):
    if isinstance(ra_dec[0], (list, tuple)):
        # Тройка -> вызываем raToDeg / decToDeg
        ra_str = f"raToDeg({ra_dec[0][0]}, {ra_dec[0][1]}, {ra_dec[0][2]})"
        dec_str = f"decToDeg({ra_dec[1][0]}, {ra_dec[1][1]}, {ra_dec[1][2]})"
        return f"({ra_str} to {dec_str})"
    else:
        # Уже в градусах (если одно значение)
        return f"({float(ra_dec[0])} to {float(ra_dec[1])})"

def format_pixel_coords(coords):
    return ', '.join([f"{float(x)} to {float(y)}" for x, y in coords])

def format_azalt(azalt):
    return ', '.join([f"({float(az)} to {float(alt)})" for az, alt in azalt])

def convert_case(case):
    return f"""
        TestCase(
            pixelCoords = listOf({format_pixel_coords(case["pixel_coords"])}),
            raDec = listOf({', '.join(format_ra_dec(rd) for rd in case["RA_Dec"])}),
            azAltDeg = listOf({format_azalt(case["AzAlt_deg"])}),
            positionalAngleDeg = {float(case["positional_angle_deg"])},
            rotationAngleDeg = {float(case["rotation_angle_deg"])},
            datetime = "{case["datetime"]}",
            longitude = {float(case["longitude"])},
            latitude = {float(case["latitude"])}
        )
    """

def convert_all(cases):
    header = """package kotlintranslation.test

import kotlin.math.toRadians
import kotlin.math.toDegrees
import kotlin.math.abs

fun raToDeg(h: Int, m: Int, s: Int): Double {
    return (h * 3600 + m * 60 + s) / 86400.0 * 360.0
}

fun decToDeg(d: Int, m: Int, s: Int): Double {
    val sign = if (d < 0) -1 else 1
    return sign * (abs(d.toDouble()) + m / 60.0 + s / 3600.0)
}

data class TestCase(
    val pixelCoords: List<Pair<Double, Double>>,
    val raDec: List<Pair<Double, Double>>,
    val azAltDeg: List<Pair<Double, Double>>,
    val positionalAngleDeg: Double,
    val rotationAngleDeg: Double,
    val datetime: String,
    val longitude: Double,
    val latitude: Double
)

object TestCases {
    val testCases = listOf(
"""
    footer = """
    )
}
"""
    body = ','.join([convert_case(case) for case in cases])
    return header + body + footer

# ТВОИ TEST_CASES
from AstroNavigation.backend.math.test_cases_Stellarium import TEST_CASES

# Конвертация
kotlin_code = convert_all(TEST_CASES)

# Сохранение в файл
with open("src/test/kotlin/TestCases.kt", "w", encoding="utf-8") as f:
    f.write(kotlin_code)

print("✅ TestCases.kt успешно сгенерирован!")
