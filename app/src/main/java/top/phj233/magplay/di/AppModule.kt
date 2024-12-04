 package top.phj233.magplay.di

import androidx.media3.exoplayer.ExoPlayer
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import top.phj233.magplay.repository.preferences.SettingsMMKV
import top.phj233.magplay.ui.screens.storage.StorageViewModel
import top.phj233.magplay.ui.screens.work.music_player.MusicPlayerViewModel

 val appModule = module {
    singleOf(::SettingsMMKV)
    viewModelOf(::StorageViewModel)
    single { ExoPlayer.Builder(androidContext()).build() }
    viewModelOf(::MusicPlayerViewModel)
}