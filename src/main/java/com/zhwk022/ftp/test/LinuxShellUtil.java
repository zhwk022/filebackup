package com.zhwk022.ftp.test;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.EofMatch;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;
import org.apache.log4j.Logger;

import java.util.*;

public class LinuxShellUtil {
	private static Logger log = Logger.getLogger(LinuxShellUtil.class);
	private static Logger logger = Logger.getLogger(LinuxShellUtil.class);
	private Session session;
	private ChannelShell channel;
	private static Expect4j expect = null;
	private static final long defaultTimeOut = 1000;
	private StringBuffer buffer = new StringBuffer();

	public static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
	public static final String BACKSLASH_R = "\r";
	public static final String BACKSLASH_N = "\n";
	public static final String COLON_CHAR = ":";
	public static String ENTER_CHARACTER = BACKSLASH_R;
	public static final int SSH_PORT = 22;

	// 正则匹配，用于处理服务器返回的结果
	public static String[] linuxPromptRegEx = new String[] { "~]#", "~#", "#", ":~#", "/$", ">" };

	public static String[] errorMsg = new String[] { "could not acquire the config lock " };

	// ssh服务器的ip地址
	private String ip;
	// ssh服务器的登入端口
	private int port;
	// ssh服务器的登入用户名
	private String user;
	// ssh服务器的登入密码
	private String password;

	public LinuxShellUtil(String ip, int port, String user, String password) {
		this.ip = ip;
		this.port = port;
		this.user = user;
		this.password = password;
		expect = getExpect();
	}


	/**
	 * 关闭SSH远程连接
	**/ 
	public void disconnect() {
		if (channel != null) {
			channel.disconnect();
		}
		if (session != null) {
			session.disconnect();
		}
	}

	/**
	 * 获取服务器返回的信息
	 * 
	 * @return 服务端的执行结果
	**/  
	public String getResponse() {
		return buffer.toString();
	}

	public void cleanResponse() {
		if(StringUtils.isNotEmpty(buffer.toString())) {
			this.buffer.delete(0, buffer.length());
		}
	}

	public int getExitStatus() {
		return this.channel.getExitStatus();
	}

	public String getExpectLastState() {
		return this.expect.getLastState().getBuffer();
	}

	// 获得Expect4j对象，该对用可以往SSH发送命令请求
	private Expect4j getExpect() {
		try {
			log.debug(String.format("Start logging to %s@%s:%s", user, ip, port));
			JSch jsch = new JSch();
			session = jsch.getSession(user, ip, port);
			session.setPassword(password);
			Hashtable<String, String> config = new Hashtable<String, String>();
			//config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256");
			//config.put("StrictHostKeyChecking", "no");
			//config.put("cipher.c2s", "3des-cbc,aes192-cbc,aes128-cbc,aes256-cbc");
			//config.put("mac.c2s", "hmac-sha2-256");
			//session.setConfig(config);
			localUserInfo ui = new localUserInfo();
			session.setUserInfo(ui);
			session.connect(30000);
			channel = (ChannelShell) session.openChannel("shell");
			//TODO 设置channel的输出 FileOutputStream fileOut = new FileOutputStream( outputFileName );
			//channel.setOutputStream( System.out );
			Expect4j expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
			channel.connect();
			log.debug(String.format("Logging to %s@%s:%s successfully!", user, ip, port));
			isLogin = true;
			return expect;
		} catch (Exception ex) {
			log.error("Connect to " + ip + ":" + port + "failed,please check your username and password!");
			ex.printStackTrace();
			isLogin = false;
		}
		return null;
	}

	/**
	 * 执行配置命令
	 * 
	 * @param commands
	 *            要执行的命令，为字符数组
	 * @return 执行是否成功
	**/  
	public boolean executeCommands(String[] commands) {
		// 如果expect返回为0，说明登入没有成功
		if (expect == null) {
			return false;
		}

		log.debug("----------Running commands are listed as follows:----------");
		for (String command : commands) {
			log.debug(command);
		}
		log.debug("----------End----------");

		Closure closure = new Closure() {
			public void run(ExpectState expectState) throws Exception {
				String[] list = expectState.getBuffer().split("\r\n");
				logger.debug(expectState.getBuffer());
				buffer.append(list.length > 2 ? list[list.length - 2] : "");
														// buffer is string
														// buffer for appending
														// output of executed
														// command
				expectState.exp_continue();

			}
		};
		List<Match> lstPattern = new ArrayList<Match>();
		String[] regEx = linuxPromptRegEx;
		if (regEx != null && regEx.length > 0) {
			synchronized (regEx) {
				for (String regexElement : regEx) {// list of regx like, :>, />
													// etc. it is possible
													// command prompts of your
													// remote machine
					try {
						RegExpMatch mat = new RegExpMatch(regexElement, closure);
						lstPattern.add(mat);
					} catch (Exception e) {
						return false;
					}
				}
				lstPattern.add(new EofMatch(new Closure() { // should cause
															// entire page to be
															// collected
							public void run(ExpectState state) {
							}
						}));
				lstPattern.add(new TimeoutMatch(defaultTimeOut, new Closure() {
					public void run(ExpectState state) {
					}
				}));
			}
		}
		try {
			boolean isSuccess = true;
			for (String strCmd : commands) {
				isSuccess = isSuccess(lstPattern, strCmd);
			}
			// 防止最后一个命令执行不了
			isSuccess = !checkResult(expect.expect(lstPattern));

			// 找不到错误信息标示成功
			String response = buffer.toString().toLowerCase();
			for (String msg : errorMsg) {
				if (response.indexOf(msg) > -1) {
					return false;
				}
			}

			return isSuccess;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	// 检查执行是否成功
	private boolean isSuccess(List<Match> objPattern, String strCommandPattern) {
		try {
			boolean isFailed = checkResult(expect.expect(objPattern));
			if (!isFailed) {
				expect.send(strCommandPattern);
				expect.send("\r");
				return true;
			}
			return false;
		} catch (Exception ex) {
			return false;
		}
	}

	// 检查执行返回的状态
	private boolean checkResult(int intRetVal) {
		if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
			return true;
		}
		return false;
	}

	// 登入SSH时的控制信息
	// 设置不提示输入密码、不显示登入信息等
	public static class localUserInfo implements UserInfo {
		String passwd;

		public String getPassword() {
			return passwd;
		}

		public boolean promptYesNo(String str) {
			return true;
		}

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return true;
		}

		public boolean promptPassword(String message) {
			return true;
		}

		public void showMessage(String message) {

		}
	}
	
	public boolean isLogin() {
		return isLogin;
	}

	public void setLogin(boolean isLogin) {
		this.isLogin = isLogin;
	}

	private boolean isLogin;

	public String executeCommands(String commands) {
		// 如果expect返回为0，说明登入没有成功
		if (expect == null) {
			return "登录失败";
		}

		log.debug("----------Running commands are listed as follows:----------");

			log.debug(commands);

		log.debug("----------End----------");

		Closure closure = new Closure() {
			public void run(ExpectState expectState) throws Exception {
				String[] list = expectState.getBuffer().split("\r\n");
				logger.debug(expectState.getBuffer());
				buffer.append(expectState.getBuffer());
				// buffer is string
				// buffer for appending
				// output of executed
				// command
				expectState.exp_continue();

			}
		};
		List<Match> lstPattern = new ArrayList<Match>();
		String[] regEx = linuxPromptRegEx;
		if (regEx != null && regEx.length > 0) {
			synchronized (regEx) {
				for (String regexElement : regEx) {// list of regx like, :>, />
					// etc. it is possible
					// command prompts of your
					// remote machine
					try {
						RegExpMatch mat = new RegExpMatch(regexElement, closure);
						lstPattern.add(mat);
					} catch (Exception e) {
						return "执行失败";
					}
				}
				lstPattern.add(new EofMatch(new Closure() { // should cause
					// entire page to be
					// collected
					public void run(ExpectState state) {
					}
				}));
				lstPattern.add(new TimeoutMatch(6000, new Closure() {
					public void run(ExpectState state) {
					}
				}));
			}
		}
		try {
			boolean isSuccess = true;

				isSuccess = isSuccess(lstPattern, commands);

			// 防止最后一个命令执行不了
			isSuccess = !checkResult(expect.expect(lstPattern));

			// 找不到错误信息标示成功
			String response = buffer.toString().toLowerCase();
			for (String msg : errorMsg) {
				if (response.indexOf(msg) > -1) {
					return "执行失败";
				}
			}

			return buffer.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "执行失败";
		}
	}

	public static void main2(String[] args){
		// LinuxShellUtil linuxShellUtil = new LinuxShellUtil("11.11.176.127", 22, "root", "uhy@edA");//uhy@edA
		long preTime = new Date().getTime();
		LinuxShellUtil linuxShellUtil = new LinuxShellUtil("11.11.123.134", 22, "root", "bbqHCP%0");//uhy@edA
		// linuxShellUtil.executeCommands(new String[] {"mkdir /geoinit/sssssss", "echo 123456 > /root/test1.txt","echo 1234569527777777 >> /root/test.txt" });
		// // linuxShellUtil.executeCommands(new String[] { "echo 123456 > /root/test.txt" });
		// // linuxShellUtil.executeCommands(new String[] { "echo 1234569527777777 >> /root/test.txt" });
		// long afterTime = new Date().getTime();
		// logger.info(linuxShellUtil.getResponse());
        //
		// System.out.println(preTime-afterTime);


		String commandFlag = "commandFlag";
		String command = "command";
		String outputFile = "/tmp/"+commandFlag;
		String pidFile = outputFile + ".pid";
		String commandBash = commandFlag + ".bash";
		List<String> commandList = new ArrayList<>();
		StringBuilder s = new StringBuilder();
		s.append("echo -e \"");
		// s.append("\\#\\!/bin/bash");
		s.append("echo \\$\\$ > " + pidFile);
		s.append("\n");
		s.append("./" + command + " > " + outputFile + " 2>&1");
		s.append("\n");
		s.append("rm -rf " + pidFile);
		s.append("\"");
		s.append(" > " + commandBash);
		commandList.add(s.toString());
		commandList.add("chmod +x " + commandBash);
		// commandList.add("./"+commandBash);
		linuxShellUtil.executeCommands(commandList.toArray(new String[commandList.size()]));

		linuxShellUtil.disconnect();
	}

	public void wrapDo(String command){
		wrapDo(command, command);
	}

	public String wrapDo(String commandFlag, String command){
		this.cleanResponse();
		String uuid = UUID.randomUUID().toString().replace("-","");
		String outputFile = "/tmp/" + commandFlag + uuid;
		String pidFile = outputFile + ".pid";
		String commandBash = commandFlag + ".bash";
		List<String> commandList = new ArrayList<>();
		StringBuilder s = new StringBuilder();
		s.append("echo -e \"");
		// s.append("\\#\\!/bin/bash");
		s.append("echo \\$\\$ > " + pidFile);
		s.append("\n");
		s.append(command + " > " + outputFile + " 2>&1");
		s.append("\n");
		s.append("rm -rf " + pidFile);
		s.append("\"");
		s.append(" > " + commandBash);
		commandList.add(s.toString());
		commandList.add("chmod +x " + commandBash);
		commandList.add("./"+commandBash);
		this.executeCommands(commandList.toArray(new String[commandList.size()]));
		this.cleanResponse();
		int flag = 0;
		while(true){
			if(flag > 2*30)
				break;
			try {
				Thread.sleep(2000);
			}catch(Exception e){
			}
			flag += 2;
			this.executeCommands(new String[] { "ls " + pidFile });
			String response =  this.getResponse();
			if(response.contains("No such file or directory")){
				break;
			}
			this.cleanResponse();
		}
		this.cleanResponse();
		this.executeCommands("cat " + outputFile);
		String response =  this.getResponse();
		System.out.println(response);
		this.executeCommands("rm -rf " + outputFile);
		this.cleanResponse();
		System.out.println(commandFlag + " finish............................................................................");
		return response;
	}

	public static String getSHA256CheckSum(String host, int port, String username, String password, String remoteJarDir, String file){
		LinuxShellUtil linuxShellUtil = new LinuxShellUtil(host, port, username, password);
		String command = "java -cp " + remoteJarDir + "/ftp2.jar com.zhwk022.ftp.test.SHA256File " + file;
		String commandFlag = "fileSHA256";
		String response = linuxShellUtil.wrapDo(commandFlag,command);
		linuxShellUtil.disconnect();
		return response;
	}
}
