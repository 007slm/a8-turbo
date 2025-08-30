package org.openjdbcproxy.grpc.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@ToString
public class Parameter implements Serializable {
    private Integer index;
    private ParameterType type;
    private List<Object> values;
}
