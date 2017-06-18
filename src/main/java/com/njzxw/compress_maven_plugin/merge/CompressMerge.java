package com.njzxw.compress_maven_plugin.merge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.njzxw.compress_maven_plugin.CompressAbs;
import com.njzxw.compress_maven_plugin.CompressMojo;

/**
 * 合并操作
 * @author eliay
 *
 */
public class CompressMerge extends CompressAbs{

	public CompressMerge(CompressMojo compressMojo) {
		super(compressMojo);
	}

	@Override
	public void init() {
		int count = getCompressMojo().getMergeXmlFile().size();
		log.info("需要合并压缩的文件数量："+count);
		if(getCompressMojo().getMergeXmlFile().size() >= getCompressMojo().getPoolNum()){
			getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
		}else{
			getCompressMojo().setLatch(new CountDownLatch(count));
		}
	}

	@Override
	public void start() {
		int count = getCompressMojo().getMergeXmlFile().size();
		for (int j = 0; j < count; j++) {
			final File mergeXmlF = getCompressMojo().getMergeXmlFile().get(j);
			getCompressMojo().addPool(new Runnable() {
				@Override
				public void run() {
					mergeFile(mergeXmlF);
					getCompressMojo().getLatch().countDown();
				}
			});
			if((j+1)%getCompressMojo().getPoolNum() == 0){
				try {
					getCompressMojo().getLatch().await();
					if(getCompressMojo().getMergeXmlFile().size()-(j+1) >= getCompressMojo().getPoolNum()){
						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getPoolNum()));
					}else{
						getCompressMojo().setLatch(new CountDownLatch(getCompressMojo().getMergeXmlFile().size()-(j+1)));
					}
				} catch (InterruptedException e) {
					log.error("执行合并出现错误:"+mergeXmlF.getPath(),e);
				}
			}
		}
	}
	
	/**
	 * 执行压缩操作，单个文件执行
	 * @param jsfile
	 */
	private void mergeFile(File mergeFile){
		String outpath = getCompressMojo().getOutDir().getPath() + getCompressMojo().orgFile(mergeFile.getPath());
		// 创建对应的输出文件夹路径
		checkDir(outpath);
		
		mergeFile(mergeFile,outpath);
	}
	
	private void mergeFile(File file,String outPath) {
		try {
			SAXReader reader = new SAXReader();
			Document document = reader.read(file);
			Element node = document.getRootElement();
			List<Element> elementList = node.elements();
			for (int i = 0; i < elementList.size(); i++) {
				String name = elementList.get(i).attributeValue("name");
				List<Element> jsElList = elementList.get(i).elements("js");
				List<Element> cssElList = elementList.get(i).elements("css");
				mergeFileEl("js", name, jsElList);
				mergeFileEl("css", name,cssElList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void mergeFileEl(String type, String name, List<Element> elList){
		String path = "";
		if ("js".equals(type)) {
			File jsOutDirFile = new File(getCompressMojo().getOutDir().getPath() + File.separator + getCompressMojo().getJsOutDir());
			if (!jsOutDirFile.exists()) {
				jsOutDirFile.mkdirs();
			}
			path = getCompressMojo().getOutDir().getPath() + File.separator + getCompressMojo().getJsOutDir() + File.separator + name + ".js";
		} else {
			File cssOutDirFile = new File(getCompressMojo().getOutDir().getPath() + "/" + getCompressMojo().getCssOutDir());
			if (!cssOutDirFile.exists()) {
				cssOutDirFile.mkdirs();
			}
			path = getCompressMojo().getOutDir().getPath() + File.separator + getCompressMojo().getCssOutDir() + File.separator + name + ".css";
		}
		File file = new File(path);
		OutputStreamWriter fw = null;
		InputStreamReader fr = null;
		BufferedReader br = null;
		try {
			file.createNewFile();
			fw = new OutputStreamWriter(new FileOutputStream(file),getCompressMojo().getEncoding());
			for (int i = 0; i < elList.size(); i++) {
				String spath = getCompressMojo().getSpath(elList.get(i).attribute("path")
						.getStringValue());

				//log.debug("spath:" + spath);
				if (!"".equals(spath)) {
					fr = new InputStreamReader(new FileInputStream(spath),
							getCompressMojo().getEncoding());
					br = new BufferedReader(fr);
					String line = "";
					while ((line = br.readLine()) != null) {
						fw.write(line);
						fw.write("\r\n");
					}
				}
			}
			fw.flush();
		} catch (Exception e) {
			getCompressMojo().addErrorLog("合并出错："+ elList +" \n"+ e.getMessage());
			log.error("合并出错：", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("合并出错：", e);
				}
			}
			if (fr != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("合并出错：", e);
				}
			}
			if (fw != null) {
				try {
					fw.close();
				} catch (Exception e) {
					log.error("合并出错：", e);
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
