package io.github.xuse.romking.manager;

import java.io.File;

import org.junit.Test;

import com.github.xuse.querydsl.util.IOUtils;

import io.github.xuse.romking.manager.obj.RomDir;
import io.github.xuse.romking.manager.obj.RootCard;

public class RomManager extends ToolBase{
	@Test
	public void tuimuiPicChecker() {
		while(true) {
			File root=super.getInputPath("ROMDir Card:");
			
			File image=super.getInputPath("ImageRoot");
			if(!root.isDirectory()) {
				continue;
			}
			if(!image.isDirectory()){
				continue;
			}
			doCheck(root, image);
		}
	}
	
	private void doCheck(File root, File image) {
		for(File file:root.listFiles()) {
			if(file.getName().endsWith(".db")||file.getName().startsWith(".")) {
				continue;
			}
			if(file.isDirectory()) {
				File image2=new File(image, file.getName());
				if(image2.isDirectory()) {
					doCheck(file,image2);
				}else {
					System.out.println(file.getAbsolutePath()+"没有对应图片的目录。");
				}
			}else {
				//文件
				String name=IOUtils.removeExt(file.getName());
				File image2=findImage(image, name);
				if(image2==null) {
					System.out.println(file.getAbsolutePath()+"没有对应图片。");
				}
			}
		}
		
		
	}

	public static File findImage(File image, String name) {
		File i=new File(image,name+".jpg");
		if(i.exists()) {
			return i;
		}
		i=new File(image,name+".png");
		if(i.exists()) {
			return i;
		}
		i=new File(image,name+".jpeg");
		if(i.exists()) {
			return i;
		}
		return null;
	}

	@Test
	public void testScan() {
		File root=super.getInputPath("TF Card:");
		if(root.isDirectory()) {
			RootCard card=new RootCard(root);
			card.scan();
		}
		
	}

	@Test
	public void testScanNewRoms() {
		while(true) {
			File root=super.getInputPath("Rom DIR:");
			if(root.isDirectory()) {
				File gamelist=new File(root,"gamelist.xml");
				if(gamelist.isFile()) {
					RomDir roms=new RomDir(gamelist, root.getName());
					System.out.println("找到"+roms.size()+"个游戏配置，有效"+roms.getGood()+"个");
					roms.scanNewRom();
				}else {
					System.out.println("该目录不是ROM文件夹");
				}
			}	
		}
		
	}
	

	@Test
	public void testScanFilesRemove() {
		while(true) {
			File root=super.getInputPath("Rom DIR:");
			if(root.isDirectory()) {
				File gamelist=new File(root,"gamelist.xml");
				if(gamelist.isFile()) {
					RomDir roms=new RomDir(gamelist, root.getName());
					if(roms.removeInvalid()) {
						roms.writeXML();
					}
					System.out.println("找到"+roms.size()+"个游戏配置，有效"+roms.getGood()+"个");
					roms.scanUnusedFile();
					
				}else {
					System.out.println("该目录不是ROM文件夹");
				}
			}	
		}
		
	}
}
