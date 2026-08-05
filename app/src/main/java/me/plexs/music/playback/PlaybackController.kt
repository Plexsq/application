package me.plexs.music.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.plexs.music.MainActivity
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
    }

    fun cycleRepeat() {
        _repeat.value = (_repeat.value + 1) % 3
        player?.repeatMode = when (_repeat.value) {
            0 -> Player.REPEAT_MODE_OFF
            1 -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_ONE
        }
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
        val q = _queue.value
        val ctx2 = ctx ?: contextRef ?: return
        if (index < 0 || index >= q.size) return
        _queueIndex.value = index
        val song = q[index]
        recordRecentlyPlayed(song)
        val streamUrl = "https://music.plexs.me/api/embed/stream/" + song.id
        _playing.value = false
        play(ctx2, streamUrl, song.title, song.artist)
    }

    private fun recordRecentlyPlayed(song: Song) {
        val cur = _recentlyPlayed.value.toMutableList()
        cur.removeAll { it.id == song.id }
        cur.add(0, song)
        if (cur.size > 50) cur.removeRange(50, cur.size)
        _recentlyPlayed.value = cur
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
            .build()
        session = s
        ensureServiceStarted(context)
        return s
    }

    fun play(context: Context, url: String, title: String, artist: String?) {
        val exo = player ?: run {
            ensureSession(context)
            player ?: return
        }
        ensureServiceStarted(context)
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

    private fun widgetUpdaterJob() {
        if (updater?.isActive == true) return
        updater = scope.launch {
            while (true) {
                val exo = player
                if (exo != null) {
                    _currentTime.value = exo.currentPosition
                    _duration.value = exo.duration
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun release() {
        saveUserDataNow()
        scope.coroutineContext[Job]?.cancel()
        session?.release()
        player?.release()
        session = null
        player = null
        _playing.value = false
        _hasItem.value = false
    }

    fun restore(context: Context) {
        val s = me.plexs.music.data.session.SessionStore(context)
        if (!s.isSignedIn) return
        repo = me.plexs.music.data.api.UserDataRepository(s)
        repoSave?.cancel()
        repoSave = scope.launch {
            val d = repo?.fetch() ?: return@launch
            if (d.favorites.isNotEmpty()) _favorites.value = d.favorites
            if (d.recentlyPlayed.isNotEmpty()) _recentlyPlayed.value = d.recentlyPlayed
            if (d.queue.isNotEmpty()) {
                _queue.value = d.queue
                _queueIndex.value = if (d.queueIndex in d.queue.indices) d.queueIndex else 0
            }
        }
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
        )
        if (!d.favorites.isEmpty() || !d.recentlyPlayed.isEmpty() || !d.queue.isEmpty()) {
            scope.launch { r.save(d) }
        }
    }

    private fun ensureServiceStarted(context: Context) {
        val intent = Intent(context, PlaybackService::class.java)
        context.startService(intent)
    }
}
