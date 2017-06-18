package com.njzxw.compress_maven_plugin.html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.CountDownLatch;

import com.njzxw.compress_maven_plugin.CompressAbs;
import com.njzxw.compress_maven_plugin.CompressMojo;

/**
 * 压缩html文件
 * @author eliay
 *
 */
public class CompressHtml extends CompressAbs{

	public CompressHtml(CompressMojo compressMojo) {
		super(compressMojo);
	}

	@Override
	public void init() {
		int count = getCompressMojo().getHtmlFile().size();
		log.info("html压缩数量："+count);
		if(getCompressMojo().getHtmlFile().size() >= getCompressMojo().getPoolNum()){
			getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
		}else{
			getCompressMojo().setLatch(new CountDownLatch(count));
		}
	}

	@Override
	public void start() {
		int count = getCompressMojo().getHtmlFile().size();
		for (int j = 0; j < count; j++) {
			final File htmlfile = getCompressMojo().getHtmlFile().get(j);
			getCompressMojo().addPool(new Runnable() {
				@Override
				public void run() {
					compressHtml(htmlfile);
					getCompressMojo().getLatch().countDown();
				}
			});
			if((j+1)%getCompressMojo().getPoolNum() == 0){
				try {
					getCompressMojo().getLatch().await();
					if(getCompressMojo().getHtmlFile().size()-(j+1) >= getCompressMojo().getPoolNum()){
						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
					}else{
						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getHtmlFile().size()-(j+1)));
					}
				} catch (InterruptedException e) {
					log.error("执行压缩js出现错误:"+htmlfile.getPath(),e);
				}
			}
		}
	}
	
	/**
	 * 执行压缩操作，单个文件执行
	 * @param jsfile
	 */
	private void compressHtml(File cssfile){
		String outpath = getCompressMojo().getOutDir().getPath() + getCompressMojo().orgFile(cssfile.getPath());
		// 创建对应的输出文件夹路径
		checkDir(outpath);
		
		compressHtml(cssfile.getPath(),outpath);
	}
	
	private void compressHtml(String spath,String outPath) {
		BufferedReader br = null;
    	InputStreamReader fr = null;
    	OutputStreamWriter fw = null;
    	try {
    		File outDirFile = new File(outPath.substring(0,
    				outPath.lastIndexOf(File.separator)));
			if (!outDirFile.exists()) {
				outDirFile.mkdirs();
			}
    		fw = new OutputStreamWriter(new FileOutputStream(outPath),getCompressMojo().getEncoding());
			fr = new InputStreamReader(new FileInputStream(spath),getCompressMojo().getEncoding());
			br = new BufferedReader(fr);
	    	StringBuffer str = new StringBuffer();
	    	String line = "";
			while ((line = br.readLine()) != null) {
				str.append(line);
			}
	    	fw.write(HtmlCompressor.compress(str.toString()));
	    	fw.flush();
		} catch (Exception e) {
			log.warn("压缩失败:"+spath,e);
		} finally{
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("压缩html出错：", e);
				}
			}
			if (fr != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("压缩html出错：", e);
				}
			}
			if (fw != null) {
				try {
					fw.close();
				} catch (Exception e) {
					log.error("压缩html出错：", e);
				}
			}
		}
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

}
