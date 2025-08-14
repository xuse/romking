package io.github.xuse.romking.metadata.ee;

import java.util.List;

import lombok.Data;

@Data
public class Gamelist {
	public Gamelist(List<Game> games) {
		this.games=games;
	}

	private List<Game> games;

}
