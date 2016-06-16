<img align="left" width="0" height="150px" hspace="20"/>
<a href="https://qabel.de" align="left">
	<img src="https://files.qabel.de/img/qabel_logo_orange_preview.png" height="150px" align="left"/>
</a>
<img align="left" width="0" height="150px" hspace="25"/>
> The Qabel Android Client

This project provides a Client for <a href="https://qabel.de"><img alt="Qabel" src="https://files.qabel.de/img/qabel-kl.png" height="18px"/></a> targeting Android. It is a small part of the qabel platform.

<br style="clear: both"/>
<br style="clear: both"/>
<p align="center">
	<a href="#introduction">Introduction</a> |
	<a href="#getting_started">Install</a> |
	<a href="#getting_started">Getting Started</a> |
	<a href="#usage">Usage</a> |
	<a href="#structure">Structure</a> |
	<a href="#contribution">Contribution</a>
</p>

# Introduction

<img src="https://files.qabel.de/img/qabel_app_screenshot_720x1280_1.2_dateimanager_en.png" width="280px"/>
<img src="https://files.qabel.de/img/qabel_app_screenshot_720x1280_1.3_seitenmenue_en.png" width="280px"/>
<img src="https://files.qabel.de/img/qabel_app_screenshot_720x1280_1.5_identitaeten_en.png" width="280px"/>


For a comprehensive documentation of the whole Qabel Platform use https://qabel.de as the main source of information. http://qabel.github.io/docs/ may provide additional technical information.

Qabel consists of multiple Projects:
 * [Qabel Android Client](https://github.com/Qabel/qabel-android)
 * [Qabel Desktop Client](https://github.com/Qabel/qabel-desktop)
 * [Qabel Core](https://github.com/Qabel/qabel-core) is a library that includes the common code between both clients to keep them consistent
 * [Qabel Drop Server](https://github.com/Qabel/qabel-drop) is the target server for drop messages according to the [Qabel Drop Protocol](http://qabel.github.io/docs/Qabel-Protocol-Drop/)
 * [Qabel Accounting Server](https://github.com/Qabel/qabel-accounting) manages Qabel-Accounts that authorize Qabel Box usage according to the [Qabel Box Protocol](http://qabel.github.io/docs/Qabel-Protocol-Box/)
 * [Qabel Block Server](https://github.com/Qabel/qabel-block) serves as the storage backend according to the [Qabel Box Protocol](http://qabel.github.io/docs/Qabel-Protocol-Box/)

# Install

Official distributions of the [Qabel Android Client](https://github.com/Qabel/qabel-android) are provided by the [official Qabel website](https://qabel.de) at https://qabel.de/de/qabelnow and via [Google Play](https://play.google.com/store/apps/details?id=de.qabel.qabel).
Everything below this line describes the usage of the Qabel Android Client for development purposes.

# <a name="getting_started"></a>Getting started

* Install the Android [SDK](https://developer.android.com/studio/index.html)
* Install the SDK version 23. Make sure you install the "Android Support Repository" and the latest "Android SDK build-tools"
* Install the [NDK](https://developer.android.com/ndk/index.html)
* Checkout the project from git
* Create the file /qabelbox/src/main/res/values/params.xml with the following contents:

    `<resources> <string name="hockeykey">dummykey</string> </resources>`
* Create a local.properties file at the root of your source checkout and add an sdk.dir and ndk.dir entry to it.  For example:

        sdk.dir=/opt/Android/SDK
        ndk.dir=/opt/Android/NDK

* Run `./gradlew build`
* Usage of Android Studio with the Kotlin plugin is recommended for development

# Usage

You can run the tests with the following gradle tasks:

* `./gradlew testDeveloperDebugUnitTest` runs the local unit tests
* `./gradlew spoonDeveloperDebugAndroidTest` runs all instrumentation tests on all connected devices and emulators

# Development

To run the unit tests directly from Android Studio, you need to change the default options for the JUnit task. Change the vm-options to `-ea -Djava.library.path=libs/` so that the android studio testrunner finds the crypto library we use.

## Common errors

### Cannot run program "".../aapt": error=2, No such file or directory

You are missing some libs for the 32bit aapt. For ubuntu try
```bash
sudo apt-get install lib32stdc++6 lib32z1
```

# Structure

The test server addresses are hard coded in TestConstants.java and the live servers are configured in a string ressource `servers.xml`


# Contribution

For issues using the Qabel Android Client, use the feedback feature inside the app (Settings -> Feedback).

Otherwise, use the Issue tracker of GitHub.

Please read the contribution guidelines at https://github.com/Qabel/qabel-core/blob/master/CONTRIBUTING.md carefully.
