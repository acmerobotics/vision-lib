# vision-lib

OpenCV-based computer vision library for FTC that implements beacon detection. Note that this library is not Android-specific and can be used in any normal Java project. For example, our [vision-test](https://github.com/acmerobotics/vision-test) repo contains a Gradle-based Java project for quickly testing this library on a set of beacon images. Additionally, our [ftc_app](https://github.com/acmerobotics/ftc_app) repo contains the rest of our codebase including an Android-compatible installation of this library. It also contains its own test application for testing on the actual phones.

## Status

This library is still under active development; there is no guarantee that future releases will be backwards-compatible. Additionally, documentation will not be added until the library is relatively stable.

## Installing as a Submodule

Installing the library can be kind of tricky. We recommend looking at the projects mentioned above; these instructions are only for advanced users.

* Navigate into the root of your Gradle/Android project (or Eclipse workspace) and run this command: `git submodule add https://github.com/acmerobotics/vision-lib VisionLib`. This creates a submodule in the VisionLib directory as well as a `.gitmodules` file that points to the library repo. Note that while this command configures the library for the current project, other projects will need to be configured using the `git submodule update --init` command. You can read more about submodules [here](https://git-scm.com/book/en/v2/Git-Tools-Submodules).

* Now, add the following line to your `settings.gradle` file: 

  ```groovy
  include ':VisionLib'
  ```

* You've successfully installed the Gradle module! Test it out using `gradle build` or, if your project uses the Gradle wrapper, `./gradlew build`. If you would like to further configure an Eclipse or Android Studio project, continue with the additional instructions below.

### Eclipse Project

* Step inside the submodule using `cd VisionLib`.

* Generate the necessary project files with `gradle eclipse`.

* Now, if you haven't already, open Eclipse.

* Under the File menu, select Import.... In the Import wizard, select Existing Projects into Workspace from under the General tab and click Next. 

* Click the Browse... button next to Select root directory and select the workspace from the filesystem.

* In the Projects area below, make sure VisionLib is selected and then click Finish.

* You should now be able to use the library in your project. Make sure the you put the following line in the beginning of every `main()` method to load OpenCV (with the appropriate import):

  ```java
  System.loadLibrary(Core.NATIVE_LIBRARY_NAME); 
  ```

### Android Studio Project

* Somewhere outside the project, download the OpenCV Android SDK from [SourceForge](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/3.1.0/OpenCV-3.1.0-android-sdk.zip/download) and extract it.

* In Android Studio, navigate to File > New > Import Module.... Select the `sdk\java` folder from the SDK and change the Module name to OpenCV. Finally, click Next and then Finish.

* Add the following lines (in the dependencies section) to the `build.gradle` file for every module you would like to use the library:

  ```groovy
  compile project(':OpenCV')
  compile project(':VisionLib')
  ```

  ​