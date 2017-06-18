package com.njzxw.compress_maven_plugin.css;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import com.njzxw.compress_maven_plugin.CompressAbs;
import com.njzxw.compress_maven_plugin.CompressMojo;

/**
 * 压缩css
 * @author eliay
 *
 */
public class CompressSass extends CompressAbs {

	public CompressSass(CompressMojo compressMojo) {
		super(compressMojo);
	}

	@Override
	public void init() {
		//检查是否包含sass命令
		String result = cmds("sass -v");
		if(result.toUpperCase().contains("不是内部或外部命令")){
			log.error("没有安装sass命令，请参考：http://www.w3cplus.com/sassguide/install.html 安装sass命令");
			return;
		}
		int count = getCompressMojo().getSassFile().size();
		log.info("scss/sass压缩数量："+count);
		if(getCompressMojo().getSassFile().size() >= getCompressMojo().getPoolNum()){
			getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
		}else{
			getCompressMojo().setLatch(new CountDownLatch(count));
		}
	}

	@Override
	public void start() {
		getCompressMojo().addErrorLog("***************************************************************");
		getCompressMojo().addErrorLog("************************sass错误信息 记录开始********************");
		getCompressMojo().addErrorLog("***************************************************************");
		String result = cmds("sass -v");
		log.error(result);
		if(result.toUpperCase().contains("不是内部或外部命令")){
			log.error("没有安装sass命令，请参考：http://www.w3cplus.com/sassguide/install.html 安装sass命令");
			getCompressMojo().addErrorLog("没有安装sass命令，请参考：http://www.w3cplus.com/sassguide/install.html 安装sass命令");
			return;
		}
		int count = getCompressMojo().getSassFile().size();
		for (int j = 0; j < count; j++) {
			final File cssfile = getCompressMojo().getSassFile().get(j);
			compressSass(cssfile);
//			getCompressMojo().addPool(new Runnable() {
//				@Override
//				public void run() {
//					compressSass(cssfile);
//					getCompressMojo().getLatch().countDown();
//				}
//			});
//			if((j+1)%getCompressMojo().getPoolNum() == 0){
//				try {
//					getCompressMojo().getLatch().await();
//					if(getCompressMojo().getSassFile().size()-(j+1) >= getCompressMojo().getPoolNum()){
//						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
//					}else{
//						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getSassFile().size()-(j+1)));
//					}
//				} catch (InterruptedException e) {
//					log.error("执行压缩js出现错误:"+cssfile.getPath(),e);
//				}
//			}
		}
		getCompressMojo().addErrorLog("***************************************************************");
		getCompressMojo().addErrorLog("************************sass错误信息 记录结束********************");
		getCompressMojo().addErrorLog("***************************************************************");
	}

	/**
	 * 执行压缩操作，单个文件执行
	 * @param jsfile
	 */
	private void compressSass(File cssfile){
		String outpath = getCompressMojo().getOutDir().getPath() + getCompressMojo().orgFile(cssfile.getPath());
		// 创建对应的输出文件夹路径
		checkDir(outpath);
		
		compressSass(cssfile.getPath(),  outpath.replace(".scss",".css").replace(".sass",".css"));
	}
	
	/**
	 * 检查文件路径是否存在，如果不存在就进行创建
	 */
	private void checkDir(String outpath){
		// 创建对应的输出文件夹路径
		File outDirFile = new File(outpath.substring(0,
				outpath.lastIndexOf(File.separator)));
		if (!outDirFile.exists()) {
			outDirFile.mkdirs();
		}
	}
	
	/**
	 * 使用yuicompress进行压缩js
	 * @param path
	 * @param outputFilePath
	 * @param charset
	 * @param linebreakpos
	 * @param munge
	 * @param verbose
	 * @param preserveAllSemiColons
	 * @param disableOptimizations
	 * @param preserveUnknownHints
	 */
	private void compressSass(String path, String outputFilePath){
		//log.info("sass " + path + " " + outputFilePath + " --style compressed --sourcemap=none ");
		String result = cmds("sass " + path + " " + outputFilePath + " --style compressed --sourcemap=none  --no-cache ");
		if(!"".equals(result.trim())){
			log.error("["+path+"]压缩css出现错误："+result);
		}
	}
	
	private String cmds(String cmd) {
		BufferedReader br = null;
		BufferedReader errbr = null;
		try {
			// ----手动生成css文件编译
			// sass /opt/myeclipseWork/zxwPay/WebRoot/css/common/common.scss
			// /opt/myeclipseWork/zxwPay/WebRoot/css/common/common.min.css
			// --style compressed --sourcemap=none
			// ---自动监控并压缩
			// sass --watch
			// /opt/myeclipseWork/zxwPay/WebRoot/css/common/common.scss:/opt/myeclipseWork/zxwPay/WebRoot/css/common/common.min.css
			// --style compressed --sourcemap=none
			Runtime run = Runtime.getRuntime();
			Process p = null;
			if (System.getProperty("os.name").toLowerCase().indexOf("windows 8.1") >= 0) {
				p = run.exec("cmd.exe /c " + cmd);
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
			log.error("压缩出现错误",e);
			e.printStackTrace();
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
		return "";
	}
	
}
