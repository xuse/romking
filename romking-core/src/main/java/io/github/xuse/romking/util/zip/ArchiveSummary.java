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
package io.github.xuse.romking.util.zip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ArchiveSummary {
	private long sourceSizeTotal = 0;
	private long packedSizeTotal = 0;
	private List<ZipEntryWrapper> names = new ArrayList<>();

	public long getUnpSize() {
		return sourceSizeTotal;
	}

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		sb.append("Items: ").append(names.size()).append(" total:").append(packedSizeTotal).append(" original:");
		sb.append(sourceSizeTotal).append(" radio").append(getPackRadio());
		return sb.toString();
	}


	public void setUnpSize(long unpSize) {
		this.sourceSizeTotal = unpSize;
	}

	public long getPackedSize() {
		return packedSizeTotal;
	}

	public void setPackedSize(long packedSize) {
		this.packedSizeTotal = packedSize;
	}

	public int getItemCount() {
		return names.size();
	}

	public void addItem(ZipEntryWrapper entry) {
		names.add(entry);
		packedSizeTotal += entry.getComparessedLength();
		sourceSizeTotal+=entry.getSouceLength();
	}

	public float getPackRadio() {
		BigDecimal p = new BigDecimal(packedSizeTotal);
		return p.divide(new BigDecimal(sourceSizeTotal),4,RoundingMode.HALF_UP).floatValue();
	}

	public ZipEntryWrapper getOne() {
		if(names.isEmpty()) {
			return null;
		}
		return names.get(0);
	}
}
