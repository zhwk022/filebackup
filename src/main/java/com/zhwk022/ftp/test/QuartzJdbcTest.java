package com.zhwk022.ftp.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzJdbcTest {
    public static String uploadDir = null;
    public static String localDir = null;
    public static String remoteJarDir = null;
    public static String host = null;
    public static String username = null;
    public static String password = null;
    public static Integer port = null;
    public static String startTime = null;
    public static String endTime = null;
    public static String notifyEmail = null;
    public static int bandwidth = 1;
    static Map<String, String> sftpDetails;
    // 当前checkSum文件
    static final String FILECHECKSUMFILE = "fileCheckSum.properties";
    // 之前checkSum文件
    static final String PREFILECHECKSUMFILE = "pre.fileCheckSum.properties";
    // 远程checkSum文件
    static final String REMOTEFILECHECKSUMFILE = "remote.fileCheckSum.properties";
    static final String FILECOUNT = "file.count.properties";
    // <文件名,上传失败次数>
    static final Map<String, Integer> FILENAMEERRORCOUNT = new Hashtable<>();

    static boolean finished = false;

    public static LinuxShellUtil getLinuxShellUtil() {
        return new LinuxShellUtil(host, port, username, password);
    }

    public static Properties getProperties(String fileName) {
        File file = new File(fileName);
        Properties properties = new Properties();
        if (file.exists() && file.isFile()) {
            try (InputStream inputStream = new FileInputStream(fileName)) {
                properties.load(inputStream);
                inputStream.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return properties;
    }

    public static void main(String[] args) throws Exception {
//        String jarFilePath = "C:/Users/zhwk022/IdeaProjects/ftp2/out/artifacts/ftp2_jar/ftp2.jar";
        String jarFilePath = QuartzJdbcTest.class.getProtectionDomain().getCodeSource().getLocation().getFile();
//        System.out.println(jarFilePath);

        Properties properties = getProperties("config.properties");

        uploadDir = properties.getProperty("upload.dir");
        localDir = properties.getProperty("local.dir");
        remoteJarDir = properties.getProperty("remote.jar.dir");
        host = properties.getProperty("host");
        username = properties.getProperty("username");
        password = properties.getProperty("password");
        port = Integer.parseInt(properties.getProperty("port"));
        startTime = properties.getProperty("start.time");
        endTime = properties.getProperty("end.time");

        {
            String[] startTimeArr = startTime.split(":");
            int hour = Integer.parseInt(startTimeArr[0]);
            int minute = Integer.parseInt(startTimeArr[1]);
            if (hour < 0 || hour > 23) {
                throw new Exception("0 <= 开始时间小时 < 24，实际值为" + hour);
            }
            if (minute < 0 || minute > 59) {
                throw new Exception("0 <= 开始时间分钟 < 60，实际值为" + minute);
            }
        }
        {
            String[] endTimeArr = endTime.split(":");
            int hour = Integer.parseInt(endTimeArr[0]);
            int minute = Integer.parseInt(endTimeArr[1]);
            if (hour < 0 || hour > 23) {
                throw new Exception("0 <= 结束时间小时 < 24，实际值为" + hour);
            }
            if (minute < 0 || minute > 59) {
                throw new Exception("0 <= 结束时间分钟 < 60，实际值为" + minute);
            }
        }

        notifyEmail = properties.getProperty("notify.email");
        bandwidth = Integer.parseInt(properties.getProperty("bandwidth"));

        File localDirFile = new File(localDir);
        if (!localDirFile.exists()) {
            throw new RuntimeException(localDirFile + "不存在");
        }
        if (!localDirFile.isDirectory()) {
            throw new RuntimeException(localDir + "不是目录");
        }

        // 设置主机ip，端口，用户名，密码
        sftpDetails = new HashMap<String, String>();
        sftpDetails.put(SftpUtil2.SFTP_REQ_HOST, host);
        sftpDetails.put(SftpUtil2.SFTP_REQ_USERNAME, username);
        sftpDetails.put(SftpUtil2.SFTP_REQ_PASSWORD, password);
        sftpDetails.put(SftpUtil2.SFTP_REQ_PORT, port + "");

        //测试文件上传
        String src = jarFilePath; // 本地文件名
        Properties props = System.getProperties(); //获得系统属性集
        String osName = props.getProperty("os.name").toLowerCase(); //操作系统名称
        if (osName.contains("windows"))
            src = jarFilePath.substring(1); // 本地文件名
        String jarName = jarFilePath.substring(jarFilePath.lastIndexOf("/"));
        String dst = remoteJarDir + jarName; // 目标文件名
        System.out.println(src);

        SftpUtil2.getInstance().uploadFile(jarFilePath, dst, sftpDetails);
        startSchedule();
        //resumeJob();
    }


    /**
     * 开始一个simpleSchedule()调度
     */
    public static void startSchedule() {
        try {
            // 1、创建一个JobDetail实例，指定Quartz
            JobDetail jobDetail = JobBuilder.newJob(StartJob.class)
                    // 任务执行类
                    .withIdentity("job1_1", "jGroup1")
                    // 任务名，任务组
                    .build();

            JobDetail jobDetail2 = JobBuilder.newJob(EndJob.class)
                    // 任务执行类
                    .withIdentity("job1_2", "jGroup2")
                    // 任务名，任务组
                    .build();

            int tmp = 35;

            String[] startTimeArr = startTime.split(":");
            int hour = Integer.parseInt(startTimeArr[0]);
            int minute = Integer.parseInt(startTimeArr[1]);
            String cronExpression = "0 " + minute + " " + hour + " ? * *";


            String[] endTimeArr = endTime.split(":");
            hour = Integer.parseInt(endTimeArr[0]);
            minute = Integer.parseInt(endTimeArr[1]);
            String cronExpression2 = "0 " + minute + " " + hour + " ? * *";
//            String cronExpression = "0 0/5 * * * ?";
//            String cronExpression2 = cronExpression;

            //触发器类型
            CronScheduleBuilder builder = CronScheduleBuilder.cronSchedule(cronExpression);
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1_1", "tGroup1").startNow()
                    .withSchedule(builder)
                    .build();

            SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger3", "group1")
                    .startAt(new Date())
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(2 * 60)
                                    .withRepeatCount(10))//重复执行的次数，因为加入任务的时候马上执行了，所以不需要重复，否则会多一次。
                    .build();

            CronScheduleBuilder builder2 = CronScheduleBuilder.cronSchedule(cronExpression2);
            Trigger trigger2 = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1_2", "tGroup2").startNow()
                    .withSchedule(builder2)
                    .build();

            // 3、创建Scheduler
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            // 4、调度执行
            scheduler.scheduleJob(jobDetail, trigger);
//            try {
//                Thread.sleep(5 * 60 * 1000);
//            } catch (Exception e) {
//
//            }
            scheduler.scheduleJob(jobDetail2, trigger2);
//            try {
//                Thread.sleep(60000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            //关闭调度器
//            scheduler.shutdown();

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从数据库中找到已经存在的job，并重新开户调度
     */
    public static void resumeJob() {
        try {

            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            Scheduler scheduler = schedulerFactory.getScheduler();
            JobKey jobKey = new JobKey("job1_1", "jGroup1");
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            //SELECT TRIGGER_NAME, TRIGGER_GROUP FROM {0}TRIGGERS WHERE SCHED_NAME = {1} AND JOB_NAME = ? AND JOB_GROUP = ?
            // 重新恢复在jGroup1组中，名为job1_1的 job的触发器运行
            if (triggers.size() > 0) {
                for (Trigger tg : triggers) {
                    // 根据类型判断
                    if ((tg instanceof CronTrigger) || (tg instanceof SimpleTrigger)) {
                        // 恢复job运行
                        scheduler.resumeJob(jobKey);
                    }
                }
                scheduler.start();
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}