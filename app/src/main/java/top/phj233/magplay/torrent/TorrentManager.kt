package top.phj233.magplay.torrent

import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.MetadataReceivedAlert
import org.libtorrent4j.swig.settings_pack
import java.io.File

object TorrentManager {

    suspend fun magnetLinkParser(magnetLink: String): Result<MagPlayTorrentInfo> = withContext(Dispatchers.IO) {
        try {
            val deferred = CompletableDeferred<MagPlayTorrentInfo>()
            val session = SessionManager(true)

            val sp = SessionParams(SettingsPack().apply {
                setString(settings_pack.string_types.user_agent.swigValue(), "MagPlay/0.0.1")
                setBoolean(settings_pack.bool_types.announce_to_all_trackers.swigValue(), true)
                setBoolean(settings_pack.bool_types.dht_restrict_search_ips.swigValue(), false)
                setBoolean(settings_pack.bool_types.dht_restrict_routing_ips.swigValue(), false)
            })

            Log.d("TorrentParser", "magnetLink: $magnetLink")
            val updatedMagnet = mergeTracker(magnetLink, getTrackerList())
            Log.d("TorrentParser", "updatedMagnet: $updatedMagnet")

            session.addListener(object : AlertListener {
                override fun types(): IntArray? = null

                override fun alert(alert: Alert<*>?) {
                    when (alert?.type()) {
                        AlertType.METADATA_RECEIVED -> {
                            val th = (alert as MetadataReceivedAlert).handle()
                            val info = th.torrentFile()
                            val result = MagPlayTorrentInfo(
                                info.name(),
                                info.totalSize(),
                                (0 until info.numFiles()).map {
                                    TorrentFile(
                                        info.files().fileName(it),
                                        info.files().filePath(it),
                                        info.files().fileSize(it),
                                    )
                                },
                                info.infoHash().toString()
                            )
                            session.stop()
                            deferred.complete(result)
                        }
                        else -> {
                            Log.d("TorrentParser", "alert: ${alert?.type()}")
                        }
                    }
                }
            })

            session.start(sp)
            session.startDht()

            // 设置超时
            withTimeoutOrNull(30000) {
                Result.success(deferred.await())
            } ?: run {
                session.stop()
                Result.failure(Exception("解析超时"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getTrackerList():List<String> {
        val trackerList = mutableListOf<String>()
        val file = File(Environment.getExternalStoragePublicDirectory(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID
        ), "trackers.txt")
        if (!file.exists()) {
            // 使用ktor 请求下载https://cdn.jsdmirror.com/gh/XIU2/TrackersListCollection/best.txt
            // 并保存到 trackers.txt
            val client = HttpClient()
            val response = client.get("https://cdn.jsdmirror.com/gh/XIU2/TrackersListCollection/best.txt")
            response.bodyAsText().let {
                file.writeText(it)
            }
        }
        file.forEachLine {
            trackerList.add(it)
        }
        return trackerList
    }

    // 合并tracker 和 magnetLink
    fun mergeTracker(magnetLink: String, trackerList: List<String>): String {
        val tracker = trackerList.joinToString("&tr=") { it }
        return "$magnetLink&tr=$tracker"
    }
}