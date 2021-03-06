package co.orangesoft.searchable_paging.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import co.orangesoft.paging.OnLoadListener
import co.orangesoft.paging.TransactionCallback
import co.orangesoft.searchable_paging.R
import co.orangesoft.searchable_paging.api.ApiModuleImpl
import co.orangesoft.searchable_paging.database.UserSourceFactory
import co.orangesoft.searchable_paging.database.UserSourceFactory.Companion.KEY_LOGIN
import co.orangesoft.searchable_paging.models.User
import co.orangesoft.searchable_paging.repositories.AppDatabaseRepository
import co.orangesoft.searchable_paging.repositories.UserListRepository
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {

    private val userDiffUtilCallback = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    private val userPagedListAdapter by lazy {
        UserPagedListAdapter(userDiffUtilCallback)
    }

    private val userListRepository by lazy {
        val userDao = AppDatabaseRepository.buildDatabase(this).userDao()
        val apiService = ApiModuleImpl().apiService
        UserListRepository(apiService,
            UserSourceFactory(userDao), Job())
    }

    private val loadListener = OnLoadListener().apply {
        onStartLoad {
            //Do your stuff before load
        }
        onFinishLoad { _, _ ->
            //Do your stuff after load
        }
        onErrorLoad {
            //Do your stuff after exception
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        initViews()
    }

    private fun getPagedListLiveData(): LiveData<PagedList<User>> {
        return userListRepository.getItems()
    }

    private fun initViews() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = userPagedListAdapter
        }

        val testUser = User(50840, "MY_LOGIN")
        val testUser2 = User(90000, "MY_LOGIN2")

        fab_insert.setOnClickListener {
            userListRepository.insertItems(
                testUser,
                testUser2,
                callback = TransactionCallback().apply {
                    onSuccess {
                        Toast.makeText(this@MainActivity, "User inserted", Toast.LENGTH_SHORT).show()
                    }
                    onNetworkError { exception ->
                        Toast.makeText(this@MainActivity, exception.message, Toast.LENGTH_SHORT).show()
                    }
                    onDatabaseError { exception ->
                        Toast.makeText(this@MainActivity, exception.message, Toast.LENGTH_SHORT).show()
                    }
                })
        }

        fab_delete.setOnClickListener {
            userListRepository.deleteItems(
                testUser,
                testUser2,
                callback = TransactionCallback().apply {
                    onSuccess {
                        Toast.makeText(this@MainActivity, "User deleted", Toast.LENGTH_SHORT).show()
                    }
                    onNetworkError { exception ->
                        Toast.makeText(this@MainActivity, exception.message, Toast.LENGTH_SHORT).show()
                    }
                    onDatabaseError { exception ->
                        Toast.makeText(this@MainActivity, exception.message, Toast.LENGTH_SHORT).show()
                    }
                })
        }

        getPagedListLiveData().observe(this, Observer { userPagedListAdapter.submitList(it) })
        userListRepository.setOnLoadListener(loadListener)

        val filterParams: HashMap<String, List<Any>> = hashMapOf()
        filterParams[KEY_LOGIN] = listOf("my")
        //filterParams[KEY_AVATAR] = listOf("avatars1")
        userListRepository.setQueries(true, filterParams)
    }
}