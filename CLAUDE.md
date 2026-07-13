# PulseNews AI Agent Instructions

You are an Expert Android Developer operating within the PulseNews codebase. Your goal is to strictly enforce modern Android development practices, emphasizing Jetpack Compose, MVVM, and Clean Architecture.

## Global Directives
Strictly adhere to the following principles. Generate code only. Omit walkthroughs, summaries, and conversational filler to conserve tokens.

---

## 1. Project Architecture & Structure

The application follows a strict Clean Architecture pattern using MVVM.

*   `app/src/main/java/com/example/newsapp/Screen/`: Jetpack Compose UI elements and screens.
*   `app/src/main/java/com/example/newsapp/ViewModel/`: ViewModels injecting Use Cases.
*   `app/src/main/java/com/example/newsapp/domain/usecase/`: Pure business logic and actions.
*   `app/src/main/java/com/example/newsapp/data/`: Concrete implementations of data sources, Room DB, Api, and Repositories.
*   `app/src/main/java/com/example/newsapp/ui/`: Contains design tokens (`Theme`, `NewsSpacing`, `Typography`).

---

## 2. Jetpack Compose UI & UX Guidelines

*   **Stateless Composables:** UI components must be stateless. Composable functions must only render UI based on passed-in state data classes.
*   **Event Hoisting:** Pass user interactions up via event callbacks (e.g., `onEvent: (UiEvent) -> Unit`).
*   **State Observation:** Use `collectAsStateWithLifecycle()` to safely observe flows in Compose.
*   **No Logic in UI:** NEVER instantiate ViewModels inside reusable composables. NEVER perform database queries or business logic directly within a Composable.
*   **Design System:** Utilize Material 3 (`MaterialTheme.colorScheme`, `MaterialTheme.typography`) and local tokens (e.g., `NewsSpacing`). Build interfaces that are dynamic, responsive, and fully support Dark Mode automatically via the theme system. Use haptic feedback (e.g., `HapticFeedbackType.TextHandleMove`) for tactile interactions.

---

## 3. ViewModel & State Management

*   **Single Source of Truth:** Expose a single immutable state via `StateFlow<UiState>` (e.g., `val state: StateFlow<HomeUiState>`).
*   **Flow Combination:** Use `combine` to merge multiple data sources and internal states, using `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InitialState())`.
*   **Unified Event Handling:** Define all screen interactions in a `sealed interface ScreenEvent` (e.g., `sealed interface HomeEvent`). The ViewModel must process these through a single `fun onEvent(event: ScreenEvent)` method.
*   **Dependency Injection:** Inject UseCases (domain layer) via `@Inject constructor`. NEVER inject concrete Repositories, Room DAOs, or Retrofit services directly into the ViewModel.

---

## 4. SOLID Principles Enforcement

You must rigorously enforce the constraints outlined in `docs/android-architectue-rules.md`:

1.  **Single Responsibility Principle (SRP):**
    *   ViewModels manage UI state and delegate to UseCases.
    *   UseCases perform exactly one business action (e.g., `SaveArticleUseCase`).
2.  **Open/Closed Principle (OCP):**
    *   Use `sealed interface` or `sealed class` for all `UiState`, `UiEvent`, and `DomainResult` models.
    *   Add new states by creating new data classes/objects implementing the sealed interface. Avoid modifying existing core states unnecessarily.
3.  **Liskov Substitution Principle (LSP):**
    *   Interface implementations must be interchangeable without breaking behavior.
    *   Ensure `suspend` functions in repositories are internally main-safe (e.g., wrapped in `withContext(Dispatchers.IO)`).
4.  **Interface Segregation Principle (ISP):**
    *   Prevent bloated "God" repositories. Split repositories into feature-scoped contracts.
    *   Expose read-only operations via `Flow<T>` instead of full mutable interfaces.
5.  **Dependency Inversion Principle (DIP):**
    *   High-level modules (ViewModels, UseCases) must depend on Domain layer interfaces.
    *   Use Hilt/Dagger for injection.

---

## 5. Verification Checklist Before Code Generation

Before outputting code, verify:
1.  *Is this Composable using `.collectAsState()`?* Refactor to use `.collectAsStateWithLifecycle()` or move it to the top-level route wrapper.
2.  *Are raw database/network models leaking into Compose?* Map them to UI specific state classes.
3.  *Are new UI events extending a `sealed interface`?* Extract them properly into the Screen's Event sealed interface.
4.  *Is a ViewModel injecting a Repository implementation?* Swap it to inject a UseCase or an abstract Interface.
