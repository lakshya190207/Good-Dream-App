---
name: android-database-migration-testing
description: >-
  Automated testing runbook for Room database schema migrations using MigrationTestHelper to eliminate app update crashes and data loss before production deployment.
---

# Room Database Migration Verification & Testing

Use this skill whenever incrementing Room database `version`, adding or modifying tables/columns, converting column data types, or ensuring zero crashes when users update the app from an older version.

---

## 1. The Migration Trap: Why Apps Crash on Update

When a user updates an app:
1. Room compares the disk schema with the compiled entity schema.
2. If the version changed and **no valid migration is found**, Room throws:
   `IllegalStateException: A migration from X to Y was required but not found.`
3. If `fallbackToDestructiveMigration()` was set, **all user data is silently erased!**
4. If SQL has a typo or column constraint mismatch (e.g. `NOT NULL` without default), the app crashes on the very first query after update.

---

## 2. Setting Up Schema Export in Gradle

Room must export its schema JSON files to test migrations:

In `app/build.gradle.kts`:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
        getByName("test").assets.srcDir("$projectDir/schemas")
    }
}
```

---

## 3. Writing the Migration Test

Add `androidx.room:room-testing` to `testImplementation`:

```kotlin
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val TEST_DB = "migration-test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2_containsAllDataAndNewColumns() {
        // 1. Create database in version 1 state
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert mock row matching v1 schema
            execSQL("INSERT INTO products (id, name, price) VALUES ('prod_1', 'SpringHaven Mattress', 1299.99)")
            close()
        }

        // 2. Run migration to version 2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // 3. Verify data integrity and new column default values
        val cursor = db.query("SELECT id, name, price, discount_percent FROM products WHERE id = 'prod_1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("prod_1", cursor.getString(0))
        assertEquals("SpringHaven Mattress", cursor.getString(1))
        assertEquals(1299.99, cursor.getDouble(2), 0.001)
        assertEquals(0, cursor.getInt(3)) // Default value for new column
        cursor.close()
    }
}
```

---

## 4. Pre-Release Migration Verification Protocol

Before submitting any update:
1. Run `./gradlew test` to execute migration tests across all historic versions.
2. Verify that `$projectDir/schemas/<version>.json` is checked into git.
3. Test upgrading on an emulator with an existing installed APK.
