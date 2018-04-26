package com.zhwk022.ftp;

import com.jcraft.jsch.SftpProgressMonitor;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.Formatter;

public class ProgressMonitorByBytes implements SftpProgressMonitor {

    private long transfered;
    private long filesize;
    private String remotef;
    private Formatter f = null;
    private long stime;
    private long etime;
    private String transferID;

    public ProgressMonitorByBytes(String transid, String remotefile,
                                  long totalsize
    ) {
        this.filesize = totalsize;
        this.remotef = remotefile;
        this.transferID = transid;
    }


    public void sendmsg() {
        DecimalFormat df = new DecimalFormat("#.##");
        String per = df.format(100 * (1.0f * transfered / filesize));
        System.out.println("Currently transferred total size: " + transfered + " bytes, percent: " + per + "% [" + remotef + "]");
        f.format("%1$10s\n", "Currently transferred total size: " + transfered + " bytes, percent: " + per + "% [" + remotef + "] ");
    }

    @Override
    public boolean count(long count) {
        long now = System.currentTimeMillis();
        long timeelapse = (now - stime);
        boolean cancelCondition = (timeelapse > 10 * 1000) && "TRANS1000".equals(transferID);

        if (transfered != filesize) {
            sendmsg();
            transfered = transfered + count;
            if (cancelCondition) {    //作为取消下载使用，可以取消(暂停下载)
                f.format("%1$10s\n", "Cancel transfer: [" + remotef + "]");
                System.out.printf("%1$10s\n", "Cancel transfer: [" + remotef + "]");
                return false;
            }

            boolean sleepCondition = (1000 * (1.0d / 1024) * transfered / timeelapse) > 4096;
            if (sleepCondition) {
                try {
                    Thread.currentThread().sleep(1000); //作为限速使用，貌似可以限制下载速度
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            return true;

        }


        return false;
    }

    @Override
    public void end() {
        etime = System.currentTimeMillis();
        sendmsg();
        System.out.println("Transferring done. [" + remotef + "]");
        f.format("%1$10s\n", "Transferring done. [" + remotef + "]");
        f.format("%1$10s\n", "Time Elapsed:" + getTimePassed(etime - stime) + " [" + remotef + "]");
        f.close();

    }

    public String getTimePassed(long times) {
        String res = "";
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        long millseconds = 0;
        hours = times / (1000 * 60 * 60);
        minutes = (times - hours * (1000 * 60 * 60)) / (1000 * 60);
        seconds = (times - hours * (1000 * 60 * 60) - minutes * 1000 * 60) / 1000;
        millseconds = times - hours * (1000 * 60 * 60) - minutes * 1000 * 60 - seconds * 1000;
        res = Long.toString(hours, 10) + "小时" + Long.toString(minutes, 10) + "分钟" + Long.toString(seconds, 10) + "秒" + Long.toString(millseconds, 10) + "毫秒";

        return res;
    }

    /**
     * @param arg0 1:下载，0:上传
     * @param arg1 原始文件PATH
     * @param arg2 目标文件PATH
     * @param arg3 文件大小
     */
    @Override
    public void init(int arg0, String arg1, String arg2, long arg3) {
        stime = System.currentTimeMillis();
        try {
            f = new Formatter(new FileOutputStream("E:/xdownlog.log", true));

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        f.format("%1$10s\n", "Transferring begin. [" + remotef + "]");
        //filesize = arg3;
        System.out.println("Transferring begin. [" + remotef + "]");
        System.out.println("======================");
        System.out.println(arg0);
        System.out.println(arg1);
        System.out.println(arg2);
        System.out.println(arg3);
        System.out.println("======================");


    }

}
