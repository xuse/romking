package io.github.xuse.romking.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

import com.github.xuse.querydsl.util.StringUtils;
import com.sun.jna.platform.FileUtils;

import lombok.SneakyThrows;

public abstract class ToolBase {
	private FileUtils fileUtils = FileUtils.getInstance();
	
//	protected File ffmpeg = findFile("D:\\Tools\\MP4Box\\libs\\ffmpeg.exe", "E:\\Tools\\MP4Box\\ffmpeg-4.3.1\\bin\\ffmpeg.exe");
//
//	protected File realesrgan = findFile("D:\\Tools\\upsolution\\realesrgan-ncnn-vulkan.exe", "E:\\Tools\\upsolution\\realesrgan-ncnn-vulkan.exe");



	@SneakyThrows
	protected void delete(File... mp4)  {
		FileUtils.getInstance().moveToTrash(mp4);
	}
	@SneakyThrows
	protected void delete(Collection<File> mp4)  {
		FileUtils.getInstance().moveToTrash(mp4.toArray(new File[0]));
	}
	
	public static String getInputStringNoTrim(String hint) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.print(hint);
		try {
			String s=reader.readLine();
			return StringUtils.isEmpty(s)? null: s;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String getInputString(String hint) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.print(hint);
		try {
			return StringUtils.trimToNull(reader.readLine());

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static int getInputInt(String hint) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.print(hint);
		int result;
		while (true) {
			try {
				 result=StringUtils.toInt(reader.readLine(),-1);
				 if(result>-1) {
					 return result;
				 }
			} catch (IOException e) {
				e.printStackTrace();
				return -1;
			}	
		}
	}

	public boolean trash(File... files) {
		if (fileUtils.hasTrash()) {
			try {
				fileUtils.moveToTrash(files);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			System.out.println("警告，没有回收站，文件将被删除");
			boolean flag = true;
			for(File f:files) {
				flag=flag && f.delete();
			}
			return flag;
		}
	}

	protected static File getInputPath(String hint) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.print(hint);
		String path;
		try {
			while (true) {
				path = reader.readLine();
				if (StringUtils.isEmpty(path)) {
					System.out.print(hint);
					continue;
				}
				if ("q".equalsIgnoreCase(path) || "exit".equalsIgnoreCase(path)) {
					return null;
				}
				if(path.startsWith("\"")) {
					path=path.substring(1,path.length()-1);
				}
				File f = new File(path);
				if (f.exists()) {
					return f;
				} else {
					System.out.println("路径不存在。");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}


	private File findFile(String... paths) {
		for (String path : paths) {
			File file = new File(path);
			if (file.exists()) {
				return file;
			}
		}
		throw new IllegalStateException("Not found file");
	}
}
