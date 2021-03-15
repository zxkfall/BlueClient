package com.flywinter.blueclient

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author Zhang Xingkun
 *
 * @note 这个Activity的主要作用就是获取已经连接的蓝牙设备目录，然后通过点击
 * 设备，获取mac地址，建立连接，同时如果没有打开蓝牙，提示打开蓝牙
 *
 * @tips 蓝牙建立通信,最好不要在App里面扫描未连接的蓝牙设备去连接，
 * 因为高版本的Android对安全性有了更高的要求，如果在App里面连接了陌生的蓝牙设备，
 * 那么关闭App时，相应的蓝牙设备也会关闭，并且不会保存在系统的蓝牙设备列表里面，
 * 某些情况下，可能还会导致连接问题，
 * 比如如果App内连接Wifi，一定概率导致socket客户端建立失败
 * 初学者碰到这种问题往往可能会很迷惑，毕竟这是版本更新的问题
 * 所以如果建立连接的相关代码正确，可是却一直无法建立连接，倒不妨考虑一下是不是权限的问题，
 * 毕竟这些有可能时默认禁止的，即使在xml中添加了权限也不一定有用
 *
 * @ps 如果直接复制代码，注意修改package为自己的包名，包名通常都是自己拥有的域名倒写，
 * 比如我有一个网站www.flywinter.com
 */
class MainActivity : AppCompatActivity() {

    private var myBluetoothAdapter: BluetoothAdapter? = null
    private lateinit var myPairedDevices: Set<BluetoothDevice>
    private val requestEnableBlue = 1
    private var isSupportBlue = true
    companion object {
        const val BLUE_ADDRESS: String = "DeviceAddress"
        const val BLUE_NAME: String = "DeviceName"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(myBluetoothAdapter == null) {
            Toast.makeText(this, "该设备不支持蓝牙", Toast.LENGTH_SHORT).show()
            isSupportBlue = false
        }
        //如果支持蓝牙，但是蓝牙没有打开，请求打开蓝牙
        //通过onActivityResult获取结果
        if(isSupportBlue&&!myBluetoothAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, requestEnableBlue)
        }


        btn_refresh_blue_device.setOnClickListener {
            pairedDeviceList()
        }

    }

    //获取已经配对的蓝牙列表
    private fun pairedDeviceList() {
        myPairedDevices = myBluetoothAdapter!!.bondedDevices
        //   val list : ArrayList<BluetoothDevice> = ArrayList()
        val list : ArrayList<BlueDevice> = ArrayList()
        if (!myPairedDevices.isEmpty()) {
            for (device: BluetoothDevice in myPairedDevices) {
                list.add(BlueDevice(device.name,device))
                Log.i("device", ""+device)
            }
        } else {
            Toast.makeText(this, "没有找到蓝牙设备", Toast.LENGTH_SHORT).show()
        }
        val layoutManager = LinearLayoutManager(this)
        recycler_blue_device_list.layoutManager = layoutManager
        val adapter = BlueDeviceListAdapter(list,this)
        recycler_blue_device_list.adapter = adapter
    }





    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestEnableBlue) {
            if (resultCode == Activity.RESULT_OK) {
                if (myBluetoothAdapter!!.isEnabled) {
                    //同意开启了蓝夜设备
                    Toast.makeText(this, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
                } else {
                    //没有同意开启蓝牙设备
                    Toast.makeText(this, "Bluetooth has been disabled", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //没有同意开启蓝牙设备
                Toast.makeText(this, "Bluetooth enabling has been canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }







    //创建实体类,存放蓝牙名和蓝牙地址
   class BlueDevice(val deviceName:String,val device:BluetoothDevice)

    //创建RecyclerView适配器
    class BlueDeviceListAdapter(val deviceList: List<BlueDevice>, val context: Context): RecyclerView.Adapter<BlueDeviceListAdapter.ViewHolder>(){
        inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
            val deviceName: TextView = view.findViewById(R.id.item_name)
            val deviceAddress: TextView = view.findViewById(R.id.address_item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_blue_item,parent,false)
            val viewHolder = ViewHolder(view)

            //点击事件,启动蓝牙收发Activity
            viewHolder.itemView.setOnClickListener {
                val position = viewHolder.adapterPosition
                val device = deviceList[position]
                val intent = Intent(context, BlueDeviceActivity::class.java)
                intent.putExtra(BLUE_ADDRESS, device.device.address)
                intent.putExtra(BLUE_NAME,device.deviceName)
                context.startActivity(intent)
            }
            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = deviceList[position]
            holder.deviceName.text = device.deviceName
            holder.deviceAddress.text = device.device.address
        }

        override fun getItemCount(): Int {
            return deviceList.size
        }
    }

}