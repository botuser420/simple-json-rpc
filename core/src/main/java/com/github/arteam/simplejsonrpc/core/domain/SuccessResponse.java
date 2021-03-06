package com.github.arteam.simplejsonrpc.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 07.06.14
 * Time: 12:31
 * Representation of a successful JSON-RPC response
 */
public class SuccessResponse extends Response {

    @Nullable
    @JsonProperty("result")
    private final Object result;

    public SuccessResponse(@JsonProperty("id") @NotNull ValueNode id,
                           @JsonProperty("result") @Nullable Object result) {
        super(id);
        this.result = result;
    }

    @Nullable
    public Object getResult() {
        return result;
    }
}
