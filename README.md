# my-java-app

這是一個用 Java 17 和 Maven 建立的最小 Java 教學專案，目的是練習以下完整流程：

1. 建立 Java 程式與單元測試
2. 使用 Maven 編譯、測試和打包
3. 使用 GitHub Actions 自動執行 CI
4. 將產生的 JAR 上傳為 GitHub Actions artifact
5. 下載 artifact 後，在本機使用指定的 JDK 執行

程式執行後會輸出：

```text
Hello, World!
```

## 專案內容

```text
my-java-app/
├── pom.xml
├── src/
│   ├── main/java/com/example/App.java
│   └── test/java/com/example/AppTest.java
└── .github/workflows/ci.yml
```

### Java 程式

`App.java` 提供 `getGreeting()` 方法和 `main()` 入口。`main()` 會建立 `App` 物件並輸出問候文字。

### 單元測試

`AppTest.java` 使用 JUnit 5 檢查 `getGreeting()` 是否回傳 `Hello, World!`。

### Maven 設定

`pom.xml` 定義：

- `groupId`: `com.example`
- `artifactId`: `my-java-app`
- 版本：`1.0-SNAPSHOT`
- Java source/target：17
- JUnit Jupiter API：5.10.0

## 本機需求

- JDK 17
- Maven 3.x（若要在本機執行 `mvn` 指令）
- Git

注意：JDK 和 JRE 不是同一件事。編譯結果使用 Java 17 class file；使用 Java 8 執行時會發生 `UnsupportedClassVersionError`。

本次使用的 JDK 17 路徑是：

```text
~\projects\docker-handson\devsecops-task-app\backend\.tools\jdk\jdk-17.0.20.1+1
```

## 本機編譯與測試

在專案根目錄執行：

```powershell
mvn clean test
```

只編譯主程式：

```powershell
mvn compile
```

打包 JAR：

```powershell
mvn package -DskipTests
```

打包完成後，JAR 會放在：

```text
target/my-java-app-1.0-SNAPSHOT.jar
```

## 使用指定 JDK 17 執行

在 PowerShell 中設定目前視窗使用的 JDK：

```powershell
$env:JAVA_HOME = "~\projects\docker-handson\devsecops-task-app\backend\.tools\jdk\jdk-17.0.20.1+1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

確認版本顯示 `17.0.20.1` 後，可以從專案根目錄執行：

```powershell
mvn package -DskipTests
java -cp ".\target\my-java-app-1.0-SNAPSHOT.jar" com.example.App
```

預期輸出：

```text
Hello, World!
```

## 使用 GitHub Actions

workflow 檔案是 `.github/workflows/ci.yml`，在 `main` 分支 push 或 pull request 時執行：

1. 使用 `actions/checkout@v4` 取得原始碼
2. 使用 `actions/setup-java@v4` 設定 Temurin JDK 17
3. 執行 `mvn compile`
4. 執行 `mvn test`
5. 執行 `mvn package -DskipTests`
6. 使用 `actions/upload-artifact@v4` 上傳 `target/*.jar`

GitHub Actions 執行完成後，在該次 workflow 頁面的底部 **Artifacts** 區域可以下載 `java-app-jar`。

Artifact 不是 Docker image。這個專案目前沒有 `Dockerfile`，workflow 也沒有執行 Docker build 或 push，因此不會在 GitHub Container Registry 或 Docker Hub 產生 image。

## 下載 artifact 後執行

假設下載並解壓後的 JAR 放在：

```text
~\projects\docker-handson\my-java-app-1.0-SNAPSHOT.jar
```

執行：

```powershell
$env:JAVA_HOME = "~\projects\docker-handson\devsecops-task-app\backend\.tools\jdk\jdk-17.0.20.1+1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd ~\projects\docker-handson
java -cp ".\my-java-app-1.0-SNAPSHOT.jar" com.example.App
```

這個 Maven JAR 沒有設定 `Main-Class` manifest，因此不能直接使用：

```powershell
java -jar .\my-java-app-1.0-SNAPSHOT.jar
```

必須使用 `-cp` 指定 JAR，再指定完整類別名稱 `com.example.App`。

## 遇到的問題與解法

### 1. GitHub Actions 回傳 exit code 127

原本 workflow 使用了：

```yaml
run: mvm compile
```

`mvm` 是拼字錯誤，runner 找不到這個命令，所以回傳 `exit code 127`。修正為：

```yaml
run: mvn compile
```

### 2. 本機找不到 Maven

本機執行 `mvn compile` 時若出現 PowerShell 的 `CommandNotFoundException`，表示 Maven 沒有安裝，或 Maven 的 `bin` 沒有加入 `PATH`。

解法是安裝 Maven 並設定 `MAVEN_HOME`/`PATH`，或直接使用已安裝的 Maven Wrapper（本專案目前沒有 `mvnw`）。GitHub Actions runner 會提供 Maven，因此 CI 可以正常執行。

### 3. Java 版本不相容

本機原先使用 Java 8，但 artifact 是用 Java 17 編譯。執行時出現：

```text
UnsupportedClassVersionError
```

解法是改用 Java 17。本專案已使用位於 `devsecops-task-app/backend/.tools/jdk/` 的 JDK 17 驗證成功。

### 4. Artifact、JAR 和 Docker image 的差異

- **JAR**：Java 編譯與打包後的程式檔案，本 workflow 上傳的是 JAR。
- **Artifact**：GitHub Actions 保存的建置輸出，方便下載；本例的 artifact 名稱是 `java-app-jar`。
- **Docker image**：Docker 的可部署映像檔，需要 `Dockerfile` 和 Docker build 流程；目前專案尚未建立 image。

### 5. README 修改不會觸發 CI

workflow 的 `push` 和 `pull_request` 都設定了：

```yaml
paths-ignore:
  - '**/*.md'
```

因此只修改 README.md 時，GitHub Actions 不會啟動。這可以避免文件修改浪費 CI 執行次數，但如果希望文件變更也觸發 CI，就需要移除 `paths-ignore` 或調整規則。

## 教學重點與難點

### 重點

- Java 編譯版本必須和執行版本相容。
- CI 的命令必須和本機使用的命令一致，例如 `mvn compile`、`mvn test`。
- `mvn package` 才會產生可下載的 JAR；只有 `mvn compile` 和 `mvn test` 不會留下可下載 artifact。
- `actions/upload-artifact` 只會保存指定路徑的檔案，不能自動產生 Docker image。
- workflow 中的每一步都會影響後續步驟，例如必須先 package，才能上傳 `target/*.jar`。

### 難點

- 分辨 `exit code 127` 是命令不存在，而不是 Java 程式本身失敗。
- 分辨 Java 8 執行 Java 17 編譯結果時的 `UnsupportedClassVersionError`。
- 理解 GitHub Actions artifact 是檔案保存機制，不等同於 Docker image 或套件發布平台。
- 注意 Maven 預設產生的 JAR 不一定設定 `Main-Class`，因此不一定能使用 `java -jar` 啟動。
- 本機環境和 GitHub runner 可能不同；CI 有 Maven，但本機未必有 Maven，必須檢查 `java -version` 和 `mvn -version`。

## 後續可以練習的方向

- 加入 Maven Wrapper，讓不同電腦不必預先安裝 Maven。
- 在 `pom.xml` 設定 `maven-jar-plugin` 的 `Main-Class`，讓 JAR 可以使用 `java -jar` 執行。
- 新增 `Dockerfile`，將 JAR 打包成 Docker image。
- 在 GitHub Actions 加入 Docker build、registry login 和 image push。
- 將測試報告或其他建置輸出分開上傳為 artifact。
