package com.simonslauncher.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simonslauncher.app.model.AppInfo
import com.simonslauncher.app.model.Brand
import com.simonslauncher.app.model.MockApps
import com.simonslauncher.app.model.Program
import com.simonslauncher.app.model.TvSource
import com.simonslauncher.app.model.Weather
import com.simonslauncher.app.util.AppLoader
import com.simonslauncher.app.util.RemoteArt
import com.simonslauncher.app.util.TvContent
import com.simonslauncher.app.util.TvSources
import com.simonslauncher.app.util.WeatherClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    // Last scan from disk: the row is complete on the very first frame, the real scan refines it.
    private val _allApps = MutableStateFlow(readCachedApps())
    /** Order chosen with "Sposta"; apps not in it keep the default place after these. */
    private val _order = MutableStateFlow(prefs.getString(ORDER_KEY, null).orEmpty().split('\n').filter { it.isNotEmpty() })
    private val _hidden = MutableStateFlow(prefs.getStringSet(HIDDEN_KEY, null).orEmpty().toSet())

    /** The app row: the user's order, else streaming apps first and the rest alphabetically. */
    val apps: StateFlow<List<AppInfo>> = combine(_allApps, _order, _hidden) { all, order, hidden ->
        arrange(all.filterNot { it.packageName in hidden }, order)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Apps in the saved order first. Apps installed later: a streaming brand slots in before the
     * first app of a later brand (e.g. a newly installed Disney+ lands before RaiPlay), anything else goes last.
     */
    private fun arrange(apps: List<AppInfo>, order: List<String>): List<AppInfo> {
        val position = order.withIndex().associate { (i, pkg) -> pkg to i }
        val (known, added) = apps.partition { it.packageName in position }
        val row = known.sortedBy { position.getValue(it.packageName) }.toMutableList()
        for (app in added) {
            val brand = app.brand
            val at = if (brand == null) -1 else row.indexOfFirst { (it.brand?.ordinal ?: -1) > brand.ordinal }
            if (at >= 0) row.add(at, app) else row.add(app)
        }
        return row
    }

    val hiddenApps: StateFlow<List<AppInfo>> = combine(_allApps, _hidden) { all, hidden ->
        all.filter { it.packageName in hidden }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Bumped when Home is pressed while the launcher is already open: back to the start. */
    private val _homePresses = MutableStateFlow(0)
    val homePresses: StateFlow<Int> = _homePresses

    fun onHomePressed() {
        _spotlightPackage.value = null
        _homePresses.value++
    }

    fun move(packageName: String, delta: Int) {
        val row = apps.value.map { it.packageName }.toMutableList()
        val from = row.indexOf(packageName)
        val to = from + delta
        if (from < 0 || to !in row.indices) return
        row.add(to, row.removeAt(from))
        _order.value = row
        prefs.edit().putString(ORDER_KEY, row.joinToString("\n")).apply()
    }

    fun hide(packageName: String) = saveHidden(_hidden.value + packageName)

    fun unhide(packageName: String) = saveHidden(_hidden.value - packageName)

    private fun saveHidden(hidden: Set<String>) {
        _hidden.value = hidden
        prefs.edit().putStringSet(HIDDEN_KEY, hidden).apply()
    }

    private val _sources = MutableStateFlow<List<TvSource>>(emptyList())
    val sources: StateFlow<List<TvSource>> = _sources

    fun openSource(source: TvSource) {
        runCatching { getApplication<Application>().startActivity(TvSources.intentFor(source)) }
    }

    /** Titles the apps publish on their preview channels, per package. */
    private val _programs = MutableStateFlow<Map<String, List<Program>>>(emptyMap())
    private val _spotlightPackage = MutableStateFlow<String?>(null)

    /**
     * The three titles in the spotlight: those of the app last focused in the row, or at start
     * the first title of each app, in the row order (DAZN, Netflix, Prime Video...).
     */
    val spotlight: StateFlow<List<Program>> = combine(_programs, _spotlightPackage) { programs, pkg ->
        pkg?.let { programs[it] }?.take(SPOTLIGHT_SIZE) ?: mixed(programs)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Ticks once a minute, only while Home is on screen. */
    val clock: StateFlow<String> = flow {
        while (true) {
            emit(formatTime())
            delay(MINUTE_MS - System.currentTimeMillis() % MINUTE_MS)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), formatTime())

    private var weatherFetchedAt = 0L
    private var lastWeather: Weather? = null

    /** Refreshed every 30 min while Home is on screen; returning after longer refreshes at once. */
    val weather: StateFlow<Weather?> = flow {
        lastWeather?.let { emit(it) }
        while (true) {
            val wait = weatherFetchedAt + WEATHER_REFRESH_MS - System.currentTimeMillis()
            if (wait > 0) delay(wait)
            val fresh = WeatherClient.fetch()
            if (fresh != null) {
                lastWeather = fresh
                weatherFetchedAt = System.currentTimeMillis()
                emit(fresh)
            } else {
                delay(WEATHER_RETRY_MS)
            }
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadApps()
        }
    }

    init {
        reloadApps()
        registerPackageReceiver()
        viewModelScope.launch(Dispatchers.IO) { RemoteArt.pruneDisk(application) }
    }

    /** Apps refresh their channels in background: re-read them every time Home comes back. */
    fun refreshContent() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            _sources.value = TvSources.load(app)
            _programs.value = TvContent.load(app).mapValues { (_, list) -> withRealArt(app, list) }
        }
    }

    fun hasContent(packageName: String): Boolean = !_programs.value[packageName].isNullOrEmpty()

    fun programsFor(packageName: String): List<Program> = _programs.value[packageName].orEmpty()

    /** Focusing an app that publishes titles shows them in the spotlight; others leave it as is. */
    fun focusApp(packageName: String) {
        if (hasContent(packageName)) _spotlightPackage.value = packageName
    }

    fun openProgram(program: Program) {
        val context = getApplication<Application>()
        val intent = TvContent.intentFor(program)
        val opened = intent != null && runCatching { context.startActivity(intent) }.isSuccess
        if (!opened) launchApp(program.packageName)
    }

    /** Drops logo-only art among the first titles of an app; the rest is never shown up front. */
    private fun withRealArt(context: Context, programs: List<Program>): List<Program> {
        val result = mutableListOf<Program>()
        for ((i, program) in programs.withIndex()) {
            if (result.size >= ART_CHECKED) return result + programs.drop(i)
            val url = program.artUrl ?: continue
            if (!RemoteArt.isPlaceholder(context, url)) result.add(program)
        }
        return result
    }

    private fun mixed(programs: Map<String, List<Program>>): List<Program> {
        val lists = Brand.entries.flatMap { brand -> brand.packages.mapNotNull { programs[it] } }.filter { it.isNotEmpty() }
        val result = mutableListOf<Program>()
        var round = 0
        while (result.size < SPOTLIGHT_SIZE && lists.any { round < it.size }) {
            lists.forEach { list -> if (round < list.size && result.size < SPOTLIGHT_SIZE) result.add(list[round]) }
            round++
        }
        return result
    }



    fun openSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { getApplication<Application>().startActivity(intent) }
    }

    fun launchApp(packageName: String) {
        val context = getApplication<Application>()
        AppLoader.getLaunchIntent(context, packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    fun launchBrand(brand: Brand) {
        val pkg = _allApps.value.firstOrNull { it.brand == brand }?.packageName ?: brand.packages.first()
        launchApp(pkg)
    }


    private fun reloadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = AppLoader.loadApps(getApplication())
            // Emulators ship almost no apps: show the mockup set so the layout can be checked.
            val apps = if (loaded.size < 4) MockApps.get() else loaded
            if (apps != _allApps.value) {
                _allApps.value = apps
                prefs.edit().putString(APPS_KEY, apps.joinToString("\n") { "${it.packageName}\t${it.label}" }).apply()
            }
            // Banners decoded ahead, so the row is complete when it first appears.
            apps.take(PREFETCHED_BANNERS).forEach { AppLoader.loadTileArt(getApplication(), it.packageName) }
        }
    }

    private fun readCachedApps(): List<AppInfo> =
        prefs.getString(APPS_KEY, null).orEmpty().lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 2) null else AppInfo(parts[0], parts[1])
        }.toList()

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        getApplication<Application>().registerReceiver(packageReceiver, filter)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(packageReceiver)
        } catch (_: Exception) {}
    }

    companion object {
        private const val APPS_KEY = "apps_v2"
        private const val ORDER_KEY = "order_v1"
        private const val HIDDEN_KEY = "hidden_v1"
        private const val PREFETCHED_BANNERS = 10
        private const val MINUTE_MS = 60_000L
        private const val SPOTLIGHT_SIZE = 3
        private const val ART_CHECKED = 6
        private const val WEATHER_REFRESH_MS = 30 * 60 * 1000L
        private const val WEATHER_RETRY_MS = 2 * 60 * 1000L

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALIAN)
        private fun formatTime(): String = timeFormat.format(Date())
    }
}
