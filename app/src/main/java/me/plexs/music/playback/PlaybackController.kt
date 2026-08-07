package me.plexs.music.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import me.plexs.music.MainActivity
import me.plexs.music.data.api.Http
import me.plexs.music.data.api.Song

object PlaybackController {

    @Volatile
    var session: MediaSession? = null
        private set

    @Volatile
    private var player: ExoPlayer? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing

    private val _hasItem = MutableStateFlow(false)
    val hasItem: StateFlow<Boolean> = _hasItem

private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue
    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle

    private val _repeat = MutableStateFlow(0)
    val repeat: StateFlow<Int> = _repeat

    private val _favorites = MutableStateFlow<List<Song>>(emptyList())
    val favorites: StateFlow<List<Song>> = _favorites

    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed

    @Volatile
    private var repo: me.plexs.music.data.api.UserDataRepository? = null

    @Volatile
    private var notifCallback: (() -> Unit)? = null

    fun attachNotificationCallback(cb: () -> Unit) {
        notifCallback = cb
    }

    fun detachNotificationCallback() {
        notifCallback = null
    }

    @Volatile
    private var contextRef: android.content.Context? = null

    @Volatile
    private var offlineRepo: me.plexs.music.data.offline.OfflineRepository? = null

    @Volatile
    private var playlistsStore: me.plexs.music.data.playlists.PlaylistStore? = null

    private val _playtime = MutableStateFlow(me.plexs.music.data.api.PlaytimeData())
    val playtime: StateFlow<me.plexs.music.data.api.PlaytimeData> = _playtime

    private val playCounts = java.util.concurrent.ConcurrentHashMap<String, Int>()

    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    val currentSong: Song?
        get() = _queue.value.getOrNull(_queueIndex.value)

    fun playSongs(context: android.content.Context, songs: List<Song>, index: Int) {
        if (songs.isEmpty()) return
        contextRef = context.applicationContext
        val (songs2, index2) = normalizeForShuffle(songs, index)
        _queue.value = songs2
        _queueIndex.value = index2
        playAt(context.applicationContext, index2)
    }

    /** Queues a list of songs to play immediately after the current track. */
    fun playNext(context: android.content.Context?, songs: List<Song>) {
        val q = _queue.value.toMutableList()
        if (q.isEmpty()) {
            // Nothing playing yet — just start the requested songs.
            contextRef = context?.applicationContext ?: contextRef
            _queue.value = songs.filter { it.id.isNotEmpty() }
            _queueIndex.value = 0
            _playing.value = false
            resolveAndPlay(contextRef ?: return, _queue.value[0])
            scheduleUserDataSave()
            return
        }
        val idx = _queueIndex.value.coerceAtLeast(0)
        val fresh = songs.filter { it.id.isNotEmpty() }
        q.addAll(idx + 1, fresh)
        contextRef = context?.applicationContext ?: contextRef
        _queue.value = q
        scheduleUserDataSave()
    }

    /** Appends songs to the end of the queue. */
    fun addToQueue(context: android.content.Context?, songs: List<Song>) {
        val q = _queue.value.toMutableList()
        val fresh = songs.filter { s -> s.id.isNotEmpty() && !q.any { it.id == s.id } }
        q.addAll(fresh)
        contextRef = context?.applicationContext ?: contextRef
        _queue.value = q
        scheduleUserDataSave()
    }

    /** Removes a queued song by id. */
    fun removeFromQueue(id: String) {
        val q = _queue.value.toMutableList()
        val rmIdx = q.indexOfFirst { it.id == id }
        if (rmIdx < 0) return
        q.removeAt(rmIdx)
        _queue.value = q
        if (rmIdx < _queueIndex.value) _queueIndex.value -= 1
        scheduleUserDataSave()
    }

    private fun normalizeForShuffle(songs: List<Song>, index: Int): Pair<List<Song>, Int> {
        if (!_shuffle.value || songs.size <= 1) return songs to index
        val list = songs.toMutableList()
        val first = list.removeAt(index)
        list.shuffle()
        list.add(0, first)
        return list to 0
    }

    fun playAtIndex(index: Int) {
        val q = _queue.value
        if (index < 0 || index >= q.size) return
        playAt(contextRef, index)
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
        notifCallback?.invoke()
    }

    fun cycleRepeat() {
        _repeat.value = (_repeat.value + 1) % 3
        player?.repeatMode = when (_repeat.value) {
            0 -> Player.REPEAT_MODE_OFF
            1 -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_ONE
        }
        notifCallback?.invoke()
    }

    fun isFavorited(id: String): Boolean = _favorites.value.any { it.id == id }

    fun toggleFavorite(song: Song) {
        val cur = _favorites.value.toMutableList()
        val idx = cur.indexOfFirst { it.id == song.id }
        if (idx >= 0) cur.removeAt(idx) else cur.add(song)
        _favorites.value = cur
        scheduleUserDataSave()
    }

    fun addFavorites(songs: List<Song>) {
        val cur = _favorites.value.toMutableList()
        for (s in songs) if (cur.none { it.id == s.id }) cur.add(s)
        _favorites.value = cur
        scheduleUserDataSave()
    }

    fun next() {
        playAt(contextRef, _queueIndex.value + 1)
    }

    fun previous() {
        if (_currentTime.value > 3000) {
            seekTo(0)
            return
        }
        playAt(contextRef, _queueIndex.value - 1)
    }

    private fun onPlaybackEnded() {
        if (_repeat.value == 2) {
            playAt(contextRef, _queueIndex.value)
            return
        }
        playAt(contextRef, _queueIndex.value + 1)
    }

    private fun playAt(ctx: android.content.Context?, index: Int) {
        val ctx2 = ctx ?: contextRef ?: return
        if (index < 0 || index >= _queue.value.size) return
        _queueIndex.value = index
        val song = _queue.value[index]
        recordRecentlyPlayed(song)
        _playing.value = false
        resolveAndPlay(ctx2, song)
    }

    /** Monotonic counter so a slow/stuck resolve can't override a newer song tap. */
    @Volatile
    private var playGeneration = 0L

    /** Resolves a playable URL then hands it to ExoPlayer. Offline → direct-on-device → proxy. */
    private fun resolveAndPlay(ctx: android.content.Context, song: Song) {
        val gen = ++playGeneration
        scope.launch {
            val url = resolveStreamUrl(song.id)
            if (gen != playGeneration) return@launch // a newer song was picked meanwhile
            play(ctx, url, song.title, song.artist)
        }
    }

    /**
     * Source priority: offline copy → on-device innertube (direct googlevideo, ~6s cap)
     * → worker stream relay (Cloudflare edge, reliable) → server proxy. The device
     * resolve is ~3s vs the cold proxy chain which can take multiple round-trips, so
     * this is the main "songs start fast" fix.
     */
    private suspend fun resolveStreamUrl(id: String): String {
        val offline = offlineRepo
        if (offline != null && offline.isDownloaded(id)) {
            val f = java.io.File(offline.offlineDir(), id + ".m4a")
            if (f.exists()) return f.toURI().toString()
        }
        val url = withTimeoutOrNull(6000) {
            directResolve(id)
        }
        if (url != null && url.isNotEmpty()) return url
        // Worker stream relay is the reliable fast path (Cloudflare edge, ~0.2s).
        return "https://plex-meta.urdonkey6.workers.dev/api/stream/" + id
    }

    private val resolver = me.plexs.music.innertube.InnertubeResolver()
    private var innertubeKey: String? = null
    private var innertubeClients: List<me.plexs.music.data.api.InnertubeClient>? = null

    /** Fetches innertube key/clients from config once (off-thread), never throwing. */
    private suspend fun ensureConfigLoaded() {
        if (innertubeKey != null && innertubeClients != null) return
        runCatching {
            val cfg = (contextRef?.applicationContext as? me.plexs.music.PlexApp)?.services?.config?.config()
            innertubeKey = cfg?.innertubeKey
            innertubeClients = cfg?.innertubeClients
        }
    }

    suspend fun directResolve(id: String): String? {
        ensureConfigLoaded()
        val key = innertubeKey ?: return null
        val clients = innertubeClients ?: return null
        if (clients.isEmpty()) return null
        return runCatching { resolver.resolve(id, key, clients)?.url }.getOrNull()
    }

    private fun recordRecentlyPlayed(song: Song) {
        val cur = _recentlyPlayed.value.toMutableList()
        cur.removeAll { it.id == song.id }
        cur.add(0, song)
        if (cur.size > 50) _recentlyPlayed.value = cur.subList(0, 50)
        else _recentlyPlayed.value = cur
        scheduleUserDataSave()
    }

    fun ensureSession(context: Context): MediaSession {
        session?.let { return it }
        val exo = ExoPlayer.Builder(context).build()
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playing.value = isPlaying
                notifCallback?.invoke()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onPlaybackEnded()
            }
        })
        player = exo
        widgetUpdaterJob()

        val activityIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val s = MediaSession.Builder(context, exo)
            .setSessionActivity(activityIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult {
                    // Expose shuffle & repeat as custom session commands so they appear
                    // as notification media buttons on supported Android versions.
                    val custom = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(SessionCommand(PlaybackService.CMD_SHUFFLE, Bundle.EMPTY))
                        .add(SessionCommand(PlaybackService.CMD_REPEAT, Bundle.EMPTY))
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(custom)
                        .setMediaButtonPreferences(listOf(shuffleButton(), repeatButton()))
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle,
                ): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        PlaybackService.CMD_SHUFFLE -> toggleShuffle()
                        PlaybackService.CMD_REPEAT -> cycleRepeat()
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            })
            .build()
        session = s
        ensureServiceStarted(context)
        return s
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun shuffleButton(): CommandButton {
        return CommandButton.Builder(if (_shuffle.value) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF)
            .setDisplayName("Shuffle")
            .setSessionCommand(SessionCommand(PlaybackService.CMD_SHUFFLE, Bundle.EMPTY))
            .setEnabled(true)
            .build()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun repeatButton(): CommandButton {
        return CommandButton.Builder(if (_repeat.value >= 2) CommandButton.ICON_REPEAT_ALL else CommandButton.ICON_REPEAT_ONE)
            .setDisplayName("Repeat")
            .setSessionCommand(SessionCommand(PlaybackService.CMD_REPEAT, Bundle.EMPTY))
            .setEnabled(true)
            .build()
    }

    fun play(context: Context, url: String, title: String, artist: String?) {
        val exo = player ?: run {
            ensureSession(context)
            player ?: return
        }
        ensureServiceStarted(context)
        preResolveNext(context)
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(currentSong?.thumbnail?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
        exo.setMediaItem(item)
        exo.prepare()
        exo.play()
        _hasItem.value = true
    }

    private val preResolved = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /** Backgrounds the on-device resolve for the next queued track so a skip is instant. */
    private fun preResolveNext(context: Context) {
        val q = _queue.value
        val next = q.getOrNull(_queueIndex.value + 1) ?: return
        val id = next.id
        if (id.isEmpty() || !preResolved.add(id)) return
        scope.launch(Dispatchers.IO) {
            val offline = offlineRepo
            val downloaded = offline != null && offline.isDownloaded(id)
            if (!downloaded) {
                directResolve(id) // cache innertubeKey/Clients + warm the innertube stream
                // Warm the server proxy too so a fallback is cached, not cold.
                runCatching { warmProxy(id) }
            }
        }
        while (preResolved.size > 64) preResolved.remove(preResolved.iterator().next())
    }

    /** Pokes the worker stream relay so its server-side resolution is cached before it's needed. */
    private fun warmProxy(id: String) {
        val req = okhttp3.Request.Builder()
            .url("https://plex-meta.urdonkey6.workers.dev/api/stream/" + id)
            .header("Range", "bytes=0-0")
            .header("Connection", "close")
            .build()
        Http.client.newCall(req).execute().use { }
    }

    fun playPause() {
        val exo = player ?: return
        if (exo.isPlaying) exo.pause() else exo.play()
        notifCallback?.invoke()
    }

    fun pause() {
        val exo = player ?: return
        if (exo.isPlaying) exo.pause()
        notifCallback?.invoke()
    }

    fun seekTo(ms: Long) {
        player?.seekTo(ms)
    }

    @Volatile
    private var updater: Job? = null

    private val countedPlays = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val autoDownloading = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private const val MIN_LISTEN_MS = 30000L

    private var statsRepo: me.plexs.music.data.api.StatsRepository? = null
    private var lastStatsMark = System.currentTimeMillis()
    private var lastSecondsReport = 0L
    private var pendingSeconds = 0L
    private var lastPlayReport = 0L

    private fun widgetUpdaterJob() {
        if (updater?.isActive == true) return
        updater = scope.launch {
            while (true) {
                val exo = player
                if (exo != null) {
                    _currentTime.value = exo.currentPosition
                    _duration.value = exo.duration
                    if (exo.isPlaying && exo.currentPosition >= MIN_LISTEN_MS) {
                        val song = currentSong
                        val id = song?.id
                        if (id != null && countedPlays.add(id)) {
                            offlineRepo?.recordPlay(id)
                            recordPlayCount(id)
                            maybeAutoDownload(song)
                            reportPlay(song)
                        }
                        accumulateStats()
                    }
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private fun reportPlay(song: Song) {
        val now = System.currentTimeMillis()
        if (now - lastPlayReport < 3000) return
        lastPlayReport = now
        val repo = statsRepo
        val r = repo ?: run {
            statsRepo = (contextRef as? android.content.Context)
                ?.let { (it.applicationContext as me.plexs.music.PlexApp).services.stats }
            statsRepo
        }
        r?.let { scope.launch { it.reportPlay(song) } }
    }

    private fun accumulateStats() {
        val now = System.currentTimeMillis()
        val step = now - lastStatsMark
        lastStatsMark = now
        if (step in 1..2000) pendingSeconds += step
        if (now - lastSecondsReport >= 60000 && pendingSeconds > 0) {
            lastSecondsReport = now
            val secs = pendingSeconds
            pendingSeconds = 0
            recordPlaytime(secs / 1000)
            val repo = statsRepo
            val r = repo ?: run {
                statsRepo = (contextRef as? android.content.Context)
                    ?.let { (it.applicationContext as me.plexs.music.PlexApp).services.stats }
                statsRepo
            }
            r?.let { scope.launch { it.reportSeconds(secs / 1000) } }
        }
    }

    private fun maybeAutoDownload(song: Song) {
        val offline = offlineRepo ?: return
        val id = song.id
        if (offline.isDownloaded(id)) return
        if (offline.playCount(id) < 3) return
        if (!autoDownloading.add(id)) return
        (contextRef?.applicationContext as? me.plexs.music.PlexApp)?.services?.downloads?.download(song)
    }

    fun release() {
        saveUserDataNow()
        playlistSyncJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        session?.release()
        player?.release()
        session = null
        player = null
        _playing.value = false
        _hasItem.value = false
    }

    fun restore(context: Context) {
        offlineRepo = (context.applicationContext as me.plexs.music.PlexApp).services.offline
        playlistsStore = (context.applicationContext as me.plexs.music.PlexApp).services.playlists
        // Warm the innertube config early so the first play doesn't burn its resolve
        // budget fetching app config for the first time.
        warmConfig()
        val s = me.plexs.music.data.session.SessionStore(context)
        if (!s.isSignedIn) return
        repo = me.plexs.music.data.api.UserDataRepository(s)
        repoSave?.cancel()
        repoSave = scope.launch {
            val d = repo?.fetch() ?: return@launch
            mergeServerData(d)
        }
        watchPlaylistWrites()
    }

    /** Loads the innertube key/clients off the main thread so they're ready before first play. */
    private fun warmConfig() {
        scope.launch(Dispatchers.IO) { ensureConfigLoaded() }
    }

    @Volatile
    private var playlistSyncJob: Job? = null

    private fun watchPlaylistWrites() {
        playlistsStore?.let { store ->
            playlistSyncJob?.cancel()
            playlistSyncJob = scope.launch {
                store.version.collect { scheduleUserDataSave() }
            }
        }
    }

    /** Debounced ~1s refresh: re-fetch and re-merge server data (favorites, recents, playlists, stats). */
    fun refresh(context: Context) {
        offlineRepo = (context.applicationContext as me.plexs.music.PlexApp).services.offline
        playlistsStore = (context.applicationContext as me.plexs.music.PlexApp).services.playlists
        val s = me.plexs.music.data.session.SessionStore(context)
        if (!s.isSignedIn) return
        repo = me.plexs.music.data.api.UserDataRepository(s)
        repoSave?.cancel()
        repoSave = scope.launch {
            delay(1000)
            val d = repo?.fetch() ?: return@launch
            mergeServerData(d)
        }
        watchPlaylistWrites()
    }

    /** Merges fetched server data, keeping non-empty local values when the server side is empty. */
    private fun mergeServerData(d: me.plexs.music.data.api.UserData) {
        if (d.favorites.isNotEmpty()) _favorites.value = d.favorites
        if (d.recentlyPlayed.isNotEmpty()) _recentlyPlayed.value = d.recentlyPlayed
        if (d.queue.isNotEmpty()) {
            _queue.value = d.queue
            _queueIndex.value = if (d.queueIndex in d.queue.indices) d.queueIndex else 0
        }
        playlistsStore?.syncFromServer(d.playlists)
        if (d.playtime.daily > 0 || d.playtime.monthly > 0) {
            val cur = _playtime.value
            _playtime.value = me.plexs.music.data.api.PlaytimeData(
                daily = maxOf(cur.daily, d.playtime.daily),
                monthly = maxOf(cur.monthly, d.playtime.monthly),
            )
        }
        d.play_counts.forEach { (id, c) -> playCounts.merge(id, c) { a, b -> maxOf(a, b) } }
    }

    @Volatile
    private var repoSave: Job? = null

    private fun scheduleUserDataSave() {
        repoSave?.cancel()
        repoSave = scope.launch {
            delay(1200)
            saveUserDataNow()
        }
    }

    private fun saveUserDataNow() {
        val r = repo ?: return
        val d = me.plexs.music.data.api.UserData(
            queue = _queue.value,
            queueIndex = _queueIndex.value,
            favorites = _favorites.value,
            recentlyPlayed = _recentlyPlayed.value,
            playlists = playlistsStore?.list()?.map {
                me.plexs.music.data.api.UserPlaylist(
                    id = it.id,
                    name = it.name,
                    image = it.image,
                    songs = it.songs,
                    createdAt = it.createdAt,
                )
            } ?: emptyList(),
            playtime = _playtime.value,
            play_counts = playCounts.toMap(),
        )
        if (!d.favorites.isEmpty() || !d.recentlyPlayed.isEmpty() || !d.queue.isEmpty() || !d.playlists.isEmpty() || d.playtime.daily > 0 || d.play_counts.isNotEmpty()) {
            scope.launch { r.save(d) }
        }
    }

    /** Accumulates listen time (seconds) into the local playtime counters. */
    fun recordPlaytime(seconds: Long) {
        if (seconds <= 0) return
        val cur = _playtime.value
        _playtime.value = me.plexs.music.data.api.PlaytimeData(
            daily = cur.daily + seconds,
            monthly = cur.monthly + seconds,
        )
        scheduleUserDataSave()
    }

    /** Increments the play count for a song (matches desktop play_counts). */
    fun recordPlayCount(id: String) {
        if (id.isEmpty()) return
        playCounts.merge(id, 1) { a, b -> a + b }
        scheduleUserDataSave()
    }

    private fun ensureServiceStarted(context: Context) {
        val intent = Intent(context, PlaybackService::class.java)
        context.startService(intent)
    }
}
