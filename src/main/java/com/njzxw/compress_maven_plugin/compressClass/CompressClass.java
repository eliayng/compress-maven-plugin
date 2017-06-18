package com.njzxw.compress_maven_plugin.compressClass;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Resource;

import com.njzxw.compress_maven_plugin.CompressAbs;
import com.njzxw.compress_maven_plugin.CompressMojo;
import com.njzxw.compress_maven_plugin.util.DynamicCompilerUtil;

/**
 * 压缩html文件
 * @author eliay
 *
 */
public class CompressClass extends CompressAbs{

	String sourceDir;
	String libDir;
	String classDir;
	boolean isResourcesCopy;
	List<Resource> resList;
	String defautResourceDir;
	Properties properties;
	Map<WatchKey,Path> watchKeyMap = new HashMap<>();
	
	Map<String,Long> modifyFileMap = new HashMap<>();
	
	public CompressClass(CompressMojo compressMojo) {
		super(compressMojo);
	}

	@Override
	public void init() {
		//开启监视java文件，并进行class编译操作
		sourceDir = getCompressMojo().getBuild().getSourceDirectory();
		//循环获取需要监控的文件路径
		libDir = getCompressMojo().getJarDir().getPath();
		classDir = getCompressMojo().getBuild().getOutputDirectory();
		defautResourceDir = sourceDir.replace("\\java", "\\resources");
		properties = getCompressMojo().getProject().getProperties();
		isResourcesCopy = getCompressMojo().isResourcesCopy();
		if(isResourcesCopy){//需要进行资源文件替换，如果改动资源文件就进行复制过去
			resList = getCompressMojo().getBuild().getResources();
		}
		
		getCompressMojo().getProject().getProperties();
		log.debug("defautResourceDir:"+defautResourceDir);
		log.debug("sourceDir:"+sourceDir);
		log.debug("libDir:"+libDir);
		log.debug("classDir:"+classDir);
		
		new Thread(new watchFile()).start();
	}

	@Override
	public void start() {
	}
	
	public void startCompilerFile(String fileName){
		new DynamicCompilerUtil(log).compiler(fileName,getCompressMojo().getEncoding(), libDir, sourceDir, sourceDir, classDir);
	}
	
	public class watchFile implements Runnable{

		public void run() {
			watch();
		}
    	
    }
	
	public void setSourceDir(String sourceDir) {
		this.sourceDir = sourceDir;
	}

	public CompressClass(){}
	
	public static void main(String[] args) {
		CompressClass compressClass = new CompressClass();
		compressClass.setSourceDir("D:\\www");
		compressClass.test();
	}
	
	public void test(){
		new Thread(new watchFile()).start();
	}
	
	/**
	 * 监控文件变化
	 */
	public void watch(){
		WatchService watchService; 
		try{
			String pathname = sourceDir;
			watchService = FileSystems.getDefault().newWatchService();
			List<String> resultFileName = new ArrayList<String>();
			resultFileName.add(pathname);
			if(resList == null || resList.isEmpty()){
				resultFileName.add(defautResourceDir);
				ergodic(new File(defautResourceDir), resultFileName);
			}else{
				for(Resource resource:resList){
					resultFileName.add(resource.getDirectory());
					ergodic(new File(resource.getDirectory()), resultFileName);
				}
			}
			ergodic(new File(pathname), resultFileName);
			
			//去重复
			List<String> resultFileNames = new ArrayList<String>(new HashSet<String>(resultFileName));
			
			for(int i=0;i<resultFileNames.size();i++){
				Path path = Paths.get(resultFileNames.get(i));
				WatchKey watchKey = path.register(watchService,StandardWatchEventKinds.ENTRY_MODIFY,StandardWatchEventKinds.ENTRY_CREATE);
				watchKeyMap.put(watchKey, path);
			}
	        while(true)  {  
	            WatchKey key = watchService.take();  
	            for(WatchEvent<?> event:key.pollEvents())  {  
	                try{
	                	Path path = watchKeyMap.get(key);
	                	String filePath = path.toFile().getPath()+File.separator+event.context();
//	                	log.info("修改文件："+filePath);
	                	if(checkModify(filePath)){
	                		log.info("修改文件："+filePath);
	                		if(filePath.contains(sourceDir.replaceAll("/", File.separator))){
		                		startCompilerFile(event.context().toString());
		                	}else{
		                		copyFile(filePath);
		                	}
	                	}
	                }catch(Exception e){
	                	e.printStackTrace();
	                	log.error("监控失败，出现错误"+e.getMessage(),e);
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
	
	public boolean checkModify(String filePath){
		long modyfiyTime = new File(filePath).lastModified();
		Long modifyDate = modifyFileMap.get(filePath);
//		log.debug("修改文件："+modyfiyTime+"-"+modifyDate+"["+filePath+"]"+getCompressMojo().getCompressIntervalTime());
		if(modifyDate == null){
			modifyFileMap.put(filePath, modyfiyTime);
			return true;
		}else{
			if(modyfiyTime == modifyDate){
				return false;
			}else{
				//检查间隔时间
				if(modyfiyTime-modifyDate>getCompressMojo().getCompressIntervalTime()){
					modifyFileMap.put(filePath, modyfiyTime);
					return true;
				}else{
					return false;
				}
			}
		}
	}
	
	/**
	 * 进行文件复制
	 * @param filePath chou'yan
	 * @throws Exception 
	 */
	public void copyFile(String filePath) throws Exception{
		//检查该路径对应的是那个资源文件
//		log.debug("resList:"+resList);
		if(resList == null || resList.isEmpty()){
			//直接复制到默认路径
			String endPath = filePath.replace(defautResourceDir, "");
			Path paths = Paths.get(filePath);
			File targetFile = new File((classDir+endPath).substring(0,(classDir+endPath).lastIndexOf("/")));
            if(targetFile.exists()){
            	targetFile.mkdirs();
            }
			String targetPath = classDir+endPath;
			Files.copy(paths, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);  
		}else{//进行校验数据信息
			checkResources(filePath);
		}
	}
	
	public void checkResources(String filePath) throws Exception{
		for (Resource resource : resList) {
//			log.debug("resource:"+resource.toString());
			String path = resource.getDirectory();
			boolean filtering = resource.isFiltering();
			String targetPath = resource.getTargetPath();
			List<String> excludes = resource.getExcludes();
			List<String> includes = resource.getIncludes();
			String endPath = filePath.replace(path, "");
			if(targetPath == null || "".equals(targetPath) || "null".equals(targetPath)){
				targetPath = classDir;
			}
			if(filePath.startsWith(path)){
				boolean isExclude = isExclude(excludes, filePath);//true 包含在排除之内
				boolean isIncludes = isExclude(includes, filePath);//true 包含在不排除之内
				if((isExclude&&isIncludes)||(!isExclude&&isIncludes)){//满足此条件直接进行复制
					if(!filtering){
						File targetFile = new File((targetPath+endPath).substring(0,(targetPath+endPath).lastIndexOf("/")));
			            if(targetFile.exists()){
			            	targetFile.mkdirs();
			            }
						Path paths = Paths.get(filePath);
						String targetPath_ = targetPath+endPath;
						Files.copy(paths, Paths.get(targetPath_), StandardCopyOption.REPLACE_EXISTING); 
					}else{
//						log.info("targetPath+endPath:"+targetPath+endPath); 
			            File targetFile = new File((targetPath+endPath).substring(0,(targetPath+endPath).lastIndexOf("\\")));
			            if(targetFile.exists()){
			            	targetFile.mkdirs();
			            }
//						FileWriter fw = new FileWriter(targetPath+endPath);
//				        BufferedWriter writer = new BufferedWriter(fw);
						
				        FileOutputStream fos = new FileOutputStream(targetPath+endPath); 
				        OutputStreamWriter osw = new OutputStreamWriter(fos, getCompressMojo().getEncoding());   
				        
						//直接进行替换，按行读取文件
						FileInputStream fis = new FileInputStream(filePath);  
			            // 指定字符编码  
						Reader rd = new InputStreamReader(fis, getCompressMojo().getEncoding());  
						BufferedReader br = new BufferedReader(rd);  
//			            StringBuffer sb = new StringBuffer();  
			            String line = null;  
			            while((line = br.readLine()) != null) {
			            	Enumeration<Object> enu = properties.keys();
			            	while(enu.hasMoreElements()){
			            		String key = String.valueOf(enu.nextElement());
			            		line = line.replaceAll("\\$(\\s*)\\{(\\s*)"+key+"(\\s*)\\}", properties.getProperty(key));
			            	}
			            	osw.write(line);
			            	osw.write("\r\n");
			            }
			            osw.flush();  
			            osw.close();
			            fis.close();
					}
				}
			}
		}
	}
	
	public boolean isExclude(List<String> excludes,String path){
		if(excludes == null || excludes.isEmpty()){
			return true;
		}else{
			for(String exclude:excludes){
				if(path.matches("^".concat(exclude.replace("**", ".*")).concat("$"))){
					return true;
				}
			}
		}
		return false;
	}
	
	private List<String> ergodic(File file,List<String> resultFileName){
        File[] files = file.listFiles();
        if(files == null) 
        	return resultFileName;// 判断目录下是不是空的
        for (File f : files) {
            if(f.isDirectory()){// 判断是否文件夹
                resultFileName.add(f.getPath());
                ergodic(f,resultFileName);// 调用自身,查找子目录
            }
        }
        return resultFileName;
    }
	
}
