package top.phj233.magplay.torrent

import android.content.Context
import org.koin.core.context.GlobalContext
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.swig.settings_pack
import top.phj233.magplay.repository.preferences.SettingsMMKV

/**
 * Torrent会话管理类
 * 负责创建和管理libtorrent会话，处理DHT和其他设置
 */
class TorrentSession {
    private val context by lazy { GlobalContext.get().get<Context>() }
    private val settingsMMKV by lazy { GlobalContext.get().get<SettingsMMKV>() }
    private var session: SessionManager? = null

    /**
     * 创建并配置一个新的会话
     */
    fun createSession(): SessionManager {
        if (session == null) {
            val settings = SettingsPack().apply {
                setString(settings_pack.string_types.user_agent.swigValue(), "MagPlay/0.0.1")
                setBoolean(settings_pack.bool_types.enable_dht.swigValue(), true)
                setBoolean(settings_pack.bool_types.enable_lsd.swigValue(), true)
                setBoolean(settings_pack.bool_types.enable_upnp.swigValue(), true)
                setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(), true)
                val dhtNodes = listOf(
                    "router.bittorrent.com:6881",
                    "router.utorrent.com:6881",
                    "router.bitcomet.com:6881",
                    "dht.transmissionbt.com:6881",
                    "dht.aelitis.com:6881",
                    "dht.libtorrent.org:25401",
                )
                setString(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), dhtNodes.joinToString(","))
                setBoolean(settings_pack.bool_types.dht_extended_routing_table.swigValue(), true)
                setBoolean(settings_pack.bool_types.announce_to_all_tiers.swigValue(), true)
                setBoolean(settings_pack.bool_types.announce_to_all_trackers.swigValue(), true)

            }
            session = SessionManager(true).apply {
                start(SessionParams(settings))
            }
            session?.apply {
                postDhtStats()
                postSessionStats()
                postTorrentUpdates()
            }
        }
        return session!!
    }

    /**
     * 添加警报监听器
     */
    fun addListener(listener: AlertListener) {
        session?.addListener(listener)
    }

    /**
     * 移除警报监听器
     */
    fun removeListener(listener: AlertListener) {
        session?.removeListener(listener)
    }

    /**
     * 获取种子句柄
     */
    fun getHandle(infoHash: Sha1Hash): TorrentHandle? {
        return session?.find(infoHash)
    }

    /**
     * 停止会话
     */
    fun stop() {
        session?.stop()
        session = null
    }

    companion object {
        @Volatile
        private var instance: TorrentSession? = null

        fun getInstance(): TorrentSession {
            return instance ?: synchronized(this) {
                instance ?: TorrentSession().also { instance = it }
            }
        }
    }
}
