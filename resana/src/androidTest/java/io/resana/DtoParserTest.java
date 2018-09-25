package io.resana;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DtoParserTest {
    @Test
    public void missingMandatoryField() {
        String json = getCompleteJson().replace("d1:32423 ,", "");
        try {
            DtoParser.parse(json, Dto.class);
            fail("Expected " + DtoParser.MissingMandatoryFieldException.class.getName());
        } catch (Exception e) {
            assertTrue(e instanceof DtoParser.MissingMandatoryFieldException);
            assertThat(e.getMessage(), containsString("d1'"));
        }
    }

    @Test
    public void missingMandatoryFieldInParentDto() {
        String json = getCompleteJson().replace("s1:hojjat ,", "");
        try {
            DtoParser.parse(json, Dto.class);
            fail("Expected " + DtoParser.MissingMandatoryFieldException.class.getName());
        } catch (Exception e) {
            assertTrue(e instanceof DtoParser.MissingMandatoryFieldException);
            assertThat(e.getMessage(), containsString("s1'"));
        }
    }

    @Test
    public void missingMandatoryFieldInArray() {
        String json = getCompleteJson().replace("k:k2 ,", "");
        try {
            DtoParser.parse(json, Dto.class);
            fail("Expected " + DtoParser.MissingMandatoryFieldException.class.getName());
        } catch (Exception e) {
            assertEquals(DtoParser.MissingMandatoryFieldException.class.getName(), e.getClass().getName());
            assertThat(e.getMessage(), containsString("k'"));
        }
    }

    @Test
    public void missingMandatoryFieldInCollection() {
        String json = getCompleteJson().replace("k:k3 ,", "");
        try {
            DtoParser.parse(json, Dto.class);
            fail("Expected " + DtoParser.MissingMandatoryFieldException.class.getName());
        } catch (Exception e) {
            assertEquals(DtoParser.MissingMandatoryFieldException.class.getName(), e.getClass().getName());
            assertThat(e.getMessage(), containsString("k'"));
        }
    }

    @Test
    public void violateRange() {
        String json = getCompleteJson().replace("rf:3.45", "rf:10");
        try {
            DtoParser.parse(json, Dto.class);
            fail("Expected " + DtoParser.ConstraintViolateException.class.getName());
        } catch (Exception e) {
            assertEquals(DtoParser.ConstraintViolateException.class.getName(), e.getClass().getName());
            assertThat(e.getMessage(), containsString("rf'"));
        }
    }

    @Test
    public void violateNumericValuesConstraint() {
        String json = getCompleteJson().replace("vi:2", "vi:0");
        try {
            DtoParser.parse(json, Dto.class);
            fail("Expected " + DtoParser.ConstraintViolateException.class.getName());
        } catch (Exception e) {
            assertEquals(DtoParser.ConstraintViolateException.class.getName(), e.getClass().getName());
            assertThat(e.getMessage(), containsString("vi'"));
        }
    }

    @Test
    public void violateStringValuesConstraint() {
        String json = getCompleteJson().replace("vs:\"bar\"", "vs:\"qux\"");
        try {
            DtoParser.parse(json, Dto.class);
            fail("Expected " + DtoParser.ConstraintViolateException.class.getName());
        } catch (Exception e) {
            assertEquals(DtoParser.ConstraintViolateException.class.getName(), e.getClass().getName());
            assertThat(e.getMessage(), containsString("vs'"));
        }
    }

    @Test
    public void supportsBase64() {
        String json = getCompleteJson();
        final Dto dto = DtoParser.parse(json, Dto.class);
        assertNotNull(dto);
        assertEquals("Hojjat", dto.b64s);
        assertEquals(1, dto.lll);
    }

    private String getCompleteJson() {
        return "{s1:hojjat , i1:5 , lll:null , f1:0.5 , d1:32423 , arrItems:[{k:k1 , v:v1} , {k:k2 , v:v2}] , colItems:[{k:k3 , v:v3}] , rf:3.45 , vi:2 , vs:\"bar\" , \"b64s\":\"SG9qamF0\"}";
    }

    public static class ParentDto {
        @Mandatory
        String s1;

        Integer i1;
    }

    public static class Dto extends ParentDto {
        Float f1;

        int lll = 1;

        @Mandatory
        Double d1;

        ItemDto[] arrItems;

        Collection<ItemDto> colItems;

        @Range(from = 0, to = 5)
        @Mandatory
        Float rf;

        @NumericValues({1, 2, 3})
        Integer vi;

        @StringValues({"foo", "bar", "baz"})
        String vs;

        @Base64
        String b64s;
    }

    public static class ItemDto {
        @Mandatory
        String k;

        @Mandatory
        String v;
    }
}