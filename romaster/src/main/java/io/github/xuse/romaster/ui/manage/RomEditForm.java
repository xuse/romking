package io.github.xuse.romaster.ui.manage;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.common.InputType;
import io.github.xuse.romking.core.GameType;
import io.github.xuse.romking.repo.enums.Region;
import io.github.xuse.romking.repo.obj.RomFile;
import lombok.Data;

/**
 * ROM信息编辑表单
 */
@Data
public class RomEditForm {

	@FormField(caption = "游戏名", placeHolder = "游戏名", type = InputType.TEXT)
	private String name;

	@FormField(caption = "游戏ID", placeHolder = "平台标准编号", type = InputType.TEXT)
	private String gameid;

	@FormField(caption = "区域", type = InputType.COMBO)
	private Region region;

	@FormField(caption = "游戏类型", type = InputType.COMBO)
	private GameType gameType;

	@FormField(caption = "版本/DLC信息", placeHolder = "版本信息", type = InputType.TEXT)
	private String version;

	@FormField(caption = "Hack说明", placeHolder = "改版说明", type = InputType.TEXT)
	private String hackComment;

	@FormField(caption = "收藏等级", type = InputType.NUMBER)
	private int favorite;

	/**
	 * 从RomFile实体构建编辑表单
	 */
	public static RomEditForm fromRomFile(RomFile romFile) {
		RomEditForm form = new RomEditForm();
		form.setName(romFile.getName());
		form.setGameid(romFile.getGameid());
		form.setRegion(romFile.getRegion());
		form.setGameType(romFile.getGameType());
		form.setVersion(romFile.getVersion());
		form.setHackComment(romFile.getHackComment());
		form.setFavorite(romFile.getFavorite());
		return form;
	}
}
