package com.flywinter.blueclient

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import kotlinx.android.synthetic.main.activity_blue_client.*
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

/**
 * @author Zhang Xingkun
 *
 * @note 蓝牙接收其实存在一个问题，那就是发送端可能只是发送了一次消息，但是接收端
 * 却分为两次接收，这样如果需要给发送端加上一个日期标签，那么App接收的消息就会很混乱
 * 比如PC发送1234\r\n，APP可能收到1    234，如果加上前缀，就变成了
 * 收到的消息1收到的消息234\r\n
 * 这个问题还有待解决，不过简单的蓝牙传输已经可以了
 *
 * @ps 如果直接复制代码，注意修改package为自己的包名，包名通常都是自己拥有的域名倒写，
 * 比如我有一个网站www.flywinter.com
 *
 */
class BlueDeviceActivity : AppCompatActivity() {

    //初始化变量
    companion object {
        var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var mBluetoothSocket: BluetoothSocket? = null
        lateinit var mBluetoothAdapter: BluetoothAdapter
        var isBlueConnected: Boolean = false
        const val MESSAGE_RECEIVE_TAG = 111
        lateinit var blueAddress: String
        lateinit var blueName: String
        private val BUNDLE_RECEIVE_DATA = "ReceiveData"
        private val TAG = "BlueDeviceActivity"
        //设置发送和接收的字符编码格式
        private val ENCODING_FORMAT = "GBK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blue_client)
        //获取蓝牙设备的名字和地址
        blueAddress = intent.getStringExtra(MainActivity.BLUE_ADDRESS)!!
        blueName = intent.getStringExtra(MainActivity.BLUE_NAME)!!
        //显示蓝牙设备的名字和地址
        txt_blue_name.text = blueName
        txt_blue_address.text = blueAddress

        //设置接收信息框上下滚动，如果设置了多个，只会有一个起作用
        txt_blue_receive.movementMethod = ScrollingMovementMethod.getInstance()

        //默认打开蓝牙
        if (switch_blue_status.isChecked) {
            //开始连接蓝牙
            funStartBlueClientConnect()
            //打开蓝牙接收消息
            funBlueClientStartReceive()
        }
        //打开或关闭蓝牙连接
        switch_blue_status.setOnClickListener {
            if (switch_blue_status.isChecked) {
                //开始连接蓝牙
                funStartBlueClientConnect()
                //打开蓝牙接收消息
                funBlueClientStartReceive()
            } else {
                disconnect()
            }
        }
        //点击发送消息
        btn_blue_send.setOnClickListener {
            var toString = edit_blue_send.text.toString()
            if (check_blue_add_newline.isChecked) {
                toString += "\r\n"
            }
            if (check_blue_add_renew.isChecked) {
                if (isBlueConnected) {
                    stringBuffer.append("发送的消息:" + toString)
                    txt_blue_receive.text = stringBuffer.toString()
                } else {
                    stringBuffer.append("发送失败，蓝牙连接已经断开")
                    txt_blue_receive.text = stringBuffer.toString()
                }
            }
            funBlueClientSend(toString)
        }
        //清除接收消息文本框内容
        btn_blue_message_clear.setOnClickListener {
            stringBuffer.delete(0, stringBuffer.length)
            txt_blue_receive.text = stringBuffer.toString()
        }


    }

    //开始连接蓝牙
    private fun funStartBlueClientConnect() {
        thread {
            try {
                //这一段代码必须在子线程处理，直接使用协程会阻塞主线程，所以用Thread,其实也可以直接用Thread，不用协程
                if (mBluetoothSocket == null || !isBlueConnected) {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = mBluetoothAdapter.getRemoteDevice(blueAddress)
                    mBluetoothSocket =
                        device.createInsecureRfcommSocketToServiceRecord(myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    mBluetoothSocket!!.connect()
                    isBlueConnected = true
                }
            } catch (e: IOException) {
                //连接失败销毁Activity
                finish()
                e.printStackTrace()
            }
        }
    }

    //打开蓝牙接收消息
    private fun funBlueClientStartReceive() {
        thread {
            while (true) {
                //启动蓝牙接收消息
                //注意,如果不在子线程或者协程进行，会导致主线程阻塞，无法绘制
                try {
                    if (mBluetoothSocket != null) {
                        if (mBluetoothSocket!!.isConnected) {
                            Log.e("eee", "现在可以接收数据了")
                            receiveMessage()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "funBlueClientStartReceive:" + e.toString())
                }
            }
        }
    }

    //蓝牙接收消息的函数体
    private fun receiveMessage() {
        val mmInStream: InputStream = mBluetoothSocket!!.inputStream
        val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
        var bytes = 0
        //java.lang.OutOfMemoryError: pthread_create (1040KB stack) failed: Try again
        //已经Thread了就不要再次thread了
        //   thread {
        while (true) {
            // Read from the InputStream.
            try {
                bytes = mmInStream.read(mmBuffer)
            } catch (e: IOException) {
                Log.d(TAG, "Input stream was disconnected", e)
                break
            }
            val message = Message()
            val bundle = Bundle()
            //默认GBK编码
            val string = String(mmBuffer, 0, bytes, Charset.forName(ENCODING_FORMAT))
            bundle.putString(BUNDLE_RECEIVE_DATA, string)
            message.what = MESSAGE_RECEIVE_TAG
            message.data = bundle
            handler.sendMessage(message)
            Log.e("receive", string)
        }
        //  }
    }

    //蓝牙发送消息
    private fun funBlueClientSend(input: String) {
        if (mBluetoothSocket != null && isBlueConnected) {
            try {
                mBluetoothSocket!!.outputStream.write(input.toByteArray(Charset.forName(ENCODING_FORMAT)))
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "sendCommand: 发送消息失败", e)
            }
        }
    }

    //这是官方推荐的方法
    val stringBuffer = StringBuffer()
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_RECEIVE_TAG -> {
                    val string = stringBuffer.append(msg.data.getString(BUNDLE_RECEIVE_DATA))
                    txt_blue_receive.text = string
                }
            }
        }
    }

    //蓝牙断开连接
    private fun disconnect() {
        if (mBluetoothSocket != null) {
            try {
                mBluetoothSocket!!.close()
                mBluetoothSocket = null
                isBlueConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "disconnect: 蓝牙关闭失败", e)
            }
        }
    }

    //获取系统时间
    @SuppressLint("SimpleDateFormat")
    private fun funGetSystemTime(): String {
        val simpleFormatter = SimpleDateFormat("YYYY.MM.dd HH:mm:ss")
        val date = Date(System.currentTimeMillis())
        return simpleFormatter.format(date)
    }
}