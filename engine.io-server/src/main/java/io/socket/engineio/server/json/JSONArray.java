/*
This file is part of the json-simple library at https://github.com/fangyidong/json-simple

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

/*
 * $Id: JSONArray.java,v 1.1 2006/04/15 14:10:48 platform Exp $
 * Created on 2006-4-10
 */
package io.socket.engineio.server.json;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * A JSON array. JSONObject supports java.util.List interface.
 *
 * @author FangYidong<fangyidong @ yahoo.com.cn>
 */
public class JSONArray extends ArrayList<Object> implements List<Object>, JSONAware, JSONStreamAware {
    private static final long serialVersionUID = 3957988303675231981L;

    /**
     * Encode a list into JSON text and write it to out.
     * If this list is also a JSONStreamAware or a JSONAware, JSONStreamAware and JSONAware specific behaviours will be ignored at this top level.
     *
     * @param list
     * @param out
     * @see JSONValue#writeJSONString(Object, Writer)
     */
    public static void writeJSONString(List<Object> list, Writer out) throws IOException {
        if (list == null) {
            out.write("null");
            return;
        }

        boolean first = true;
        Iterator<Object> iter = list.iterator();

        out.write('[');
        while (iter.hasNext()) {
            if (first)
                first = false;
            else
                out.write(',');

            Object value = iter.next();
            if (value == null) {
                out.write("null");
                continue;
            }

            JSONValue.writeJSONString(value, out);
        }
        out.write(']');
    }

    public void writeJSONString(Writer out) throws IOException {
        writeJSONString(this, out);
    }

    /**
     * Convert a list to JSON text. The result is a JSON array.
     * If this list is also a JSONAware, JSONAware specific behaviours will be omitted at this top level.
     *
     * @param list
     * @return JSON text, or "null" if list is null.
     * @see JSONValue#toJSONString(Object)
     */
    public static String toJSONString(Object list) {
        if (list == null) {
            return "null";
        }

        if (list instanceof boolean[]) {
            return Arrays.toString((boolean[]) list);
        } else if (list instanceof byte[]) {
            return Arrays.toString((byte[]) list);
        } else if (list instanceof short[]) {
            return Arrays.toString((short[]) list);
        } else if (list instanceof int[]) {
            return Arrays.toString((int[]) list);
        } else if (list instanceof long[]) {
            return Arrays.toString((long[]) list);
        } else if (list instanceof float[]) {
            return Arrays.toString((float[]) list);
        } else if (list instanceof double[]) {
            return Arrays.toString((double[]) list);
        }

        if (!(list instanceof List)) {
            throw new IllegalArgumentException("Unsupported argument type: " + list.getClass().getSimpleName());
        }

        boolean first = true;
        StringBuilder sb = new StringBuilder();
        Iterator<Object> iter = ((List) list).iterator();

        sb.append('[');
        while (iter.hasNext()) {
            if (first)
                first = false;
            else
                sb.append(',');

            Object value = iter.next();
            if (value == null) {
                sb.append("null");
                continue;
            }
            sb.append(JSONValue.toJSONString(value));
        }
        sb.append(']');
        return sb.toString();
    }

    public String toJSONString() {
        return toJSONString(this);
    }

    public String toString() {
        return toJSONString();
    }
}
