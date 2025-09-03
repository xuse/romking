package io.github.xuse.romking;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.xuse.querydsl.annotation.dbdef.TableSpec;
import com.github.xuse.querydsl.asm.ASMUtils;
import com.github.xuse.querydsl.asm.ASMUtils.ClassAnnotationExtracter;
import com.github.xuse.querydsl.asm.ClassReader;
import com.github.xuse.querydsl.util.ClassScanner;

import io.github.xuse.querydsl.sql.code.generate.QCalssGenerator;
import io.github.xuse.romking.repo.obj.GlobalTask;
import io.github.xuse.romking.repo.obj.RomFile;
import io.github.xuse.simple.context.util.RandomData;

public class CodeGenerateTest {
	@Test
	public void scanCodeGenerate() {
		String pkg = "io.github.xuse.romking.repo.obj";
		List<String> classNames = new ArrayList<>();
		new ClassScanner().excludeInnerClass(true).filterWithClassReader(cl -> {
			ClassAnnotationExtracter extractor = new ClassAnnotationExtracter();
			cl.accept(extractor, ClassReader.SKIP_CODE);
			if (extractor.hasAnnotation(TableSpec.class)) {
				classNames.add(ASMUtils.getJavaClassName(cl));
				return true;
			}
			return false;
		}).scan(pkg);
		QCalssGenerator g = new QCalssGenerator();
		for (String clzName : classNames) {
			try {
				Class<?> clz = Class.forName(clzName);
				File file = g.generate(clz);
				System.out.println(file.getAbsolutePath());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void codeGenerate() {
		QCalssGenerator g = new QCalssGenerator();
		File file = g.generate(GlobalTask.class);
		System.out.println(file.getAbsolutePath());
	}
	
	@Test
	public void dataRandom() {
		List<RomFile> files= Arrays.asList(RandomData.newArrayInstance(RomFile.class, 10));
	}
}
