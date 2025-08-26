package io.github.xuse.jetui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xuse.querydsl.util.StringUtils;
import com.google.common.cache.CacheLoader;

import io.github.xuse.jetui.annotation.FormField;
import io.github.xuse.jetui.model.FormFieldModel;

/**
 * 加载模型缓存
 * @author jiyi
 */

public class FormModelCacheLoader extends CacheLoader<Class<?>, List<FormFieldModel>> {
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public List<FormFieldModel> load(@Nonnull Class<?> iclz) throws Exception {
		logger.info("Creating Form model for class [{}]", iclz);
		List<FormFieldModel> models = null;
		if (models == null) {
			models = new ArrayList<FormFieldModel>();
			Class<?> clz = iclz;
			while (clz != Object.class && clz != Object.class) {
				for (Field field : clz.getDeclaredFields()) {
					addFields(field, models);
				}
				clz = clz.getSuperclass();
			}
			for (Method method : iclz.getMethods()) {
				addMethod(method, models);
			}
			Collections.sort(models);
		}
		return models;
	}


	private static void addFields(Field field, List<FormFieldModel> models) {
		FormField f = field.getAnnotation(FormField.class);
		if (f == null || !f.edit()) {
			return;
		}
		FormField ff=field.getAnnotation(FormField.class);
		if(ff!=null) {
			models.add(FormFieldModel.of(ff, field));
		}
	}

	private static void addMethod(Method method, List<FormFieldModel> models) {
		FormField f = method.getAnnotation(FormField.class);
		if (f == null || !f.edit()) {
			return;
		}
		// 计算字段CODE
		String name = method.getName();
		if (name.startsWith("get")) {
			name = StringUtils.uncapitalize(name.substring(3));
		} else if (name.startsWith("is")) {
			name = StringUtils.uncapitalize(name.substring(2));
		}
		models.add(FormFieldModel.of(f, name, method));
	}

}
