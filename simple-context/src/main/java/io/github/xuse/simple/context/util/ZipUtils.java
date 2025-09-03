/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.xuse.simple.context.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xuse.querydsl.util.Assert;
import com.github.xuse.querydsl.util.IOUtils;
import com.github.xuse.querydsl.util.StringUtils;

import io.github.xuse.simple.context.util.zip.ArchiveSummary;
import io.github.xuse.simple.context.util.zip.TarEntry;
import io.github.xuse.simple.context.util.zip.ZipEntryWrapper;
import io.github.xuse.simple.context.util.zip.ZipInputStream;
import io.github.xuse.simple.context.util.zip.ZipOutputStream;
import lombok.SneakyThrows;

/**
 * JEF压缩解压的通用包：目前支持以下格式的文件
 * <ul>
 * <li>zip 压缩/解压 密码不支持，修复了JDK的编码问题。</li>
 * <li>tar.gz 压缩/解压 修复了Apache同名类的编码问题。</li>
 * <li>tar 压缩/解压</li>
 * </ul>
 */
public class ZipUtils {
	private static final Logger log = LoggerFactory.getLogger(ZipUtils.class);
	static {
		ZipOutputStream.DEFAULT_NAME_ENCODING = "GB18030";
		TarEntry.DEFAULT_NAME_ENCODING = "GB18030";
	}
	public static int TarLongFileNameMode = 0;

	/**
	 * 压缩为zip文件
	 * 
	 * @param zipFileName 压缩包路径
	 * @param inputName   源路径
	 * @return file
	 * @throws IOException
	 */
	public static File zip(String zipFileName, String inputName) throws IOException {
		return zip(new File(zipFileName), new File(inputName));
	}

	/**
	 * 压缩为zip文件
	 * 
	 * @param zipFile    压缩包文件
	 * @param inputFiles 多个压缩源
	 * @return file
	 * @throws IOException
	 */
	public static File zip(File zipFile, File... inputFiles) throws IOException {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
		for (File f : inputFiles) {
			zip(out, f, null, null);
		}
		out.flush();
		out.close();
		return zipFile;
	}

	/**
	 * 压缩为zip文件
	 * 
	 * @param zipFile    压缩包文件
	 * @param ep         压缩处理回调
	 * @param inputFiles 压缩源文件
	 * @return file
	 * @throws IOException
	 */
	public static File zip(File zipFile, EntryProcessor ep, File... inputFiles) throws IOException {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
		for (File f : inputFiles) {
			zip(out, f, null, ep);
		}
		out.flush();
		out.close();
		return zipFile;
	}

	/*
	 * 递归压缩方法
	 * 
	 * @param out 压缩包输出流
	 * 
	 * @param f 需要压缩的文件
	 * 
	 * @param base压缩包中的路径
	 */
	private static void zip(ZipOutputStream out, File f, String base, EntryProcessor ep) throws IOException {
		Assert.exist(f);
		if (StringUtils.isNotEmpty(base) && !base.endsWith("/"))
			base = base.concat("/");
		if (f.isDirectory()) {
			base = StringUtils.toString(base) + f.getName() + "/";
			base = (ep == null) ? base : ep.getZippedPath(f, base);
			if (base != null) {
				out.putNextEntry(new ZipEntry(base));
				for (File fl : f.listFiles()) {
					zip(out, fl, base, ep);
					if (ep != null && ep.breakProcess())
						break;
				}
			}
		} else { // 如果是文件，则压缩
			String entryName = StringUtils.toString(base) + f.getName();
			entryName = ep == null ? entryName : ep.getZippedPath(f, entryName);
			if (entryName != null) {
				log.debug("adding to Zip:" + entryName);
				out.putNextEntry(new ZipEntry(entryName)); // 生成下一个压缩节点
				FileInputStream in = new FileInputStream(f); // 读取文件内容
				IOUtils.copy(in, out, 8192);
				in.close();
			}
		}
	}

	/**
	 * 解压zip文件
	 * 
	 * @param zipFile   压缩包
	 * @param unzipPath 解压路径
	 * @return flag
	 */
	public static boolean unzip(String zipFile, String unzipPath) {
		return unzip(new File(zipFile), unzipPath, null, null);
	}

	/**
	 * 解压zip文件
	 * 
	 * @param file      压缩包
	 * @param charset   字符集
	 * @param unzipPath 解压路径
	 * @return flag
	 */
	@SneakyThrows
	public static boolean unzip(File file, String unzipPath, Charset charset, EntryProcessor cd) {
		try (InputStream in = IOUtils.getInputStream(file)) {
			unzip(in, unzipPath, charset, cd);
			return true;
		}
	}

	/**
	 * 解压
	 * 
	 * @param ins       压缩包
	 * @param unzipPath 解压路径
	 * @param charSet   压缩包内的文件名编码(可为null)
	 * @param cd        压缩处理器，压缩包的每个文件名都可以经过该类的检查和修正。(可以null)
	 * @throws IOException
	 */
	public static void unzip(InputStream ins, String unzipPath, Charset charSet, EntryProcessor cd) throws IOException {
		ZipInputStream in = null;
		try {
			in = new ZipInputStream(ins, charSet);
			ZipEntry fEntry = null;
			while ((fEntry = in.getNextEntry()) != null) {
				String entryName = fEntry.getName();
				if (cd != null) {
					entryName = cd.getExtractName(ZipEntryWrapper.of(fEntry));
				}
				if (entryName != null) {
					String fname = unzipPath + "/" + entryName;
					if (fname.endsWith("/")) {
						createFolder(new File(fname));
						continue;
					}
					byte[] doc = new byte[8192];
					File output = new File(fname);
					if (!output.getParentFile().exists()) {
						output.getParentFile().mkdirs();
					}
					FileOutputStream out = new FileOutputStream(fname);
					int n;
					while ((n = in.read(doc, 0, 8192)) != -1)
						out.write(doc, 0, n);
					out.close();
					out = null;
					doc = null;
				}
			}
		} finally {
			if (in != null)
				in.close(); // 关闭输入流
		}
	}

	/**
	 * 得到zip压缩文件的摘要信息
	 * 
	 * @param file
	 * @return ArchiveSummary
	 */
	public static ArchiveSummary getZipArchiveSummary(File file) {
		SummaryCollector sc = new SummaryCollector();
		unzip(file, null, null, sc);
		return sc.getSummary();
	}

	/**
	 * 默认的EntryProcessor实现B，目的收集压缩包的各项信息
	 * 
	 * @author Administrator
	 *
	 */
	public static class SummaryCollector extends EntryProcessor {
		private ArchiveSummary summary;

		public SummaryCollector() {
			summary = new ArchiveSummary();
		}

		@Override
		public boolean breakProcess() {
			return false;
		}

		@Override
		public String getExtractName(ZipEntryWrapper entry) {
			summary.addItem(entry);
			return entry.getName();
		}

		public ArchiveSummary getSummary() {
			return summary;
		}
	}

	/**
	 * 默认的EntryProcessor实现B，目的是在控制台上打印出压缩解压进度
	 * 
	 * @author Administrator
	 *
	 */
	public static class ConsoleShow extends EntryProcessor {
		private ArchiveSummary summary;
		private long currentPosition = 0;
		private long nextPromptSize = 0;// 下次提示
		private int count = 0;
		private long step; // 每次提示的步长
		private String name;

		public ConsoleShow(String name, ArchiveSummary size) {
			this.name = name;
			this.summary = size;
			this.step = size.getPackedSize() / 10;
			if (step < 100)
				step = 100;
		}

		@Override
		public boolean breakProcess() {
			return false;
		}

		@Override
		protected String getExtractName(ZipEntryWrapper wrapper) {
			String input=wrapper.getName();
			count++;
			if (currentPosition >= nextPromptSize) {
				nextPromptSize += step;
				String percent = count + "/" + summary.getItemCount() + " "
						+ toPercent(currentPosition, summary.getPackedSize());
				log.debug(name.replace("%%", percent).replace("$$", input));
			}
			
			currentPosition += wrapper.getComparessedLength();
			return input.replace('?', '_');
		}

		@Override
		public String getZippedPath(File source, String zippedBase) {
			return null;
		}
	}

	public static ConsoleShow getConsoleProgressHandler(String msg, ArchiveSummary summary) {
		return new ConsoleShow(msg, summary);
	}

	/**
	 * 压缩处理器 抽象类，你可以覆盖整个类的各种方法，实现以下功能 1、指定分卷压缩的大小 2、指定压缩后文件的名称、指定解压后文件的路径
	 * 3、指定某些文件跳过不压缩或者不解压 4、通过覆盖对应的事件，可以计算压缩解压的进度、时间、字节数等
	 * 当需要定制上述特殊行为时，可以传入一个处理器，实现你需要的逻辑
	 * 
	 * @author Administrator
	 * @Date 2011-7-7
	 */
	public abstract static class EntryProcessor {
		/**
		 * 当一个文件将被解压前调用，返回压缩后的文件路径
		 * 
		 * @param entryName    压缩包中的文件路径
		 * @param packedSize   文件压缩后大小
		 * @param unpackedSize 压缩前大小
		 * @return 解压后文件（相对）路径，默认应当和entryName一致。 如果不想解压此文件，可以return null.
		 */
		protected String getExtractName(ZipEntryWrapper wrapper) {
			return wrapper.getName();
		}

		/**
		 * 当一个文件将被压缩前调用，返回文件在压缩包中的路径
		 * 
		 * @param source     源文件
		 * @param zippedPath 默认压缩包中的文件路径
		 * @return 默认压缩包中的文件路径，如果不想压缩此文件，返回null.
		 */
		protected String getZippedPath(File source, String zippedPath) {
			return zippedPath;
		}

		/**
		 * 当每个文件压缩/解压后执行
		 * 
		 * @return true,继续压缩/解压任务。false则中断整个操作
		 */
		protected boolean breakProcess() {
			return false;
		}
	}

	/**
	 * 将两个数值的比值作为百分比显示
	 * 
	 * @param a
	 * @param b
	 */
	public static String toPercent(long a, long b) {
		return String.valueOf(10000 * a / b / 100f).concat("%");
	}

	public static void createFolder(File file) {
		if (file.isFile()) {
			throw new RuntimeException("Duplicate name file exist. can't create directory " + file.getPath());
		} else if (!file.exists()) {
			file.mkdirs();
		}
	}
}
