package com.wardcare.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.firebase.messaging.FirebaseMessaging
import com.wardcare.app.R
import com.wardcare.app.data.local.AppDatabase
import com.wardcare.app.data.local.UserManager
import com.wardcare.app.data.model.AppNotification
import com.wardcare.app.data.remote.ApiClient
import com.wardcare.app.data.repository.MedicationRepository
import com.wardcare.app.data.repository.NotificationRepository
import com.wardcare.app.databinding.ActivityMainBinding
import com.wardcare.app.notifications.NotificationHelper
import com.wardcare.app.notifications.PushTokenRegistrar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val ALERTS_TOPIC = "ward-alerts"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var lastHandledNotificationId: String = ""
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(TAG, "POST_NOTIFICATIONS permission granted=$granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment, R.id.loginFragment, R.id.registerFragment -> {
                    binding.toolbar.visibility = View.GONE
                    binding.bottomNavigation.visibility = View.GONE
                    binding.syncBanner.visibility = View.GONE
                    binding.notificationBanner.visibility = View.GONE
                }
                R.id.notificationsFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.notificationBanner.visibility = View.GONE
                }
                R.id.homeDashboardFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.bottomNavigation.visibility = View.VISIBLE
                    // The user is signed in by now, so the push token can be saved to their account
                    registerForPush()
                }
                else -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        setupSyncIndicator()
        setupNotificationBanner()
        registerNetworkCallback()
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .unregisterNetworkCallback(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        val menuItem = menu?.findItem(R.id.notificationsFragment)
        val actionView = menuItem?.actionView
        if (actionView != null) {
            val tvBadge = actionView.findViewById<TextView>(R.id.tvNotificationBadge)
            actionView.setOnClickListener {
                if (navController.currentDestination?.id != R.id.notificationsFragment) {
                    navController.navigate(R.id.notificationsFragment)
                }
            }
            setupBadgeObserver(tvBadge)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(item, navController) || super.onOptionsItemSelected(item)
    }

    private fun setupBadgeObserver(badgeView: TextView) {
        val database = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            database.notificationDao().getNotificationCountFlow().collectLatest { count ->
                if (count > 0) {
                    badgeView.visibility = View.VISIBLE
                    badgeView.text = if (count > 99) "99+" else count.toString()
                } else {
                    badgeView.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSyncIndicator() {
        val database = AppDatabase.getDatabase(this)
        val medicationDao = database.medicationDao()

        lifecycleScope.launch {
            medicationDao.getPendingCount().collectLatest { count ->
                val currentId = navController.currentDestination?.id
                if (count > 0 && currentId !in listOf(R.id.splashFragment, R.id.loginFragment, R.id.registerFragment)) {
                    binding.syncBanner.visibility = View.VISIBLE
                    binding.syncStatusText.text = "$count entries pending sync"
                } else {
                    binding.syncBanner.visibility = View.GONE
                }
            }
        }

        binding.syncNowButton.setOnClickListener {
            Log.d(TAG, "Sync now pressed")
            syncNow()
        }
    }

    /** Shows the newest alert in a banner. New alerts arrive via FCM (WardCareMessagingService). */
    private fun setupNotificationBanner() {
        val database = AppDatabase.getDatabase(this)
        val notificationRepository = NotificationRepository(database.notificationDao())
        val userManager = UserManager(this)

        binding.closeNotificationBanner.setOnClickListener {
            binding.notificationBanner.visibility = View.GONE
        }

        binding.viewNotificationButton.setOnClickListener {
            binding.notificationBanner.visibility = View.GONE
            if (navController.currentDestination?.id != R.id.notificationsFragment) {
                navController.navigate(R.id.notificationsFragment)
            }
        }

        lifecycleScope.launch {
            notificationRepository.allNotifications.collectLatest { notifications: List<AppNotification> ->
                if (notifications.isNotEmpty() && userManager.notificationsEnabled) {
                    val latest = notifications.first()
                    if (latest.id != lastHandledNotificationId) {
                        lastHandledNotificationId = latest.id
                        val currentId = navController.currentDestination?.id
                        if (currentId !in listOf(R.id.splashFragment, R.id.loginFragment, R.id.registerFragment, R.id.notificationsFragment)) {
                            binding.notificationBanner.visibility = View.VISIBLE
                            val subText = if (!latest.subtitle.isNullOrBlank()) ": ${latest.subtitle}" else ""
                            binding.notificationText.text = "${latest.title}$subText"
                        }
                    }
                }
            }
        }
    }

    /** Offline mode: when the device gets a connection again, queued medication logs are synced automatically. */
    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(TAG, "Network available, syncing queued medication logs")
                syncNow()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        networkCallback = callback
    }

    private fun syncNow() {
        val repository = MedicationRepository(
            AppDatabase.getDatabase(this).medicationDao(),
            ApiClient.apiService
        )
        lifecycleScope.launch { repository.syncPendingLogs() }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** Subscribes to the ward alerts topic and saves the FCM token for this signed-in user. */
    private fun registerForPush() {
        lifecycleScope.launch {
            try {
                val messaging = FirebaseMessaging.getInstance()
                messaging.subscribeToTopic(ALERTS_TOPIC).await()
                Log.i(TAG, "registerForPush: subscribed to topic $ALERTS_TOPIC")
                PushTokenRegistrar.register(messaging.token.await())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "registerForPush failed: ${e.javaClass.simpleName}")
            }
        }
    }
}