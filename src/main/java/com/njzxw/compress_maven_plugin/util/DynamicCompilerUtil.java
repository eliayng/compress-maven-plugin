package com.njzxw.compress_maven_plugin.util;

import javax.tools.*;
import javax.tools.Diagnostic.Kind;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;


public class DynamicCompilerUtil{
	public Log logger = null;
    private String jars = "";
    /**
     * 编译的class路径
     */
    private String targetDir = "";
    /**
     * java所在目录
     */
    private String sourceDir = "";
    
    private String endfileName = "";

    public DynamicCompilerUtil(Log log){
    	this.logger = log; 
    } 
    
    /**
     * 判断字符串是否为空 有值为true 空为：false
     */
    public boolean isnull(String str) {
        if (null == str) {
            return false;
        } else if ("".equals(str)) {
            return false;
        } else if (str.equals("null")) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 需要压缩的结尾名称
     * @param fileName
     * @param encoding
     * @param jars
     * @param filePath
     * @param sourceDir
     * @param targetDir
     * @param diagnostics
     * @return
     */
    public boolean compiler(String fileName,String encoding, String jars, String filePath, String sourceDir, String targetDir){
    	this.endfileName = fileName;
    	return this.compiler(encoding, jars, filePath, sourceDir, targetDir);
    }
    
    /**
     * 编译java文件
     *
     * @param encoding    编译编码
     * @param jars        需要加载的jar
     * @param filePath    文件或者目录（若为目录，自动递归编译） 建议与sourcedIR一致
     * @param sourceDir   java源文件存放目录  建议与filepath一致
     * @param targetDir   编译后class类文件存放目录
     * @param diagnostics 存放编译过程中的错误信息
     * @return
     * @throws Exception
     */
    public boolean compiler(String encoding, String jars, String filePath, String sourceDir, String targetDir, DiagnosticCollector<JavaFileObject> diagnostics)
            throws Exception {
        // 获取编译器实例
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // 获取标准文件管理器实例
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        try {
            if (!isnull(filePath) && !isnull(sourceDir) && !isnull(targetDir)) {
                return false;
            }
            // 得到filePath目录下的所有java源文件
            File sourceFile = new File(filePath);
            List<File> sourceFileList = new ArrayList<File>();
            this.targetDir = targetDir;
            this.sourceDir = sourceDir;
            File targetDirFile = new File(targetDir);
            if(!targetDirFile.exists()){
            	targetDirFile.mkdirs();
            }
            getSourceFiles(sourceFile, sourceFileList);
            // 没有java文件，直接返回
            if (sourceFileList.size() == 0) {
            	logger.debug(filePath + "目录下查找不到任何java文件");
                return false;
            }
//            System.out.println("编译java文件："+sourceFileList); 
            // 获取要编译的编译单元
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFileList);
            /**
             * 编译选项，在编译java文件时，编译程序会自动的去寻找java文件引用的其他的java源文件或者class。 -sourcepath选项就是定义java源文件的查找目录， -classpath选项就是定义class文件的查找目录。
             */
            Iterable<String> options = null;
            if("".equals(jars)){
            	options = Arrays.asList("-encoding", encoding, "-d", targetDir, "-sourcepath", sourceDir);
            }else{
            	options = Arrays.asList("-encoding", encoding, "-classpath", jars, "-d", targetDir, "-sourcepath", sourceDir);
            }
            
            // 运行编译任务
            return compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();
        } finally {
            fileManager.close();
        }
    }

    /**
     * 查找该目录下的所有的java文件
     *
     * @param sourceFile
     * @param sourceFileList
     * @throws Exception
     */
    private void getSourceFiles(final File sourceFile, List<File> sourceFileList) throws Exception {
        if (sourceFile.exists() && sourceFileList != null) {//文件或者目录必须存在
            if (sourceFile.isDirectory()) {// 若file对象为目录
                // 得到该目录下以.java结尾的文件或者目录
                File[] childrenFiles = sourceFile.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        if (pathname.isDirectory()) {
//                            try {
//                                new CopyDirectory().copyDirectiory(pathname.getPath(), targetDir + pathname.getPath().substring(sourceDir.length(), pathname.getPath().length()));
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
                            return true;
                        } else {
                            String name = pathname.getName();
                            if (name.endsWith(".java")) {
//                            	logger.debug("endfileName:"+endfileName+"---"+name);
//                            	System.out.println("endfileName:"+endfileName+"---"+name);
                            	if(endfileName != null && !"".equals(endfileName)){
                            		if(name.endsWith(endfileName)){
                            			return true;
                            		}else{
                            			return false;
                            		}
                            	}
                                return true;
                            }
//                            try {
//                                new CopyDirectory().copyFile(pathname, new File(targetDir + pathname.getPath().substring(pathname.getPath().indexOf("src") + 3, pathname.getPath().length())));
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
                            return false;
                        }
                    }
                });
                // 递归调用
                for (File childFile : childrenFiles) {
                    getSourceFiles(childFile, sourceFileList);
                }
            } else {// 若file对象为文件
                sourceFileList.add(sourceFile);
            }
        }
    }

    /**
     * 查找该目录下的所有的jar文件
     *
     * @param jarPaths 多目录使用‘;’隔开
     * @throws Exception
     */
    private String getJarFiles(String jarPaths) throws Exception {
    	StringTokenizer strt = new StringTokenizer(jarPaths, ";");
    	while(strt.hasMoreElements()){
    		String jarPath = strt.nextToken();
    		File sourceFile = new File(jarPath);
            // String jars="";
            if (sourceFile.exists()) {// 文件或者目录必须存在
                if (sourceFile.isDirectory()) {// 若file对象为目录
                    // 得到该目录下以.java结尾的文件或者目录
                    File[] childrenFiles = sourceFile.listFiles(new FileFilter() {
                        public boolean accept(File pathname) {
                            if (pathname.isDirectory()) {
                                return true;
                            } else {
                                String name = pathname.getName();
                                if ((name.endsWith(".jar") || name.endsWith(".class")) ? true : false) {
                                    jars = jars + pathname.getPath() + ";";
                                    return true;
                                }
                                return false;
                            }
                        }
                    });
                }else{//若file为文件的时候
                	String name = sourceFile.getName();
                    if ((name.endsWith(".jar") || name.endsWith(".class")) ? true : false) {
                        jars = jars + sourceFile.getPath() + ";";
                    }
                }
            }
    	}
        return jars;
    }
    
    public boolean compiler(String encoding, String jarPath, String filePath, String sourceDir, String targetDir){
    	DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        boolean compilerResult = true;
		try {
			compilerResult = this.compiler("UTF-8", this.getJarFiles(jarPath), filePath, sourceDir, targetDir, diagnostics);
			if(!compilerResult){
				for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
					Kind kind = diagnostic.getKind();
					if(kind.equals(Kind.NOTE)){
						logger.info(diagnostic.getSource().toString().replace("RegularFileObject","")+"[line "+diagnostic.getLineNumber()+" column "+diagnostic.getColumnNumber()+"]"+diagnostic.getMessage(null));
					}else if(kind.equals(Kind.ERROR)){
						logger.error(diagnostic.getSource().toString().replace("RegularFileObject","")+"[line "+diagnostic.getLineNumber()+" column "+diagnostic.getColumnNumber()+"]"+diagnostic.getMessage(null));
					}else if(kind.equals(Kind.WARNING)){
						logger.warn(diagnostic.getSource().toString().replace("RegularFileObject","")+"[line "+diagnostic.getLineNumber()+" column "+diagnostic.getColumnNumber()+"]"+diagnostic.getMessage(null));
					}
				}
			}
		} catch (Exception e) {
			logger.error("编译出现异常：",e);
		}
        return compilerResult;
    }

    public static void main(String[] args) {
        try {
            String filePath = "D:/Users/Administrator/Workspaces/work1/server/src/main/java";
            String sourceDir = "D:/Users/Administrator/Workspaces/work1/server/src/main/java";
            String jarPath = "D:/Users/Administrator/Workspaces/work1/server/target/lib";
//            String jarPath = "";
            String targetDir = "F:\\java\\project\\bin";
//            boolean compilerResult = new DynamicCompilerUtil().compiler("UTF-8", jarPath, filePath, sourceDir, targetDir);
//            if (compilerResult) {
//                System.out.println("编译成功");
//            } else {
//                System.out.println("编译失败");
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
