package me.plexs.music.ui.navigation

object Destinations {
    const val SPLASH = "splash"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val FORGOT = "forgot"
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIKED = "liked"
    const val RECENTS = "recents"
    const val SETTINGS = "settings"
    const val NOW_PLAYING = "now_playing"

    val CATEGORIES = listOf(HOME, SEARCH, LIKED, RECENTS, SETTINGS)
}