package com.imn.iman.ui.screens.quran

import android.app.Application

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

import androidx.lifecycle.viewModelScope

import com.imn.iman.data.model.AyahBlock
import com.imn.iman.data.model.ContentBlock
import com.imn.iman.R
import com.imn.iman.data.model.ReciterMeta
import com.imn.iman.i18n.LocaleHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

class QuranAudioViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val AYAH_LOADING_TIMEOUT_MS = 15_000L
    }

    sealed interface AudioState {
        data object Idle : AudioState
        data object Loading : AudioState
        data object Playing : AudioState
        data object Paused : AudioState
        data class Error(val msg: String) : AudioState
    }

    val audioState = mutableStateOf<AudioState>(AudioState.Idle)
    val currentSurah = mutableStateOf<Int?>(null)
    val playingAyahIndex = mutableStateOf<Int?>(null)
    val currentAyahKey = mutableStateOf<String?>(null)

    private fun audioError() = AudioState.Error(
        LocaleHelper.wrap(getApplication<Application>()).getString(R.string.quran_audio_error)
    )

    private var mediaController: MediaController? = null
    private var ayahLoadingTimeoutJob: Job? = null
    private var ayahLoadAttempt = 0L

    fun setController(controller: MediaController?) {
        this.mediaController = controller
        controller?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> audioState.value = AudioState.Loading
                    Player.STATE_READY -> {
                        cancelAyahLoadingTimeout()
                        audioState.value = AudioState.Playing
                    }

                    Player.STATE_ENDED -> handleAyahCompletion()
                    Player.STATE_IDLE -> {
                        if (audioState.value !is AudioState.Error) {
                            audioState.value = AudioState.Idle
                            currentAyahKey.value = null
                        }
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    cancelAyahLoadingTimeout()
                    audioState.value = AudioState.Playing
                } else if (audioState.value !is AudioState.Error) {
                    audioState.value = AudioState.Paused
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                cancelAyahLoadingTimeout()
                audioState.value = audioError()
            }
        })
    }

    private var currentBlocks: List<ContentBlock>? = null
    private var isSequenceMode: Boolean = false
    private var currentReciterId: String? = null

    private fun getAyahUrl(surahNumber: Int, ayahIndex: Int, blocks: List<ContentBlock>, reciterId: String): String? {
        val block = blocks.getOrNull(ayahIndex) as? AyahBlock ?: return null
        val reciters = ReciterMeta.everyayahDefaults()
        val reciter = reciters.firstOrNull { it.id == reciterId }
            ?: reciters.firstOrNull { it.id == "everyayah_$reciterId" }
            ?: reciters.first()
        val paddedSurah = surahNumber.toString().padStart(3, '0')
        val paddedAyah = block.ayah.toString().padStart(3, '0')

        currentReciterId = reciter.id
        return reciter.audioUrlPattern
            .replace("{surah_3}", paddedSurah)
            .replace("{ayah_3}", paddedAyah)
    }

    fun playAyah(
        surahNumber: Int,
        ayahIndex: Int,
        blocks: List<ContentBlock>,
        reciterId: String,
        sequence: Boolean
    ) {
        stop()

        currentBlocks = blocks
        isSequenceMode = sequence
        playingAyahIndex.value = ayahIndex
        val ayah = blocks.getOrNull(ayahIndex) as? AyahBlock
        currentAyahKey.value = ayah?.let { "$surahNumber:${it.ayah}" }
        currentReciterId = reciterId

        val url = getAyahUrl(surahNumber, ayahIndex, blocks, reciterId) ?: return
        playInternal(surahNumber, url)
    }

    private fun playInternal(surahNumber: Int, dataSource: String) {
        currentSurah.value = surahNumber
        audioState.value = AudioState.Loading

        val controller = mediaController ?: run {
            audioState.value = audioError()
            return
        }
        try {
            startAyahLoadingTimeout(controller)
            controller.setMediaItem(MediaItem.fromUri(dataSource))
            controller.prepare()
            controller.play()
        } catch (_: Exception) {
            cancelAyahLoadingTimeout()
            audioState.value = audioError()
        }
    }

    private fun startAyahLoadingTimeout(controller: MediaController) {
        cancelAyahLoadingTimeout()
        val attempt = ayahLoadAttempt
        ayahLoadingTimeoutJob = viewModelScope.launch {
            delay(AYAH_LOADING_TIMEOUT_MS)
            if (attempt == ayahLoadAttempt && audioState.value == AudioState.Loading) {
                audioState.value = audioError()
                controller.stop()
            }
        }
    }

    private fun cancelAyahLoadingTimeout() {
        ayahLoadAttempt++
        ayahLoadingTimeoutJob?.cancel()
        ayahLoadingTimeoutJob = null
    }

    private fun handleAyahCompletion() {
        if (isSequenceMode) {
            val currentIdx = playingAyahIndex.value ?: -1
            val nextIdx = findNextAyahIndex(currentIdx + 1)
            playingAyahIndex.value = nextIdx

            if (nextIdx != null) {
                val surahNum = currentSurah.value ?: return
                val reciterId = currentReciterId ?: return
                val blocks = currentBlocks ?: return
                val nextAyah = blocks.getOrNull(nextIdx) as? AyahBlock
                currentAyahKey.value = nextAyah?.let { "$surahNum:${it.ayah}" }
                val nextUrl = getAyahUrl(surahNum, nextIdx, blocks, reciterId) ?: return

                val controller = mediaController ?: return
                startAyahLoadingTimeout(controller)
                controller.setMediaItem(MediaItem.fromUri(nextUrl))
                controller.prepare()
                controller.play()
            } else {
                stop()
            }
        } else {
            stop()
        }
    }

    private fun findNextAyahIndex(startIndex: Int): Int? {
        val blocks = currentBlocks ?: return null
        if (startIndex < 0 || startIndex >= blocks.size) return null
        for (i in startIndex until blocks.size) {
            if (blocks[i] is AyahBlock) return i
        }
        return null
    }



    fun stopAyah() {
        cancelAyahLoadingTimeout()
        mediaController?.stop()
        audioState.value = AudioState.Idle
        playingAyahIndex.value = null
        currentSurah.value = null
        currentAyahKey.value = null
    }

    fun stop() = stopAyah()

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
