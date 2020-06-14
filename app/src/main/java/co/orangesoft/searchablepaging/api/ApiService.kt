package co.orangesoft.searchablepaging.api

import co.orangesoft.searchablepaging.models.User
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("users")
    suspend fun getUsers(@Query("per_page") per_page: Int? = 100, @Query("since") since: Long? = 0): List<User>
    //suspend fun getUsers(): List<User>
}