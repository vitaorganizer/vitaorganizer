## VitaOrganizer 0.2

Desktop tool for listing and uploading games and homebrew applications to PSVITA without the size requirements
of uploading the whole VPK and extracting it later.

It is written in Kotlin/Java.

It should work on Windows, Linux and MacOS. It is a Java desktop application, packed in an executable .JAR, that
can be executed directly with double click on most cases.

In other cases, you can run it with `java -jar vitaorganizer-0.2.jar`

You can download a prebuild binary here, or just build from source:
[Download VitaOrganizer 0.2 here](https://github.com/soywiz/vitaorganizer/releases/download/0.2/vitaorganizer-0.2.jar)

### Building from source

You can open build.gradle in intelliJ IDEA 2016.2 (Community Edition is ok) to get started directly.
The main class is defined in : `src/com/soywiz/vitaorganizer/VitaOrganizer.kt`

You can compile without intelliJ directly from the console just with gradle. Just call

```
gradle jar
```

It will generate the file `build/libs/vitaorganizer-0.1.jar` with all the dependencies included as an executable jar
that should work on desktop java versions.

![](extra/screenshot.png)
