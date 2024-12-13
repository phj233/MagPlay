package top.phj233.magplay.torrent

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.MetadataReceivedAlert
import org.libtorrent4j.swig.settings_pack
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

object TorrentManager {
    private val context by lazy { GlobalContext.get().get<Context>() }

    suspend fun magnetLinkParser(magnetLink: String): Result<MagPlayTorrentInfo> = withContext(Dispatchers.IO) {
        val session = SessionManager(true)
        
        try {
            val sp = SessionParams(SettingsPack().apply {
                setString(settings_pack.string_types.user_agent.swigValue(), "MagPlay/0.0.1")
                setBoolean(settings_pack.bool_types.enable_dht.swigValue(), true)
                setBoolean(settings_pack.bool_types.enable_lsd.swigValue(), true)
                setBoolean(settings_pack.bool_types.enable_upnp.swigValue(), true)
                setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(), true)
                
                // DHT设置
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
            })

            Log.d("TorrentParser", "Starting session with magnetLink: $magnetLink")
            val updatedMagnet = mergeTracker(URLDecoder.decode(magnetLink,"UTF-8"), getTrackerList())
            Log.d("TorrentParser", "Updated magnet with trackers: $updatedMagnet")

            callbackFlow {
                val listener = object : AlertListener {
                    override fun types(): IntArray? = null

                    override fun alert(alert: Alert<*>?) {
                        when (alert?.type()) {
                            AlertType.METADATA_RECEIVED -> {
                                try {
                                    Log.d("TorrentParser", "METADATA_RECEIVED")
                                    val th = (alert as MetadataReceivedAlert).handle()
                                    val info = th.torrentFile()
                                    Log.d("TorrentParser", "Torrent info: $info")
                                    trySend(Result.success(MagPlayTorrentInfo(
                                        name = info.name(),
                                        totalSize = info.totalSize(),
                                        numFiles = info.numFiles(),
                                        files = (0 until info.numFiles()).map {
                                            TorrentFile(
                                                name = info.files().fileName(it),
                                                path = info.files().filePath(it),
                                                size = info.files().fileSize(it),
                                            )
                                        },
                                        infoHash = info.infoHash().toHex(),
                                    )))
                                    close()
                                } catch (e: Exception) {
                                    Log.e("TorrentParser", "Error handling metadata", e)
                                    trySend(Result.failure(e))
                                    close(e)
                                }
                            }
                            AlertType.DHT_BOOTSTRAP -> {
                                Log.d("TorrentParser", "DHT Bootstrap")
                            }
                            AlertType.DHT_ERROR -> {
                                Log.e("TorrentParser", "DHT Error: ${alert.message()}")
                            }
                            AlertType.LISTEN_SUCCEEDED -> {
                                Log.d("TorrentParser", "Listen Succeeded")
                            }
                            AlertType.LISTEN_FAILED -> {
                                Log.e("TorrentParser", "Listen Failed: ${alert.message()}")
                            }
                            else -> {

                            }
                        }
                    }
                }

                session.addListener(listener)
                session.start(sp)
                session.startDht()
                session.fetchMagnet(updatedMagnet, 10000, File(context.filesDir, "torrents"))

                awaitClose {
                    session.removeListener(listener)
                    session.stop()
                }
            }.firstOrNull() ?: Result.failure(Exception("解析超时"))

        } catch (e: Exception) {
            Log.e("TorrentParser", "Error parsing magnet: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun getTrackerList(): List<String> {
        val trackerFile = File(context.filesDir, "trackers.txt")
        val trackerList = mutableListOf<String>()

        try {
            if (!trackerFile.exists()) {
                // 使用ktor 请求下载https://cdn.jsdmirror.com/gh/XIU2/TrackersListCollection/best.txt
                // 并保存到 trackers.txt
                val client = HttpClient()
                val response = client.get("https://cdn.jsdmirror.com/gh/XIU2/TrackersListCollection/best.txt")
                response.bodyAsText().let {
                    trackerFile.writeText(it)
                }
                client.close()
            }

            trackerFile.forEachLine { line ->
                if (line.isNotBlank()) {
                    trackerList.add(line)
                }
            }
        } catch (e: Exception) {
            Log.e("TorrentManager", "Error getting tracker list: ${e.message}")
            // 如果发生错误，使用默认tracker列表
            trackerList.addAll(listOf(
                "udp://tracker.opentrackr.org:1337/announce",
                "udp://open.tracker.cl:1337/announce",
                "udp://tracker.openbittorrent.com:6969/announce",
                "http://tracker.openbittorrent.com:80/announce",
                "udp://opentracker.i2p.rocks:6969/announce",
                "https://opentracker.i2p.rocks:443/announce"
            ))
        }

        return trackerList
    }

    // 合并tracker 和 magnetLink
    private fun mergeTracker(magnetLink: String, trackerList: List<String>): String {
        if (trackerList.isEmpty()) return magnetLink
        val tracker = trackerList.joinToString("&tr=") { URLEncoder.encode(it, "UTF-8") }
        return "$magnetLink&tr=$tracker"
    }
}