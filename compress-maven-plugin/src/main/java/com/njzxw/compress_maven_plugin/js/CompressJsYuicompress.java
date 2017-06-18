package com.njzxw.compress_maven_plugin.js;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.njzxw.compress_maven_plugin.CompressAbs;
import com.njzxw.compress_maven_plugin.CompressMojo;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * 使用yuicompress进行js压缩操作
 * @author eliay
 *
 */
public class CompressJsYuicompress extends CompressAbs {

	public CompressJsYuicompress(CompressMojo compressMojo) {
		super( compressMojo);
	}

	@Override
	public void init() {
		int count = getCompressMojo().getJsFile().size();
		log.info("js存在文件个数：" + count);
		if(getCompressMojo().getJsFile().size() >= getCompressMojo().getPoolNum()){
			getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
		}else{
			getCompressMojo().setLatch(new CountDownLatch(count));
		}
	}

	@Override
	public void start() {
		getCompressMojo().addErrorLog("***************************************************************");
		getCompressMojo().addErrorLog("************************js错误信息 记录开始*********************");
		getCompressMojo().addErrorLog("***************************************************************");
		int count = getCompressMojo().getJsFile().size();
		for (int j = 0; j < count; j++) {
			final File jsfile = getCompressMojo().getJsFile().get(j);
			getCompressMojo().addPool(new Runnable() {
				@Override
				public void run() {
					compressJs(jsfile);
					getCompressMojo().getLatch().countDown();
				}
			});
			if((j+1)%getCompressMojo().getPoolNum() == 0){
				try {
					getCompressMojo().getLatch().await();
					if(getCompressMojo().getJsFile().size()-(j+1) >= getCompressMojo().getPoolNum()){
						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
					}else{
						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getJsFile().size()-(j+1)));
					}
				} catch (InterruptedException e) {
					log.error("执行压缩js出现错误:"+jsfile.getPath(),e);
				}
			}
		}
		getCompressMojo().addErrorLog("***************************************************************");
		getCompressMojo().addErrorLog("************************js错误信息 记录结束*********************");
		getCompressMojo().addErrorLog("***************************************************************");
	}
	
	/**
	 * 执行压缩操作，单个文件执行
	 * @param jsfile
	 */
	private void compressJs(File jsfile){
		String outpath = getCompressMojo().getOutDir().getPath() + getCompressMojo().orgFile(jsfile.getPath());
		// 创建对应的输出文件夹路径
		checkDir(outpath);
		
		compressJsYuiComprss(jsfile.getPath(), outpath, getCompressMojo().getEncoding(), -1, true, true,
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
	private void compressJsYuiComprss(final String path, final String outputFilePath,
			String charset, int linebreakpos, boolean munge, boolean verbose,
			boolean preserveAllSemiColons, boolean disableOptimizations,
			boolean preserveUnknownHints){
		Reader in = null;
		Writer out = null;
		Writer mungemap = null;
		try {
			in = new InputStreamReader(new FileInputStream(path), charset);

			final String localFilename = path;
			JavaScriptCompressor compressor = new JavaScriptCompressor(in,
					new ErrorReporter() {

						public void warning(String message, String sourceName,
								int line, String lineSource, int lineOffset) {
						}

						public void error(String message, String sourceName,
								int line, String lineSource, int lineOffset) {
//							log.error("压缩编译文件[" + localFilename + "]失败：("
//									+ line + ":" + lineOffset + "):错误信息:("
//									+ message + ")错误所在行对应的js:" + lineSource);
							getCompressMojo().addErrorLog("压缩编译文件[" + localFilename + "]失败：("
									+ line + ":" + lineOffset + "):错误信息:("
									+ message + ")错误所在行对应的js:" + lineSource);
							//将进行源码复制，或者其他操作
							errorFileDispose(path, outputFilePath);
						}

						public EvaluatorException runtimeError(String message,
								String sourceName, int line, String lineSource,
								int lineOffset) {
							// error(message, sourceName, line, lineSource,
							// lineOffset);
							return new EvaluatorException(message);
						}
					});

			// Close the input stream first, and then open the output stream,
			// in case the output file should override the input file.
			in.close();
			in = null;

			if (outputFilePath == null) {
				out = new OutputStreamWriter(System.out, charset);
			} else {
				out = new OutputStreamWriter(new FileOutputStream(
						outputFilePath), charset);
				if (mungemap != null) {
					mungemap.write("\n\nFile: " + outputFilePath + "\n\n");
				}
			}

			compressor.compress(out, linebreakpos, munge, verbose,
					preserveAllSemiColons, disableOptimizations);

		} catch (Exception e) {
			//将进行源码复制，或者其他操作
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
