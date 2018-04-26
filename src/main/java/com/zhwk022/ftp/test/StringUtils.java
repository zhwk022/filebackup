package com.zhwk022.ftp.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.sql.Clob;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

	private static final Logger logger = LoggerFactory.getLogger(StringUtils.class);
	public static final char UNDERLINE = '_';


	// 判断一个字符串是否为数字加一个字符
	public static boolean isDigitAndChar(String strNum) {
		return strNum.matches(pattern);
	}

	/**
	 * 驼峰格式字符串转换为下划线格式字符串
	 *
	 * @param param
	 * @return
	 */
	public static String camelToUnderline(String param) {
		if (param == null || "".equals(param.trim())) {
			return "";
		}
		int len = param.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = param.charAt(i);
			if (Character.isUpperCase(c)) {
				sb.append(UNDERLINE);
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 下划线格式字符串转换为驼峰格式字符串
	 *
	 * @param param
	 * @return
	 */
	public static String underlineToCamel(String param) {
		if (param == null || "".equals(param.trim())) {
			return "";
		}
		int len = param.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = param.charAt(i);
			if (c == UNDERLINE) {
				if (++i < len) {
					sb.append(Character.toUpperCase(param.charAt(i)));
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 利用正则表达式判断字符串是否是数字
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str){
		Pattern pattern = Pattern.compile("[0-9]*");
		Matcher isNum = pattern.matcher(str);
		if( !isNum.matches() ){
			return false;
		}
		return true;
	}
	
	// 截取数字
	public static int getIntNumbers(String content, boolean... valids) {
		Pattern pattern = Pattern.compile(dpattern);
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			if(isDigitAndChar(matcher.group(0))){
				return Integer.parseInt(matcher.group(0));
			} 
		}
		boolean valid = true;
		if(valids != null && valids.length > 0) {
			valid = valids[0];
		}
		return valid ? -1 : 0;
	}
	
	// 截取数字
	public static int getIntNumbers(Object content) {
		Pattern pattern = Pattern.compile(dpattern);
		Matcher matcher = pattern.matcher(content.toString());
		while (matcher.find()) {
			if(isDigitAndChar(matcher.group(0))){
				return Integer.parseInt(matcher.group(0));
			} 
		}
		return -1;
	}

	public static Double getDoubleNumbers(Object content) {
		Pattern pattern = Pattern.compile(dpattern);
		Matcher matcher = pattern.matcher(content.toString());
		while (matcher.find()) {
			if(isDigitAndChar(matcher.group(0))){
				return Double.parseDouble(matcher.group(0));
			}
		}
		return -1d;
	}

    /**
     *double去初小数点后多余的0
     */
    public synchronized static String DoubleToDoubleExistZero(Double d){
        String str = Double.toString(d);
        return subZeroAndDot(str);
    }


	/**
	 * 使用java正则表达式去掉多余的.与0
	 * @param s
	 * @return
	 */
	public static String subZeroAndDot(String s){
		if(s.indexOf(".") > 0){
			s = s.replaceAll("0+?$", "");//去掉多余的0
			s = s.replaceAll("[.]$", "");//如最后一位是.则去掉
		}
		return s;
	}

	// 首字母转大写
	public static String toUpperCaseFirstOne(String s) {
		if (Character.isUpperCase(s.charAt(0)))
			return s;
		else
			return (new StringBuilder())
					.append(Character.toUpperCase(s.charAt(0)))
					.append(s.substring(1)).toString();
	}

	private final static String pattern = "[0-9]+[A-Za-z]*";
	private final static String dpattern = "\\d*";

	/**
	 * 将String转成Clob ,静态方法
	 * 
	 * @param str
	 *            字段
	 * @return clob对象，如果出现错误，返回 null
	 */
	public static Clob stringToClob(String str) {
		if (null == str || str.trim().equalsIgnoreCase("")) {
			return null;
		}
		try {
			java.sql.Clob c = new javax.sql.rowset.serial.SerialClob(str.toCharArray());
			return c;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 将Clob转成String ,静态方法
	 * 
	 * @param clob 字段
	 * @return 内容字串，如果出现错误，返回 null
	 */
	public static String clobToString(Clob clob) {
		if (clob == null)
			return null;

		StringBuffer sb = new StringBuffer(65535);// 64K
		Reader clobStream = null;
		try {
			clobStream = clob.getCharacterStream();
			char[] b = new char[60000];// 每次获取60K
			int i = 0;
			while ((i = clobStream.read(b)) != -1) {
				sb.append(b, 0, i);
			}
		} catch (Exception ex) {
			sb = null;
		} finally {
			try {
				if (clobStream != null) {
					clobStream.close();
				}
			} catch (Exception e) {
			}
		}
		if (sb == null)
			return null;
		else
			return sb.toString();
	}
	
	public static boolean isContainChinese(String str) {

        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

	//将string数组转化为sql的in条件,例如select * from tableName id in (字符串)
	public static String stringArray2Strin (String[] strs, String split){
		if(strs==null||strs.length==0){
			return "";
		}
		StringBuffer idsStr = new StringBuffer();
		for (int i = 0; i < strs.length; i++) { 
		if (i > 0) { 
			idsStr.append(split); 
		} 
			idsStr.append("'").append(strs[i]).append("'"); 
		}
		return idsStr.toString();
	}
	//将string数组转化为sql的in条件,例如select * from tableName id in (字符串)
	public static String stringArray2StrinNot (String[] strs, String split){
		if(strs==null||strs.length==0){
			return "";
		}
		StringBuffer idsStr = new StringBuffer();
		for (int i = 0; i < strs.length; i++) { 
		if (i > 0) { 
			idsStr.append(split); 
		} 
			idsStr.append(strs[i]); 
		}
		return idsStr.toString();
	}
	
	public static String getStringBySet(Collection<String> list){
		StringBuilder sb = new StringBuilder();
		for(String ite : list){
			sb.append(ite + ",");
		}
		return sb.toString().isEmpty() ? "" : sb.toString().substring(0, sb.toString().lastIndexOf(","));
	}
	
	public static Date getDate(Object obj){
		if(obj instanceof Date){
			return (Date)obj;
		}
		try {
			SimpleDateFormat TimeFormatter = new SimpleDateFormat(Common_TimeFormatter);
			return TimeFormatter.parse(obj.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error("",e);
		}
		return null;
	}

	public static Date getTryDate(Object obj) {
		if(obj instanceof Date){
			return (Date)obj;
		}
		try {
			SimpleDateFormat TimeFormatter = new SimpleDateFormat(Common_TimeFormatter);
			return TimeFormatter.parse(obj.toString());
		} catch (ParseException e) {
			SimpleDateFormat TimeFormatter2 = new SimpleDateFormat(Common_TimeFormatter2);
			try {
				return TimeFormatter2.parse(obj.toString());
			} catch (ParseException e1) {
				return null;
			}
		}
	}
	
	public static Date getDate2(Object obj){
		if(obj instanceof Date){
			return (Date)obj;
		}
		try {
			SimpleDateFormat TimeFormatter2 = new SimpleDateFormat(Common_TimeFormatter2);
			return TimeFormatter2.parse(obj.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.error("",e);
		}
		return null;
	}
	
	public static String getDateStr(Object obj){
		SimpleDateFormat TimeFormatter = new SimpleDateFormat(Common_TimeFormatter);
		if(obj instanceof Date){
			return TimeFormatter.format(obj);
		}
		return obj == null ? "" : obj.toString();
	}
	
	public static String getDate2Str(Object obj){
		SimpleDateFormat TimeFormatter2 = new SimpleDateFormat(Common_TimeFormatter2);
		if(obj instanceof Date){
			return TimeFormatter2.format(obj);
		} else if(obj instanceof String) {
			Date dateObj;
			try {
				dateObj = TimeFormatter2.parse(obj.toString());
				return TimeFormatter2.format(dateObj);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				logger.error("",e);
			}			
		}
		return obj == null ? null : obj.toString();
	}
	
	public static String getNowDateAddDayStr(int day, Object date){
		Date curDate = null ;
		SimpleDateFormat TimeFormatter2 = new SimpleDateFormat(Common_TimeFormatter2);
		if(date == null) {
			curDate = new Date();
		} else if ( date instanceof Date) {
			curDate = (Date)date;
		} else {
			try {
				curDate = TimeFormatter2.parse(date.toString());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				logger.error("",e);
				curDate =  new Date();
			}
		}
		return TimeFormatter2.format(new Date(curDate.getTime() + day * 24 * 60 * 60 * 1000));
	}
	

    public static void main(String[] args) {
        // Date curDate = new Date();
        // System.out.println(new Date(curDate.getTime() + 40 * 24 * 60 * 60 * 1000));
        // System.out.println(new Date(curDate.getTime() + 30 * 24 * 60 * 60 * 1000));
        //
        // System.out.println(new Date());
    }
	
	public static boolean isEmpty(String value) {
        if (value == null || value.length() == 0) {
            return true;
        }

        return false;
    }
	
	public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    /**
     *清除空格的
     * @param str
     * @return
     */
    public static String trimToEmpty(String str) {
        return str == null?"":str.trim();
    }
	
	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}
	
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}
	
	private static final String Common_TimeFormatter = "yyyy-MM-dd HH:mm:ss";
	private static final String Common_TimeFormatter2 = "yyyy-MM-dd";
}
