# TrackMeCalories

A high-performance, ad-free, local-first calorie and macronutrient tracking application for Android. Focused on speed, accuracy, and absolute user privacy.

## 🚀 Features

### 1. Logging & Entry
*   **Barcode Scanner**: Instant recognition of UPC/EAN codes using Google ML Kit, integrated with the Open Food Facts API.
*   **Manual Entry**: Fallback form for all nutrients (Kcal, Protein, Carbs, Fats, Fiber, Salt).
*   **Smart Weight Calculation**: All inputs use the professional formula: 
    $$\text{Total} = \left( \frac{\text{Value per 100g}}{100} \right) \times \text{Grams Eaten}$$
*   **Quick Add**: "Fast Log" button to add flat calories without naming a specific food.
*   **Meal Categorization**: Organize logs into Breakfast, Lunch, Dinner, or Snacks.

### 2. Dashboard & Feedback
*   **Daily Progress Card**: At-a-glance summary of "Calories Remaining" ($Goal - Eaten$).
*   **Macro Breakdown**: Visual progress "pills" for Protein, Carbs, and Fat goals.
*   **Historical Scroll**: Quick date-selection header to view/log for 3 days in the past or future.
*   **Daily List**: Clean list of logs with calorie counts and macro summaries.

### 3. Entry Management
*   **Swipe-to-Delete**: Effortlessly remove items from your log.
*   **Edit & Copy**: Tap any entry to update its weight or copy it to Tomorrow/Yesterday (perfect for leftovers).
*   **Recent Foods**: A dedicated tab for one-tap entry of your most frequently logged items.

### 4. Privacy & Settings
*   **Local-First**: All data is stored securely on your device in a Room Database.
*   **Personal Goals**: Customizable targets for calories and macronutrients.
*   **Data Control**: One-tap "Wipe All Data" feature for absolute privacy.
*   **Offline Mode**: 100% functional without internet (except for new barcode lookups).

---

## 🛠 Codebase & Logic

The project follows a modern MVVM (Model-View-ViewModel) architecture with a clean, package-based file structure.

### Project Structure
```
com.jsemalja.trackmecalories/
├── data/
│   ├── AppDatabase.kt      # Room DB configuration & Migrations
│   ├── FoodDao.kt          # Database operations (CRUD)
│   ├── FoodDatabase.kt     # Entities: FoodEntry, CustomFood, UserGoal
│   └── FoodRepository.kt   # Open Food Facts API integration (Ktor)
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt    # Dashboard, Date selector, Daily log
│   │   ├── ScannerScreen.kt # ML Kit Barcode logic & result handling
│   │   ├── RecentsScreen.kt # List of saved food templates
│   │   └── SettingsScreen.kt# Goal configuration & data management
│   ├── viewmodel/
│   │   └── MainViewModel.kt # Central state (StateFlow) and Business Logic
│   └── theme/              # Material 3 Theme configuration
└── MainActivity.kt         # Entry point & Navigation management
```

### Core Logic
1.  **State Management**: `MainViewModel` uses `StateFlow` and `flatMapLatest` to provide a reactive stream of food entries that automatically updates whenever the `selectedDate` changes.
2.  **Room Persistence**: Data is persisted across three main tables:
    *   `food_entries`: The actual daily log.
    *   `custom_foods`: Saved templates from scans or manual entries.
    *   `user_goals`: A singleton table storing the user's nutritional targets.
3.  **Barcode Processing**: The `ScannerScreen` uses `LifecycleCameraController` with `MlKitAnalyzer`. If a barcode is not found in the local `custom_foods` cache, it queries the `FoodRepository` (Open Food Facts).
4.  **Nutrient Calculation**: To ensure precision, all nutrients are calculated based on the ratio of `consumedAmount / 100.0` multiplied by the base value per 100g.

## 📦 Tech Stack
*   **Language**: Kotlin 2.1.0
*   **UI**: Jetpack Compose (Material 3)
*   **Database**: Room (Local Persistence)
*   **Annotation Processing**: KSP (Kotlin Symbol Processing)
*   **Networking**: Ktor (Open Food Facts API)
*   **AI/Vision**: Google ML Kit (Barcode Scanning)
*   **Navigation**: Adaptive Navigation Suite
