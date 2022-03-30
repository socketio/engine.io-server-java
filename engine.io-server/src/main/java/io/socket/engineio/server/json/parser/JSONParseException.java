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

package io.socket.engineio.server.json.parser;

/**
 * ParseException explains why and where the error occurs in source JSON text.
 *
 * @author FangYidong<fangyidong @ yahoo.com.cn>
 */
public class JSONParseException extends Exception {
    private static final long serialVersionUID = -7880698968187728548L;

    public static final int ERROR_UNEXPECTED_CHAR = 0;
    public static final int ERROR_UNEXPECTED_TOKEN = 1;
    public static final int ERROR_UNEXPECTED_EXCEPTION = 2;

    private int errorType;
    private Object unexpectedObject;
    private int position;

    public JSONParseException(int errorType) {
        this(-1, errorType, null);
    }

    public JSONParseException(int errorType, Object unexpectedObject) {
        this(-1, errorType, unexpectedObject);
    }

    public JSONParseException(int position, int errorType, Object unexpectedObject) {
        this.position = position;
        this.errorType = errorType;
        this.unexpectedObject = unexpectedObject;
    }

    public int getErrorType() {
        return errorType;
    }

    public void setErrorType(int errorType) {
        this.errorType = errorType;
    }

    /**
     * @return The character position (starting with 0) of the input where the error occurs.
     * @see io.socket.engineio.server.json.parser.JSONParser#getPosition()
     */
    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * @return One of the following base on the value of errorType:
     * ERROR_UNEXPECTED_CHAR		java.lang.Character
     * ERROR_UNEXPECTED_TOKEN		io.socket.engineio.server.json.parser.Yytoken
     * ERROR_UNEXPECTED_EXCEPTION	java.lang.Exception
     * @see io.socket.engineio.server.json.parser.Yytoken
     */
    public Object getUnexpectedObject() {
        return unexpectedObject;
    }

    public void setUnexpectedObject(Object unexpectedObject) {
        this.unexpectedObject = unexpectedObject;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        switch (errorType) {
            case ERROR_UNEXPECTED_CHAR:
                sb.append("Unexpected character (").append(unexpectedObject).append(") at position ").append(position).append(".");
                break;
            case ERROR_UNEXPECTED_TOKEN:
                sb.append("Unexpected token ").append(unexpectedObject).append(" at position ").append(position).append(".");
                break;
            case ERROR_UNEXPECTED_EXCEPTION:
                sb.append("Unexpected exception at position ").append(position).append(": ").append(unexpectedObject);
                break;
            default:
                sb.append("Unkown error at position ").append(position).append(".");
                break;
        }
        return sb.toString();
    }
}
