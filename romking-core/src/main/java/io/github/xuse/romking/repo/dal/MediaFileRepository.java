package io.github.xuse.romking.repo.dal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.xuse.querydsl.lambda.LambdaColumn;
import com.github.xuse.querydsl.lambda.LambdaTable;
import com.github.xuse.querydsl.lambda.NumberLambdaColumn;
import com.github.xuse.querydsl.lambda.StringLambdaColumn;
import com.github.xuse.querydsl.sql.SQLQueryFactory;

import io.github.xuse.romking.repo.enums.MediaType;
import io.github.xuse.romking.repo.obj.MediaFile;
import io.github.xuse.romking.repo.obj.MediaFileFilter;
import io.github.xuse.simple.context.Service;

@Service
public class MediaFileRepository extends AbstractRepository<MediaFile, Integer, MediaFileFilter> {
	public static final LambdaTable<MediaFile> t = () -> MediaFile.class;

	public static final StringLambdaColumn<MediaFile> filepath = MediaFile::getFilepath;
	public static final StringLambdaColumn<MediaFile> ext = MediaFile::getExt;
	public static final StringLambdaColumn<MediaFile> md5 = MediaFile::getMd5;

	public static final NumberLambdaColumn<MediaFile, Integer> dirId = MediaFile::getDirId;
	public static final NumberLambdaColumn<MediaFile, Integer> referCount = MediaFile::getReferCount;

	public static final LambdaColumn<MediaFile, MediaType> platform = MediaFile::getType;

	public SQLQueryFactory getFactory() {
		return factory;
	}

	public static void main(String[] args) throws SecurityException, NoSuchFieldException {
		Map<TypeVariable<?>,Type> context=new LinkedHashMap<>();
		Class<?> c = MediaFileRepository.class;
		while (c != Object.class) {
			Type tSuperContext = c.getGenericSuperclass();
			if (tSuperContext instanceof ParameterizedType) {
				Type[] typeArgs = ((ParameterizedType) tSuperContext).getActualTypeArguments();
				TypeVariable<?>[] vars = c.getSuperclass().getTypeParameters();
				for (int i = 0; i < vars.length; i++) {
					TypeVariable<?> key = vars[i];
					Type value = typeArgs[i];
					if (value instanceof TypeVariable<?>) {
						value = typeArgs[i] = context.get((TypeVariable<?>) value);
						context.put(key, value);
					} else {
						context.put(key, value);
					}
				}
				System.out.println(c.getSimpleName() + " extends " + c.getSuperclass().getSimpleName() + " <"
						+ Arrays.toString(typeArgs) + ">");

			}
			c=c.getSuperclass();
		};
	}
}
