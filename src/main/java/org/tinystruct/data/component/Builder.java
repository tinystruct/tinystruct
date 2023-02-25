/*******************************************************************************
 * Copyright  (c) 2023 James Mover Zhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.data.component;

import org.tinystruct.ApplicationException;
import org.tinystruct.system.util.StringUtilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Builder extends HashMap<String, Object> implements Struct, Serializable {

    private static final long serialVersionUID = 3484789992424316230L;
    private static final String QUOTE = "\"";
    private static final Logger logger = Logger.getLogger(Builder.class.getName());
    private int closedPosition = 0;

    public Builder() {
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        Set<Entry<String, Object>> keys = this.entrySet();
        Object value;
        String key;
        for (Entry<String, Object> entry : keys) {
            value = entry.getValue();
            key = entry.getKey();

            if (value instanceof String || value instanceof StringBuffer || value instanceof StringBuilder)
                buffer.append(QUOTE).append(key).append(QUOTE).append(":").append(QUOTE).append(StringUtilities.escape(value.toString())).append(QUOTE);
            else
                buffer.append(QUOTE).append(key).append(QUOTE).append(":").append(value);

            buffer.append(",");
        }

        if (buffer.length() > 0)
            buffer.setLength(buffer.length() - 1);

        return "{" + buffer + "}";
    }

    /**
     * Parse the resource.
     * Format:{"Name":"Mover","Birthday":"1982-03-20","Ability":{"Item1":"Music"
     * ,"Item2","Java"}}
     *
     * @param resource JSON string
     * @throws ApplicationException application exception
     */
    @Override
    public void parse(String resource) throws ApplicationException {
        // 默认相信任何一个被传入的都是合法的字符串
        resource = resource.trim();
        if (!resource.startsWith("{") && !resource.endsWith("}")) {
            throw new ApplicationException("Invalid data format!");
        }
        if (resource.startsWith("{")) {
            logger.log(Level.FINE, "待处理:{}", resource);
            // 查找关闭位置
            this.closedPosition = this.seekPosition(resource);
            // 获得传入的实体对象里{key:value,key:value,...key:value},{key:value,key:value,...key:value}序列
            // 脱去传入的实体对象外壳 key:value,key:value,...key:value 序列
            String values = resource.substring(1, closedPosition - 1);
            logger.log(Level.FINE, "已脱壳:{}", values);

            this.parseValue(values);
        }
    }

    public int getClosedPosition() {
        return closedPosition;
    }

    public void setClosedPosition(int closedPosition) {
        this.closedPosition = closedPosition;
    }

    private void parseValue(String value) throws ApplicationException {
        logger.log(Level.FINE, "待分析:{}", value);
        value = value.trim();
        if (value.startsWith(QUOTE)) {
            // 处理传入的实体对象外壳 "key":value,"key":"value",..."key":value 序列
            int COLON_POSITION = value.indexOf(':');
            int start = COLON_POSITION + 1;
            String keyName = value.substring(1, COLON_POSITION - 1);

            logger.log(Level.FINE, "取键值:{}", keyName);
            String $value = value.substring(start).trim();
            logger.log(Level.FINE, "分析值:{}", $value);

            Object keyValue;
            if ($value.startsWith(QUOTE)) {
                logger.log(Level.FINE, "提取值:{}", $value);

                int $end = this.next($value, '"');
                keyValue = $value.substring(1, $end - 1).trim();

                if ($end + 1 < $value.length()) {
                    $value = $value.substring($end + ",".length());

                    this.parseValue($value);
                }
            } else if ($value.indexOf('{') == 0) {
                logger.log(Level.FINE, "遇实体:{}", $value);
                int closedPosition = this.seekPosition($value);

                // 获得传入的实体对象里{key:value,key:value,...key:value},{key:value,key:value,...key:value}序列
                // 脱去传入的实体对象外壳 key:value,key:value,...key:value 序列

                String _$value = $value.substring(0, closedPosition);

                logger.log(Level.FINE, "分实体:{}", _$value);
                Builder builder = new Builder();
                builder.parse(_$value);

                keyValue = builder;
                if (closedPosition < $value.length()) {
                    _$value = $value.substring(closedPosition + ",".length());
                    logger.log(Level.FINE, "分实体:{}", _$value);
                    this.parseValue(_$value);
                }
            } else if ($value.indexOf('[') == 0) {
                Builders builders = new Builders();
                logger.log(Level.FINE, $value);
                String remainings = builders.parse($value);
                keyValue = builders;
                if (!Objects.equals(remainings, ""))
                    this.parseValue(remainings);
            } else {
                if ($value.indexOf(',') != -1) {
                    String _value = $value.substring(0, $value.indexOf(','));
                    if (_value.length() > 0) {
                        keyValue = getValue(_value);
                    } else {
                        keyValue = _value;
                    }

                    $value = $value.substring($value.indexOf(',') + 1);
                    this.parseValue($value);
                } else {
                    if ($value.length() > 0) {
                        keyValue = getValue($value);
                    } else {
                        keyValue = $value;
                    }
                }
            }

            this.put(keyName, keyValue);
        }
    }

    private Object getValue(String value) {
        Object keyValue;
        if (Pattern.compile("^-?\\d+$").matcher(value).find()) {
            try {
                keyValue = Integer.parseInt(value);
            } catch (Exception e) {
                keyValue = Long.valueOf(value);
            }
        } else if (Pattern.compile("^-?\\d+(\\.\\d+)$").matcher(value).find()) {
            keyValue = Double.parseDouble(value);
        } else if (Pattern.compile("^(true|false)$").matcher(value.toLowerCase(Locale.ROOT)).find()) {
            keyValue = Boolean.parseBoolean(value);
        } else {
            keyValue = value;
        }
        return keyValue;
    }

    private int seekPosition(String value) {
        char[] chars = value.toCharArray();
        int i = 0;
        int n = 0;
        int position = chars.length;

        while (i < position) {
            char c = chars[i];
            if (c == '{') {
                if (i - 1 >= 0 && chars[i - 1] == '\\') {
                } else
                    n++;
            } else if (c == '}') {
                if (i - 1 >= 0 && chars[i - 1] == '\\') {
                } else
                    n--;
            }

            i++;
            if (n == 0)
                position = i;
        }

        return position;
    }

    private int next(String value, char begin) {
        char[] chars = value.toCharArray();
        int i = 0;
        int n = 0;
        int position = chars.length;

        while (i < position) {
            char c = chars[i];
            if (c == begin) {
                if (i - 1 >= 0 && chars[i - 1] == '\\') {
                } else
                    n++;
            }

            i++;
            if (n == 2)
                position = i;
        }

        return position;
    }

    @Override
    public Row toData() {
        Row row = new Row();
        Field field = new Field();
        Set<Entry<String, Object>> keySet = this.entrySet();
        String key;
        Object value;
        for (Entry<String, Object> entry : keySet) {
            value = entry.getValue();
            key = entry.getKey();
            FieldInfo info = new FieldInfo();
            info.set("name", key);
            info.set("value", value);
            field.append(key, info);
        }

        row.append(field);
        return row;
    }

    public void saveAsFile(File file) throws ApplicationException {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(this.toString());
        } catch (FileNotFoundException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public int size() {
        return this.keySet().size();
    }

}
