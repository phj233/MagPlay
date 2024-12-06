 package top.phj233.magplay.di

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.tencent.mmkv.MMKV
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import top.phj233.magplay.repository.preferences.PlaylistMMKV
import top.phj233.magplay.repository.preferences.SettingsMMKV
import top.phj233.magplay.ui.screens.storage.StorageViewModel
import top.phj233.magplay.ui.screens.work.music_player.MusicPlayerViewModel

/**
 * 应用程序依赖注入模块
 *
 * 负责提供应用程序级别的依赖项，包括：
 * - ExoPlayer实例
 * - MediaSession实例
 * - ViewModel实例
 * - 其他全局依赖项
 *
 * 主要组件：
 * - ExoPlayer：配置音频播放属性
 * - MediaSession：配置媒体会话
 * - ViewModel：提供各个页面的ViewModel
 *
 * @author phj233
 */
@UnstableApi
val appModule = module {
    // MMKV实例配置
    single { 
        MMKV.defaultMMKV()
    }
    
    singleOf(::SettingsMMKV)
    singleOf(::PlaylistMMKV)
    viewModelOf(::StorageViewModel)

    /**
     * ExoPlayer单例配置
     * 配置音频属性和使用场景
     */
    single {
        ExoPlayer.Builder(androidContext())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .build()
    }

    /**
     * MediaSession单例配置
     * 配置媒体会话ID和ExoPlayer实例
     */
    single {
        MediaSession.Builder(androidContext(), get<ExoPlayer>())
            .setId("MagPlaySession")
            .build()
    }

    viewModel { MusicPlayerViewModel() }
}