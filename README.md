# Đọc sách (DocSach)

An Android book-reading app built with Jetpack Compose, backed by the Google Books API.

| Requirement (Đề 7) | Screen |
|---|---|
| Hiển thị danh sách truyện theo thể loại | Home (3 sections) + Danh mục + category grid |
| Giao diện thông tin tóm tắt của một truyện | Book detail |
| Giao diện đọc truyện | Reader |
| Giao diện tủ sách lưu trữ các truyện ưa thích | Tủ sách (Room-backed) |

## Setup

The app needs a **Google Books API key**. It is read from `local.properties`, which is
git-ignored, so no key is ever committed.

```bash
cp local.properties.example local.properties
```

Then open `local.properties` and set your key:

```properties
GOOGLE_BOOKS_API_KEY=AIza...
```

To create one:

1. Go to <https://console.cloud.google.com/> and create or select a project.
2. **APIs & Services → Library** → enable **Books API**.
3. **APIs & Services → Credentials → Create credentials → API key**.
4. Leave it unrestricted, or restrict it to the Books API. An *Android-restricted* key
   also needs the package name `com.example.docsach` and your debug SHA-1.

> A keyless build still compiles and launches — every screen simply shows
> "Chưa cấu hình Google Books API key" instead of books. Gradle also prints a warning
> when the key is missing.

## Build and run

```bash
./gradlew :app:assembleDebug     # build
./gradlew :app:installDebug      # install on a running device/emulator
```

Requires JDK 17+ (the Gradle toolchain fetches JDK 25), Android SDK 37, minSdk 24.

## Architecture

```
data/
  remote/   Retrofit + Gson against Google Books, API-key interceptor, DTO -> domain mapper
  local/    Room: saved books + reading progress (also the offline cache for Tủ sách)
  model/    Book, plus the named queries the home sections and genres share
di/         AppContainer — hand-rolled graph, no DI framework
ui/         One screen package per destination, each with a ViewModel exposing UiState
```

Single `MainActivity`, Navigation-Compose, Material 3. Strings are Vietnamese by default
(`values/`) with an English translation in `values-en/`.

### The reader

Google Books exposes no plain book text, so the reader loads Google's own preview page in a
`WebView` and injects a script that hides everything except `#viewport`, the page container —
otherwise the whole Google Books site would come with it. Volumes Google will not let us embed
fall back to a native text reader with font-size controls and saved scroll position.
