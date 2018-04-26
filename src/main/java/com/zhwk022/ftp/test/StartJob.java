package com.zhwk022.ftp.test;

import org.apache.commons.lang.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static com.zhwk022.ftp.test.QuartzJdbcTest.FILENAMEERRORCOUNT;

public class StartJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(StartJob.class);

    public void execute2(JobExecutionContext context) throws JobExecutionException {
        System.out.println("StartJob start............" + new Date());
        String fileName = "tools_r25.2.3-windows.zip";
        String dst = QuartzJdbcTest.uploadDir + "/" + fileName;
        System.out.println(dst);

        //判断是否上传成功
        String host = QuartzJdbcTest.host;
        int port = QuartzJdbcTest.port;
        String username = QuartzJdbcTest.username;
        String password = QuartzJdbcTest.password;
        String remoteJarDir = QuartzJdbcTest.remoteJarDir;
        String file = dst;
        System.out.println(host);
        System.out.println(port);
        System.out.println(username);
        System.out.println(password);
        System.out.println(remoteJarDir);
        System.out.println(file);
        String response = LinuxShellUtil.getSHA256CheckSum(host, port, username, password, remoteJarDir, file);
        System.out.println(response);
    }
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("StartJob start............" + new Date());

        QuartzJdbcTest.finished = false;

        FILENAMEERRORCOUNT.clear();
        // 本地文件名校验码
        Properties fileCountProperties = QuartzJdbcTest.getProperties(QuartzJdbcTest.FILECOUNT);
        while (fileCountProperties.keys().hasMoreElements()) {
            String fileName = fileCountProperties.keys().nextElement().toString();
            int count = Integer.parseInt(fileCountProperties.getProperty(fileName));
            FILENAMEERRORCOUNT.put(fileName, count);
        }

        File checkSumFile = new File(QuartzJdbcTest.FILECHECKSUMFILE);

        // 上传同步的文件名校验码
        Properties preProperties = new Properties();
        if (checkSumFile.exists() && checkSumFile.isFile()) {
            checkSumFile.renameTo(new File(QuartzJdbcTest.PREFILECHECKSUMFILE));

            preProperties = QuartzJdbcTest.getProperties(QuartzJdbcTest.PREFILECHECKSUMFILE);
        }
        // 生成本地文件名校验码
        SHA256File.generateSHA256PropertiesFile(new File(QuartzJdbcTest.localDir));

        // 本地文件名校验码
        Properties properties = QuartzJdbcTest.getProperties(QuartzJdbcTest.FILECHECKSUMFILE);

        // 上传文件名校验码
        File uploadCheckSumFile = new File(QuartzJdbcTest.REMOTEFILECHECKSUMFILE);
        Properties properties2 = new Properties();
        if (uploadCheckSumFile.exists() && uploadCheckSumFile.isFile()) {
            properties2 = QuartzJdbcTest.getProperties(QuartzJdbcTest.REMOTEFILECHECKSUMFILE);
        }

        // 本地文件名校验名与远程文件名校验码对比
        List<String> remoteFileNames = new ArrayList<>();
        // 需要重新上传的文件，
        Queue<String> uploadFileNames = new LinkedBlockingQueue<>();
        //新上传的文件
        Queue<String> uploadFileNames2 = new LinkedBlockingQueue<>();
        for(String fileName : properties2.stringPropertyNames()) {
            remoteFileNames.add(fileName);
        }
        for(String fileName : properties.stringPropertyNames()){
            if (remoteFileNames.contains(fileName)) {
                if (!properties.getProperty(fileName).equals(properties2.getProperty(fileName))) {
                    uploadFileNames.add(fileName);
                }
            } else {
                uploadFileNames2.add(fileName);
            }
        }
        if (QuartzJdbcTest.finished) {
            return;
        }

        Queue<String> queue = new LinkedBlockingQueue<>();
        // 上传第一次上传的文件
        upload(uploadFileNames2, queue, properties, properties2);
        // TODO 后期判断传一半的文件进行优化
        upload(uploadFileNames, queue, properties, properties2);
        // 上传失败的文件
        upload(queue, queue, properties, properties2);

        List<String> fileNames = new ArrayList<>();
        if (QuartzJdbcTest.finished) {
            fileNames.addAll(uploadFileNames);
            fileNames.addAll(uploadFileNames2);
            fileNames.addAll(queue);
            List<String> fail3List = new ArrayList<String>();
            for (String fileName : fileNames) {
                addCount(fileName, fail3List);
            }
            if (fail3List.size() > 0) {
                String emailContent = StringUtils.join(fail3List, "\n");
                //TODO 发送邮件
                System.out.println(emailContent);
            }
        }

        QuartzJdbcTest.finished = true;

        Properties tmp = new Properties();
        for (String key : FILENAMEERRORCOUNT.keySet()) {
            tmp.put(key, FILENAMEERRORCOUNT.get(key));
        }
        try (FileWriter fw = new FileWriter(QuartzJdbcTest.FILECOUNT)) {
            tmp.store(fw, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (FileWriter fw = new FileWriter(QuartzJdbcTest.REMOTEFILECHECKSUMFILE)) {
            properties2.store(fw, "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("StartJob end............" + new Date());
    }

    public void upload(Queue<String> fileNames, Queue<String> failQueue, Properties properties, Properties remoteProperties) {
        while (true) {
            if (QuartzJdbcTest.finished) break;
            String fileName = fileNames.poll();
            if (fileName == null) {
                break;
            }
            String src = QuartzJdbcTest.localDir + "/" + fileName;
            String dst = QuartzJdbcTest.uploadDir + "/" + fileName;
            try {
                SftpUtil2.getInstance().uploadFile(src, dst, true);
            } catch (Exception e) {
                failQueue.add(fileName);
                continue;
            }
            //判断是否上传成功
            String host = QuartzJdbcTest.host;
            int port = QuartzJdbcTest.port;
            String username = QuartzJdbcTest.username;
            String password = QuartzJdbcTest.password;
            String remoteJarDir = QuartzJdbcTest.remoteJarDir;
            String file = dst;
            String response = LinuxShellUtil.getSHA256CheckSum(host, port, username, password, remoteJarDir, file);
            String sha256checksum = properties.getProperty(fileName);
            System.out.println(sha256checksum);
            if (!response.contains(sha256checksum)) {
                failQueue.add(fileName);
                continue;
            }
            remoteProperties.put(fileName,sha256checksum);
            FILENAMEERRORCOUNT.remove(fileName);
        }
    }

    public void addCount(String fileName, List<String> fail3List) {
        Integer count = FILENAMEERRORCOUNT.get(fileName);
        if (count == null)
            count = 0;
        if (count + 1 >= 3) {
            fail3List.add("[" + fileName + "," + (count + 1) + "]");
        }
        FILENAMEERRORCOUNT.put(fileName, count + 1);
    }
}