package com.zhwk022.ftp.test;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class EndJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(EndJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("EndJob............" + new Date());
        if (!QuartzJdbcTest.finished) {
            try {
                SftpUtil2.getInstance().closeChannel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        QuartzJdbcTest.finished = true;
    }
}