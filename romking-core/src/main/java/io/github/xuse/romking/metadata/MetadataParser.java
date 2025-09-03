package io.github.xuse.romking.metadata;

import java.io.File;

public interface MetadataParser {
	Metadata parse(File file);
}
