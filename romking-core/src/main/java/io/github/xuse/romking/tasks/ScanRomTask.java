package io.github.xuse.romking.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.xuse.querydsl.init.ScanOptions;
import com.github.xuse.querydsl.util.Assert;
import com.github.xuse.querydsl.util.IOUtils;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.repo.enums.MediaType;
import io.github.xuse.romking.repo.enums.WrapType;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.romking.util.ZipUtils;
import io.github.xuse.romking.util.zip.ArchiveSummary;



public class ScanRomTask implements Task{
	private File dir;
	private String label;
	private ScanOptions options;
	private long begin;
	private Platform platform;
	
	private List<File> metadatas = new ArrayList<>();
	
	static Map<String,MediaType> mediaMap=new HashMap<>();
	static {
		mediaMap.put("mp3", MediaType.SOUND);
		mediaMap.put("ogg", MediaType.SOUND);
		mediaMap.put("mp4", MediaType.VIDEO);
		mediaMap.put("jpeg", MediaType.IMAGE);
		mediaMap.put("jpg", MediaType.IMAGE);
		mediaMap.put("png", MediaType.IMAGE);
		mediaMap.put("rar", MediaType.OTHER);
		mediaMap.put("7z", MediaType.OTHER);
		mediaMap.put("md", MediaType.OTHER);
		//mediaMap.put("txt", MediaType.OTHER);
	}

	
	public ScanRomTask(File dir, String label, ScanOptions options) {
		super();
		this.dir = dir;
		this.label = label;
		this.options = options;
		Assert.isTrue(dir.isDirectory());
	}

	@Override
	public TaskType getType() {
		return TaskType.SCAN_DIR;
	}

	@Override
	public String getName() {
		return label+"<"+dir.getAbsolutePath()+">";
	}

	@Override
	public String getProgress() {
		return "";
	}

	@Override
	public ProcessResult execute() {
		this.begin=System.currentTimeMillis();
		scanFiles(dir);
		scanMetadata();
		return new ProcessResult(200,"");
	}

	private void scanMetadata() {
		// TODO Auto-generated method stub
		
	}

	private void scanFiles(File dir) {
		for(String f:dir.list()){
			File entry=new File(dir,f);
			if(entry.isDirectory()) {
				scanFiles(entry);
				continue;
			}
			String ext=IOUtils.getExtName(f);
			MediaType type=mediaMap.get(ext);
			if(type!=null) {
				addMedia(entry,type);
				continue;
			}
			if(isMetadata(f,ext)) {
				metadatas.add(entry);
				continue;
			}
			
			WrapType wrapType;
			String crc;
			if(isZip(ext)){
				ArchiveSummary summary=ZipUtils.getZipArchiveSummary(entry);
				wrapType = summary.getItemCount()>1 ?  WrapType.ZIPPED_DIRECTORY: WrapType.ZIPPED_ROM;

				
				crc= Long.toHexString(summary.getOne().getCrc());
			}else {
				wrapType = WrapType.ROM;
				//检查类型
			}
			
			RomFile file=new RomFile();
			
			
			
			file.setCrc(ext);
		}
	}
			

	private boolean isZip(String ext) {
		return "zip".equals(ext);
	}

	private boolean isMetadata(String filename,String ext) {
		if(filename.startsWith("gamelist") && "xml".equals(ext)) {
			return true;
		}
		//if()
		return false;
	}

	private void addMedia(File entry,MediaType type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getBegin() {
		return begin;
	}
}
