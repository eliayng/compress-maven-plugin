package com.njzxw.compress_maven_plugin.js;

import java.io.IOException;
import java.io.PrintStream;

import com.google.javascript.jscomp.CommandLineRunner;

public class MyCommandLineRunner extends CommandLineRunner {

	public MyCommandLineRunner(String[] args) {
		super(args);
	}
	
	public MyCommandLineRunner(String[] args,PrintStream out, PrintStream err){
		super(args,out,err);
	}
	
	@Override
	public int doRun() throws IOException {
		return super.doRun();
	}

//	public static void main(String[] args) {
//		MyCommandLineRunner runner = new MyCommandLineRunner(args);
//		if (runner.shouldRunCompiler()) {
//			try {
//				runner.doRun();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		if (runner.hasErrors()) {
//			System.exit(-1);
//		}
//	}

	public static void main(String[] args) {
//		String[] agrs = new String[8];
//		agrs[0] = "--warning_level";
//		agrs[1] = "QUIET";
//		agrs[2] = "--compilation_level";
//		agrs[3] = "SIMPLE_OPTIMIZATIONS";
//		agrs[4] = "--js";
//		agrs[5] = "C:\\Users\\eliay\\Desktop\\java\\common.js";
//		agrs[6] = "--js_output_file";
//		agrs[7] = "C:\\Users\\eliay\\Desktop\\java\\common1111111111.js";
//		
		String[] agrs = new String[0];
		System.out.println(args.length);
		MyCommandLineRunner runner = new MyCommandLineRunner(agrs);
		if (runner.shouldRunCompiler()) {
			try {
				int exc = runner.doRun();
				System.out.println(exc);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
		
	}
	
}
