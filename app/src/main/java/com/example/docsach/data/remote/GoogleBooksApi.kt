package com.example.docsach.data.remote

import com.example.docsach.data.remote.dto.VolumeDto
import com.example.docsach.data.remote.dto.VolumesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleBooksApi {

    @GET("volumes")
    suspend fun searchVolumes(
        @Query("q") query: String,
        @Query("orderBy") orderBy: String? = null,
        @Query("langRestrict") langRestrict: String? = null,
        @Query("startIndex") startIndex: Int = 0,
        @Query("maxResults") maxResults: Int = DEFAULT_PAGE_SIZE,
    ): VolumesResponse

    @GET("volumes/{volumeId}")
    suspend fun getVolume(@Path("volumeId") volumeId: String): VolumeDto

    companion object {
        const val BASE_URL = "https://www.googleapis.com/books/v1/"
        const val DEFAULT_PAGE_SIZE = 20
    }
}
