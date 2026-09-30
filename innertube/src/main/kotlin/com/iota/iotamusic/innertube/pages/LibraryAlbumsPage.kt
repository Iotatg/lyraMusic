/*
 * IotaMusic Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.iota.iotamusic.innertube.pages

import com.iota.iotamusic.innertube.models.Album
import com.iota.iotamusic.innertube.models.AlbumItem
import com.iota.iotamusic.innertube.models.Artist
import com.iota.iotamusic.innertube.models.ArtistItem
import com.iota.iotamusic.innertube.models.MusicResponsiveListItemRenderer
import com.iota.iotamusic.innertube.models.MusicTwoRowItemRenderer
import com.iota.iotamusic.innertube.models.PlaylistItem
import com.iota.iotamusic.innertube.models.SongItem
import com.iota.iotamusic.innertube.models.YTItem
import com.iota.iotamusic.innertube.models.oddElements
import com.iota.iotamusic.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
            val playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                ?.watchPlaylistEndpoint?.playlistId
                ?: renderer.menu?.menuRenderer?.items?.firstOrNull()
                    ?.menuNavigationItemRenderer?.navigationEndpoint
                    ?.watchPlaylistEndpoint?.playlistId
                ?: browseId.removePrefix("MPREb_").let { "OLAK5uy_$it" }

            return AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                artists = null,
                year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.subtitleBadges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null
            )
        }
    }
}
