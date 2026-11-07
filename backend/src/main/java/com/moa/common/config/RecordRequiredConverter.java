package com.moa.common.config;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.type.TypeFactory;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

/**
 * API 설명서의 필수 칸 규칙. record의 칸은 기본 필수(required)다.
 * 값이 없을 수 있는 칸만 {@code @Nullable}(org.jspecify)이나 {@code @Schema(requiredMode = NOT_REQUIRED)}를 붙여 선택으로 둔다.
 * 프론트가 /v3/api-docs로 타입을 만들 때 늘 오는 칸이 선택(?)으로 나오지 않게 한다. springdoc이 ModelConverter 빈을 쓴다.
 */
@Component
public class RecordRequiredConverter implements ModelConverter {

	private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

	@Override
	@SuppressWarnings("rawtypes")
	public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
		if (!chain.hasNext()) {
			return null;
		}
		Schema<?> schema = chain.next().resolve(type, context, chain);
		Class<?> raw = TypeFactory.defaultInstance().constructType(type.getType()).getRawClass();
		if (schema == null || raw == null || !raw.isRecord()) {
			return schema;
		}
		Schema<?> model = schema.get$ref() == null ? schema
			: context.getDefinedModels().get(schema.get$ref().substring(SCHEMA_REF_PREFIX.length()));
		if (model == null || model.getProperties() == null) {
			return schema;
		}
		for (RecordComponent component : raw.getRecordComponents()) {
			String name = component.getName();
			boolean listed = model.getRequired() != null && model.getRequired().contains(name);
			if (model.getProperties().containsKey(name) && !listed && !optional(raw, component)) {
				model.addRequiredItem(name);
			}
		}
		return schema;
	}

	private static boolean optional(Class<?> record, RecordComponent component) {
		if (component.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
			return true;
		}
		io.swagger.v3.oas.annotations.media.Schema annotation = schemaAnnotation(record, component);
		return annotation != null
			&& annotation.requiredMode() == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
	}

	private static io.swagger.v3.oas.annotations.media.Schema schemaAnnotation(Class<?> record,
		RecordComponent component) {
		try {
			Field field = record.getDeclaredField(component.getName());
			io.swagger.v3.oas.annotations.media.Schema onField = field
				.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
			return onField != null ? onField
				: component.getAccessor().getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
		} catch (NoSuchFieldException ex) {
			return null;
		}
	}
}
