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

    @Bind(R.id.ed_server_ip)           EditText             mEdServerIp;
    @Bind(R.id.ed_server_port)         EditText             mEdServerPort;
    @Bind(R.id.ed_server_send_message) EditText             mEdServerSendMessage;
    @Bind(R.id.lv_server_msg)          ListView             mLvServerMsg;
    @Bind(R.id.ed_client_ip)           EditText             mEdClientIp;
    @Bind(R.id.ed_client_port)         EditText             mEdClientPort;
    @Bind(R.id.ed_client_send_message) EditText             mEdClientSendMessage;
    @Bind(R.id.lv_client_msg)          ListView             mLvClientMsg;
    private                            List<String>         mServerMessageList;
    private                            List<String>         mClientMessageList;
    private                            ArrayAdapter<String> mServerMessageAdapter;
    private                            ArrayAdapter<String> mClientMessageAdapter;
    private                            String               mClientIp;
    private                            int                  mClientPort;
    private                            String               mClientSendMessage;
    private                            String               mClientInetAddress;//服务端地址
    private                            String               mClientLocalAddress;//客户端地址
    private                            TcpClient            mTcpClient;
    private                            TcpServer            mTcpServer;
    private                            int                  mServerPort;
    private                            String               mServerSendMessage;
    private                            String               mServerInetAddress;//客户端地址
    private                            String               mServerLocalAddress;//服务端地址

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
                mTcpServer = TcpServer.getInstance();
                mTcpServer.setPort(mServerPort);
                mTcpServer.setOnListener(new TcpServer.OnListener() {
                    @Override
                    public void onStart() {
                        String log = getTime() + " 服务器启动";
                        logServer(log);
                    }

                    @Override
                    public void onNewClient(String serverIp, String clientIp, int count) {
                        mServerLocalAddress = serverIp;
                        String log = getTime()
                                     + " 服务端地址：" + serverIp
                                     + " 客户端地址：" + clientIp
                                     + " 新的客户端接入，当前客户端数：" + count;
                        logServer(log);
                    }

                    @Override
                    public void onError(Throwable e, String message) {
                        String log = getTime() + " " + message;
                        logServer(log);
                    }

                    @Override
                    public void onMessage(String ip, String message) {
                        String log = getTime()
                                     + " 服务端地址：" + mServerLocalAddress
                                     + " 客户端地址：" + ip
                                     + " 服务端收到消息：" + message;
                        logServer(log);
                    }

                    @Override
                    public void onAutoReplyMessage(String ip, String message) {
                        String log = getTime()
                                     + " 服务端地址：" + mServerLocalAddress
                                     + " 客户端地址：" + ip
                                     + " 服务器自动回复：" + message;
                        logServer(log);
                    }

                    @Override
                    public void onClientDisConnect(String ip) {
                        String log = getTime()
                                     + " 服务端地址：" + mServerLocalAddress
                                     + " 客户端地址：" + ip
                                     + " 客户端断开连接";
                        logServer(log);
                    }

                    @Override
                    public void onConnectTimeOut(String ip) {
                        String log = getTime()
                                     + " 服务端地址：" + mServerLocalAddress
                                     + " 客户端地址：" + ip
                                     + " 连接超时";
                        logServer(log);
                    }
                });
                mTcpServer.start();
                break;
            case R.id.btn_server_send:
                getServerSendMessage();
                if (mTcpServer != null) {
                    mTcpServer.sendMessage(mServerSendMessage);
                }
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
                        mClientInetAddress = inetAddress;
                        mClientLocalAddress = localAddress;
                        String log = getTime()
                                     + " 服务端地址：" + mClientInetAddress
                                     + " 客户端地址：" + mClientLocalAddress
                                     + " 连接成功";
                        logClient(log);
                    }

                    @Override
                    public void onConnectFail(Throwable e, String message) {
                        String log = getTime()
                                     + " 服务端地址：" + mClientInetAddress
                                     + " 客户端地址：" + mClientLocalAddress
                                     + message;
                        logClient(log);
                    }

                    @Override
                    public void onConnectError(Throwable e, String message) {
                        String log = getTime()
                                     + " 服务端地址：" + mClientInetAddress
                                     + " 客户端地址：" + mClientLocalAddress
                                     + message;
                        logClient(log);
                    }

                    @Override
                    public void onMessage(String message) {
                        String log = getTime()
                                     + " 服务端地址：" + mClientInetAddress
                                     + " 客户端地址：" + mClientLocalAddress
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
        WifiInfo    wifiInfo    = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();

        //返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d",
                             (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                             (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }
}
