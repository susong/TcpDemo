package com.dream.tcp;

/**
 * Author:      SuSong
 * Email:       751971697@qq.com | susong0618@163.com
 * GitHub:      https://github.com/susong0618
 * Date:        16/4/5 下午11:01
 * Description: TcpDemo
 */
public class TcpServer {

    private static final String TAG = "TcpServer";
    private static TcpServer mTcpServer = new TcpServer();

    public static TcpServer getInstance() {
        return mTcpServer;
    }
}
