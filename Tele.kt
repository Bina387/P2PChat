// --- On the "Hub" Phone ---
// (Simplified - actual implementation is much more complex)

class HubService : Service() {
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver // For Wi-Fi Direct events

    override fun onCreate() {
        super.onCreate()
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)

        // Register a BroadcastReceiver to listen for Wi-Fi Direct events (peers discovered, connection status changes)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> { /* Check P2P state */ }
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> { /* Request peers and discover */ }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        // A connection status has changed, check details
                        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                        if (networkInfo?.isConnected == true) {
                            // Connected to a client, get group info (IP addresses)
                            wifiP2pManager.requestGroupInfo(channel) { group ->
                                // Now you have the IP addresses of connected clients
                                // Start your TCP server socket to listen for client connections
                                startTcpServer(group?.clientList)
                            }
                        } else {
                            // Disconnected
                        }
                    }
                    WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> { /* Device details changed */ }
                }
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        registerReceiver(receiver, intentFilter)

        // Create a Wi-Fi Direct Group (this phone becomes the Group Owner)
        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Group created successfully, ready for clients to connect
            }
            override fun onFailure(reason: Int) {
                // Failed to create group
            }
        })
    }

    private fun startTcpServer(clientList: Collection<WifiP2pDevice>?) {
        // In a Coroutine or background thread:
        // Use ServerSocket to listen for incoming connections from user phones.
        // Once connected, establish input/output streams to send/receive data.
        // Implement logic to relay voice/text between connected clients.
        // For voice, you'd integrate WebRTC here, using the established sockets as a transport.
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        // Remove the Wi-Fi Direct group when service stops
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })
    }
}

// --- On the "User" Phone ---
// (Simplified - actual implementation is much more complex)

class UserActivity : AppCompatActivity() {
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver // For Wi-Fi Direct events

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... (UI setup)

        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)

        // Similar BroadcastReceiver setup to discover peers
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        wifiP2pManager.requestPeers(channel) { peers ->
                            // Find the "Hub" device in the peers list
                            val hubDevice = peers.deviceList.find { it.deviceName == "Your_Hub_Device_Name" }
                            hubDevice?.let {
                                // Initiate connection to the hub device
                                val config = WifiP2pConfig().apply {
                                    deviceAddress = it.deviceAddress
                                }
                                wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
                                    override fun onSuccess() {
                                        // Successfully connected to the hub.
                                        // Now initiate a TCP client socket connection to the hub's IP address
                                        // (You'll need to obtain the hub's IP after connection is established)
                                        connectToHubTcp(it.deviceAddress) // This needs the actual group owner IP
                                    }
                                    override fun onFailure(reason: Int) {
                                        // Connection failed
                                    }
                                })
                            }
                        }
                    }
                    // Handle connection changes etc.
                }
            }
        }
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        registerReceiver(receiver, intentFilter)

        // Start peer discovery
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { /* Discovery started */ }
            override fun onFailure(reason: Int) { /* Discovery failed */ }
        })
    }

    private fun connectToHubTcp(hubIpAddress: String) {
        // In a Coroutine or background thread:
        // Use Socket to connect to the hub's ServerSocket.
        // Once connected, establish input/output streams to send/receive voice packets and text messages.
        // For voice, use WebRTC over this socket connection.
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        // Disconnect from the group
    }
}
