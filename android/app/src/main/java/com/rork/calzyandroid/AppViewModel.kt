package com.rork.calzyandroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.calzyandroid.data.AppData
import com.rork.calzyandroid.data.Dates
import com.rork.calzyandroid.data.ExerciseEntry
import com.rork.calzyandroid.data.FoodItem
import com.rork.calzyandroid.data.MealEntry
import com.rork.calzyandroid.data.MealSlot
import com.rork.calzyandroid.data.ProgressPhoto
import com.rork.calzyandroid.data.SavedFood
import com.rork.calzyandroid.data.UserProfile
import com.rork.calzyandroid.data.WaterEntry
import com.rork.calzyandroid.data.WeightEntry
import com.rork.calzyandroid.data.uid
import java.io.File
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Single source of truth for the app — profile, meals, exercises, water,
 * weights, photos and bookmarks. Persists as one JSON file, mirroring the
 * web build's localStorage document.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val store: File = File(application.filesDir, "calzy-data-v1.json")

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private var saveJob: Job? = null

    /**
     * Loads the store, preserving anything it cannot read.
     *
     * A decode failure used to fall straight through to an empty `AppData`, and
     * the next mutation then wrote that empty document over the file - so one bad
     * decode, from a corrupt write or a schema change, silently destroyed the
     * user's entire history with no way back.
     *
     * The unreadable file is now copied aside first. Only the first failure is
     * kept: after that the app is running on an empty document, so a later backup
     * would just overwrite the one copy that still holds real data.
     */
    private fun load(): AppData = try {
        if (store.exists()) json.decodeFromString<AppData>(store.readText()) else AppData()
    } catch (error: Exception) {
        runCatching {
            val backup = File(store.parentFile, store.name + ".unreadable")
            if (!backup.exists()) store.copyTo(backup)
        }
        AppData()
    }

    /** Writes the current document immediately, off the debounce. */
    private fun persistNow() {
        try {
            store.writeText(json.encodeToString(AppData.serializer(), _data.value))
        } catch (error: Exception) {
            // Storage may be full; state stays live in memory.
        }
    }

    /** Debounced persistence so slider drags don't thrash the disk. */
    private fun mutate(transform: (AppData) -> AppData) {
        _data.value = transform(_data.value)
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(260)
            persistNow()
        }
    }

    /**
     * Flush a pending write before the scope that owns it dies.
     *
     * `viewModelScope` is cancelled as part of clearing, taking the debounced
     * write with it - so anything logged inside the last 260 ms was lost when the
     * process went away. Writing synchronously here is main-thread I/O, which is
     * not free, but it is one small file and it is the last chance to keep the
     * user's most recent entry.
     */
    override fun onCleared() {
        if (saveJob?.isActive == true) {
            saveJob?.cancel()
            persistNow()
        }
        super.onCleared()
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setProfile(update: (UserProfile) -> UserProfile) {
        mutate { it.copy(profile = update(it.profile)) }
    }

    fun completeOnboarding(profile: UserProfile) {
        mutate { current ->
            current.copy(
                profile = profile.copy(hasOnboarded = true),
                weights = current.weights.ifEmpty {
                    listOf(
                        WeightEntry(
                            date = Dates.nowIso(),
                            kilograms = profile.currentWeightKg,
                        ),
                    )
                },
            )
        }
    }

    fun addMeal(meal: MealEntry) {
        mutate { it.copy(meals = it.meals + meal) }
    }

    fun updateMeal(meal: MealEntry) {
        mutate { current ->
            current.copy(meals = current.meals.map { if (it.id == meal.id) meal else it })
        }
    }

    fun deleteMeal(id: String) {
        mutate { current -> current.copy(meals = current.meals.filter { it.id != id }) }
    }

    fun addExercise(entry: ExerciseEntry) {
        mutate { it.copy(exercises = it.exercises + entry) }
    }

    fun deleteExercise(id: String) {
        mutate { current -> current.copy(exercises = current.exercises.filter { it.id != id }) }
    }

    fun addWater(ml: Int, day: LocalDate) {
        mutate {
            it.copy(
                water = it.water + WaterEntry(
                    date = Dates.mergedTimestamp(day),
                    milliliters = ml,
                ),
            )
        }
    }

    fun undoWater(day: LocalDate) {
        mutate { current ->
            val index = current.water.indexOfLast { Dates.isSameDay(it.date, day) }
            if (index == -1) {
                current
            } else {
                current.copy(water = current.water.filterIndexed { i, _ -> i != index })
            }
        }
    }

    fun logWeight(kg: Double, day: LocalDate = LocalDate.now()) {
        val rounded = (kg * 10).roundToInt() / 10.0
        mutate { current ->
            val index = current.weights.indexOfFirst { Dates.isSameDay(it.date, day) }
            val weights = current.weights.toMutableList()
            if (index >= 0) {
                weights[index] = weights[index].copy(kilograms = rounded)
            } else {
                weights.add(
                    WeightEntry(date = Dates.mergedTimestamp(day), kilograms = rounded),
                )
            }
            val latestDay = weights.maxOfOrNull { com.rork.calzyandroid.data.Dates.localDate(it.date) }
            val isLatest = latestDay == null || !day.isBefore(latestDay)
            current.copy(
                weights = weights,
                profile = if (isLatest) {
                    current.profile.copy(currentWeightKg = rounded)
                } else {
                    current.profile
                },
            )
        }
    }

    fun deleteWeight(id: String) {
        mutate { current -> current.copy(weights = current.weights.filter { it.id != id }) }
    }

    fun toggleSaved(title: String, items: List<FoodItem>, slot: MealSlot) {
        mutate { current ->
            val index = current.saved.indexOfFirst { it.title.equals(title, ignoreCase = true) }
            if (index >= 0) {
                current.copy(saved = current.saved.filterIndexed { i, _ -> i != index })
            } else {
                current.copy(
                    saved = current.saved + SavedFood(
                        id = uid(),
                        title = title,
                        items = items,
                        slot = slot,
                    ),
                )
            }
        }
    }

    fun deleteSaved(id: String) {
        mutate { current -> current.copy(saved = current.saved.filter { it.id != id }) }
    }

    fun addProgressPhoto(photo: String) {
        mutate { current ->
            current.copy(
                photos = current.photos + ProgressPhoto(
                    date = Dates.nowIso(),
                    photo = photo,
                    weightKg = current.profile.currentWeightKg,
                ),
            )
        }
    }

    fun deletePhoto(id: String) {
        mutate { current -> current.copy(photos = current.photos.filter { it.id != id }) }
    }

    fun eraseAll() {
        mutate { AppData() }
        _selectedDate.value = LocalDate.now()
    }
}
