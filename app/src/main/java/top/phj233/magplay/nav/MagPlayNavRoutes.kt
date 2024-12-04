package top.phj233.magplay.nav

import kotlinx.serialization.Serializable

@Serializable
object MagPlayNavRoutes {
    const val MAIN = "main"
    const val MAIN_WORK = "main/work"

    const val WORK_CALCULATE = "main/work/calculate"
    const val WORK_CONTACTS = "main/work/contacts"
    const val WORK_CONTACTS_SEARCH = "main/work/contacts/search"
    const val WORK_CONTACTS_CREATE = "main/work/contacts/create"
    const val WORK_MUSIC_PLAYER = "main/work/music_player"

    const val SETTINGS = "settings"

    const val MAGNET_PARSE = "magnet/parse"
}