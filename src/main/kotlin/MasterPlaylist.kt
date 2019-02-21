data class PlaylistMetadata(val programId: String, val bandwidth: String, val resolution: String)

class MasterPlaylist(val version: Number, val metadataMap: Map<PlaylistMetadata, String>) : Playlist() {
    override fun synthesize(): String {
        return "wip"
    }
}