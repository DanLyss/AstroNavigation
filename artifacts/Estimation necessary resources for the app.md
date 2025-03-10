
# Using Astrometry on Android via NDK 📱

## Overview 🛠️

We will integrate the **Astrometry** library into our Android application using the **Android NDK**. This approach enables us to run the Unix-based library natively on Android devices while maintaining high performance.

## Storage Considerations 💾

One major downside of using **Astrometry** is that its model fit data files (`.fits`) take up more than **400MB** of storage. However, since we only need the **brightest stars** (which are visible in smartphone photos), we can **significantly reduce the file size** by about **90%**. This optimization will make the application much more lightweight and overall we expect  **Astrometry** to take up to **50MB** of storage.

## Local Testing Results ✅

We have successfully tested **Astrometry** on a local PC, where it worked correctly and produced results. The output can be found in the corresponding [folder](https://github.com/DanLyss/AstroNavigation/tree/main/artifacts/Astrometry%20guide) . This confirms that the library functions as expected before deploying it to Android.


## Conclusion 🎯

By leveraging **Android NDK**, we can run **Astrometry** on Android while significantly reducing its storage footprint. Our initial tests show promising results, and further optimization will make this approach even better. 

Overall project plan drawn as a scheme can be found [here](https://github.com/DanLyss/AstroNavigation/blob/main/artifacts/Project_Scheme.png)


