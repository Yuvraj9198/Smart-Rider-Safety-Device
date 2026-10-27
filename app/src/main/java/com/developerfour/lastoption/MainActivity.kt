package com.developerfour.lastoption

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnScan: Button
    private lateinit var btnSend: Button
    private lateinit var rightBtn: Button
    private lateinit var leftBtn: Button
    private lateinit var forwardBtn: Button
    private lateinit var navBtn: Button
    private lateinit var etMessage: EditText
    private lateinit var lvDevices: ListView
    private lateinit var tvStatus: TextView

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnScan = findViewById(R.id.btnScan)
        btnSend = findViewById(R.id.btnSend)
        rightBtn = findViewById(R.id.rightbtn)
        leftBtn = findViewById(R.id.leftbtn)
        forwardBtn = findViewById(R.id.forwardbtn)
        navBtn = findViewById(R.id.navbtn)
        etMessage = findViewById(R.id.etMessage)
        lvDevices = findViewById(R.id.lvDevices)
        tvStatus = findViewById(R.id.tvStatus)

        btnSend.isEnabled = false

        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        lvDevices.adapter = deviceAdapter

        btnScan.setOnClickListener { checkPermissionsAndScan() }
        lvDevices.setOnItemClickListener { _, _, position, _ -> connectToDevice(deviceList[position]) }
        btnSend.setOnClickListener { sendMessage(etMessage.text.toString()) }
        rightBtn.setOnClickListener { sendMessage("RIGHT") }
        leftBtn.setOnClickListener { sendMessage("LEFT") }
        forwardBtn.setOnClickListener { sendMessage("FORWARD") }
        navBtn.setOnClickListener { startActivity(Intent(this, NavigationActivity::class.java)) }

        registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    private fun checkPermissionsAndScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 1)
        } else {
            scanDevices()
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            showToast("Enable Bluetooth first")
            return
        }
        showToast("Scanning for devices...")
        deviceList.clear()
        deviceAdapter.clear()
        bluetoothAdapter.bondedDevices.forEach { device ->
            deviceList.add(device)
            deviceAdapter.add("Paired: ${device.name ?: "Unknown"} - ${device.address}")
        }
        if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        bluetoothAdapter.startDiscovery()
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (!deviceList.contains(it)) {
                        deviceList.add(it)
                        deviceAdapter.add("Found: ${it.name ?: "Unknown"} - ${it.address}")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        showToast("Connecting to ${device.name ?: "Unknown"}...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_SPP)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                MainActivity.bluetoothSocket = bluetoothSocket
                withContext(Dispatchers.Main) {
                    tvStatus.text = "Status: Connected to ${device.name ?: "Unknown"}"
                    btnSend.isEnabled = true
                    showToast("Connected")
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "Status: Connection Failed"
                    btnSend.isEnabled = false
                    showToast("Connection Failed")
                }
            }
        }
    }

    private fun sendMessage(message: String) {
        if (message.isEmpty() || outputStream == null) {
            showToast("Enter a message or connect first")
            return
        }
        try {
            outputStream?.write(message.toByteArray())
            showToast("Sent: $message")
        } catch (e: IOException) {
            showToast("Error sending message")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Log.e("Bluetooth", "Receiver not registered")
        }
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {}
    }

    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }
    companion object {
        var bluetoothSocket: BluetoothSocket? = null
    }

}
