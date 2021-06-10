package edu.cs4730.servicedemo;

import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Messenger;
import android.os.Process;
import android.os.Message;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;

public class myService1 extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final static String TAG = myService1.class.getSimpleName();
    //my variables
    Random r;
    int NotID = 1;
    NotificationManager nm;
    String distance="1000";
    static final int UdpServerPORT = 12345;
    UdpServerThread udpServerThread;
    // Handler that receives messages from the thread
    private class UdpServerThread extends Thread{

        int serverPort;
        DatagramSocket socket;

        boolean running;

        public UdpServerThread(int serverPort) {
            super();
            this.serverPort = serverPort;
        }

        public void setRunning(boolean running){
            this.running = running;
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(serverPort);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            running = true;
            try {
                while(running){
                    byte[] buf = new byte[256];

                    // receive request
                    DatagramPacket packet= new DatagramPacket(buf, buf.length);

                    socket.receive(packet);     //this code block the program flow
                    String data = new String(packet.getData());
                    // send the response to the client at "address" and "port"
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();
//                    String addressrp="192.168.0.108";
                    InetAddress addressrp = InetAddress.getByName("192.168.0.108");
                    int portrp = 12347;
//                    address.;
                    distance= data;
                    String dString = new Date().toString() + "\n"
                            + "Your address " + address + ":" + String.valueOf(port);
                    buf = dString.getBytes();
                    packet = new DatagramPacket(buf, buf.length, addressrp, portrp);
                    socket.send(packet);


                }

                Log.e(TAG, "UDP Server ended");

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(socket != null){
                    socket.close();
                    Log.e(TAG, "socket.close()");
                }
            }
        }
    }
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            //setup how many messages
            nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int times = 0, i;
            Messenger messenger = null;
            Bundle extras = msg.getData();


            if (extras != null) {
                times = extras.getInt("times", 0);
                messenger = (Messenger) extras.get("MESSENGER");
            }
            udpServerThread = new UdpServerThread(UdpServerPORT);
            udpServerThread.start();
            while(true) {
                for (i = 0; i < times; i++) {
                    synchronized (this) {
                        try {
                            wait(2000);
                        } catch (InterruptedException e) {
                        }
                    }
                    String info = i + " random " + r.nextInt(100)+" "+distance.toString();
                    info=distance.toString();
                    Log.d("intentServer", info);
                    if (messenger != null) {
                        Message mymsg = Message.obtain();
                        mymsg.obj = info;
                        try {
                            messenger.send(mymsg);
                        } catch (android.os.RemoteException e1) {
                            Log.w(getClass().getName(), "Exception sending message", e1);
                        }
                    } else {
                        //no handler, so use notification
                        makenoti(info);
                    }
                }
                // Stop the service using the startId, so that we don't stop
                // the service in the middle of handling another job
//                stopSelf(msg.arg1);
            }
        }
    }

    @Override
    public void onCreate() {
        r = new Random();
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;//needed for stop.
        msg.setData(intent.getExtras());
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        if(udpServerThread != null){
            udpServerThread.setRunning(false);
            udpServerThread = null;
        }
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    public void makenoti(String message) {

        Notification noti = new NotificationCompat.Builder(getApplicationContext(), MainActivity.id)
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())  //When the event occurred, now, since noti are stored by time.

                .setContentTitle("Service")   //Title message top row.
                .setContentText(message)  //message when looking at the notification, second row
                .setAutoCancel(true)   //allow auto cancel when pressed.
                .build();  //finally build and return a Notification.

        //Show the notification
        long[] vibrates = {1000, 0, 1000, 0 };
        noti.vibrate = vibrates;
        nm.notify(NotID, noti);
//        NotID++;
    }
}
