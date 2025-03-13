# PulseNews

NewsApp is an Android news application built with Kotlin that delivers real-time news from various categories using the NewsAPI. The app offers an engaging user experience by displaying top headlines, allowing comprehensive news searches, and providing the ability to save articles for offline reading.

## Features

- **Dynamic News Categories:**  
  On launch, the home screen displays multiple news categories (with "science" selected by default) and a corresponding feed of articles. Switching categories (e.g., to "business") updates the feed with relevant news.

- **Top Headlines & Comprehensive Search:**  
  - **Top Headlines:** Quickly view trending news filtered by country and category.  
  - **Everything:** Search for news on any topic with options to filter by popularity, publication date, and relevancy.

- **Article Details & Offline Reading:**  
  Tap on an article to view the full story and save it for later. Saved articles are accessible even when offline.

## API Integration

NewsApp uses NewsAPI to fetch current news data. The main endpoints integrated are:

```kotlin
interface NewsApi {
    @GET("v2/top-headlines")
    suspend fun getTopHealines(
        @Query("country") country: String = "us",
        @Query("category") category: String,
        @Query("page") pageNumber: Int,
        @Query("apiKey") apikey: String
    ): Response<News>

    @GET("v2/everything")
    suspend fun getEverything(
        @Query("q") searchquery: String,
        @Query("sortBy") sortBy: String,
        @Query("pageSize") pageNumber: Int = 10,
        @Query("apiKey") apikey: String
    ): Response<News>
}
```

## Built With

- **Kotlin:** Primary language for Android development.
- **NewsAPI:** Provides up-to-date news data.
- **Android SDK:** Used to build a native Android application.

## Installation

1. **Clone the Repository:**
   ```
   git clone https://github.com/Kutubuddin-Rasel/NewsApp.git
   ```
2. **Open in Android Studio:**  
   Import the project into Android Studio to build and run the application.

3. **Configure API Key:**  
   - Sign up for an API key at [NewsAPI](https://newsapi.org/).  
   - Insert your API key in the project’s configuration file.

4. **Build and Run:**  
   Compile the project in Android Studio and run the app on an emulator or physical device.

## Usage

- **Home Screen:**  
  View multiple news categories at the top with a default selection (e.g., science). The middle section shows the corresponding news feed, while the bottom provides options for top headlines, comprehensive search ("everything"), and saved articles.

- **Article Interaction:**  
  Select a news article to read the full content and save articles for offline reading.

- **News Search:**  
  Use the "everything" option to search for news on any topic and apply filters based on popularity, publication date, or relevancy.

## Contribution

Contributions are welcome! Feel free to fork the repository and submit a pull request with your improvements.
