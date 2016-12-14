/**
 *  @version 1.1 (28.01.2013)
 *  http://english.cxem.net/arduino/arduino5.php
 *  @author Koltykov A.V. (�������� �.�.)
 * 
 */

package com.example.bluetooth1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
 
import com.example.bluetooth1.R;
 
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
 
public class MainActivity extends Activity {
  private static final String TAG = "bluetooth1";
   
  Button btnOn, btnOff;
  boolean connected;

  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private OutputStream outStream = null;
  private InputStream inStream = null;

  Thread receiver;

  // SPP UUID service 
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 
  // MAC-address of Bluetooth module (you must edit this line)
  private static String address = "00:15:FF:F2:19:5F";
   
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
 
    setContentView(R.layout.activity_main);
 
    btnOn = (Button) findViewById(R.id.btnOn);
    btnOff = (Button) findViewById(R.id.btnOff);
     
    btAdapter = BluetoothAdapter.getDefaultAdapter();
    checkBTState();
 
    btnOn.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        sendData("AT");
        Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
      }
    });
 
    btnOff.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        receiver.run();
      }
    });
  }

  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
      if(Build.VERSION.SDK_INT >= 10){
          try {
            Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            return (BluetoothSocket) m.invoke(device, 1);
          } catch (Exception e) {
              Log.e(TAG, "Could not create Insecure RFComm Connection",e);
          }
      }
      return  device.createRfcommSocketToServiceRecord(MY_UUID);
  }
   
  @Override
  public void onResume() {
    super.onResume();
 
    Log.d(TAG, "...onResume - try connect...");

    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
    // If there are paired devices
    if (pairedDevices.size() > 0) {
      // Loop through paired devices
      for (BluetoothDevice devicetmp : pairedDevices) {
        // Add the name and address to an array adapter to show in a ListView
        address=devicetmp.getAddress();
      }
    }

    // Set up a pointer to the remote node using it's address.
    BluetoothDevice device = btAdapter.getRemoteDevice(address);


    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
   
	try {
		btSocket = createBluetoothSocket(device);
	} catch (IOException e1) {
		errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
	}
    
    /*try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }*/
   
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
   
    // Establish the connection.  This will block until it connects.
    Log.d(TAG, "...Connecting...");
    try {
      btSocket.connect();
      connected=true;
      Log.d(TAG, "...Connection ok...");
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
     
    // Create a data stream so we can talk to server.
    Log.d(TAG, "...Create Socket...");
 
    try {
      outStream = btSocket.getOutputStream();
      inStream = btSocket.getInputStream();
      receiver=new Thread(new Runnable() {
        public void run() {
          while (true) {
            if (connected) {
              try {
                byte ch, buffer[] = new byte[1024];
                int i = 0;
                char DELIMITER='\n';

                String s = "";
                while ((ch = (byte) inStream.read()) != DELIMITER) {
                  buffer[i++] = ch;
                }
                buffer[i] = '\0';

                final String msg = new String(buffer);

                Log.i(TAG, "run: " + msg.trim());

              } catch (IOException e) {

              }
            }
          }
          }
      });
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
    }


  }
 
  @Override
  public void onPause() {
    super.onPause();
 
    Log.d(TAG, "...In onPause()...");
 
    if (outStream != null) {
      try {
        outStream.flush();
      } catch (IOException e) {
        errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
      }
    }

    if (inStream!=null) {
      try {
        inStream.reset();
      } catch (Exception e) {
        Log.i(TAG, "onPause: "+e.getLocalizedMessage());
      }
    }
 
    try     {
      btSocket.close();
    } catch (IOException e2) {
      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
    }
  }
   
  private void checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on
    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) { 
      errorExit("Fatal Error", "Bluetooth not support");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth ON...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
      }
    }
  }
 
  private void errorExit(String title, String message){
    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    finish();
  }

  private String readData() {
    try {
      byte[] buffer = new byte[255];

      inStream.read(buffer);
      return buffer.toString();
    } catch (Exception e) {
      Log.i(TAG, "readData: "+e.getLocalizedMessage());
    }
    return null;
  }
 
  private void sendData(String message) {
    byte[] msgBuffer = message.getBytes();
 
    Log.d(TAG, "...Send data: " + message + "...");
 
    try {
      outStream.write(msgBuffer);
    } catch (IOException e) {
      String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
      if (address.equals("00:00:00:00:00:00")) 
        msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
      	msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
       
      	errorExit("Fatal Error", msg);       
    }
  }
}

