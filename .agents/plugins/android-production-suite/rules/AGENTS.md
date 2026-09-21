# Android Production Suite Rules

When building or refactoring code in this Android repository, the agent MUST enforce these production rules:

## 1. Architecture & State Discipline
- **Immutable UI State**: State exposed to Compose must be an immutable data class or sealed hierarchy.
- **Single Source of Truth**: UI components must never mutate database or network entities directly; all mutations go through the ViewModel and Repository.
- **Error Transparency**: Do not hide errors with empty catches. Surface meaningful user error states or fallback UI.

## 2. Compose Best Practices
- **Stability**: Parameters passed to `@Composable` functions should be `@Stable` or `@Immutable`. For collections, prefer `ImmutableList` or convert to stable wrappers if experiencing unnecessary recompositions.
- **Lambdas**: Avoid instantiating new lambda instances inside loops; pass stable method references or event callbacks.
- **Derived State**: Use `remember { derivedStateOf { ... } }` when reading rapidly changing states (e.g. scroll offsets, filtered lists).

## 3. Storage & Background Tasks
- **Room Migrations**: Never use `fallbackToDestructiveMigration()` in production. Always write explicit migrations or use `AutoMigration`.
- **WorkManager**: Use `CoroutineWorker` for reliable, deferred, and periodic background tasks (e.g., sync, uploads). Specify network and battery constraints.

## 4. Security & Sensitive Data
- Do not store unencrypted tokens or passwords in standard SharedPreferences. Use `EncryptedSharedPreferences`.
- Ensure all WebViews, if any, have `setJavaScriptEnabled(false)` unless explicitly necessary, with restricted origins.
- Set `android:exported="false"` on internal activities, services, and receivers.
