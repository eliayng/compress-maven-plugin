package com.njzxw.compress_maven_plugin;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.njzxw.compress_maven_plugin.compressClass.CompressClass;
import com.njzxw.compress_maven_plugin.css.CompressCssYuicompress;
import com.njzxw.compress_maven_plugin.css.CompressSass;
import com.njzxw.compress_maven_plugin.html.CompressHtml;
import com.njzxw.compress_maven_plugin.js.CompressJsYuicompress;
import com.njzxw.compress_maven_plugin.merge.CompressMerge;

@Mojo(name = "compress")
public class CompressMojo extends AbstractMojo {

	private Log log = getLog();

	/** 
	 * 
	 */
	@Parameter(defaultValue = "${project}")
	private MavenProject project;

	/**
	 * 排除文件
	 */
	@Parameter
	private List<String> excludes;

	/**
	 * 包含文件
	 * 
	 * @parameter
	 */
	@Parameter
	private List<String> includes;

	/**
	 * 设置编码，默认utf-8
	 */
	@Parameter(defaultValue = "UTF-8")
	private String encoding;

	/**
	 * 是否开启资源文件合并，会在${sourceDirectory}下查找所有存在merge.xml为后缀的文件进行合并 merge.xml
	 * 格式为:<group name="sss" ><js>xxx.js</js><css>xxx.css</css></group>
	 * 会合并为sss.js与sss.css文件
	 */
	@Parameter(defaultValue = "true")
	private boolean isMerge;

	/**
	 * 是否启用压缩js
	 */
	@Parameter(defaultValue = "true")
	private boolean isCompressJs;

	/**
	 * 是否启用压缩css
	 */
	@Parameter(defaultValue = "true")
	private boolean isCompressCss;

	/**
	 * 是否启用压缩html、jsp等文件
	 */
	@Parameter(defaultValue = "true")
	private boolean isCompressHtml;

	/**
	 * 是否启用压缩sass\scss等文件
	 */
	@Parameter(defaultValue = "true")
	private boolean isCompressSass;

	/**
	 * 设置webapps的路径信息
	 */
	@Parameter
	private File[] webapps;

//	/**
//	 * js出现警告显示级别 QUIET, DEFAULT, VERBOSE
//	 */
//	@Parameter(defaultValue = "QUIET")
//	private String js_warning_level;
//
//	/**
//	 * 压缩等级
//	 */
//	@Parameter(defaultValue = "SIMPLE_OPTIMIZATIONS")
//	private String js_compilation_level;

	/**
	 * 压缩输出的跟目录文件 ，默认是“staticCompress”直接会在target下创建目录staticCompress
	 */
	@Parameter(defaultValue = "target\\staticCompress")
	private File outDir;

	/**
	 * js合并默认输出路径，根据outdir的路径添加
	 */
	@Parameter(defaultValue = "js")
	private String jsOutDir;

	/**
	 * css合并默认输出路径，根据outdir的路径添加
	 */
	@Parameter(defaultValue = "css")
	private String cssOutDir;
	/**
	 * 获取所有的merge.xml文件
	 */
	private List<File> mergeXmlFile = new ArrayList<File>();
	/**
	 * 获取所有的merge.xml文件
	 */
	private List<File> jsFile = new ArrayList<File>();
	private List<File> cssFile = new ArrayList<File>();
	private List<File> htmlFile = new ArrayList<File>();
	private List<File> hbJsFile = new ArrayList<File>();
	private List<File> hbCssFile = new ArrayList<File>();
	private List<File> sassFile = new ArrayList<File>();

	/**
	 * 运行最大并发数，提高编译速度，默认为10
	 */
	@Parameter(defaultValue = "10")
	private int poolNum;

	/**
	 * 压缩出现错误后的执行操作：copy、no_copy、exit
	 */
	@Parameter(defaultValue = "copy")
	private String compressErrorAction;

	/**
	 * 所有jar所在目录
	 */
	@Parameter(defaultValue = "target\\lib")
	private File jarDir;

	/**
	 * 是否开启编译java文件为class文件
	 */
	@Parameter(defaultValue = "true")
	private boolean isCompressClass;
	
	/**
	 * 是否进行资源文件复制
	 */
	@Parameter(defaultValue = "true")
	private boolean isResourcesCopy;
	
	/**
	 * 编译间隔时间，间隔多长时间类修改相同文件不编译或者不复制，单位毫秒 默认3秒
	 */
	@Parameter(defaultValue = "3000")
	private int compressIntervalTime;

	/**
	 * 是否跳过该插件
	 */
	@Parameter(defaultValue = "false")
	private boolean skip;

	private ExecutorService pool = null;

	private ByteArrayOutputStream outReader = new ByteArrayOutputStream();
	private ByteArrayOutputStream errReader = new ByteArrayOutputStream();

	/**
	 * 技数
	 */
	private CountDownLatch latch;

	/**
	 * 写 锁
	 */
	private ReadWriteLock rwl = new ReentrantReadWriteLock();

	/**
	 * 选择压缩方式，可选择项为：yuicompress、closure-compiler、command yuicompress：雅虎压缩方式
	 * closure-compiler:谷歌压缩方式 command：命令行方式，可执行命令行方式的js压缩操作 默认为：yuicompress
	 */
	@Parameter(defaultValue = "yuicompress")
	private String jsCompressType;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			log.debug("跳过执行压缩插件：" + project.getName());
			return;
		}
		//System.out.println(this.getProject().getProperties());
		List<Resource> res = this.getBuild().getResources();
		for(int i=0;i<res.size();i++){
			System.out.println(res.get(i).isFiltering()+"----"+ res.get(i).getDirectory()+"--"+res.get(i).getTargetPath()+res.get(i).getExcludes()+"--"+res.get(i).getIncludes());
		}
		
		long start = System.currentTimeMillis();
		pool = Executors.newFixedThreadPool(poolNum);
		System.out.println("---------------" + log);
		log.info("开始执行压缩操作----------------------------------------" + start);
		Build build = project.getBuild();
		String outputDirectory_ = build.getOutputDirectory();
		String sourceDirectory_ = build.getSourceDirectory();
		log.debug("outputDirectory_:" + outputDirectory_
				+ "--sourceDirectory_:" + sourceDirectory_);

		log.info("删除[" + outDir.getPath() + "]目录并重新创建开始。");
		log.debug("outDir:" + outDir.getPath());
		deleteFile(outDir);
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		log.info("删除[" + outDir.getPath() + "]目录并重新创建成功。");

		log.info("检查是否由设置webapps目录，如果没有将使用默认设置路径："
				+ sourceDirectory_.replace(File.separator + "java", "")
				+ File.separator + "webapp");

		if (webapps == null) {
			log.warn("isMerge设置为true,但未设置webapps项，将使用默认配置webapp");
			webapps = new File[1];
			webapps[0] = new File(sourceDirectory_.replace(File.separator
					+ "java", "")
					+ File.separator + "webapp");
			if (!webapps[0].exists()) {
				log.error(sourceDirectory_.replace(File.separator + "java", "")
						+ File.separator + "webapp未找到");
				System.exit(-1);
			}
		}

		log.info("检查分解资源文件中的所有文件归类");
		for (int i = 0; i < webapps.length; i++) {
			getAllCompressFilePath(webapps[i]);
		}

		// 执行压缩js操作
		if (isCompressJs) {
			CompressAbs comAbs = null;
			switch (jsCompressType) {
			case "yuicompress":
				comAbs = new CompressJsYuicompress(this); 
				comAbs.init();
				break;
			case "closure-compiler":
				comAbs = new CompressJsYuicompress(this);
				comAbs.init();
				break;
			case "command":
				log.debug("暂时不支持command方式压缩.");
				break;
			default:
				log.warn("跳过js压缩，可能jsCompressType值未设置正确，你当前设置为："
						+ jsCompressType);
				break;
			}
			comAbs.start();
		} else {
			log.info("跳过js压缩");
		}

		// 进行合并css操作
		if (isCompressCss) {
			// compressCss(build);
			CompressAbs comAbs = new CompressCssYuicompress(this);
			comAbs.init();
			comAbs.start();
		} else {
			log.info("跳过css压缩");
		}

		if (isCompressHtml) {
			CompressAbs comAbs = new CompressHtml(this);
			comAbs.init();
			comAbs.start();
		} else {
			log.info("跳过html压缩");
		}

		// 先进行合并操作
		if (isMerge) {
			CompressAbs comAbs = new CompressMerge(this);
			comAbs.init();
			comAbs.start();
		} else {
			log.info("跳过js或者css合并压缩操作");
		}

		if (isCompressSass) {
			if (getSassFile().size() == 0) {
				log.warn("没有找到sass/scss文件");
			} else {
				CompressAbs comAbs = new CompressSass(this);
				comAbs.init();
				comAbs.start();
			}
		} else {
			log.info("跳过sass/scss编译压缩");
		}

		if (isCompressClass) {
			CompressAbs comAbs = new CompressClass(this);
			comAbs.init();
//			comAbs.start();
		} else {
			log.info("跳过编译java文件");
		}

		long end = System.currentTimeMillis();
		log.info("结束执行压缩操作----------------------------------------" + end);
		log.info("结束执行压缩操作，总耗时：" + (end - start) + "mm");
	}

	public File getJarDir() {
		return jarDir;
	}

	public Build getBuild() {
		return project.getBuild();
	}

	/**
	 * 获取所有的资源文件
	 * 
	 * @param rootPath
	 * @return
	 */
	public void getAllCompressFilePath(File file) {
		File[] files = file.listFiles(new MyFileCompressFilter());
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					getAllCompressFilePath(files[i]);
				} else {
					if (files[i].getPath().endsWith(".js")) {
						jsFile.add(files[i]);
					} else if (files[i].getPath().endsWith(".css")) {
						cssFile.add(files[i]);
					} else if (files[i].getPath().endsWith("merge.xml")) {
						mergeXmlFile.add(files[i]);
					} else if (files[i].getPath().endsWith(".sass")
							|| files[i].getPath().endsWith(".scss")) {
						sassFile.add(files[i]);
					} else if (files[i].getPath().endsWith(".html")
							|| files[i].getPath().endsWith(".jsp")) {
						htmlFile.add(files[i]);
					}
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
	 * @param overlay
	 *            如果目标文件存在，是否覆盖
	 * @return 如果复制成功返回true，否则返回false
	 */
	public boolean copyFile(String srcFileName, String destFileName,
			boolean overlay) {
		File srcFile = new File(srcFileName);

		// 判断源文件是否存在
		if (!srcFile.exists()) {
			log.warn("复制失败源文件：" + srcFileName + "不存在！");
			return false;
		} else if (!srcFile.isFile()) {
			log.warn("复制文件失败，源文件：" + srcFileName + "不是一个文件！");
			return false;
		}

		// 判断目标文件是否存在
		File destFile = new File(destFileName);
		if (destFile.exists()) {
			// 如果目标文件存在并允许覆盖
			if (overlay) {
				// 删除已经存在的目标文件，无论目标文件是目录还是单个文件
				new File(destFileName).delete();
			}
		} else {
			// 如果目标文件所在目录不存在，则创建目录
			if (!destFile.getParentFile().exists()) {
				// 目标文件所在目录不存在
				if (!destFile.getParentFile().mkdirs()) {
					// 复制文件失败：创建目标文件所在目录失败
					return false;
				}
			}
		}

		// 复制文件
		int byteread = 0; // 读取的字节数
		InputStream in = null;
		FileOutputStream out = null;

		try {
			in = new FileInputStream(srcFile);
			out = new FileOutputStream(destFile);
			byte[] buffer = new byte[1024];

			while ((byteread = in.read(buffer)) != -1) {
				out.write(buffer, 0, byteread);
			}
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (out != null)
					out.close();
				if (in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String cmds(String cmd, String path) throws Exception {
		BufferedReader br = null;
		BufferedReader errbr = null;
		try {
			Runtime run = Runtime.getRuntime();
			Process p = null;
			if (System.getProperty("os.name").toLowerCase()
					.indexOf("Windows 10") >= 0) {
				p = run.exec(cmd);
			} else if (System.getProperty("os.name").toLowerCase()
					.indexOf("win") >= 0) {
				p = run.exec("cmd.exe /c " + cmd);
			} else {
				p = run.exec(cmd);
			}
			br = new BufferedReader(new InputStreamReader(p.getInputStream(),
					Charset.forName("GBK")));
			errbr = new BufferedReader(new InputStreamReader(
					p.getErrorStream(), Charset.forName("GBK")));
			int ch;
			StringBuffer text = new StringBuffer("");
			while ((ch = br.read()) != -1) {
				text.append((char) ch);
			}
			// System.out.println(text);
			StringBuffer text1 = new StringBuffer("");
			while ((ch = errbr.read()) != -1) {
				text1.append((char) ch);
			}
			if (!text1.toString().isEmpty()) {
				return text1.toString();
			}
			return "";
		} catch (Exception e) {
			// log.error("压缩出现错误:"+path,e);
			// e.printStackTrace();
			throw new Exception("压缩异常", e);
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
		// return "";
	}

	public class MyFileCompressFilter implements FileFilter {

		public boolean accept(File fl) {
			if (fl.isDirectory())
				return true;
			else {
				if (fl.getPath().endsWith("merge.xml")) {
					if (isMerge) {
						return true;
					}
					return false;
				} else if (fl.getPath().endsWith(".js")) {
					if (isCompressJs) {
						if (includes != null && !includes.isEmpty()) {
							for (int i = 0; i < includes.size(); i++) {
								if (fl.getPath().matches(includes.get(i))) {
									return true;
								}
							}
							return false;
						} else {
							return true;
						}
					}
					return false;
				} else if (fl.getPath().endsWith(".css")) {
					if (isCompressCss) {
						if (includes != null && !includes.isEmpty()) {
							for (int i = 0; i < includes.size(); i++) {
								if (fl.getPath().matches(includes.get(i))) {
									return true;
								}
							}
							return false;
						} else {
							return true;
						}
					} else {
						return false;
					}
				} else if (fl.getPath().endsWith(".html")
						|| fl.getPath().endsWith(".jsp")) {
					if (isCompressHtml) {
						return true;
					}
					return false;
				} else if (fl.getPath().endsWith(".sass")
						|| fl.getPath().endsWith(".scss")) {
					if (isCompressHtml) {
						return true;
					}
					return false;
				} else {
					return false;
				}
			}
		}
	}

	/**
	 * 获取真实路径地址
	 * 
	 * @param spath
	 * @return
	 */
	public String getSpath(String spath) {
		if ("".equals(spath.trim())) {
			return "";
		}
		// 先检查是否存在已经压缩的文件，如果存在就使用压缩的文件
		if (spath.endsWith(".js") && isCompressJs) {
			String path = outDir.getPath() + File.separator + spath;
			// log.debug("是否存在：" + new File(path).exists() + "--" + path);
			if (new File(path).exists()) {
				return path;
			}
		} else if (spath.endsWith(".css") && isCompressCss) {
			String path = outDir.getPath() + spath;
			if (new File(path).exists()) {
				return path;
			}
		}
		for (int i = 0; i < webapps.length; i++) {
			String path = webapps[i].getPath() + File.separator + spath;
			if (new File(path).exists()) {
				return path;
			}
		}
		return "";
	}

	private void deleteFile(File file) {
		if (file.exists()) {// 判断文件是否存在
			if (file.isFile()) {// 判断是否是文件
				file.delete();// 删除文件
			} else if (file.isDirectory()) {// 否则如果它是一个目录
				File[] files = file.listFiles();// 声明目录下所有的文件 files[];
				for (int i = 0; i < files.length; i++) {// 遍历目录下所有的文件
					this.deleteFile(files[i]);// 把每个文件用这个方法进行迭代
				}
				file.delete();// 删除文件夹
			}
		} else {
			// System.out.println("所删除的文件不存在");
		}
	}

	/**
	 * 读取文件
	 * 
	 * @param path
	 * @param str
	 * @param root
	 */
	public void readFile(String path, StringBuffer str) {
		InputStreamReader fr = null;
		BufferedReader br = null;
		try {
			System.out.println(path);
			fr = new InputStreamReader(new FileInputStream(path), "UTF-8");
			br = new BufferedReader(fr);
			String line = "";
			String[] arrs = null;
			while ((line = br.readLine()) != null) {
				str.append(line);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("获取静态资源文件出错：", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("获取静态资源文件出错：", e);
				}
			}
			if (fr != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("获取静态资源文件出错：", e);
				}
			}
		}
	}

	public Log getMyLog() {
		return log;
	}

	public MavenProject getProject() {
		return project;
	}

	public List<String> getExcludes() {
		return excludes;
	}

	public List<String> getIncludes() {
		return includes;
	}

	public String getEncoding() {
		return encoding;
	}

	public boolean isMerge() {
		return isMerge;
	}

	public boolean isCompressJs() {
		return isCompressJs;
	}

	public boolean isCompressCss() {
		return isCompressCss;
	}

	public boolean isCompressHtml() {
		return isCompressHtml;
	}

	public File[] getWebapps() {
		return webapps;
	}

//	public String getJs_warning_level() {
//		return js_warning_level;
//	}
//
//	public String getJs_compilation_level() {
//		return js_compilation_level;
//	}

	public File getOutDir() {
		return outDir;
	}

	public String getJsOutDir() {
		return jsOutDir;
	}

	public String getCssOutDir() {
		return cssOutDir;
	}

	public List<File> getMergeXmlFile() {
		return mergeXmlFile;
	}

	public List<File> getJsFile() {
		return jsFile;
	}

	public List<File> getCssFile() {
		return cssFile;
	}

	public List<File> getHtmlFile() {
		return htmlFile;
	}

	public List<File> getHbJsFile() {
		return hbJsFile;
	}

	public List<File> getHbCssFile() {
		return hbCssFile;
	}

	public int getPoolNum() {
		return poolNum;
	}

	public String getCompressErrorAction() {
		return compressErrorAction;
	}

	public boolean isSkip() {
		return skip;
	}

	public ExecutorService getPool() {
		return pool;
	}

	public ByteArrayOutputStream getOutReader() {
		return outReader;
	}

	public ByteArrayOutputStream getErrReader() {
		return errReader;
	}

	public CountDownLatch getLatch() {
		return latch;
	}

	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public ReadWriteLock getRwl() {
		return rwl;
	}

	/**
	 * 添加线程处理
	 * 
	 * @param runnable
	 */
	public void addPool(Runnable runnable) {
		pool.execute(runnable);
	}

	public List<File> getSassFile() {
		return sassFile;
	}

	/**
	 * 获取替换文件真实路径
	 * 
	 * @param path
	 * @return
	 */
	public String orgFile(String path) {
		for (int i = 0; i < webapps.length; i++) {
			if (path.startsWith(webapps[i].getPath())) {
				return path.replace(webapps[i].getPath(), "");
			}
		}
		return "";
	}

	/**
	 * 添加log执行日志
	 */
	public void addErrorLog(String msg) {
		rwl.writeLock().lock();// 取到写锁
		FileWriter fw = null;
		PrintWriter pw = null;
		try {
			// 如果文件存在，则追加内容；如果文件不存在，则创建文件
			File f = new File(outDir + "_error_compress.log");
			if (f.exists()) {
				f.delete();
			}
			fw = new FileWriter(f, true);
			pw = new PrintWriter(fw);
			pw.println(msg);
			pw.flush();
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				try {
					pw.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (fw != null) {
				try {
					fw.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			rwl.writeLock().unlock();// 释放写锁
		}
	}

	public boolean isResourcesCopy() {
		return isResourcesCopy;
	}
	
	public Log getLogH(){
		return this.log;
	}

	public int getCompressIntervalTime() {
		return compressIntervalTime;
	}

}
