package co.orangesoft.searchable_paging.repositories

import co.orangesoft.paging.BaseRefreshableRepository
import co.orangesoft.paging.SearchableDataSourceFactory
import co.orangesoft.searchable_paging.api.ApiService
import co.orangesoft.searchable_paging.database.UserSourceFactory.Companion.KEY_AVATAR
import co.orangesoft.searchable_paging.database.UserSourceFactory.Companion.KEY_LOGIN
import co.orangesoft.searchable_paging.models.User
import kotlinx.coroutines.Job
import retrofit2.Response
import java.lang.StringBuilder

class UserListRepository(private val apiService: ApiService, factory: SearchableDataSourceFactory<User, User>, parentJob: Job? = null)
    : BaseRefreshableRepository<User, User>(factory, parentJob = parentJob, PAGE_SIZE = 10) {

    override fun validateQueryKey(key: String): Boolean {
        return when(key) {
            KEY_LOGIN -> true
            KEY_AVATAR -> true
            else -> false
        }
    }

    override suspend fun loadData(page: Int, limit: Int, params: Map<String, List<Any>>): List<User> {
        var resultQuery: StringBuilder? = null

        if (params.isNotEmpty()) {

            resultQuery = StringBuilder()

            val loginValues = params[KEY_LOGIN]
            if (!loginValues.isNullOrEmpty()) {
                resultQuery.append(loginValues[0])
                    .append(" in:")
                    .append(KEY_LOGIN)
            }
        }

        return apiService.getSearchUsers(limit, page.toLong(), resultQuery?.toString()).items
    }

    override suspend fun insertItemsApi(items: Collection<User>): ResponseModel {
        val response = apiService.insertUsers(items)
        return ResponseModel(success = response.isSuccessful, errorMessage = response.message())
    }

    override suspend fun deleteItemsApi(items: Collection<User>): ResponseModel {
        val response = apiService.deleteUsers(items)
        return ResponseModel(success = response.isSuccessful, errorMessage = response.message())
    }
}