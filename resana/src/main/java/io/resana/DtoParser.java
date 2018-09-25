package io.resana;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author Hojjat Imani
 */

class DtoParser {
    private static Gson mandatoryFieldCheckerGson;
    private static Gson gson;
    private static HashSet<Class<?>> nonDtoClasses =
            new HashSet<>(Arrays.asList(new Class<?>[]{Object.class, Integer.class, Float.class, Double.class, String.class}));


    static <T> T parse(String raw, Class<T> type) {
        return getParser().fromJson(raw, type);
    }

    static String toString(Object src) {
        return getGson().toJson(src);
    }

    private static Gson getParser() {
        if (mandatoryFieldCheckerGson == null) {
            mandatoryFieldCheckerGson = new GsonBuilder().registerTypeHierarchyAdapter(Object.class, new JsonDeserializer<Object>() {
                @Override
                public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    final Object o = getGson().fromJson(json, typeOfT);
                    new FieldConstraintChecker().checkObject(o);
                    return o;
                }
            }).registerTypeHierarchyAdapter(Object.class, new JsonSerializer<Object>() {
                @Override
                public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
                    throw new RuntimeException("Not Implemented! (this custom json object can only deserialize objects)");
                }
            }).create();
        }
        return mandatoryFieldCheckerGson;
    }

    private static Gson getGson() {
        if (gson == null)
            gson = new Gson();
        return gson;
    }

    private static class FieldConstraintChecker {
        HashSet<Object> checked = new HashSet<>();

        private void checkObject(Object o) {
            if (o == null || !checked.add(o))
                return;
            final Class<?> type = o.getClass();
            if (type.isEnum()) {
            } else if (type.isArray()) {
                checkArrayObject(o);
            } else if (o instanceof Collection) {
                checkCollectionObject((Collection) o);
            } else {
                for (Field f : getFieldsUpTo(type))
                    checkObjectField(o, f);
            }
        }

        private void checkArrayObject(Object arr) {
            for (int i = 0; i < Array.getLength(arr); i++)
                checkObject(Array.get(arr, i));
        }

        private void checkCollectionObject(Collection coll) {
            if (coll.size() > 0)
                for (Object item : coll)
                    checkObject(item);
        }

        private void checkObjectField(Object object, Field field) {
            if (field.getType().isPrimitive())
                return;
            try {
                field.setAccessible(true);
                Object value = field.get(object);
                for (Annotation annotation : field.getAnnotations()) {
                    if (annotation instanceof Mandatory) {
                        checkMandatoryAnnotation(field, value);
                    } else if (annotation instanceof Range) {
                        checkRangeAnnotation(field, value, (Range) annotation);
                    } else if (annotation instanceof NumericValues) {
                        checkNumericValuesAnnotation(field, value, (NumericValues) annotation);
                    } else if (annotation instanceof StringValues) {
                        checkStringValuesAnnotation(field, value, (StringValues) annotation);
                    } else if (annotation instanceof Base64) {
                        checkBase64Annotaion(object, field, value);
                    }
                }
                checkObject(value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private void checkBase64Annotaion(Object object, Field field, Object value) throws IllegalAccessException {
            if (value != null)
                field.set(object, BefrestImpl.Util.decodeBase64(value.toString()));
        }

        private void checkNumericValuesAnnotation(Field field, Object value, NumericValues annotation) {
            if (value != null) {
                final Double a = Double.valueOf(value.toString());
                boolean isValid = false;
                for (double v : annotation.value())
                    if (a == v) {
                        isValid = true;
                        break;
                    }
                if (!isValid) {
                    final String msg = getFieldsDisplayName(field) + "   value=" + value + "    expected one of the following " + Arrays.toString(annotation.value());
                    throw new ConstraintViolateException(msg);
                }
            }
        }

        private void checkStringValuesAnnotation(Field field, Object value, StringValues annotation) {
            if (value != null) {
                final String a = value.toString();
                boolean isValid = false;
                for (String s : annotation.value()) {
                    if (a.equals(s)) {
                        isValid = true;
                        break;
                    }
                }
                if (!isValid) {
                    final String msg = getFieldsDisplayName(field) + "   value=" + value + "    expected one of the following " + Arrays.toString(annotation.value());
                    throw new ConstraintViolateException(msg);
                }
            }
        }

        private void checkRangeAnnotation(Field field, Object value, Range annotation) {
            if (value != null) {
                final Double a = Double.valueOf(value.toString());
                if (a < annotation.from() || a > annotation.to()) {
                    final String msg = getFieldsDisplayName(field) + "   value=" + value + "    expected in range [" + annotation.from() + " , " + annotation.to() + "]";
                    throw new ConstraintViolateException(msg);
                }
            }
        }

        private void checkMandatoryAnnotation(Field field, Object value) {
            if (value == null) {
                String fName = getFieldsDisplayName(field);
                throw new MissingMandatoryFieldException(fName);
            }
        }

        private String getFieldsDisplayName(Field field) {
            String fName = "'" + field.toString() + "'";
            if (field.getAnnotation(SerializedName.class) != null)
                fName += "  (SerializedAs '" + field.getAnnotation(SerializedName.class).value() + "')";
            return fName;
        }

        private List<Field> getFieldsUpTo(Class<?> aClass) {
            if (nonDtoClasses.contains(aClass))
                return new ArrayList<>();
            List<Field> currentClassFields = new ArrayList<>(Arrays.asList(aClass.getDeclaredFields()));
            Class<?> parentClass = aClass.getSuperclass();
            if (parentClass != null && !nonDtoClasses.contains(parentClass))
                currentClassFields.addAll(getFieldsUpTo(parentClass));
            return currentClassFields;
        }
    }

    static class MissingMandatoryFieldException extends JsonParseException {
        static String MESSAGE_PREF = "Missing mandatory field ";

        MissingMandatoryFieldException(String fieldName) {
            super(MESSAGE_PREF + fieldName);
        }
    }

    static class ConstraintViolateException extends JsonParseException {

        ConstraintViolateException(String msg) {
            super("constraint is not satisfied for " + msg);
        }
    }
}
