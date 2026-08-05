package me.plexs.music.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
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
        _queue.value = songs
        _queueIndex.value = index
        playAt(context.applicationContext, index)
    }

    fun playAtIndex(index: Int) {
        val q = _queue.value
        if (index < 0 || index >= q.size) return
        playAt(contextRef, index)
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
        playAt(contextRef, _queueIndex.value + 1)
    }

    private fun playAt(ctx: android.content.Context?, index: Int) {
        val q = _queue.value
        val ctx2 = ctx ?: contextRef ?: return
        if (index < 0 || index >= q.size) return
        _queueIndex.value = index
        val song = q[index]
        val streamUrl = "https://music.plexs.me/api/embed/stream/" + song.id
        _playing.value = false
        play(ctx2, streamUrl, song.title, song.artist)
    }

    fun ensureSession(context: Context): MediaSession {
        session?.let { return it }
        val exo = ExoPlayer.Builder(context).build()
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playing.value = isPlaying
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
        startService(context)
        return s
    }

    fun play(context: Context, url: String, title: String, artist: String?) {
        val exo = player ?: run {
            ensureSession(context)
            player ?: return
        }
        startService(context)
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
        scope.coroutineContext[Job]?.cancel()
        session?.release()
        player?.release()
        session = null
        player = null
        _playing.value = false
        _hasItem.value = false
    }

    private fun startService(context: Context) {
        val intent = Intent(context, PlaybackService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }
}
