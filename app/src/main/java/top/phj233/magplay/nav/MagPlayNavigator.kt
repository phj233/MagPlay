package top.phj233.magplay.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import top.phj233.magplay.nav.MagPlayNavRoutes.MAGNET_PARSE
import top.phj233.magplay.nav.MagPlayNavRoutes.MAIN
import top.phj233.magplay.nav.MagPlayNavRoutes.MAIN_WORK
import top.phj233.magplay.nav.MagPlayNavRoutes.SETTINGS
import top.phj233.magplay.nav.MagPlayNavRoutes.WORK_CALCULATE
import top.phj233.magplay.nav.MagPlayNavRoutes.WORK_CONTACTS
import top.phj233.magplay.nav.MagPlayNavRoutes.WORK_CONTACTS_CREATE
import top.phj233.magplay.nav.MagPlayNavRoutes.WORK_CONTACTS_SEARCH
import top.phj233.magplay.nav.MagPlayNavRoutes.WORK_MUSIC_PLAYER
import top.phj233.magplay.ui.screens.magnet.ParsePage
import top.phj233.magplay.ui.screens.main.Main
import top.phj233.magplay.ui.screens.main.WorkScreen
import top.phj233.magplay.ui.screens.work.caculate.Calculate
import top.phj233.magplay.ui.screens.work.contact.screen.ContactCreateScreen
import top.phj233.magplay.ui.screens.work.contact.screen.ContactListScreen
import top.phj233.magplay.ui.screens.work.contact.screen.ContactSearchScreen
import top.phj233.magplay.ui.screens.work.music_player.MusicPlayerScreen
import top.phj233.magplay.ui.settings.SettingsPage

val LocalNavController = staticCompositionLocalOf<NavHostController>{
    error("No NavHostController provided")
}

fun NavController.navMain(){
    navigate(MAIN)
}

fun NavController.navCalculate(){
    navigate(WORK_CALCULATE)
}

fun NavController.navContacts(){
    navigate(WORK_CONTACTS)
}

fun NavController.navContactsSearch(){
    navigate(WORK_CONTACTS_SEARCH)
}

fun NavController.navContactCreate(){
    navigate(WORK_CONTACTS_CREATE)
}

fun NavController.navMusicPlayer(){
    navigate(WORK_MUSIC_PLAYER)
}

fun NavController.navSetting(page: String){
    navigate("${SETTINGS}/${page}")
}

fun NavController.navParse(magnetLink: String){
    navigate("$MAGNET_PARSE/$magnetLink")
}

fun NavController.navigateUp(){
    //返回上一页
    popBackStack()
}


@Composable
fun MagPlayNavHost(){
    val navController = rememberNavController()
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(
            navController = navController,
            startDestination = MAIN
        ) {
            composable(MAIN) {
                Main()
            }

            composable(MAIN_WORK) {
                WorkScreen()
            }

            composable(WORK_CALCULATE) {
                Calculate()
            }

            composable(WORK_CALCULATE) {
                Calculate()
            }

            composable(WORK_CONTACTS) {
                ContactListScreen()
            }

            composable(WORK_CONTACTS_SEARCH) {
                ContactSearchScreen()
            }

            composable(WORK_CONTACTS_CREATE) {
                ContactCreateScreen()
            }

            composable(WORK_MUSIC_PLAYER) {
                MusicPlayerScreen()
            }

            composable("${SETTINGS}/{page}") { backStackEntry ->
                val page = backStackEntry.arguments?.getString("page")
                when (page) {
                    "theme" -> {
                        SettingsPage.ThemeSetting()
                    }
                }
            }
            composable("$MAGNET_PARSE/{magnetLink}") { backStackEntry ->
                val magnetLink = backStackEntry.arguments?.getString("magnetLink")
                requireNotNull(magnetLink) { "磁力链接不能为空" }
                ParsePage(magnetLink)
            }
        }
    }
}