package top.phj233.magplay.repository.preferences

import com.tencent.mmkv.MMKV
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * 播放列表持久化管理类
 *
 * 使用MMKV存储播放列表信息，包括：
 * - 音乐文件URI
 * - 播放列表顺序
 * - 上次播放位置
 */
class PlaylistMMKV(private val mmkv: MMKV) {
    companion object {
        private const val KEY_PLAYLIST = "playlist"
        private const val KEY_LAST_TRACK_INDEX = "last_track_index"
        private const val KEY_LAST_POSITION = "last_position"
    }

    /**
     * 保存播放列表
     */
    fun savePlaylist(tracks: List<MusicTrack>) {
        try {
            val json = Json.encodeToString(tracks)
            mmkv.encode(KEY_PLAYLIST, json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 加载播放列表
     */
    fun loadPlaylist(): List<MusicTrack> {
        return try {
            val json = mmkv.decodeString(KEY_PLAYLIST)
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                Json.decodeFromString(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 保存最后播放的曲目索引
     */
    fun saveLastTrackIndex(index: Int) {
        mmkv.encode(KEY_LAST_TRACK_INDEX, index)
    }

    /**
     * 获取最后播放的曲目索引
     */
    fun getLastTrackIndex(): Int {
        return mmkv.decodeInt(KEY_LAST_TRACK_INDEX, 0)
    }

    /**
     * 保存最后播放位置
     */
    fun saveLastPosition(position: Long) {
        mmkv.encode(KEY_LAST_POSITION, position)
    }

    /**
     * 获取最后播放位置
     */
    fun getLastPosition(): Long {
        return mmkv.decodeLong(KEY_LAST_POSITION, 0L)
    }

    /**
     * 清除所有数据
     */
    fun clearAll() {
        mmkv.clearAll()
    }
}
@Serializable
data class MusicTrack(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long = 0L
)
