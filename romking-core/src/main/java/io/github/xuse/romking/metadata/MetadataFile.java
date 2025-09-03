package io.github.xuse.romking.metadata;

import java.io.File;

import lombok.Data;

@Data
public class MetadataFile {
	private File file;
	private MetadataParser parser;

}
