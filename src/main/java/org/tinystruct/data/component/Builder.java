/*******************************************************************************
 * Copyright  (c) 2017 James Mover Zhou
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
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class Builder extends HashMap<String, Object> implements Struct, Serializable {

    private static final String QUOTE = "\"";
    private static final long serialVersionUID = 3484789992424316230L;
    private boolean log = false;
    private static final Logger logger = Logger.getLogger(Builder.class.getName());

    public Builder() {
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        Set<String> enumeration = this.keySet();
        Iterator<String> list = enumeration.iterator();
        while (list.hasNext()) {
            String key = list.next();
            Object value = this.get(key);

            if (value instanceof String || value instanceof StringBuffer || value instanceof StringBuilder)
                buffer.append(QUOTE + key + QUOTE).append(":").append(
                        QUOTE + StringUtilities.escape(value.toString()) + QUOTE);
            else
                buffer.append(QUOTE + key + QUOTE).append(":").append(value);

            buffer.append(",");
        }

        if (buffer.length() > 0)
            buffer.setLength(buffer.length() - 1);

        return "{" + buffer.toString() + "}";
    }

    /**
     * Parse the resource.
     * Format:{"Name":"Mover","Birthday":"1982-03-20","Ability":{"Item1":"Music"
     * ,"Item2","Java"}}
     *
     * @param resource JSON string
     * @throws ApplicationException application exception
     */
    public void parse(String resource) throws ApplicationException {
        // 默认相信任何一个被传入的都是合法的字符串
        resource = resource.trim();
        if (!resource.startsWith("{") || !resource.endsWith("}")) {
            throw new ApplicationException("Invalid data format!");
        }

        if (resource.startsWith("{")) {
            if (log) logger.info("待处理:{}" + resource);
            // 查找关闭位置
            this.closedPosition = this.seekPosition(resource);
            // 获得传入的实体对象里{key:value,key:value,...key:value},{key:value,key:value,...key:value}序列
            // 脱去传入的实体对象外壳 key:value,key:value,...key:value 序列
            String values = resource.substring(1, closedPosition - 1);
            if (log) logger.info("已脱壳:" + values);

            this.parseValue(values);
        }
    }

    private int closedPosition = 0;

    public int getClosedPosition() {
        return closedPosition;
    }

    public void setClosedPosition(int closedPosition) {
        this.closedPosition = closedPosition;
    }

    private void parseValue(String value) throws ApplicationException {
        if (log) logger.info("待分析:" + value);
        value = value.trim();
        if (value.startsWith(QUOTE)) {
            // 处理传入的实体对象外壳 "key":value,"key":"value",..."key":value 序列
            int COLON_POSITION = value.indexOf(':'), start = COLON_POSITION + 1;
            String keyName = value.substring(1, COLON_POSITION - 1);

            if (log) logger.info("取键值:" + keyName);

            String $value = value.substring(start).trim();

            if (log) logger.info("分析值:" + $value);

            Object keyValue = null;
            if ($value.startsWith(QUOTE)) {
                if (log) logger.info("提取值:" + $value);

                int $end = this.next($value, '"');
                keyValue = $value.substring(1, $end - 1).trim();

                if ($end + 1 < $value.length()) {
                    $value = $value.substring($end + ",".length());

                    this.parseValue($value);
                }
            } else if ($value.indexOf('{') == 0) {
                if (log) logger.info("遇实体:" + $value);
                int closedPosition = this.seekPosition($value);

                // 获得传入的实体对象里{key:value,key:value,...key:value},{key:value,key:value,...key:value}序列
                // 脱去传入的实体对象外壳 key:value,key:value,...key:value 序列

                String _$value = $value.substring(0, closedPosition);

                if (log) logger.info("分实体:" + _$value);
                Builder builder = new Builder();
                builder.parse(_$value);

                keyValue = builder;

                if (closedPosition < $value.length()) {
                    _$value = $value.substring(closedPosition + ",".length());
                    if (log) logger.info("分实体:" + _$value);
                    this.parseValue(_$value);
                }
            } else if ($value.indexOf('[') == 0) {
                Builders builders = new Builders();
                if (log) logger.info($value);
                builders.parse($value);

                keyValue = builders;
            } else {
                if ($value.indexOf(',') != -1) {
                    keyValue = $value.substring(0, $value.indexOf(','));
                    $value = $value.substring($value.indexOf(',') + 1);

                    this.parseValue($value);
                } else {
                    keyValue = $value;
                }
            }

            this.put(keyName, keyValue);
        }
    }

    private int seekPosition(String value) {
        char[] charray = value.toCharArray();
        int i = 0, n = 0;
        int position = charray.length;

        while (i < position) {
            char c = charray[i];
            if (c == '{') {
                if (i - 1 >= 0 && charray[i - 1] == '\\') {
                    ;
                } else
                    n++;
            } else if (c == '}') {
                if (i - 1 >= 0 && charray[i - 1] == '\\') {
                    ;
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
        char[] charray = value.toCharArray();
        int i = 0, n = 0, position = charray.length;

        while (i < position) {
            char c = charray[i];
            if (c == begin) {
                if (i - 1 >= 0 && charray[i - 1] == '\\') {
                    ;
                } else
                    n++;
            }

            i++;
            if (n == 2)
                position = i;
        }

        return position;
    }

    public Row toData() {
        Row row = new Row();
        Field field = new Field();
        Set<String> enumeration = this.keySet();
        Iterator<String> list = enumeration.iterator();
        while (list.hasNext()) {
            String key = list.next();
            Object value = this.get(key);
            FieldInfo info = new FieldInfo();
            info.set("name", key);
            info.set("value", value);
            field.append(key, info);
        }

        row.append(field);
        return row;
    }

    public void saveAsFile(File file) throws ApplicationException {
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.write(this.toString());
            writer.close();
        } catch (FileNotFoundException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    @Override
    public int size() {
        return this.keySet().size();
    }

}
