package com.dream.tcp;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity2 extends AppCompatActivity {

    private static final String TAG = "TcpDemo";

    @Bind(R.id.ed_server_ip) EditText mEdServerIp;
    @Bind(R.id.ed_server_port) EditText mEdServerPort;
    @Bind(R.id.ed_server_send_message) EditText mEdServerSendMessage;
    @Bind(R.id.lv_server_msg) ListView mLvServerMsg;
    @Bind(R.id.ed_client_ip) EditText mEdClientIp;
    @Bind(R.id.ed_client_port) EditText mEdClientPort;
    @Bind(R.id.ed_client_send_message) EditText mEdClientSendMessage;
    @Bind(R.id.lv_client_msg) ListView mLvClientMsg;
    private List<String> mServerMessageList;
    private List<String> mClientMessageList;
    private ArrayAdapter<String> mServerMessageAdapter;
    private ArrayAdapter<String> mClientMessageAdapter;
    private InputStream mInputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        String serverIp = getLocalIpAddress();
        mEdServerIp.setText(serverIp);
        mEdClientIp.setText(serverIp);
        mServerMessageList = new ArrayList<>();
        mClientMessageList = new ArrayList<>();
        mServerMessageAdapter = new ArrayAdapter<>(this, R.layout.item_log, R.id.item_log, mServerMessageList);
        mClientMessageAdapter = new ArrayAdapter<>(this, R.layout.item_log, R.id.item_log, mClientMessageList);
        mLvServerMsg.setAdapter(mServerMessageAdapter);
        mLvClientMsg.setAdapter(mClientMessageAdapter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // 杀死该应用进程
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private void getServerPort() {
        mServerPort = Integer.parseInt(mEdServerPort.getText().toString().trim());
    }

    private void getServerSendMessage() {
        mServerSendMessage = mEdServerSendMessage.getText().toString().trim();
    }

    private void getClientPort() {
        mClientPort = Integer.parseInt(mEdClientPort.getText().toString().trim());
    }

    private void getClientIp() {
        mClientIp = mEdClientIp.getText().toString().trim();
    }

    private void getClientSendMessage() {
        mClientSendMessage = mEdClientSendMessage.getText().toString().trim();
    }

    @OnClick({
            R.id.btn_start_server,
            R.id.btn_server_send,
            R.id.btn_server_clear_message,
            R.id.btn_start_client,
            R.id.btn_client_send,
            R.id.btn_client_clear_message
    })
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start_server:
                getServerPort();
                new ServerSocketThread().start();
                break;
            case R.id.btn_server_send:
                getServerSendMessage();
                serverSendMessage();
                break;
            case R.id.btn_server_clear_message:
                mServerMessageList.clear();
                mServerMessageAdapter.notifyDataSetChanged();
                break;
            case R.id.btn_start_client:
                getClientIp();
                getClientPort();
                new ClientSocketThread().start();
                break;
            case R.id.btn_client_send:
                getClientSendMessage();
                clientSendMessage();
                break;
            case R.id.btn_client_clear_message:
                mClientMessageList.clear();
                mClientMessageAdapter.notifyDataSetChanged();
                break;
        }
    }

    private int mServerPort;
    private String mServerSendMessage;
    private List<Socket> mSocketList = new ArrayList<>();

    class ServerSocketThread extends Thread {
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(mServerPort);
                final String log = getTime()
                        + " 服务器已启动...";
                Log.d(TAG, log);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mServerMessageList.add(0, log);
                        mServerMessageAdapter.notifyDataSetChanged();
                    }
                });
                while (true) {
                    Socket socket = serverSocket.accept();
                    boolean isContain = false;
                    int index = 0;
                    for (Socket s : mSocketList) {
                        if (s.getInetAddress().equals(socket.getInetAddress())) {
                            isContain = true;
                            break;
                        }
                        index++;
                    }
                    if (isContain) {
                        mSocketList.remove(index);
                        mSocketList.add(socket);
                    } else {
                        mSocketList.add(socket);
                    }
                    final String log2 = getTime()
                            + " 服务端地址：" + socket.getLocalAddress()
                            + " 客户端地址：" + socket.getInetAddress()
                            + " 新的客户端接入，当前客户端数：" + mSocketList.size();
                    Log.d(TAG, log2);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mServerMessageList.add(0, log2);
                            mServerMessageAdapter.notifyDataSetChanged();
                        }
                    });
                    ServerReceiveRunnable runnable = new ServerReceiveRunnable(socket);
                    new Thread(runnable).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
                final String log = getTime()
                        + " 服务端已经启动";
                Log.d(TAG, log);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mServerMessageList.add(0, log);
                        mServerMessageAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    class ServerReceiveRunnable implements Runnable {

        private Socket socket;
        private BufferedReader in;

        public ServerReceiveRunnable(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //客户端只要一连到服务器，便向客户端发送下面的信息。
                mServerSendMessage = " 当前客户端数：" + mSocketList.size();
                serverSendMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String serverReceiverMessage;
                    if ((serverReceiverMessage = in.readLine()) != null) {
                        // 服务端接收到消息
                        final String log = getTime()
                                + " 服务端地址：" + socket.getLocalAddress()
                                + " 客户端地址：" + socket.getInetAddress()
                                + " 服务端收到消息：" + serverReceiverMessage;
                        Log.d(TAG, log);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mServerMessageList.add(0, log);
                                mServerMessageAdapter.notifyDataSetChanged();
                            }
                        });
                        if (serverReceiverMessage.equalsIgnoreCase("exit")) {
                            //当客户端发送的信息为：exit时，关闭连接
                            int size = mSocketList.size() - 1;
                            mServerSendMessage = "客户端：" + socket.getInetAddress()
                                    + " 退出，当前客户端数：" + size;
                            serverSendMessage();
                            mSocketList.remove(socket);
                            in.close();
                            socket.close();
                            break;
                        } else {
                            //服务端发送消息，给单个客户端自动回复
                            mServerSendMessage = serverReceiverMessage + "（服务器自动回复）";
                            final String log2 = getTime()
                                    + " 服务端地址：" + socket.getLocalAddress()
                                    + " 客户端地址：" + socket.getInetAddress()
                                    + " 服务端发送消息：" + mServerSendMessage;
                            Log.d(TAG, log2);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mServerMessageList.add(0, log2);
                                    mServerMessageAdapter.notifyDataSetChanged();
                                }
                            });
                            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                            out.println(mServerSendMessage);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 循环遍历客户端集合，给每个客户端都发送信息。
     */
    private void serverSendMessage() {
        for (int i = 0, size = mSocketList.size(); i < size; i++) {
            Socket socket = mSocketList.get(i);
            final String log = getTime()
                    + " 服务端地址：" + socket.getLocalAddress()
                    + " 客户端地址：" + socket.getInetAddress()
                    + " 服务端发送消息：" + mServerSendMessage;
            Log.d(TAG, log);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mServerMessageList.add(0, log);
                    mServerMessageAdapter.notifyDataSetChanged();
                }
            });
            PrintWriter out;
            try {
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(mServerSendMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //=================================================================================
    // Client
    //=================================================================================

    private String mClientIp;
    private int mClientPort;
    private Socket mClientSocket;
    private BufferedReader mClientIn;
    private PrintWriter mClientOut;
    private String mClientSendMessage;

    class ClientSocketThread extends Thread {
        @Override
        public void run() {
            try {
                mClientSocket = new Socket(mClientIp, mClientPort);
                mClientIn = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));
                mClientOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mClientSocket.getOutputStream())), true);

                mInputStream = mClientSocket.getInputStream();

                ClientReceiverRunnable runnable = new ClientReceiverRunnable(mClientSocket);
                new Thread(runnable).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ClientReceiverRunnable implements Runnable {

        Socket socket;

        public ClientReceiverRunnable(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {

                while (true) {
                    byte[] bytes = "\n".getBytes();
                    for (byte b : bytes) {
                        Log.d(TAG, b + "");
                    }
                    Log.d(TAG, "ClientReceiver");
                    if (socket != null && !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown()) {
//                        String clientReceiverMessage;
//                        if ((clientReceiverMessage = mClientIn.readLine()) != null) {
//                            // 客户端接收到消息
//                            final String log = getTime()
//                                    + " 服务端地址：" + socket.getInetAddress()
//                                    + " 客户端地址：" + socket.getLocalAddress()
//                                    + " 客户端收到消息：" + clientReceiverMessage;
//                            Log.d(TAG, log);
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    mClientMessageList.add(0, log);
//                                    mClientMessageAdapter.notifyDataSetChanged();
//                                }
//                            });
//                        }
                        Log.d(TAG, "准备接收消息");
                        byte[] buf = new byte[1024];
                        int len;
                        StringBuilder sb = new StringBuilder();
                        while ((len = mInputStream.read(buf)) != -1) {//len = -1 时，表示服务器已经断开连接
                            Log.d(TAG, "接收到消息:" + len);
                            sb.append(new String(buf, 0, len));
                        }
                        String clientReceiverMessage = sb.toString();
                        Log.d(TAG, "接收到完整消息：" + clientReceiverMessage);
                        if (clientReceiverMessage.length() != 0) {
                            // 客户端接收到消息
                            final String log = getTime()
                                    + " 服务端地址：" + socket.getInetAddress()
                                    + " 客户端地址：" + socket.getLocalAddress()
                                    + " 客户端收到消息：" + clientReceiverMessage;
                            Log.d(TAG, log);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mClientMessageList.add(0, log);
                                    mClientMessageAdapter.notifyDataSetChanged();
                                }
                            });
                        }

                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void clientSendMessage() {
        String log = getTime()
                + " 服务端地址：" + mClientSocket.getInetAddress()
                + " 客户端地址：" + mClientSocket.getLocalAddress()
                + " 客户端发送消息：" + mClientSendMessage;
        Log.d(TAG, log);
        mClientMessageList.add(0, log);
        mClientMessageAdapter.notifyDataSetChanged();
        if (mClientSocket != null && mClientSocket.isConnected() && !mClientSocket.isOutputShutdown()) {
            mClientOut.println(mClientSendMessage);
        }
    }

    private String getTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINA);
        return format.format(new Date());
    }

    /**
     * 获取WIFI下ip地址
     */
    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();

        //返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }
}
