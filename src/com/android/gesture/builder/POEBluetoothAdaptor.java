package com.android.gesture.builder;

import android.bluetooth.*;
import android.widget.Toast;


public class POEBluetoothAdaptor {
	
	BluetoothAdapter bluetooth;
	String mStatus;
	
	public POEBluetoothAdaptor() {
		bluetooth = BluetoothAdapter.getDefaultAdapter();
		
		if(bluetooth != null)
		{
			if (bluetooth.isEnabled()) {
			    String mydeviceaddress = bluetooth.getAddress();
			    String mydevicename = bluetooth.getName();
			    mStatus = mydevicename + " : " + mydeviceaddress;
			}
			else
			{
			    mStatus = "Bluetooth is not Enabled.";
			}
		}
	}
	
	public void connect() {
		// TO DO
		// 1. Find the available bluetooth device
		
		// 2. Choose our Arduino bluetooth shield
		
		// 3. Initiate pairing
	}
	
	public void send(int[] toArduino) {
		// Send toArduino array to serial port
	}
}
