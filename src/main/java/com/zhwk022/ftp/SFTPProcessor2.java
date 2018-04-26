package com.zhwk022.ftp;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.log4j.Logger;

import java.util.*;

public class SFTPProcessor2 {

    private static final Logger LOGGER = Logger.getLogger(SFTPProcessor2.class);

    private static Session session = null;


    public Session getConnect(Map<String, String> serverMap) {
        String ftpHost = serverMap.get(SFTPConstants.SFTP_SERVER_HOST);
        String port = serverMap.get(SFTPConstants.SFTP_SERVER_PORT);
        String ftpUserName = serverMap.get(SFTPConstants.SFTP_SERVER_USERNAME);
        String ftpPassword = serverMap.get(SFTPConstants.SFTP_SERVER_PASSWORD);

        // 默认的端口22 此处我是定义到常量类中；
        int ftpPort = SFTPConstants.SFTP_DEFAULT_PORT;

        // 判断端口号是否为空，如果不为空，则赋值
        if (port != null && !port.equals("")) {
            ftpPort = Integer.valueOf(port);
        }
        JSch jsch = new JSch(); // 创建JSch对象
        // 按照用户名,主机ip,端口获取一个Session对象
        try {
            session = jsch.getSession(ftpUserName, ftpHost, ftpPort);

            LOGGER.debug("Session created.");
            if (ftpPassword != null) {
                session.setPassword(ftpPassword); // 设置密码
            }

            // 具体config中需要配置那些内容，请参照sshd服务器的配置文件/etc/ssh/sshd_config的配置
            Properties config = new Properties();

            // 设置不用检查hostKey
            // 如果设置成“yes”，ssh就会自动把计算机的密匙加入“$HOME/.ssh/known_hosts”文件，
            // 并且一旦计算机的密匙发生了变化，就拒绝连接。
            config.put("StrictHostKeyChecking", "no");

            // UseDNS指定，sshd的是否应该看远程主机名，检查解析主机名的远程IP地址映射到相同的IP地址。
            // 默认值是 “yes” 此处是由于我们SFTP服务器的DNS解析有问题，则把UseDNS设置为“no”
            config.put("UseDNS", "no");

            session.setConfig(config); // 为Session对象设置properties

            session.setTimeout(SFTPConstants.SFTP_DEFAULT_TIMEOUT); // 设置timeout时候
            session.connect(); // 经由过程Session建树链接
            LOGGER.debug("Session connected.");

        } catch (JSchException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return session;
    }
	
/*	public boolean upfile(InputStream is, OutputStream os) throws IOException{
		boolean res = false;
		byte[] b = new byte[1024*1024*100];
		int read;
		if(os!=null){			
			do {
                read = is.read(b, 0, b.length);
                if (read > 0) {
                    os.write(b, 0, read);
                }
                os.flush();
            } while (read >= 0);			
		}		
		return res;
	}*/
	
/*	public void uploadFile(String localFile, String newName,
			String remoteFoldPath) // throws AppBizException
	{
		InputStream input = null;
		OutputStream os = null;
		try {
			File lf = new File(localFile);
			input = new FileInputStream(lf);
			// 改变当前路径到指定路径
			channel.cd(remoteFoldPath);
			long t1 = System.currentTimeMillis();
			//channel.put(input, newName);
			
			os = channel.put(newName
					//, new ProgressMonitor(lf.length()) // 上传时不执行init()方法
					,new ProgressMonitorByBytes(lf.length())
					,ChannelSftp.OVERWRITE) 
					
					;
			
			upfile(input,os);
			
			
			channel.put(localFile
					, newName
					, new ProgressMonitorByBytes()
					, ChannelSftp.OVERWRITE);
			
			
			
			System.out.println("Time elapsed: "  +  (System.currentTimeMillis() - t1) + "(ms)");
		} catch (Exception e) {
			LOGGER.error("Upload file error", e);
			
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					
				}
			}
		}
	}*/


    public static void main(String[] args) throws Exception {
        SFTPProcessor2 ftpUtil = new SFTPProcessor2();
        Map<String, String> ftpServerMap = new HashMap<String, String>();
        ftpServerMap.put((String) SFTPConstants.SFTP_SERVER_HOST, "localhost");
        ftpServerMap.put((String) SFTPConstants.SFTP_SERVER_USERNAME, "name");
        ftpServerMap.put((String) SFTPConstants.SFTP_SERVER_PASSWORD, "password");
        ftpServerMap.put((String) SFTPConstants.SFTP_SERVER_PORT, "22");
        ftpUtil.getConnect(ftpServerMap);

        //ftpUtil.uploadFile("e:/eclipse-jee.zip", "eclipse-jee.zip", System.getProperty("file.separator"));

        String rf1 = "eclipse-jee.zip";
        String rp1 = System.getProperty("file.separator") + "d";
        //String rp1 = "";
        String rf2 = "zzzz.zip";
        String rp2 = System.getProperty("file.separator") + "d";
        //String rp2 = "";
        String lf1 = "e:/yyyy.zip";
        String lf2 = "e:/zzzz.zip";


        DownLoadThread d1 = new DownLoadThread(session, rf1, rp1, lf1,
                ftpServerMap.get((String) SFTPConstants.SFTP_SERVER_HOST),
                ftpServerMap.get((String) SFTPConstants.SFTP_SERVER_USERNAME),
                "TRANS100");
        DownLoadThread d2 = new DownLoadThread(session, rf2, rp2, lf2,
                ftpServerMap.get((String) SFTPConstants.SFTP_SERVER_HOST),
                ftpServerMap.get((String) SFTPConstants.SFTP_SERVER_USERNAME),
                "TRANS99");
        final Thread t1 = new Thread(d1);
        final Thread t2 = new Thread(d2);
        t1.start();
        t2.start();

        final List<Thread> lst = new ArrayList<Thread>();
        lst.add(t1);
        lst.add(t2);

        //ftpUtil.downloadFile("eclipse-jee.zip", System.getProperty("file.separator")+"d", "e:/zzzz.zip");
        //ftpUtil.downloadFile("snagit.zip", System.getProperty("file.separator")+"d", "e:/test20150608.zip");
        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    boolean nonlive = true;
                    for (int i = 0; i < lst.size(); i++) {
                        if (lst.get(i).isAlive()) {
                            nonlive = false;
                            break;
                        }
                    }
                    if (nonlive) {
                        System.out.println("No Active transmission, exit!");
                        session.disconnect();
                        break;
                    }
                }

            }
        });
        //t3.setDaemon(true);
        t3.start();


    }

}