package co.orangesoft.searchable_paging

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource

/**
 * Base abstract class for searchable DataSource. Create your own source factory class and extend it from
 * SearchableDataSourceFactory to use searchable paging library
 *
 * @param dao - dao, which implement SearchableDao
 */
abstract class SearchableDataSourceFactory<DB>(val dao: SearchableDao) : DataSource.Factory<Int, DB>() {

    private val mutableLiveData: MutableLiveData<DataSource<Int, DB>> = MutableLiveData()
    private var params: HashMap<String, List<Any>> = hashMapOf()

    /**
     * Create new filtered DataSource and notify LiveData about it
     *
     * @return the new DataSource
     */
    override fun create(): DataSource<Int, DB> {
        return getDataSource(dao, params).create().apply {
            getData().postValue(this)
        }
    }

    /**
     * Get live data of DataSource
     * override to use your own
     *
     * @return current MutableLiveData of DataSource
     */
    open fun getData(): MutableLiveData<DataSource<Int, DB>> {
        return mutableLiveData

    }

    /**
     * invalidate DataSource
     */
    fun invalidateDataSource() {
        getData().value?.invalidate()
    }

    /**
     * Get values of searching parameter
     * @param param - searching parameter
     *
     * @return values of searching parameter
     */
    fun getQuery(param: String): List<Any> {
        return params[param] ?: listOf()
    }

    /**
     * Get map of searching parameters and its values
     *
     * @return map of searching parameters and its values
     */
    fun getQueries(): Map<String, List<Any>> {
        return params
    }

    /**
     * Set searching parameter and it values
     * @param param - searching parameter
     * @param values - values of searching parameter
     */
    fun setQuery(param: String, values: List<Any>) {
        if (values.isEmpty()) {
            params.remove(param)
        } else {
            params[param] = values
        }
    }

    /**
     * Set map of searching parameters and its values
     * @param params - map of searching parameters and its values
     */
    fun setQueries(params: HashMap<String, List<Any>>) {
        this.params = params
    }

    /**
     * Clear all parameters
     */
    fun clearQueries() {
        params.clear()
    }

    /**
     * Method for build database queries, apply it into dao and get back filtered DataSource
     * @param dao - dao for applying database queries
     * @param params - map of searching parameters and its values
     *
     * @return filtered DataSource
     */
    abstract fun getDataSource(dao: SearchableDao, params: HashMap<String, List<Any>>): DataSource.Factory<Int, DB>
}