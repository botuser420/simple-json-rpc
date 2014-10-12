package com.github.arteam.simplejsonrpc.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.github.arteam.simplejsonrpc.core.domain.ErrorMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 10/12/14
 * Time: 6:23 PM
 *
 * @author Artem Prigoda
 */
public class BatchRequestBuilder extends AbstractBuilder {

    private final List<ObjectNode> requests = new ArrayList<ObjectNode>();

    private Map<JsonNode, JavaType> returnTypes = new HashMap<JsonNode, JavaType>();

    public BatchRequestBuilder(@NotNull Transport transport, @NotNull ObjectMapper mapper) {
        super(transport, mapper);
    }

    public BatchRequestBuilder add(long id, String method, Object... params) {
        requests.add(request(new LongNode(id), method, arrayParams(params)));
        return this;
    }

    public BatchRequestBuilder add(int id, String method, Object... params) {
        requests.add(request(new IntNode(id), method, arrayParams(params)));
        return this;
    }

    public BatchRequestBuilder add(String id, String method, Object... params) {
        requests.add(request(new TextNode(id), method, arrayParams(params)));
        return this;
    }

    public BatchRequestBuilder add(String method, Object... params) {
        requests.add(request(NullNode.instance, method, arrayParams(params)));
        return this;
    }

    public BatchRequestBuilder add(long id, String method, Map<String, ?> params) {
        requests.add(request(new LongNode(id), method, objectParams(params)));
        return this;
    }

    public BatchRequestBuilder add(int id, String method, Map<String, ?> params) {
        requests.add(request(new IntNode(id), method, objectParams(params)));
        return this;
    }

    public BatchRequestBuilder add(String id, String method, Map<String, ?> params) {
        requests.add(request(new TextNode(id), method, objectParams(params)));
        return this;
    }

    public BatchRequestBuilder add(String method, Map<String, ?> params) {
        requests.add(request(NullNode.instance, method, objectParams(params)));
        return this;
    }

    public BatchRequestBuilder add(long id, String method, Object[] params, Class<?> responseType) {
        return add(id, method, params).returnType(id, responseType);
    }

    public BatchRequestBuilder add(int id, String method, Object[] params, Class<?> responseType) {
        return add(id, method, params).returnType(id, responseType);
    }

    public BatchRequestBuilder add(String id, String method, Object[] params, Class<?> responseType) {
        return add(id, method, params).returnType(id, responseType);
    }

    public BatchRequestBuilder add(long id, String method, Map<String, ?> params, Class<?> responseType) {
        return add(id, method, params).returnType(id, responseType);
    }

    public BatchRequestBuilder add(int id, String method, Map<String, ?> params, Class<?> responseType) {
        return add(id, method, params).returnType(id, responseType);
    }

    public BatchRequestBuilder add(String id, String method, Map<String, ?> params, Class<?> responseType) {
        return add(id, method, params).returnType(id, responseType);
    }

    public BatchRequestBuilder returnType(long id, Class<?> responseType) {
        return returnType(id, SimpleType.construct(responseType));
    }

    public BatchRequestBuilder returnType(int id, Class<?> responseType) {
        return returnType(id, SimpleType.construct(responseType));
    }

    public BatchRequestBuilder returnType(String id, Class<?> responseType) {
        return returnType(id, SimpleType.construct(responseType));
    }

    public BatchRequestBuilder returnType(long id, JavaType javaType) {
        returnTypes.put(new LongNode(id), javaType);
        return this;
    }

    public BatchRequestBuilder returnType(int id, JavaType javaType) {
        returnTypes.put(new IntNode(id), javaType);
        return this;
    }

    public BatchRequestBuilder returnType(String id, JavaType javaType) {
        returnTypes.put(new TextNode(id), javaType);
        return this;
    }

    public BatchRequestBuilder returnType(long id, TypeReference<?> typeReference) {
        returnTypes.put(new LongNode(id), mapper.getTypeFactory().constructType(typeReference.getType()));
        return this;
    }

    public BatchRequestBuilder returnType(int id, TypeReference<?> typeReference) {
        returnTypes.put(new IntNode(id), mapper.getTypeFactory().constructType(typeReference.getType()));
        return this;
    }

    public BatchRequestBuilder returnType(String id, TypeReference<?> typeReference) {
        returnTypes.put(new TextNode(id), mapper.getTypeFactory().constructType(typeReference.getType()));
        return this;
    }

    public Map<?, ?> execute() {
        String textResponse = executeRequest();
        try {
            Map<Object, Object> responses = new HashMap<Object, Object>();
            ArrayNode arrayNode = (ArrayNode) mapper.readTree(textResponse);
            for (JsonNode responseNode : arrayNode) {
                JsonNode result = responseNode.get(RESULT);
                JsonNode error = responseNode.get(ERROR);
                JsonNode version = responseNode.get(JSONRPC);
                JsonNode id = responseNode.get(ID);

                if (version == null) {
                    throw new IllegalStateException("Not a JSON-RPC response: " + responseNode);
                }
                if (!version.asText().equals(VERSION_2_0)) {
                    throw new IllegalStateException("Bad protocol version in a response: " + responseNode);
                }
                if (error != null) {
                    ErrorMessage errorMessage = mapper.treeToValue(error, ErrorMessage.class);
                    throw new JsonRpcException(errorMessage);
                }

                if (result == null) {
                    throw new IllegalStateException("Neither result or error is set in a response: " + responseNode);
                }

                if (id == null) {
                    continue;
                }

                JavaType javaType = returnTypes.get(id);
                if (javaType != null) {
                    responses.put(nodeValue(id), mapper.convertValue(result, javaType));
                } else {
                    throw new IllegalStateException("Unspecified id=" + id);
                }
            }
            return responses;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable parse a JSON response: " + textResponse, e);
        } catch (IOException e) {
            throw new IllegalStateException("I/O error during a response processing", e);
        }
    }

    String executeRequest() {
        String textRequest;
        String textResponse;
        try {
            textRequest = mapper.writeValueAsString(requests);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable convert " + requests + " to JSON", e);
        }
        try {
            textResponse = transport.pass(textRequest);
        } catch (IOException e) {
            throw new IllegalStateException("I/O error during a request processing", e);
        }
        return textResponse;
    }

    private static Object nodeValue(JsonNode id) {
        if (id.isLong()) {
            return id.longValue();
        } else if (id.isInt()) {
            return id.intValue();
        } else if (id.isTextual()) {
            return id.textValue();
        }
        throw new IllegalArgumentException("Wrong id=" + id);
    }

}