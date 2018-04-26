package com.zhwk022.ftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

public class DownLoadThread implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(SFTPProcessor2.class);
    private ChannelSftp channel = null;
    private Session session = null;
    private ProgressMonitorByBytes monitor = null;
    private String remotef = null;
    private String remotep = null;
    private String lf = null;
    private String ftpuser = null;
    private String ftphost = null;
    private String transid = null;

    public DownLoadThread(Session ss, String rf, String rp, String lf, String host, String user, String tid) {
        this.session = ss;
        //this.monitor = p;
        this.remotef = rf;
        this.remotep = rp;
        this.lf = lf;
        this.ftphost = host;
        this.ftpuser = user;
        this.transid = tid;
    }

    @Override
    public void run() {

        try {
            channel = getOpenCh(session, ftphost, ftpuser);
            monitor = new ProgressMonitorByBytes(transid, remotef, this.getRemoteFilesize1(channel, remotef, remotep));
            downloadFile(this.remotef, this.remotep, this.lf);
            closeChannel();

            //Thread.currentThread().interrupt();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public ChannelSftp getOpenCh(Session ss, String ftphost, String ftpusername) {
        try {
            LOGGER.debug("Opening SFTP Channel.");
            channel = (ChannelSftp) ss.openChannel("sftp"); // 打开SFTP通道
            channel.connect(); // 建树SFTP通道的连接
            LOGGER.debug("Connected successfully to ftpHost = " + ftphost
                    + ",as ftpUserName = " + ftpusername + ", returning: "
                    + channel);
        } catch (JSchException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return channel;
    }

    public void closeChannel() {
        try {
            if (channel != null) {
                channel.disconnect();
            }

        } catch (Exception e) {
            LOGGER.error("close sftp error", e);
            // throw new AppBizException(AppExcCodes.CLOSE_FTP_ERROR,
            // "close ftp error.");
        }
    }

    public void downloadFile(String remoteFile, String remotePath, String localFile) {

        OutputStream output = null;
        File file = null;

        try {
            file = new File(localFile);


            if (!checkFileExist(localFile)) {
                file.createNewFile();
                output = new FileOutputStream(file);
                channel.cd(remotePath);
                //channel.get(remoteFile, output);
                channel.get(remoteFile, localFile
                        //, new ProgressMonitor(getRemoteFilesize1(channel,remoteFile,remotePath))
                        , monitor
                        , ChannelSftp.OVERWRITE);

            } else {
                //output = new FileOutputStream(file);
                channel.cd(remotePath);

                channel.get(remoteFile, localFile
                        //, new ProgressMonitor(getRemoteFilesize1(channel,remoteFile,remotePath))
                        , monitor
                        , ChannelSftp.RESUME);

            }


        } catch (Exception e) {
            LOGGER.error("Download file error", e);

        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {

                }
            }
            if (file != null) {
                //file.delete();
            }
        }
    }

    private boolean checkFileExist(String localPath) {
        File file = new File(localPath);
        return file.exists();
    }

    public long getRemoteFilesize1(ChannelSftp cf, String remoteFile, String remotepath) {

        Object o = null;
        long s = 0;
        try {
            Vector v = cf.ls(cf.pwd() + "/" + remotepath + "/" + remoteFile);
            if (v != null && v.size() == 1) {
                o = v.firstElement();
            }
            //System.out.println();
            ChannelSftp.LsEntry cl = (ChannelSftp.LsEntry) o;
            s = cl.getAttrs().getSize();
            System.out.println(s + "(bytes)");
        } catch (SftpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return s;
    }
}