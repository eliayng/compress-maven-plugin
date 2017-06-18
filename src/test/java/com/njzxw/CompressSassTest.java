package com.njzxw;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.mozilla.javascript.tools.debugger.Main;

public class CompressSassTest {

	public static void main(String[] args) {
//		System.out.println("sasa/dsa/fff/dsa/sa/.woff".matches("^.*/fff/.*/.woff$"));
		System.out.println("sadas$ {da.saa }d$ {da }sada".replaceAll("\\$(\\s*)\\{(\\s*)da.saa(\\s*)\\}", "xxx")); 
	}
	
	private String cmds(String cmd) {
		BufferedReader br = null;
		BufferedReader errbr = null;
		try {
			// ----手动生成css文件编译
			// sass /opt/myeclipseWork/zxwPay/WebRoot/css/common/common.scss /opt/myeclipseWork/zxwPay/WebRoot/css/common/common.min.css --style compressed --sourcemap=none
			// ---自动监控并压缩
			// sass --watch
			// /opt/myeclipseWork/zxwPay/WebRoot/css/common/common.scss:/opt/myeclipseWork/zxwPay/WebRoot/css/common/common.min.css
			// --style compressed --sourcemap=none
			Runtime run = Runtime.getRuntime();
			Process p = null;
			if (System.getProperty("os.name").toLowerCase().indexOf("windows 8.1") >= 0) {
				p = run.exec("cmd /c " + cmd);
			} else if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
				p = run.exec("cmd.exe /c " + cmd);
			} else {
				p = run.exec(cmd);
			}
			br = new BufferedReader(new InputStreamReader(p.getInputStream(),Charset.forName("GBK")));
			errbr = new BufferedReader(new InputStreamReader(p.getErrorStream(),Charset.forName("GBK")));
			int ch;
            StringBuffer text = new StringBuffer("");
            while ((ch = br.read()) != -1) {
            	text.append((char) ch);
            }
            StringBuffer text1 = new StringBuffer("");
            while ((ch = errbr.read()) != -1) {
             text1.append((char) ch);
            }
            if(!text1.toString().isEmpty()){
            	return text1.toString();
            }
			return "";
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (errbr != null) {
				try {
					errbr.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
