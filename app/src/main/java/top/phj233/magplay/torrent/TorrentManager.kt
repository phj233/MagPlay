package top.phj233.magplay.torrent

import android.content.Context
import android.net.Uri
import android.os.Environment
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
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.MetadataReceivedAlert
import org.libtorrent4j.alerts.TorrentLogAlert
import org.libtorrent4j.swig.torrent_flags_t
import top.phj233.magplay.repository.preferences.SettingsMMKV
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

object TorrentManager {
    private val context by lazy { GlobalContext.get().get<Context>() }
    private val torrentSession by lazy { TorrentSession.getInstance() }


    suspend fun magnetLinkParser(magnetLink: String): Result<MagPlayTorrentInfo> = withContext(Dispatchers.IO) {
        try {
            Log.d("TorrentParser", "开始解析磁力链接: $magnetLink")
            val updatedMagnet = mergeTracker(URLDecoder.decode(magnetLink,"UTF-8"), getTrackerList())
            Log.d("TorrentParser", "更新后的磁力链接: $updatedMagnet")

            callbackFlow {
                val listener = object : AlertListener {
                    override fun types(): IntArray? = null

                    override fun alert(alert: Alert<*>?) {
                        when (alert?.type()) {
                            AlertType.METADATA_RECEIVED -> {
                                try {
                                    Log.d("TorrentParser", "收到元数据")
                                    val th = (alert as MetadataReceivedAlert).handle()
                                    val info = th.torrentFile()
                                    Log.d("TorrentParser", "种子信息: $info")
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
                                    Log.e("TorrentParser", "处理元数据时出错", e)
                                    trySend(Result.failure(e))
                                    close(e)
                                }
                            }
                            AlertType.DHT_BOOTSTRAP -> {
                                Log.d("TorrentParser", "DHT启动")
                            }
                            AlertType.DHT_ERROR -> {
                                Log.e("TorrentParser", "DHT错误: ${alert.message()}")
                            }
                            AlertType.LISTEN_SUCCEEDED -> {
                                Log.d("TorrentParser", "监听成功")
                            }
                            AlertType.LISTEN_FAILED -> {
                                Log.e("TorrentParser", "监听失败: ${alert.message()}")
                            }
                            else -> {
                                // 其他警报类型的处理
                            }
                        }
                    }
                }

                val session = torrentSession.createSession()
                torrentSession.addListener(listener)
                session.fetchMagnet(updatedMagnet, 10000, File(Environment.DIRECTORY_DOWNLOADS, "torrents"))

                awaitClose {
                    torrentSession.removeListener(listener)
                }
            }.firstOrNull() ?: Result.failure(Exception("解析超时"))

        } catch (e: Exception) {
            Log.e("TorrentParser", "解析磁力链接时出错: ${e.message}", e)
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

    private fun mergeTracker(magnetLink: String, trackerList: List<String>): String {
        if (trackerList.isEmpty()) return magnetLink
        val tracker = trackerList.joinToString("&tr=") { URLEncoder.encode(it, "UTF-8") }
        return "$magnetLink&tr=$tracker"
    }

    fun getDownloadDirectory(): File {
        val storagePath = SettingsMMKV().getStoragePath()
        return if (storagePath != null) {
            val uri = Uri.parse(storagePath)
            when {
                uri.path?.startsWith("primary:") == true -> {
                    // 处理 primary: 开头的路径
                    val relativePath = uri.path?.substringAfter("primary:")
                    val externalStorage = Environment.getExternalStorageDirectory()
                    File(externalStorage, relativePath ?: "")
                }
                uri.path?.startsWith("/tree/primary:") == true -> {
                    // 处理 /tree/primary: 开头的路径
                    val relativePath = uri.path?.substringAfter("/tree/primary:")
                    val externalStorage = Environment.getExternalStorageDirectory()
                    File(externalStorage, relativePath ?: "")
                }
                else -> {
                    throw IllegalArgumentException("Unsupported storage path: $storagePath")
                }
            }
        } else {
            throw IllegalArgumentException("Storage path is null")
        }
    }

    /**
     * 下载种子文件
     * @param magnetUrl 磁力链接
     * @param fileIndex 要下载的文件索引
     * @param onProgress 下载进度回调，范围0-100
     * @param onSpeed 下载速度回调，(下载速度, 上传速度)，单位bytes/s
     */
    suspend fun downloadTorrentFile(
        magnetUrl: String,
        fileIndex: Int,
        onProgress: (Float) -> Unit = {},
        onSpeed: (Long, Long) -> Unit = { _, _ -> }
    ) {
        val newMagnet = mergeTracker(URLDecoder.decode(magnetUrl,"UTF-8"), getTrackerList())
        Log.d("TorrentDownload", "开始下载种子文件: $newMagnet, fileIndex: $fileIndex")
        val session = torrentSession.createSession()
        
        // 添加下载监听器
        val listener = object : AlertListener {
            override fun types(): IntArray? = null

            override fun alert(alert: Alert<*>?) {
                when (alert) {
                    is TorrentLogAlert -> {
                        Log.d("TorrentDownload", "收到 ${alert.type()}")
                        val th = alert.handle()
                        val status = th.status()
                        val progress = status.progress()
                        val downloadRate = status.downloadPayloadRate()
                        val uploadRate = status.uploadPayloadRate()
                        Log.d("TorrentDownload", "进度: ${progress * 100}%, 下载: $downloadRate bytes/s, 上传: $uploadRate bytes/s")
                        onProgress(progress * 100)
                        onSpeed(downloadRate.toLong(), uploadRate.toLong())
                    }
                    else -> {
                        // 其他警报类型暂不处理
                    }
                }
            }
        }
        torrentSession.addListener(listener)
        session.download(newMagnet, getDownloadDirectory(), torrent_flags_t())
    }
}