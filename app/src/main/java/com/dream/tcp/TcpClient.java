package com.dream.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Author:      SuSong
 * Email:       751971697@qq.com | susong0618@163.com
 * GitHub:      https://github.com/susong0618
 * Date:        16/4/5 下午11:00
 * Description: TcpDemo
 */
public class TcpClient {
    private static final String    TAG        = "TcpClient";
    private static       TcpClient mTcpClient = new TcpClient();
    private ClientSocketThread mClientSocketThread;
    private OnConnectListener  mOnConnectListener;

    public static TcpClient getInstance() {
        return mTcpClient;
    }


    public void setOnConnectListener(OnConnectListener onConnectListener) {
        mOnConnectListener = onConnectListener;
    }

    public interface OnConnectListener {
        /**
         * @param inetAddress  服务端地址
         * @param localAddress 客户端地址
         */
        void onConnectSuccess(String inetAddress, String localAddress);

        void onConnectFail(Throwable e, String message);

        void onConnectError(Throwable e, String message);

        void onMessage(String message);
    }

    public void setIp(String ip) {
        mClientIp = ip;
    }

    public void setPort(int port) {
        mClientPort = port;
    }

    public void connect() {
        if (mOnConnectListener == null) {
            throw new RuntimeException("请设置OnConnectListener");
        }
        if (mClientIp == null || mClientIp.length() == 0 || mClientPort == 0) {
            mOnConnectListener.onConnectFail(new RuntimeException("请设置ip与port"), "请设置ip与port");
            return;
        }
        if (mClientSocketThread == null) {
            mClientSocketThread = new ClientSocketThread();
            mClientSocketThread.start();
        } else {
            mOnConnectListener.onConnectError(new RuntimeException("已经建立连接"), "已经建立连接");
        }
    }

    public void disconnect() {
        if (mOnConnectListener == null) {
            throw new RuntimeException("请设置OnConnectListener");
        }
        if (mClientSocket != null) {
            try {
                mClientSocket.close();
                mClientIn.close();
                mClientOut.close();
                mClientOut = null;
                mClientIn = null;
                mClientSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mOnConnectListener.onConnectError(new RuntimeException("已经断开连接"), "已经断开连接");
        }
        if (mClientSocketThread != null) {
            mClientSocketThread = null;
        }
    }

    public void sendMessage(String message) {
        if (mOnConnectListener == null) {
            throw new RuntimeException("请设置OnConnectListener");
        }
        mClientSendMessage = message;
        clientSendMessage();
    }

    private void insideDisconnect() {
        if (mClientSocket != null) {
            try {
                mClientSocket.close();
                mClientIn.close();
                mClientOut.close();
                mClientOut = null;
                mClientIn = null;
                mClientSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mClientSocketThread != null) {
            mClientSocketThread = null;
        }
    }
    //=================================================================================
    // Client
    //=================================================================================

    private String         mClientIp;
    private int            mClientPort;
    private Socket         mClientSocket;
    private BufferedReader mClientIn;
    private PrintWriter    mClientOut;
    private String         mClientSendMessage;

    class ClientSocketThread extends Thread {

        @Override
        public void run() {
            try {
                mClientSocket = new Socket(mClientIp, mClientPort);
                mClientIn = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));
                mClientOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mClientSocket.getOutputStream())), true);
                ClientReceiverRunnable runnable = new ClientReceiverRunnable(mClientSocket);
                new Thread(runnable).start();
                if (mOnConnectListener != null) {
                    mOnConnectListener.onConnectSuccess(mClientSocket.getInetAddress().getHostAddress(), mClientSocket.getLocalAddress().getHostAddress());
                }
            } catch (IOException e) {
                if (mOnConnectListener != null) {
                    mOnConnectListener.onConnectFail(e, "服务器没有开启");
                }
                insideDisconnect();
                e.printStackTrace();
            }
        }
    }

    class ClientReceiverRunnable implements Runnable {

        private Socket socket;

        public ClientReceiverRunnable(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            boolean isContinue = true;
            while (isContinue) {
                try {
                    if (socket != null && !socket.isClosed() && socket.isConnected() && !socket.isInputShutdown()) {
                        String clientReceiverMessage;
                        if ((clientReceiverMessage = mClientIn.readLine()) != null) {//服务器断开连接时，读取的内容为null，这与发送消息为“”不同
                            // 客户端接收到消息
                            if (mOnConnectListener != null) {
                                mOnConnectListener.onMessage(clientReceiverMessage);
                            }
                        } else {
                            //服务端断开连接
                            isContinue = false;
                            if (mOnConnectListener != null) {
                                mOnConnectListener.onConnectError(new RuntimeException("服务器断开连接"), "服务器断开连接");
                            }
                            insideDisconnect();
                        }
                    }
                } catch (Exception e) {
                    isContinue = false;
                    e.printStackTrace();
                    if (mOnConnectListener != null) {
                        mOnConnectListener.onConnectError(e, "客户端连接超时");
                    }
                }
            }
            try {
                if (socket != null) {
                    socket.close();
                }
                if (mClientIn != null) {
                    mClientIn.close();
                }
                if (mClientOut != null) {
                    mClientOut.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void clientSendMessage() {
        if (mClientSendMessage == null || mClientSendMessage.length() == 0) {
            if (mOnConnectListener != null) {
                mOnConnectListener.onConnectError(new RuntimeException("请输入内容"), "请输入内容");
            }
            return;
        }
        if (mClientSocket != null && mClientSocket.isConnected() && !mClientSocket.isOutputShutdown()
            && mClientOut != null) {
            mClientOut.println(mClientSendMessage);
        } else {
            if (mOnConnectListener != null) {
                mOnConnectListener.onConnectError(new RuntimeException("客户端已经断开连接"), "客户端已经断开连接");
            }
        }
    }

}
