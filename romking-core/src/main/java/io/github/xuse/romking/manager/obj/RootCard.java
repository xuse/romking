package io.github.xuse.romking.manager.obj;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class RootCard {
	private final File root;
	private final List<RomDir> dirs = new ArrayList<>();
	
	public RootCard(File root) {
		this.root=root;
		
	}

	public void scan() {
		for(File file:root.listFiles()) {
			if(file.isDirectory()) {
				File gamelist=new File(file,"gamelist.xml");
				if(gamelist.exists()) {
					RomDir romdir=new RomDir(gamelist,file.getName());
					if(romdir.parse()) {
						dirs.add(romdir);
					}
				}
			}
		}
	}
}
