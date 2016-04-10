package com.dream.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Author:      SuSong
 * Email:       751971697@qq.com | susong0618@163.com
 * GitHub:      https://github.com/susong0618
 * Date:        16/4/5 下午11:01
 * Description: TcpDemo
 */
public class TcpServer {

    private static final String    TAG        = "TcpServer";
    private static       TcpServer mTcpServer = new TcpServer();
    private OnListener         mOnListener;
    private ServerSocketThread mServerSocketThread;

    public static TcpServer getInstance() {
        return mTcpServer;
    }

    public void setOnListener(OnListener onListener) {
        mOnListener = onListener;
    }

    public interface OnListener {
        void onStart();

        void onNewClient(String serverIp, String clientIp, int count);

        void onError(Throwable e, String message);

        void onMessage(String ip, String message);

        void onAutoReplyMessage(String ip, String message);

        void onClientDisConnect(String ip);

        void onConnectTimeOut(String ip);
    }

    public void setPort(int port) {
        mServerPort = port;
    }

    public void start() {
        if (mOnListener == null) {
            throw new RuntimeException("请设置OnListener");
        }
        if (mServerPort == 0) {
            mOnListener.onError(new RuntimeException("请设置port"), "请设置port");
            return;
        }
        if (mServerSocketThread == null) {
            mServerSocketThread = new ServerSocketThread();
            new Thread(mServerSocketThread).start();
        } else {
            mOnListener.onError(new RuntimeException("服务端已经启动过了"), "服务端已经启动过了");
        }
    }

    public void stop() {
        if (mOnListener == null) {
            throw new RuntimeException("请设置OnListener");
        }
        if (mServerSocketThread != null) {
            mServerSocketThread.close();
            mServerSocketThread = null;
        } else {
            mOnListener.onError(new RuntimeException("服务端已经关闭"), "服务端已经关闭");
        }
    }

    public void sendMessage(String message, String... ips) {
        if (mOnListener == null) {
            throw new RuntimeException("请设置OnListener");
        }
        mServerSendMessage = message;
        if (!check()) {
            return;
        }
        if (ips == null || ips.length == 0) {
            serverSendMessage();
        } else {
            for (Socket s : mSocketList) {
                for (String ip : ips) {
                    if (ip.equals(s.getInetAddress().getHostAddress())) {
                        PrintWriter out;
                        try {
                            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
                            out.println(mServerSendMessage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    //=================================================================================
    // Server
    //=================================================================================

    private int    mServerPort;
    private String mServerSendMessage;
    private List<Socket>                mSocketList                = new ArrayList<>();
    private List<ServerReceiveRunnable> mServerReceiveRunnableList = new ArrayList<>();

    class ServerSocketThread implements Runnable {

        private ServerSocket mServerSocket;
        private boolean isContinue = true;

        public void close() {
            try {
                isContinue = false;
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(mServerPort);
                if (mOnListener != null) {
                    mOnListener.onStart();
                }
                while (isContinue) {
                    Socket  socket    = mServerSocket.accept();
                    boolean isContain = false;
                    int     index     = 0;
                    for (Socket s : mSocketList) {
                        if (s.getInetAddress().equals(socket.getInetAddress())) {
                            isContain = true;
                            break;
                        }
                        index++;
                    }
                    if (isContain) {
                        mServerReceiveRunnableList.remove(index).close();
                        mSocketList.remove(index);
                    }
                    mSocketList.add(socket);
                    if (mOnListener != null) {
                        mOnListener.onNewClient(socket.getLocalAddress().getHostAddress(), socket.getInetAddress().getHostAddress(), mSocketList.size());
                    }
                    ServerReceiveRunnable serverReceiveRunnable = new ServerReceiveRunnable(socket);
                    new Thread(serverReceiveRunnable).start();
                    mServerReceiveRunnableList.add(serverReceiveRunnable);
                }
            } catch (Exception e) {
                if (mOnListener != null) {
                    mOnListener.onError(new RuntimeException("服务端已经启动"), "服务端已经启动");
                }
            }
        }
    }

    class ServerReceiveRunnable implements Runnable {

        private String mLocalAddress;//服务端地址
        private String mInetAddress;//客户端地址
        private boolean isContinue = true;
        private Socket         socket;
        private BufferedReader in;

        public void close() {
            try {
                isContinue = false;
                socket.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public ServerReceiveRunnable(Socket socket) {
            this.socket = socket;
            mLocalAddress = socket.getLocalAddress().getHostAddress();
            mInetAddress = socket.getInetAddress().getHostAddress();
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
                        if (mOnListener != null) {
                            mOnListener.onMessage(mInetAddress, serverReceiverMessage);
                        }
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
                            if (mOnListener != null) {
                                mOnListener.onAutoReplyMessage(mInetAddress, mServerSendMessage);
                            }
                            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                            out.println(mServerSendMessage);
                        }
                    } else {
                        //客户端断开连接
                        isContinue = false;
                        if (mOnListener != null) {
                            mOnListener.onClientDisConnect(mInetAddress);
                        }
                    }
                } catch (IOException e) {
                    isContinue = false;
                    e.printStackTrace();
                    if (mOnListener != null) {
                        mOnListener.onConnectTimeOut(mInetAddress);
                    }
                }
            }
            try {
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

    private boolean check() {
        if (mServerSendMessage == null || mServerSendMessage.length() == 0) {
            if (mOnListener != null) {
                mOnListener.onError(new RuntimeException("请输入内容"), "请输入内容");
            }
            return false;
        }
        if (mSocketList == null || mSocketList.size() == 0) {
            if (mOnListener != null) {
                mOnListener.onError(new RuntimeException("没有连接中的客户端"), "没有连接中的客户端");
            }
            return false;
        }
        return true;
    }

    /**
     * 循环遍历客户端集合，给每个客户端都发送信息。
     */
    private void serverSendMessage() {
        if (!check()) {
            return;
        }
        for (int i = 0, size = mSocketList.size(); i < size; i++) {
            Socket      socket = mSocketList.get(i);
            PrintWriter out;
            try {
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                out.println(mServerSendMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
