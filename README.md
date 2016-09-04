## VitaOrganizer 0.1

Desktop tool for listing and uploading games and homebrew applications to PSVITA without the size requirements
of uploading the whole VPK and extracting it later.

It should work on Windows, Linux and MacOS. It is a Java desktop application, packed in an executable .JAR, that
can be executed directly with double click on most cases.

In other cases, you can run it with `java -jar vitaorganizer-0.1.jar`

[Download VitaOrganizer 0.1 here](https://github.com/soywiz/vitaorganizer/releases/download/0.1/vitaorganizer-0.1.jar)

### Building from source

You can open build.gradle in intelliJ to get started. The main class is: `src/com/soywiz/vitaorganizer/VitaOrganizer`

You can compile without intelliJ directly from the console just with gradle. Just call

```
gradle jar
```

It will generate the file `build/libs/vitaorganizer-0.1.jar` with all the dependencies included as an executable jar
that should work on desktop java versions.

![](extra/screenshot.png)
