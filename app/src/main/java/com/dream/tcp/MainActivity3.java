package com.dream.tcp;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
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

public class MainActivity3 extends AppCompatActivity {

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
    private String mInetAddress;
    private String mLocalAddress;
    private TcpClient mTcpClient;

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

                mTcpClient = TcpClient.getInstance();
                mTcpClient.setIp(mClientIp);
                mTcpClient.setPort(mClientPort);

                mTcpClient.setOnConnectListener(new TcpClient.OnConnectListener() {
                    @Override
                    public void onConnectSuccess(String inetAddress, String localAddress) {
                        mInetAddress = inetAddress;
                        mLocalAddress = localAddress;
                        String log = getTime()
                                + " 服务端地址：" + mInetAddress
                                + " 客户端地址：" + mLocalAddress
                                + " 连接成功";
                        logClient(log);
                    }

                    @Override
                    public void onConnectFail(Throwable e, String message) {
                        String log = getTime()
                                + " 服务端地址：" + mInetAddress
                                + " 客户端地址：" + mLocalAddress
                                + message;
                        logClient(log);
                    }

                    @Override
                    public void onConnectError(Throwable e, String message) {
                        String log = getTime()
                                + " 服务端地址：" + mInetAddress
                                + " 客户端地址：" + mLocalAddress
                                + message;
                        logClient(log);
                    }

                    @Override
                    public void onMessage(String message) {
                        String log = getTime()
                                + " 服务端地址：" + mLocalAddress
                                + " 客户端地址：" + mInetAddress
                                + message;
                        logClient(log);
                    }
                });

                mTcpClient.connect();

                break;
            case R.id.btn_client_send:
                getClientSendMessage();
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(mClientSendMessage);
                }
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
    private List<ServerReceiveRunnable> mServerReceiveRunnableList = new ArrayList<>();

    class ServerSocketThread extends Thread {
        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(mServerPort);
                String logStart = getTime()
                        + " 服务器已启动...";
                logServer(logStart);
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
                        Log.d(TAG, "isContain");
                        mServerReceiveRunnableList.remove(index).close();
                        mSocketList.remove(index);
                    }
                    mSocketList.add(socket);
                    String logAccept = getTime()
                            + " 服务端地址：" + socket.getLocalAddress()
                            + " 客户端地址：" + socket.getInetAddress()
                            + " 新的客户端接入，当前客户端数：" + mSocketList.size();
                    logServer(logAccept);
                    ServerReceiveRunnable serverReceiveRunnable = new ServerReceiveRunnable(socket);
                    new Thread(serverReceiveRunnable).start();
                    mServerReceiveRunnableList.add(serverReceiveRunnable);
                }
            } catch (Exception e) {
                e.printStackTrace();
                String log = getTime()
                        + " 服务端已经启动";
                logServer(log);
            }
        }
    }

    class ServerReceiveRunnable implements Runnable {

        private InetAddress mLocalAddress;//服务端地址
        private InetAddress mInetAddress;//客户端地址
        private boolean isContinue = true;
        private Socket socket;
        private BufferedReader in;

        public void close() {
            try {
                socket.close();
                in.close();
                isContinue = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public ServerReceiveRunnable(Socket socket) {
            this.socket = socket;
            mLocalAddress = socket.getLocalAddress();
            mInetAddress = socket.getInetAddress();
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
            while (isContinue) {
                try {
                    String serverReceiverMessage;
                    if ((serverReceiverMessage = in.readLine()) != null) {
                        // 服务端接收到消息
                        String logReceiver = getTime()
                                + " 服务端地址：" + mLocalAddress
                                + " 客户端地址：" + mInetAddress
                                + " 服务端收到消息：" + serverReceiverMessage;
                        logServer(logReceiver);
                        if (serverReceiverMessage.equalsIgnoreCase("exit")) {
                            //当客户端发送的信息为：exit时，关闭连接
                            int size = mSocketList.size() - 1;
                            mServerSendMessage = "客户端：" + socket.getInetAddress()
                                    + " 退出，当前客户端数：" + size;
                            serverSendMessage();
                            mSocketList.remove(socket);
                            in.close();
                            socket.close();
                            isContinue = false;
                        } else {
                            //服务端发送消息，给单个客户端自动回复
                            mServerSendMessage = serverReceiverMessage + "（服务器自动回复）";
                            String logSend = getTime()
                                    + " 服务端地址：" + mLocalAddress
                                    + " 客户端地址：" + mInetAddress
                                    + " 服务端发送消息：" + mServerSendMessage;
                            logServer(logSend);
                            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                            out.println(mServerSendMessage);
                        }
                    } else {
                        //客户端断开连接
                        isContinue = false;
                        String log = getTime()
                                + " 服务端地址：" + mLocalAddress
                                + " 客户端地址：" + mInetAddress
                                + " 客户端断开连接";
                        logServer(log);
                    }
                } catch (IOException e) {
                    isContinue = false;
                    e.printStackTrace();
                    String log = getTime()
                            + " 服务端地址：" + mLocalAddress
                            + " 客户端地址：" + mInetAddress
                            + " 服务端连接超时";
                    logServer(log);
                }
            }
            try {
                String log = getTime()
                        + " 服务端地址：" + mLocalAddress
                        + " 客户端地址：" + mInetAddress
                        + " 关闭与客户端的连接";
                logServer(log);
                if (in != null) {
                    in.close();
                }
                if (socket != null) {
                    socket.close();
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
        if (mServerSendMessage == null || mServerSendMessage.length() == 0) {
            String log = getTime()
                    + " 请输入内容";
            logServer(log);
            return;
        }
        if (mSocketList == null || mSocketList.size() == 0) {
            String log = getTime()
                    + " 没有连接中的客户端";
            logServer(log);
            return;
        }
        for (int i = 0, size = mSocketList.size(); i < size; i++) {
            Socket socket = mSocketList.get(i);
            String log = getTime()
                    + " 服务端地址：" + socket.getLocalAddress()
                    + " 客户端地址：" + socket.getInetAddress()
                    + " 服务端发送消息：" + mServerSendMessage;
            logServer(log);
            PrintWriter out;
            try {
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(mServerSendMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void logServer(final String log) {
        if (!isCurrentlyOnMainThread()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logServerReal(log);
                }
            });
        } else {
            logServerReal(log);
        }
    }

    private void logServerReal(final String log) {
        Log.d(TAG, log);
        mServerMessageList.add(0, log);
        mServerMessageAdapter.notifyDataSetChanged();
    }

    //=================================================================================
    // Client
    //=================================================================================

    private String mClientIp;
    private int mClientPort;
    //    private Socket mClientSocket;
//    private BufferedReader mClientIn;
//    private PrintWriter mClientOut;
    private String mClientSendMessage;
//
//    class ClientSocketThread extends Thread {
//
//        @Override
//        public void run() {
//            try {
//                mClientSocket = new Socket(mClientIp, mClientPort);
//                mClientIn = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));
//                mClientOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mClientSocket.getOutputStream())), true);
//                ClientReceiverRunnable runnable = new ClientReceiverRunnable(mClientSocket);
//                new Thread(runnable).start();
//            } catch (IOException e) {
//                String log = getTime()
//                        + " 服务器没有开启";
//                logClient(log);
//                e.printStackTrace();
//            }
//        }
//    }
//
//    class ClientReceiverRunnable implements Runnable {
//
//        private InetAddress mInetAddress;
//        private InetAddress mLocalAddress;
//        private Socket socket;
//
//        public ClientReceiverRunnable(Socket socket) {
//            this.socket = socket;
//            mInetAddress = socket.getInetAddress();//服务端地址
//            mLocalAddress = socket.getLocalAddress();//客户端地址
//        }
//
//        @Override
//        public void run() {
//            boolean isContinue = true;
//            while (isContinue) {
//                try {
//                    if (socket != null && !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown()) {
//                        String clientReceiverMessage;
//                        if ((clientReceiverMessage = mClientIn.readLine()) != null) {//服务器断开连接时，读取的内容为null，这与发送消息为“”不同
//                            // 客户端接收到消息
//                            String log = getTime()
//                                    + " 服务端地址：" + mInetAddress
//                                    + " 客户端地址：" + mLocalAddress
//                                    + " 客户端收到消息：" + clientReceiverMessage;
//                            logClient(log);
//                        } else {
//                            //服务端断开连接
//                            isContinue = false;
//                            String log = getTime()
//                                    + " 服务端地址：" + mInetAddress
//                                    + " 客户端地址：" + mLocalAddress
//                                    + " 服务器断开连接";
//                            logClient(log);
//                        }
//                    }
//                } catch (Exception e) {
//                    isContinue = false;
//                    e.printStackTrace();
//                    String log = getTime()
//                            + " 服务端地址：" + mInetAddress
//                            + " 客户端地址：" + mLocalAddress
//                            + " 客户端连接超时";
//                    logClient(log);
//                }
//            }
//            try {
//                String log = getTime()
//                        + " 服务端地址：" + mInetAddress
//                        + " 客户端地址：" + mLocalAddress
//                        + " 客户端关闭连接";
//                logClient(log);
//                if (socket != null) {
//                    socket.close();
//                }
//                if (mClientIn != null) {
//                    mClientIn.close();
//                }
//                if (mClientOut != null) {
//                    mClientOut.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private void clientSendMessage() {
//        if (mClientSendMessage == null || mClientSendMessage.length() == 0) {
//            String log = getTime()
//                    + " 请输入内容";
//            logClient(log);
//            return;
//        }
//        if (mClientSocket != null && mClientSocket.isConnected() && !mClientSocket.isOutputShutdown()) {
//            String log = getTime()
//                    + " 服务端地址：" + mClientSocket.getInetAddress()
//                    + " 客户端地址：" + mClientSocket.getLocalAddress()
//                    + " 客户端发送消息：" + mClientSendMessage;
//            logClient(log);
//            mClientOut.println(mClientSendMessage);
//        } else {
//            String log = getTime()
//                    + " 客户端已经断开连接";
//            logClient(log);
//        }
//    }

    private void logClient(final String log) {
        if (!isCurrentlyOnMainThread()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logClientReal(log);
                }
            });
        } else {
            logClientReal(log);
        }
    }

    private void logClientReal(final String log) {
        Log.d(TAG, log);
        mClientMessageList.add(0, log);
        mClientMessageAdapter.notifyDataSetChanged();
    }

    /**
     * 判断当前线程是否在主线程
     */
    private boolean isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /**
     * 获取当前时间
     */
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
