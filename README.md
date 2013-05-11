# Versioning maven plugin

A Maven plugin that generates a small Java file with some revision info from git. This plugin makes it easy to show
info from your git repository inside your app. This makes it possible to trace a build back to the exact revision and
state the repository was in when the build was made.

An example of the output:

```java
public final class Version {
  public static final String BRANCH = "master";
  public static final String REVISION = "2f6422230fc18cb427eeb9f6201e83db3adb0f24";
  public static final String REVISION_SHORT = "2f642223";
  /**
   * Strict Clean means no changes, not even untracked files
   */
  public static final boolean STRICT_CLEAN = false;
  /**
   * Lose Clean means no changes except untracked files.
   */
  public static final boolean LOSE_CLEAN = false;
}
```

## How to use

Add the following to you <build><plugins> section of your pom file.

```xml
<plugin>
    <groupId>net.tietema.versioning</groupId>
    <artifactId>versioning-maven-plugin</artifactId>
    <version>1.0</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>git-java</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <packageName>com.example</packageName>
        <className>VersionInfo</className>
    </configuration>
</plugin>
```

The plugin will the run before each maven compile and generate an up to date version of the file. You can then reference
the static fields of the class directly into your code.

## Notes

 * It is not recommended to commit this file to your SCM.
 * If you build your code directly in your IDE it will probably not directly execute maven, but rather just copy
   configuration. In that case you need to make sure that the `verionsing-maven-plugin:git-java` goal runs before the build/make step of your
   IDE.

## Bugs, Contributions and Feedback

All collaboration is done on Github. If you have any suggestions, bugs or contributions, please contact me via Github.
Futher enhancesments could be: adding more SCM's or maybe adding more JVM output languages.

## License

This plugin is distributed under the Apache License version 2.0