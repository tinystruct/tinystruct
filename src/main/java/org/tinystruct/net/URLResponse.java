package org.tinystruct.net;

import java.util.List;
import java.util.Map;

public interface URLResponse {
    int getStatusCode();
    String getBody();
    Map<String, List<String>> getHeaders();
}
