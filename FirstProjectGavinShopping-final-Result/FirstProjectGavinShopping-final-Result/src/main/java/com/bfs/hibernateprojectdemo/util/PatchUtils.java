package com.bfs.hibernateprojectdemo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;

public class PatchUtils {
    private final ObjectMapper mapper;

    public PatchUtils(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public <T> T applyJsonPatch(JsonPatch patch, T target, Class<T> targetClass) throws Exception {
        JsonNode targetNode = mapper.valueToTree(target);
        JsonNode patched = patch.apply(targetNode);
        return mapper.treeToValue(patched, targetClass);
    }

    public <T> T applyMergePatch(JsonMergePatch patch, T target, Class<T> targetClass) throws Exception {
        JsonNode targetNode = mapper.valueToTree(target);
        JsonNode patched = patch.apply(targetNode);
        return mapper.treeToValue(patched, targetClass);
    }
}