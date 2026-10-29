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
├── data/            # Room entities + DAO (FoodDatabase.kt), migrations, the DB singleton,
│                    # food sources (Open Food Facts, Kalorické tabulky, bundled menus and base foods)
│                    # and pure domain logic (WeeklyBudget, WeightMath, RecipeMath, NutritionRating, …)
├── fasting/         # Fasting timer alarms and notifications
├── widget/          # Home-screen "Today" widget
├── ui/
│   ├── screens/     # Compose screens and dialogs (settings/ holds the settings pages)
│   ├── viewmodel/   # MainViewModel: all app state as StateFlows, all mutations
│   └── theme/       # Material 3 themes, colours, shapes, motion
├── MainActivity.kt  # Entry point, navigation, launcher-shortcut / widget actions
└── HealthRationaleActivity.kt  # Health Connect permission rationale
tools/               # Python scripts that regenerate the bundled food data in app/src/main/assets
```

### Core Logic
1.  **State Management**: `MainViewModel` uses `StateFlow` and `flatMapLatest` to provide a reactive stream of food entries that automatically updates whenever the `selectedDate` changes.
2.  **Room Persistence**: Data is persisted across three main tables:
    *   `food_entries`: The actual daily log.
    *   `custom_foods`: Saved templates from scans or manual entries.
    *   `user_goals`: A singleton table storing the user's nutritional targets.
3.  **Barcode Processing**: The `ScannerScreen` uses `LifecycleCameraController` with `MlKitAnalyzer`. If a barcode is not found in the local `custom_foods` cache, it queries the `FoodRepository` (Open Food Facts).
4.  **Nutrient Calculation**: Foods store values per reference amount (100 g/ml, or 1 piece); an entry's totals are `value / referenceAmount × amount`.

## 📦 Tech Stack
*   **Language**: Kotlin 2.1.0
*   **UI**: Jetpack Compose (Material 3)
*   **Database**: Room (Local Persistence)
*   **Annotation Processing**: KSP (Kotlin Symbol Processing)
*   **Networking**: Ktor (Open Food Facts API)
*   **AI/Vision**: Google ML Kit (Barcode Scanning)
*   **Navigation**: Adaptive Navigation Suite

## 🔧 Building
Requires JDK 21 and the Android SDK (compile SDK 36).
```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest    # unit tests
```
Release builds are signed with a key read from `keystore.properties` + `release.jks` in the repo root; neither is in the repository.

## 📚 Data Sources
*   [Open Food Facts](https://world.openfoodfacts.org) — barcode lookups and product search (ODbL).
*   [Fineli](https://fineli.fi) (THL, CC BY 4.0) and [USDA FoodData Central](https://fdc.nal.usda.gov) — nutrients of the bundled base foods.
*   [Kalorické tabulky](https://www.kaloricketabulky.cz) — Czech food search, queried live.
