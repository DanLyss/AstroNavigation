# Uncertainties in Determining Location from Star Photos 🌌

## 1. Quality of the Photo 📷
The accuracy of direct measurements of stars' coordinates is crucial. If the initial measurements have high relative errors, subsequent calculations for determining the location are unreliable. We must ensure that the relative direct measurements error remains as low as possible.

## 2. Positioning the Picture on the Celestial Sphere 🌍✨
Since the photo captures an arbitrary portion of the sky, accurately identifying its position relative to the celestial sphere is critically important. Without proper calibration of any reference point in the image, determining the coordinates becomes impossible.

## 3. Identifying the Stars 🌟🔍
Precise marking of stars is essential. If the stars are identified with significant uncertainty (e.g., a large possible location circle compared to the image size) or mismatched, determining the correct location is nearly impossible.

## 4. Product Organization 📲💾
A key consideration is whether the solution will be an independent local app or a remote service-based system. A local app must efficiently manage memory and computational resources, while a remote service can leverage powerful servers but requires stable connectivity. 

## 5. Underlying mathematics ✍️📖
The most important part of the project is mathematical computations using some data accumulated from the photo. We need to ensure that underlying math methods we are using are actualy giving us right results, otherwise the whole project will not work correctly.

# Artifacts Description 📜

### 1. [Analysis of highest possible quality to be achieved via pixel approximation](https://github.com/DanLyss/AstroNavigation/blob/main/artifacts/Night%20Sky%20Photo%20Quality%20Assessment.md) 🔍
### 2. [Research on gyroscopes operating on embedded in the phone sensors as a proof of high-quality photo's center positioning](https://github.com/DanLyss/AstroNavigation/blob/main/artifacts/Gyroscope_Presicion_Research.pdf) 📱
### 3. [Astrometry - service of stars recognition with open source code](https://github.com/DanLyss/AstroNavigation/tree/main/artifacts/Astrometry%20guide) 🌠
### 4. [Calculations on necessary resources for the app and plan of possible app architecture](https://github.com/DanLyss/AstroNavigation/blob/main/artifacts/Estimation%20necessary%20resources%20for%20the%20app.md) ⚙️
### 5. [Formal mathematical proof that methods we are planning to use work properly](https://github.com/DanLyss/AstroNavigation/blob/main/artifacts/AstroManual/Astropart_manual.pdf) 🧠
