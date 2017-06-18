package com.njzxw.compress_maven_plugin;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.Log;

import com.njzxw.compress_maven_plugin.CompressMojo;

/**
 * 执行压缩的父类
 * @author eliay
 *
 */
public abstract class CompressAbs {
	
	private Build build;
	
	private CompressMojo compressMojo;
	
	public Log log;
	
	public CompressAbs(){}
	
	public CompressAbs(CompressMojo compressMojo){
		build = compressMojo.getBuild();
		setCompressMojo(compressMojo);
		setLog(compressMojo.getLogH());
	}
	
	/**
	 * 初始化并且执行操作
	 */
	public abstract void init();
	
	/**
	 * 开始执行压缩操作
	 */
	public abstract void start();
	
	
	
	public Build getBuild() {
		return build;
	}

	private void setBuild(Build build) {
		this.build = build;
	}
	public CompressMojo getCompressMojo() {
		return compressMojo;
	}

	private void setCompressMojo(CompressMojo compressMojo) {
		this.compressMojo = compressMojo;
	}

	public void setLog(Log log) {
		this.log = log;
	}
}
