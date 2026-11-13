package com.tvfut.app

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tvfut.app.databinding.ActivityDirectPlayerBinding

class DirectPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDirectPlayerBinding
    private var player: ExoPlayer? = null
    private var mediaUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDirectPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mediaUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (mediaUrl.isBlank()) {
            finish()
            return
        }

        binding.title.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.btnClose.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener {
            binding.errorBox.visibility = View.GONE
            startPlayback()
        }
        binding.btnErrorClose.setOnClickListener { finish() }
        binding.errorBox.visibility = View.GONE

        startPlayback()
    }

    private fun startPlayback() {
        releasePlayer()

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        binding.playerView.player = exoPlayer

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    binding.errorBox.visibility = View.GONE
                }
                updateLive(exoPlayer)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateLive(exoPlayer)
            }

            override fun onPlayerError(error: PlaybackException) {
                binding.errorBox.visibility = View.VISIBLE
            }

            override fun onEvents(player: Player, events: Player.Events) {
                updateLive(player)
            }
        })

        try {
            exoPlayer.setMediaItem(MediaItem.fromUri(mediaUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (_: Exception) {
            binding.errorBox.visibility = View.VISIBLE
        }
    }

    private fun updateLive(exoPlayer: Player) {
        binding.liveBadge.visibility =
            if (exoPlayer.isCurrentMediaItemLive) View.VISIBLE else View.GONE
    }

    private fun releasePlayer() {
        binding.playerView.player = null
        player?.release()
        player = null
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }
}
