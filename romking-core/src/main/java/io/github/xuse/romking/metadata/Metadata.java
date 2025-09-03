package io.github.xuse.romking.metadata;

import java.util.List;

import io.github.xuse.romking.core.Platform;
import io.github.xuse.romking.metadata.ee.Game;
import lombok.Data;

@Data
public class Metadata {
	private Platform platform;
	private List<Game> games;
	private String launcher;

}
