package co.orangesoft.searchable_paging

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.*
import java.lang.Exception
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

/**
 * Base abstract class for searchable paging. Extends from it to use searchable paging library
 *
 * @param DB - Database model class
 * @param API - Response from server
 *
 * @param PAGE_SIZE - declare it to use custom size per page, otherwise default value
 * @param DISTANCE - declare it to use custom preload distance, otherwise default value
 * @param INITIAL_PAGE - declare it to use custom first page, otherwise default value
 * @param PAGE - declare it to use custom current page, otherwise default value
 */
abstract class BaseRefreshableRepository<DB, API>(
    protected var datasource: SearchableDataSourceFactory<DB>,
    protected val parentJob: Job? = null,
    protected open val PAGE_SIZE: Int = DEFAULT_PAGE_SIZE,
    protected open val DISTANCE: Int = DEFAULT_DISTANCE,
    protected open val INITIAL_PAGE: Int = DEFAULT_INITIAL_PAGE,
    protected open var PAGE: Int = DEFAULT_INITIAL_PAGE
): SearchableRepository<DB>, CoroutineScope {

    /**
     * Default paging parameters
     */
    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
        const val DEFAULT_DISTANCE: Int = 5
        const val DEFAULT_INITIAL_PAGE: Int = 0
    }

    override val coroutineContext: CoroutineContext by lazy { Dispatchers.Main + SupervisorJob(parentJob) }

    private var loadListener: WeakReference<OnLoadListener>? = null

    private val callback: PagedList.BoundaryCallback<DB> = object : PagedList.BoundaryCallback<DB>() {
        private var isFirstLoad = true

        override fun onZeroItemsLoaded() {
            if (isFirstLoad) {
                isFirstLoad = false
                launch { getItem() }
            }
        }

        override fun onItemAtFrontLoaded(itemAtFront: DB) {
            if (isFirstLoad) {
                launch { getItem(true) }
                isFirstLoad = false
            }
        }

        override fun onItemAtEndLoaded(itemAtEnd: DB) {
            if(PAGE > 0) {
                launch { getItem() }
            }
        }
    }

    /**
     * Default builder for PagedList.Config
     * override pagedConfig if you want to customize PagedList.Config
     */
    open val pagedConfig: PagedList.Config by lazy {
        PagedList.Config.Builder()
                .setPageSize(PAGE_SIZE)
                .setPrefetchDistance(DISTANCE)
                .setEnablePlaceholders(true)
                .build()
    }

    private val pagedItems: LiveData<PagedList<DB>> by lazy {
        LivePagedListBuilder(datasource , pagedConfig)
                .setBoundaryCallback(callback)
                .setInitialLoadKey(PAGE)
                .build()
    }

    /**
     * Use getItems() to observe data and submit it to paged list adapter
     */
    override fun getItems(): LiveData<PagedList<DB>> = pagedItems

    /**
     * Use refresh to reload data from api
     * @param force - set true if you want to reload data anyway
     */
    override fun refresh(force: Boolean) {
        launch { getItem(force) }
    }

    /**
     * Use it to declare listener for data load
     */
    override fun setOnLoadListener(listener: OnLoadListener) {
        loadListener?.apply { clear() }
        loadListener = WeakReference(listener)
    }

    private suspend fun getItem(force: Boolean = false) {

        if (force) {
            PAGE = INITIAL_PAGE
        }

        loadListener?.get()?.invoke(false)

        try {
            val result = loadData(PAGE, PAGE_SIZE, datasource.getQueries())
            onDataLoaded(result, datasource.dao, force)
            PAGE++
            loadListener?.get()?.invoke(true)

        } catch (e: Exception){
            e.printStackTrace()
            loadListener?.get()?.invoke(e)
        }
    }

    /**
     * Get values of searching parameter
     */
    override fun getQuery(param: String): List<Any> {
        return datasource.getQuery(param)
    }

    /**
     * Get map of searching parameters and its values
     */
    override fun getQueries(): Map<String, List<Any>> {
        return datasource.getQueries()
    }

    /**
     * Set searching parameter and it values
     * @param force - set true if you want to reload data anyway
     */
    override fun setQuery(force: Boolean, param: String, values: List<Any>) {

        if (!validateQueryKey(param)) {
            return
        }

        datasource.setQuery(param, values)
        updateQueries(force)
    }

    /**
     * Set map of searching parameters and its values
     * @param force - set true if you want to reload data anyway
     */
    override fun setQueries(force: Boolean, params: HashMap<String, List<Any>>) {

        params.keys.forEach { key ->
            if (!validateQueryKey(key)) {
                return
            }
        }

        datasource.setQueries(params)
        updateQueries(force)
    }

    /**
     * Clear all parameters
     * @param force - set true if you want to reload data anyway
     */
    override fun clearQueries(force: Boolean) {
        datasource.clearQueries()
        updateQueries(force)
    }

    private fun updateQueries(force: Boolean) {
        datasource.invalidateDataSource()

        PAGE = INITIAL_PAGE

        if (force) {
            refresh(force)
        }
    }

    /**
     * Check if this key match with any of your keys, which were declared for current searchable paging
     */
    protected abstract fun validateQueryKey(key: String): Boolean

    /**
     * In this method build searching query for api and return the response from server request
     * @param page - from this page you should start searching on server
     * @param limit - limit of items per page
     * @param params - map of parameters to build searching query for api
     */
    protected abstract suspend fun loadData(page: Int, limit: Int, params: Map<String, List<Any>>): API

    /**
     * Do whatever you want with the data from server. For instance, insert result into database
     * @param result - result from server
     * @param dao - dao for inserting your api results
     * @param force - flag to detect that current loading is forcible
     */
    protected abstract suspend fun onDataLoaded(result: API, dao: SearchableDao, force: Boolean)
}