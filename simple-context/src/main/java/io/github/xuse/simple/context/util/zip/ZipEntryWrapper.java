package io.github.xuse.simple.context.util.zip;

import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;

public interface ZipEntryWrapper {
	String getName();
	
	long getSouceLength();
	
	long getComparessedLength();
	
	String getComment();
	
	long getCrc();
	
	byte[] getExtra();
	
	long getModified();
	
	long getAccessed();
	
	int getMethod();
	
	
	public static ZipEntryWrapper of(ZipEntry entry) {
		return new ZipEntryWrapper() {
			@Override
			public String getName() {
				return entry.getName();
			}

			@Override
			public long getSouceLength() {
				return entry.getSize();
			}

			@Override
			public long getComparessedLength() {
				return entry.getCompressedSize();
			}

			@Override
			public String getComment() {
				return entry.getComment();
			}

			@Override
			public long getCrc() {
				return entry.getCrc();
			}

			@Override
			public byte[] getExtra() {
				return entry.getExtra();
			}

			@Override
			public long getModified() {
				return entry.getTime();
			}

			@Override
			public long getAccessed() {
				FileTime time=entry.getLastAccessTime();
				if(time==null) {
					return -1;
				}
				return time.toMillis();
			}

			@Override
			public int getMethod() {
				return entry.getMethod();
			}
		};
	}
}
