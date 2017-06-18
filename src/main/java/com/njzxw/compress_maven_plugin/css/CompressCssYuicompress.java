package com.njzxw.compress_maven_plugin.css;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;

import com.njzxw.compress_maven_plugin.CompressAbs;
import com.njzxw.compress_maven_plugin.CompressMojo;
import com.yahoo.platform.yui.compressor.CssCompressor;

/**
 * 压缩css
 * @author eliay
 *
 */
public class CompressCssYuicompress extends CompressAbs {

	public CompressCssYuicompress(CompressMojo compressMojo) {
		super(compressMojo);
	}

	@Override
	public void init() {
		int count = getCompressMojo().getCssFile().size();
		log.info("css压缩数量："+count);
		if(getCompressMojo().getCssFile().size() >= getCompressMojo().getPoolNum()){
			getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
		}else{
			getCompressMojo().setLatch(new CountDownLatch(count));
		}
	}

	@Override
	public void start() {
		int count = getCompressMojo().getCssFile().size();
		for (int j = 0; j < count; j++) {
			final File cssfile = getCompressMojo().getCssFile().get(j);
			getCompressMojo().addPool(new Runnable() {
				@Override
				public void run() {
					compressCss(cssfile);
					getCompressMojo().getLatch().countDown();
				}
			});
			if((j+1)%getCompressMojo().getPoolNum() == 0){
				try {
					getCompressMojo().getLatch().await();
					if(getCompressMojo().getCssFile().size()-(j+1) >= getCompressMojo().getPoolNum()){
						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
					}else{
						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getCssFile().size()-(j+1)));
					}
				} catch (InterruptedException e) {
					log.error("执行压缩js出现错误:"+cssfile.getPath(),e);
				}
			}
		}
	}

	/**
	 * 执行压缩操作，单个文件执行
	 * @param jsfile
	 */
	private void compressCss(File cssfile){
		String outpath = getCompressMojo().getOutDir().getPath() + getCompressMojo().orgFile(cssfile.getPath());
		// 创建对应的输出文件夹路径
		checkDir(outpath);
		
		compressCssYuiComprss(cssfile.getPath(),  outpath, getCompressMojo().getEncoding(), -1, false, true,
				true, false, false);
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
	private void compressCssYuiComprss(String path, String outputFilePath,
			String charset, int linebreakpos, boolean munge, boolean verbose,
			boolean preserveAllSemiColons, boolean disableOptimizations,
			boolean preserveUnknownHints){
		Reader in = null;
		Writer out = null;
		try {
			in = new InputStreamReader(new FileInputStream(path), charset);
			CssCompressor compressor = new CssCompressor(in);
			in.close();
			in = null;
			if (outputFilePath == null) {
				out = new OutputStreamWriter(System.out, charset);
			} else {
				out = new OutputStreamWriter(new FileOutputStream(
						outputFilePath), charset);
			}
			compressor.compress(out, linebreakpos);
		} catch (Exception e) {
			errorFileDispose(path, outputFilePath);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 复制单个文件
	 * 
	 * @param srcFileName
	 *            待复制的文件名
	 * @param descFileName
	 *            目标文件名
	 * @return 如果复制成功返回true，否则返回false
	 */
	private void errorFileDispose(String path,String outputFilePath){
		if ("copy".equals(getCompressMojo().getCompressErrorAction().toLowerCase())) {
			getCompressMojo().copyFile(path, outputFilePath, false);
		} else if ("no_copy".equals(getCompressMojo().getCompressErrorAction().toLowerCase())) {
			
		} else {
			System.exit(-1);
		}
	}
	
}
