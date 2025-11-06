package com.bfs.hibernateprojectdemo.util;

import com.bfs.hibernateprojectdemo.domain.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

class PatchUtilsTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final PatchUtils utils = new PatchUtils(mapper);

    @Test
    void simpleFieldUpdateWithMergePatch() throws Exception {
        Product p = new Product();
        p.setName("Old");
        p.setDescription("D");
        p.setRetailPrice(10);
        p.setWholesalePrice(8);
        p.setQuantity(5);

        String body = "{\"name\":\"New\",\"retailPrice\":12}";
        JsonMergePatch patch = JsonMergePatch.fromJson(mapper.readTree(body));
        Product out = utils.applyMergePatch(patch, p, Product.class);

        assertEquals("New", out.getName());
        assertEquals(12.0, out.getRetailPrice());
        assertEquals(8.0, out.getWholesalePrice());
        assertEquals(5, out.getQuantity());
    }

    static class NestedDto {
        public String title;
        public Meta meta;
        public static class Meta { public String author; public int pages; }
    }

    @Test
    void nestedObjectModificationWithMergePatch() throws Exception {
        NestedDto doc = new NestedDto();
        doc.title = "A";
        doc.meta = new NestedDto.Meta();
        doc.meta.author = "X";
        doc.meta.pages = 100;

        String body = "{\"meta\":{\"author\":\"Y\",\"pages\":101}}";
        JsonMergePatch patch = JsonMergePatch.fromJson(mapper.readTree(body));
        NestedDto out = utils.applyMergePatch(patch, doc, NestedDto.class);

        assertEquals("Y", out.meta.author);
        assertEquals(101, out.meta.pages);
        assertEquals("A", out.title);
    }

    static class ArrayDto {
        public java.util.List<String> tags;
    }

    @Test
    void arrayManipulationWithJsonPatch() throws Exception {
        ArrayDto dto = new ArrayDto();
        dto.tags = new java.util.ArrayList<>(Arrays.asList("a","b","c"));

        String ops = "[" +
                "{\"op\":\"add\",\"path\":\"/tags/1\",\"value\":\"x\"}," +
                "{\"op\":\"remove\",\"path\":\"/tags/0\"}," +
                "{\"op\":\"move\",\"from\":\"/tags/2\",\"path\":\"/tags/0\"}" +
                "]";
        JsonPatch patch = mapper.readValue(ops, JsonPatch.class);
        ArrayDto out = utils.applyJsonPatch(patch, dto, ArrayDto.class);

        assertEquals(Arrays.asList("c","x","b"), out.tags);
    }
}