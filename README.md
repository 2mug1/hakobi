# hakobi
Redisを使用してサーバー間での会話が簡単になるライブラリ
## Getting Started

### インストール

`pom.xml`
```xml
<repository>
    <id>github</id>
    <name>hakobi</name>
    <url>https://maven.pkg.github.com/2mug1/hakobi</url>
</repository>

<dependency>
  <groupId>me.hyperbone</groupId>
  <artifactId>hakobi</artifactId>
  <version>1.0.0</version>
  <scope>compile</scope>
</dependency>
```

`build.gradle`
```gradle
repositories {
  maven (url = "https://maven.pkg.github.com/2mug1/hakobi")
}
dependencies {
  implementation("me.hyperbone:hakobi:1.0.0")
}
```

## LICENSE
[MIT License](./LICENSE) (© 2022 mirusms)