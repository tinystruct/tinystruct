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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

public class Builders extends ArrayList<Object> implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(Builders.class.getName());

    public Builders() {
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();

        for (Object o : this) {
            buffer.append(o);
            buffer.append(',');
        }

        if (buffer.length() > 0)
            buffer.setLength(buffer.length() - 1);

        return "[" + buffer + "]";
    }

    public void parse(String value) throws ApplicationException {
        if (value.indexOf("{") == 0) {
            logger.info("分析实体：" + value);
            Builder builder = new Builder();
            builder.parse(value);

            this.add(builder);

            int p = builder.getClosedPosition();
            if (p < value.length()) {
                if (value.charAt(p) == ',') {
                    value = value.substring(p + 1);
                    this.parse(value);
                }
            }
        }

        if (value.indexOf('[') == 0) {
            logger.info("分析体组：" + value);
            int end = this.seekPosition(value);

            logger.info("结束位:" + end + " 长度:" + value.length());
            this.parse(value.substring(1, end - 1));

            int len = value.length();
            if (end < len - 1) {
                this.parse(value.substring(end + 1));
            }
        }
    }

    private int seekPosition(String value) {
        char[] chars = value.toCharArray();
        int i = 0, n = 0, position = chars.length;

        while (i < position) {
            char c = chars[i++];

            if (c == '[') {
                n++;
            } else if (c == ']') {
                n--;
            }

            if (n == 0)
                position = i;
        }

        return position;
    }

}

