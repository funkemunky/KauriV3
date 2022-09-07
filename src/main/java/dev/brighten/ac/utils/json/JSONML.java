package dev.brighten.ac.utils.json;

/*
Copyright (c) 2008 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import dev.brighten.ac.utils.json.JSONArray;
import dev.brighten.ac.utils.json.JSONException;
import dev.brighten.ac.utils.json.JSONObject;
import dev.brighten.ac.utils.json.XML;
import dev.brighten.ac.utils.json.XMLTokener;

import java.util.Iterator;


/**
 * This provides static methods to convert an XML text into a JSONArray or
 * JSONObject, and to covert a JSONArray or JSONObject into an XML text using
 * the JsonML transform.
 *
 * @author JSON.org
 * @version 2010-12-23
 */
public class JSONML {

    /**
     * Parse XML values and store them in a JSONArray.
     *
     * @param x         The XMLTokener containing the source string.
     * @param arrayForm true if array form, false if object form.
     * @param ja        The JSONArray that is containing the current tag or null
     *                  if we are at the outermost level.
     * @return A JSONArray if the value is the outermost tag, otherwise null.
     * @throws dev.brighten.ac.utils.json.JSONException
     */
    private static Object parse(dev.brighten.ac.utils.json.XMLTokener x, boolean arrayForm,
                                dev.brighten.ac.utils.json.JSONArray ja) throws dev.brighten.ac.utils.json.JSONException {
        String attribute;
        char c;
        String closeTag = null;
        int i;
        dev.brighten.ac.utils.json.JSONArray newja = null;
        dev.brighten.ac.utils.json.JSONObject newjo = null;
        Object token;
        String tagName = null;

// Test for and skip past these forms:
//      <!-- ... -->
//      <![  ... ]]>
//      <!   ...   >
//      <?   ...  ?>

        while (true) {
            token = x.nextContent();
            if (token == dev.brighten.ac.utils.json.XML.LT) {
                token = x.nextToken();
                if (token instanceof Character) {
                    if (token == dev.brighten.ac.utils.json.XML.SLASH) {

// Close tag </

                        token = x.nextToken();
                        if (!(token instanceof String)) {
                            throw new dev.brighten.ac.utils.json.JSONException(
                                    "Expected a closing name instead of '" +
                                            token + "'.");
                        }
                        if (x.nextToken() != dev.brighten.ac.utils.json.XML.GT) {
                            throw x.syntaxError("Misshaped close tag");
                        }
                        return token;
                    } else if (token == dev.brighten.ac.utils.json.XML.BANG) {

// <!

                        c = x.next();
                        if (c == '-') {
                            if (x.next() == '-') {
                                x.skipPast("-->");
                            }
                            x.back();
                        } else if (c == '[') {
                            token = x.nextToken();
                            if (token.equals("CDATA") && x.next() == '[') {
                                if (ja != null) {
                                    ja.put(x.nextCDATA());
                                }
                            } else {
                                throw x.syntaxError("Expected 'CDATA['");
                            }
                        } else {
                            i = 1;
                            do {
                                token = x.nextMeta();
                                if (token == null) {
                                    throw x.syntaxError("Missing '>' after '<!'.");
                                } else if (token == dev.brighten.ac.utils.json.XML.LT) {
                                    i += 1;
                                } else if (token == dev.brighten.ac.utils.json.XML.GT) {
                                    i -= 1;
                                }
                            } while (i > 0);
                        }
                    } else if (token == dev.brighten.ac.utils.json.XML.QUEST) {

// <?

                        x.skipPast("?>");
                    } else {
                        throw x.syntaxError("Misshaped tag");
                    }

// Open tag <

                } else {
                    if (!(token instanceof String)) {
                        throw x.syntaxError("Bad tagName '" + token + "'.");
                    }
                    tagName = (String) token;
                    newja = new dev.brighten.ac.utils.json.JSONArray();
                    newjo = new dev.brighten.ac.utils.json.JSONObject();
                    if (arrayForm) {
                        newja.put(tagName);
                        if (ja != null) {
                            ja.put(newja);
                        }
                    } else {
                        newjo.put("tagName", tagName);
                        if (ja != null) {
                            ja.put(newjo);
                        }
                    }
                    token = null;
                    for (; ; ) {
                        if (token == null) {
                            token = x.nextToken();
                        }
                        if (token == null) {
                            throw x.syntaxError("Misshaped tag");
                        }
                        if (!(token instanceof String)) {
                            break;
                        }

// attribute = value

                        attribute = (String) token;
                        if (!arrayForm && (attribute == "tagName" || attribute == "childNode")) {
                            throw x.syntaxError("Reserved attribute.");
                        }
                        token = x.nextToken();
                        if (token == dev.brighten.ac.utils.json.XML.EQ) {
                            token = x.nextToken();
                            if (!(token instanceof String)) {
                                throw x.syntaxError("Missing value");
                            }
                            newjo.accumulate(attribute, dev.brighten.ac.utils.json.XML.stringToValue((String) token));
                            token = null;
                        } else {
                            newjo.accumulate(attribute, "");
                        }
                    }
                    if (arrayForm && newjo.length() > 0) {
                        newja.put(newjo);
                    }

// Empty tag <.../>

                    if (token == dev.brighten.ac.utils.json.XML.SLASH) {
                        if (x.nextToken() != dev.brighten.ac.utils.json.XML.GT) {
                            throw x.syntaxError("Misshaped tag");
                        }
                        if (ja == null) {
                            if (arrayForm) {
                                return newja;
                            } else {
                                return newjo;
                            }
                        }

// Content, between <...> and </...>

                    } else {
                        if (token != dev.brighten.ac.utils.json.XML.GT) {
                            throw x.syntaxError("Misshaped tag");
                        }
                        closeTag = (String) parse(x, arrayForm, newja);
                        if (closeTag != null) {
                            if (!closeTag.equals(tagName)) {
                                throw x.syntaxError("Mismatched '" + tagName +
                                        "' and '" + closeTag + "'");
                            }
                            tagName = null;
                            if (!arrayForm && newja.length() > 0) {
                                newjo.put("childNodes", newja);
                            }
                            if (ja == null) {
                                if (arrayForm) {
                                    return newja;
                                } else {
                                    return newjo;
                                }
                            }
                        }
                    }
                }
            } else {
                if (ja != null) {
                    ja.put(token instanceof String ?
                            dev.brighten.ac.utils.json.XML.stringToValue((String) token) : token);
                }
            }
        }
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONArray using the JsonML transform. Each XML tag is represented as
     * a JSONArray in which the first element is the tag name. If the tag has
     * attributes, then the second element will be JSONObject containing the
     * name/value pairs. If the tag contains children, then strings and
     * JSONArrays will represent the child tags.
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     *
     * @param string The source string.
     * @return A JSONArray containing the structured data from the XML string.
     * @throws dev.brighten.ac.utils.json.JSONException
     */
    public static dev.brighten.ac.utils.json.JSONArray toJSONArray(String string) throws dev.brighten.ac.utils.json.JSONException {
        return toJSONArray(new dev.brighten.ac.utils.json.XMLTokener(string));
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONArray using the JsonML transform. Each XML tag is represented as
     * a JSONArray in which the first element is the tag name. If the tag has
     * attributes, then the second element will be JSONObject containing the
     * name/value pairs. If the tag contains children, then strings and
     * JSONArrays will represent the child content and tags.
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     *
     * @param x An XMLTokener.
     * @return A JSONArray containing the structured data from the XML string.
     * @throws dev.brighten.ac.utils.json.JSONException
     */
    public static dev.brighten.ac.utils.json.JSONArray toJSONArray(dev.brighten.ac.utils.json.XMLTokener x) throws dev.brighten.ac.utils.json.JSONException {
        return (dev.brighten.ac.utils.json.JSONArray) parse(x, true, null);
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONObject using the JsonML transform. Each XML tag is represented as
     * a JSONObject with a "tagName" property. If the tag has attributes, then
     * the attributes will be in the JSONObject as properties. If the tag
     * contains children, the object will have a "childNodes" property which
     * will be an array of strings and JsonML JSONObjects.
     * <p>
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     *
     * @param x An XMLTokener of the XML source text.
     * @return A JSONObject containing the structured data from the XML string.
     * @throws dev.brighten.ac.utils.json.JSONException
     */
    public static dev.brighten.ac.utils.json.JSONObject toJSONObject(dev.brighten.ac.utils.json.XMLTokener x) throws dev.brighten.ac.utils.json.JSONException {
        return (dev.brighten.ac.utils.json.JSONObject) parse(x, false, null);
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONObject using the JsonML transform. Each XML tag is represented as
     * a JSONObject with a "tagName" property. If the tag has attributes, then
     * the attributes will be in the JSONObject as properties. If the tag
     * contains children, the object will have a "childNodes" property which
     * will be an array of strings and JsonML JSONObjects.
     * <p>
     * Comments, prologs, DTDs, and <code>&lt;[ [ ]]></code> are ignored.
     *
     * @param string The XML source text.
     * @return A JSONObject containing the structured data from the XML string.
     * @throws dev.brighten.ac.utils.json.JSONException
     */
    public static dev.brighten.ac.utils.json.JSONObject toJSONObject(String string) throws dev.brighten.ac.utils.json.JSONException {
        return toJSONObject(new XMLTokener(string));
    }


    /**
     * Reverse the JSONML transformation, making an XML text from a JSONArray.
     *
     * @param ja A JSONArray.
     * @return An XML string.
     * @throws dev.brighten.ac.utils.json.JSONException
     */
    public static String toString(dev.brighten.ac.utils.json.JSONArray ja) throws dev.brighten.ac.utils.json.JSONException {
        int i;
        dev.brighten.ac.utils.json.JSONObject jo;
        String key;
        Iterator keys;
        int length;
        Object object;
        StringBuffer sb = new StringBuffer();
        String tagName;
        String value;

// Emit <tagName	    

        tagName = ja.getString(0);
        dev.brighten.ac.utils.json.XML.noSpace(tagName);
        tagName = dev.brighten.ac.utils.json.XML.escape(tagName);
        sb.append('<');
        sb.append(tagName);

        object = ja.opt(1);
        if (object instanceof dev.brighten.ac.utils.json.JSONObject) {
            i = 2;
            jo = (dev.brighten.ac.utils.json.JSONObject) object;

// Emit the attributes

            keys = jo.keys();
            while (keys.hasNext()) {
                key = keys.next().toString();
                dev.brighten.ac.utils.json.XML.noSpace(key);
                value = jo.optString(key);
                if (value != null) {
                    sb.append(' ');
                    sb.append(dev.brighten.ac.utils.json.XML.escape(key));
                    sb.append('=');
                    sb.append('"');
                    sb.append(dev.brighten.ac.utils.json.XML.escape(value));
                    sb.append('"');
                }
            }
        } else {
            i = 1;
        }

//Emit content in body

        length = ja.length();
        if (i >= length) {
            sb.append('/');
            sb.append('>');
        } else {
            sb.append('>');
            do {
                object = ja.get(i);
                i += 1;
                if (object != null) {
                    if (object instanceof String) {
                        sb.append(dev.brighten.ac.utils.json.XML.escape(object.toString()));
                    } else if (object instanceof dev.brighten.ac.utils.json.JSONObject) {
                        sb.append(toString((dev.brighten.ac.utils.json.JSONObject) object));
                    } else if (object instanceof dev.brighten.ac.utils.json.JSONArray) {
                        sb.append(toString((dev.brighten.ac.utils.json.JSONArray) object));
                    }
                }
            } while (i < length);
            sb.append('<');
            sb.append('/');
            sb.append(tagName);
            sb.append('>');
        }
        return sb.toString();
    }

    /**
     * Reverse the JSONML transformation, making an XML text from a JSONObject.
     * The JSONObject must contain a "tagName" property. If it has children,
     * then it must have a "childNodes" property containing an array of objects.
     * The other properties are attributes with string values.
     *
     * @param jo A JSONObject.
     * @return An XML string.
     * @throws dev.brighten.ac.utils.json.JSONException
     */
    public static String toString(dev.brighten.ac.utils.json.JSONObject jo) throws JSONException {
        StringBuffer sb = new StringBuffer();
        int i;
        dev.brighten.ac.utils.json.JSONArray ja;
        String key;
        Iterator keys;
        int length;
        Object object;
        String tagName;
        String value;

//Emit <tagName

        tagName = jo.optString("tagName");
        if (tagName == null) {
            return dev.brighten.ac.utils.json.XML.escape(jo.toString());
        }
        dev.brighten.ac.utils.json.XML.noSpace(tagName);
        tagName = dev.brighten.ac.utils.json.XML.escape(tagName);
        sb.append('<');
        sb.append(tagName);

//Emit the attributes

        keys = jo.keys();
        while (keys.hasNext()) {
            key = keys.next().toString();
            if (!key.equals("tagName") && !key.equals("childNodes")) {
                dev.brighten.ac.utils.json.XML.noSpace(key);
                value = jo.optString(key);
                if (value != null) {
                    sb.append(' ');
                    sb.append(dev.brighten.ac.utils.json.XML.escape(key));
                    sb.append('=');
                    sb.append('"');
                    sb.append(dev.brighten.ac.utils.json.XML.escape(value));
                    sb.append('"');
                }
            }
        }

//Emit content in body

        ja = jo.optJSONArray("childNodes");
        if (ja == null) {
            sb.append('/');
            sb.append('>');
        } else {
            sb.append('>');
            length = ja.length();
            for (i = 0; i < length; i += 1) {
                object = ja.get(i);
                if (object != null) {
                    if (object instanceof String) {
                        sb.append(XML.escape(object.toString()));
                    } else if (object instanceof dev.brighten.ac.utils.json.JSONObject) {
                        sb.append(toString((JSONObject) object));
                    } else if (object instanceof dev.brighten.ac.utils.json.JSONArray) {
                        sb.append(toString((JSONArray) object));
                    }
                }
            }
            sb.append('<');
            sb.append('/');
            sb.append(tagName);
            sb.append('>');
        }
        return sb.toString();
    }
}