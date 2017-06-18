package com.njzxw.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeFilter implements Filter {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * 获取所有的merge.xml文件
	 */
	private List<File> mergeXmlFile = new ArrayList<File>();
	
	/**
	 * xml文件解析后的文件信息
	 */
	private List<Map<String,Object>> mergeXmlData = new ArrayList<Map<String,Object>>();
	
	private String jsDir;
	private String cssDir;
	private String encoding;
	private String outDir;
	private String root;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
		outDir = filterConfig.getServletContext().getRealPath("/");
		root = filterConfig.getServletContext().getContextPath();
		logger.debug("当前项目路径地址："+outDir+"----"+root);
		
		jsDir = getInitValue(filterConfig, "jsDir", "/js");
		cssDir = getInitValue(filterConfig, "cssDir", "/css");
		encoding = getInitValue(filterConfig, "encoding", "utf-8");
		
		loadXml();
		new Thread(new watchFile()).start();
	}
	
	public void loadXml(){
		logger.debug("Began to reload the merge XML file..................");
		mergeXmlFile.clear();
		//获取所有的merger.xml文件，检查是否存在对有的列表
		getAllCompressFilePath(new File(outDir));
		int count = mergeXmlFile.size();
		logger.debug("mergeXmlFile:"+count);
		mergeXmlData.clear();
		for (int j = 0; j < count; j++) {
			mergeFile(mergeXmlFile.get(j));
		}
		//logger.debug("mergeXmlData:"+mergeXmlData.isEmpty());
	}
	
	/**
	 * 获取参数信息，如果不存在就给予defautvalue空值
	 * @param filterConfig
	 * @param key 参数名
	 * @param defautValue 
	 * @return
	 */
	public String getInitValue(FilterConfig filterConfig,String key,String defautValue){
		String value = filterConfig.getInitParameter(key);
		if(value == null || "".equals(value) || "value".equals(value)){
			return defautValue;
		}
		return value;
	}
	
	/**
	 * 获取参数信息
	 * @param url
	 * @return
	 */
	public StringBuffer getMergeVal(String url){
		//截取url最后一段的尾数
//		logger.debug("访问url："+url+"---"+root+"---"+outDir);
		String name = url.substring(url.lastIndexOf("/")+1, url.lastIndexOf("."));
		StringBuffer str = new StringBuffer();
		
		//检查是否存在
//		String path = "";
//		if (url.endsWith(".js")) {
//			File jsOutDirFile = new File(outDir + File.separator + jsDir);
//			if (!jsOutDirFile.exists()) {
//				jsOutDirFile.mkdirs();
//			}
//			path = outDir + File.separator + jsDir + File.separator + name + ".js";
//		} else {
//			File cssOutDirFile = new File(outDir + "/" + cssDir);
//			if (!cssOutDirFile.exists()) {
//				cssOutDirFile.mkdirs();
//			}
//			path = outDir + File.separator + cssDir + File.separator + name + ".css";
//		}
		//如果文件存在直接读取
//		File file = new File(path);
//		if(file.exists()){
//			InputStreamReader fr = null;
//			BufferedReader br = null;
//			try {
//				fr = new InputStreamReader(new FileInputStream(path),encoding);
//				br = new BufferedReader(fr);
//				String line = "";
//				while ((line = br.readLine()) != null) {
//					str.append(line).append("\r\n");
//				}
//			} catch (Exception e) {
//				logger.error("合并出错：", e);
//			} finally {
//				if (br != null) {
//					try {
//						br.close();
//					} catch (IOException e) {
//						logger.error("合并出错：", e);
//					}
//				}
//				if (fr != null) {
//					try {
//						br.close();
//					} catch (IOException e) {
//						logger.error("合并出错：", e);
//					}
//				}
//			}
//			if(str.length() != 0){
//				return str;
//			}
//		}
		
		//logger.debug("mergeXmlData:"+(mergeXmlData.isEmpty()));
		//进行合并操作
		if(!mergeXmlData.isEmpty()){
			for(Map<String,Object> map : mergeXmlData){
				Object nameObj = map.get("name");
				String name_ = "";
				if(nameObj != null){
					name_ = String.valueOf(nameObj);
				}
//				logger.debug("name:"+name+"--name_:"+name_);
				if(name.equals(name_)){
					logger.debug("url:"+url);
					if(url.endsWith(".js")){
						str = mergeFileEl("js", name_, (List<Element>)(map.get("jsElList")));
					}else{
						str = mergeFileEl("css", name_, (List<Element>)(map.get("cssElList")));
					}
				}
			}
		}
		return str;
	}
	
	/**
	 * 解析xml文件包含成分
	 * @param file
	 */
	private void mergeFile(File file) {
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(file);
			Element node = document.getRootElement();
			List<Element> elementList = node.elements();
			for (int i = 0; i < elementList.size(); i++) {
				Map<String,Object> map = new HashMap<String,Object>();
				map.put("name", elementList.get(i).attributeValue("name"));
				map.put("jsElList", elementList.get(i).elements("js"));
				map.put("cssElList", elementList.get(i).elements("css"));
				mergeXmlData.add(map);
			}
//			logger.debug("map:"+mergeXmlData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private StringBuffer mergeFileEl(String type, String name, List<Element> elList){
		String path = "";
		if ("js".equals(type)) {
			File jsOutDirFile = new File(outDir + File.separator + jsDir);
			if (!jsOutDirFile.exists()) {
				jsOutDirFile.mkdirs();
			}
			path = outDir + File.separator + jsDir + File.separator + name + ".js";
		} else {
			File cssOutDirFile = new File(outDir + File.separator + cssDir);
			if (!cssOutDirFile.exists()) {
				cssOutDirFile.mkdirs();
			}
			path = outDir + File.separator + cssDir + File.separator + name + ".css";
		}
		File file = new File(path);
		OutputStreamWriter fw = null;
		InputStreamReader fr = null;
		BufferedReader br = null;
		StringBuffer str = new StringBuffer();
		try {
			file.createNewFile();
			fw = new OutputStreamWriter(new FileOutputStream(file),encoding);
			//logger.debug("elList:"+elList);
			for (int i = 0; i < elList.size(); i++) {
				String spath = elList.get(i).attribute("path").getStringValue();
//				logger.debug("spath:" + spath);
				if (!"".equals(spath)) {
					fr = new InputStreamReader(new FileInputStream(outDir + File.separator +spath),encoding);
					br = new BufferedReader(fr);
					String line = "";
					while ((line = br.readLine()) != null) {
						fw.write(line);
						fw.write("\r\n");
						str.append(line).append("\r\n");
					}
				}
			}
			fw.flush();
		} catch (Exception e) {
			logger.error("合并出错：", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("合并出错：", e);
				}
			}
			if (fr != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("合并出错：", e);
				}
			}
			if (fw != null) {
				try {
					fw.close();
				} catch (Exception e) {
					logger.error("合并出错：", e);
				}
			}
		}
		return str;
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
					if (files[i].getPath().endsWith("merge.xml")) {
						mergeXmlFile.add(files[i]);
					} 
				}
			}
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		// TODO Auto-generated method stub
		//检查文件是否是对应的js或者css文件
		HttpServletRequest req = (HttpServletRequest)request;
		String url = req.getRequestURI();
		String fileName = url.substring(url.lastIndexOf("/"));
		if(url.endsWith(".js")||url.endsWith(".css")){//包含js或者css 检查是否存在合并文件之中
			if(url.endsWith(".js")){
//				logger.debug(jsDir.concat(fileName)+"---"+url);
				if(!jsDir.concat(fileName).equals(url)){
					chain.doFilter(request, response);
					return ;
				}
			}
			if(url.endsWith(".css")){
				//logger.debug(cssDir.concat(fileName)+"---"+url);
				if(!cssDir.concat(fileName).equals(url)){
					//检查如果是css文件看对应的是否存在sass文件信息，如果存在css文件
					String path = outDir+url.replace(".css", "").concat(".sass");
					StringBuffer str = null;
					if(!new File(path).exists()){
						path = outDir+url.replace(".css", "").concat(".scss");
						if(!new File(path).exists()){
							chain.doFilter(request, response);
							return ;
						}else{
							getSassValue(outDir+url, "scss");
							str = getSassValue(outDir+url);
						}
					}else{
						getSassValue(outDir+url, "sass");
						str = getSassValue(outDir+url);
					}
					if(str.length() != 0){
						response.setCharacterEncoding(encoding);
						PrintWriter output = response.getWriter();
						output.write(str.toString());
						output.flush();
						output.close();
						return;
					}else{
						chain.doFilter(request, response);
						return;
					}
				}
			}
			
			StringBuffer str = getMergeVal(url);
			if(str.length() != 0){
				response.setCharacterEncoding(encoding);
				PrintWriter output = response.getWriter();
				output.write(str.toString());
				output.flush();
				output.close();
				return;
			}else{
				chain.doFilter(request, response);
			}
		}else{
			chain.doFilter(request, response);
		}
	}
	
	/**
	 * 获取sass文件对应css的值
	 * @param path
	 * @return
	 */
	public StringBuffer getSassValue(String path){
		InputStreamReader fr = null;
		BufferedReader br = null;
		StringBuffer str = new StringBuffer();
		try {
			fr = new InputStreamReader(new FileInputStream(path),encoding);
			br = new BufferedReader(fr);
			String line = "";
			while ((line = br.readLine()) != null) {
				str.append(line).append("\r\n");
			}
		} catch (Exception e) {
			logger.error("合并出错：", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("合并出错：", e);
				}
			}
			if (fr != null) {
				try {
					br.close();
				} catch (IOException e) {
					logger.error("合并出错：", e);
				}
			}
		}
		return str;
	}
	
	/**
	 * 获取sass文件信息
	 * @param path
	 * @param type
	 * @return
	 */
	public void getSassValue(String path,String type){
		String url = path.replace(".css", "").concat("."+type);
		String result = cmds("sass " + url + " " + path + " --style compressed --sourcemap=none --no-cache ");
		if(!"".equals(result.trim())){
			logger.error("["+path+"]压缩css出现错误："+result);
		}
	}
	
	private String cmds(String cmd) {
		BufferedReader br = null;
		BufferedReader errbr = null;
		try {
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

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	
	public class MyFileCompressFilter implements FileFilter {

		public boolean accept(File fl) {
			if (fl.isDirectory())
				return true;
			else {
				if (fl.getPath().endsWith("merge.xml")) {
						return true;
				}  else {
					return false;
				}
			}
		}
	}
	
	//进行监控操作，监控只要存在更改就进行重新加载merge.xml操作
	public class watchFile implements Runnable{

		public void run() {
			watch();
		}
    	
    }
	
	/**
	 * 监控文件变化
	 */
	public void watch(){
		WatchService watchService;
		try{
			String pathname = outDir;
			watchService = FileSystems.getDefault().newWatchService();
			
			List<String> resultFileName = new ArrayList<String>();
			resultFileName.add(pathname);
			
			for(int i=0;i<resultFileName.size();i++){
				Paths.get(resultFileName.get(i)).register(watchService,StandardWatchEventKinds.ENTRY_MODIFY);
			}
	        while(true)  {  
	            WatchKey key = watchService.take();  
	            for(WatchEvent<?> event:key.pollEvents())  {  
	                try{
	                	loadXml();
	                }catch(Exception e){
	                	logger.error("监控失败，出现错误"+e.getMessage(),e);
	                }
	            }  
	            if(!key.reset())  {  
	                break;  
	            }  
	        }
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	<T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

}
