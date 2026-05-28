package com.example

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Music entity
data class Song(
    val trackId: Long,
    val trackName: String?,
    val artistName: String?,
    val previewUrl: String?,
    val artworkUrl100: String?
)

data class SaavnResponse(
    val success: Boolean,
    val data: SaavnData
)

data class SaavnData(
    val results: List<SaavnSong>
)

data class SaavnSong(
    val id: String,
    val name: String,
    val artists: SaavnArtists?,
    val image: List<SaavnImage>?,
    val downloadUrl: List<SaavnDownload>?
)

data class SaavnArtists(
    val primary: List<SaavnArtist>?
)

data class SaavnArtist(
    val name: String
)

data class SaavnImage(
    val quality: String,
    val url: String
)

data class SaavnDownload(
    val quality: String,
    val url: String
)

interface JioSaavnApi {
    @GET("api/search/songs")
    suspend fun searchSongs(
        @Query("query") query: String
    ): SaavnResponse
}

class MusicRepository {
    private val api: JioSaavnApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://saavn.sumit.co/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        api = retrofit.create(JioSaavnApi::class.java)
    }

    suspend fun getSongsByQuery(query: String): List<Song> {
        return try {
            val response = api.searchSongs(query = query)
            response.data.results.map { saavnSong ->
                val highestResImage = saavnSong.image?.maxByOrNull {
                    it.quality.split("x")[0].toIntOrNull() ?: 0
                }?.url ?: saavnSong.image?.lastOrNull()?.url
                
                val bestAudio = saavnSong.downloadUrl?.maxByOrNull {
                    it.quality.replace("kbps", "").toIntOrNull() ?: 0
                }?.url ?: saavnSong.downloadUrl?.lastOrNull()?.url
                
                Song(
                    trackId = saavnSong.id.hashCode().toLong(),
                    trackName = saavnSong.name,
                    artistName = saavnSong.artists?.primary?.firstOrNull()?.name,
                    previewUrl = bestAudio,
                    artworkUrl100 = highestResImage
                )
            }.filter { it.previewUrl != null }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

