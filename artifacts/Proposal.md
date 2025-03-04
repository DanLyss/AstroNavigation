## Idea
Develop an app that determines coordinates just from a photo of a night sky without use of GPS or internet connection.

## Problem
In case of being lost away from civilization it is crucial to determine your position on the Earth; however, celestial navigation is a pretty hard skill to master and nearly impossible to use without proper equipment.
(yet GPS is pretty much everywhere, somewhere it's not working on purpose, or some people deliberately don't want to use it)

## Product Results
- **Determination of coordinates**: Locating a place where the photo was made from.
- **Reliability**: No need for internet connection.
- **UI**: Friendly UI, no need for any knowledge of the topic in order to use the app.
- **Clock Calibration**: If the Moon is present on the photo, the precise time may also be determined.
- **Navigation help**: Azimuthal directions will also be determined, allowing for easier orientation.

## Learning Value
- **Programming Language**: Python for backend, Kotlin for frontend.
- **Database**: PostgreSQL.
- **ML**: Image recognition model to determine the position of stars in the photo.
- **Research**: Additional research regarding underlying math and astronomy principles should be conducted.

## Potential Problems
- Unclear how to precisely determine the position of the horizon.
- Will the image recognition be good enough to give reliable results?
- Lack of experience developing apps.
- Are there any existing solutions?
