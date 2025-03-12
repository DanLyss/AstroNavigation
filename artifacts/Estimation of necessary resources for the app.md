
# Estimation of necessary resources for the app 📱

## Overview 🛠️

The **Astrometry** library in it's current state is unable to be used on Android devices, so the approach that enables us to run this Unix-based library on Phones while maintaining high performance should be considered. The library is quite "heavy", so we should also adress possible storage issues.

## Android NDK 
The Android NDK is a toolset that lets you implement parts of your app in native code, so by rebuilding the entire **Astrometry** library we will be able to successfully use it on Android devices.

## Storage Considerations 💾

One major downside of using **Astrometry** is that its model fit data files (`.fits`) take up more than **400MB** of storage. However, since we only need the **brightest stars** (which are visible in smartphone photos), we can **significantly reduce the file size** by about **90%**. This optimization will make the application much more lightweight and overall we expect  **Astrometry** to take up to **50MB** of storage.

## Conclusion 🎯

By leveraging **Android NDK**, we can run **Astrometry** on Android while significantly reducing its storage footprint.

Overall project plan drawn as a scheme can be found [here](https://github.com/DanLyss/AstroNavigation/blob/main/artifacts/Project_Scheme.png)


