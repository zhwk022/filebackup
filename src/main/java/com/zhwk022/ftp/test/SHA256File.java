package com.zhwk022.ftp.test;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Properties;

public class SHA256File {

    static String official_checksum = "23d5686ffe489e5a1af95253b153ce9d6f933e5dbabe14c494631234697a0e08";

    public static void OnEncFile() {
        System.out.println("Getting file...");
//        File file = new File("F:\\BaiduNetdiskDownload\\最后一战bl.zip");
        File file = new File("J:\\ftp2test\\tools_r25.2.3-windows111111.zip");
        if (file.exists()) {
            System.out.println("File got.");
        } else {
            System.err.println("File not got.");
        }
        System.out.print("Calculating SHA-256 checksum......");
        String fileSHA256 = getFileSHA256(file);
        System.out.println("File's SHA-256 checksum is: ");
        System.out.println(fileSHA256);
        System.out.println(official_checksum + " --- the official checksum");
        if (official_checksum.equals(fileSHA256)) {
            System.out.println("SHA-256 checksums are the same.");
        } else {
            System.err.println("SHA-256 checksums differ!!!!");
        }

    }

    private static String getFileSHA256(File file) {
        if (!file.isFile()) {
            System.err.println("not file");
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    public static void main(String[] args) {
//        OnEncFile();
        if(args.length<1){
            System.out.println("参数的个数必须等于1");
        }
        File file = new File(args[0]);
        if(!file.exists()){
            System.out.println("文件不存在");
        }
        String fileSHA256 = getFileSHA256(file);
        System.out.println("success:"+fileSHA256);
    }

    public static void generateSHA256PropertiesFile(File dir) {
        Properties properties = new Properties();
        for(File file: dir.listFiles()){
            if(file.isFile()){
                System.out.println("Calculating SHA-256 checksum......");
                properties.put(file.getName(),getFileSHA256(file));
                System.out.println("Calculating SHA-256 checksum end");
            }
        }
        BufferedWriter writer = null;
        try {
            String filePath = QuartzJdbcTest.FILECHECKSUMFILE;
            writer = new BufferedWriter (new OutputStreamWriter (new FileOutputStream(filePath),"UTF-8"));
            properties.store(writer, "filename=checksum");
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            if(writer!=null){
                try {
                    writer.close();
                }catch (Exception e){
                }
            }
        }
    }
}