/*
 * Copyright 2013 the original author or authors.
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
 */

package org.ratpackframework.http.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import org.ratpackframework.http.MediaType;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class DefaultMediaType implements MediaType {

  public static final String DEFAULT_CHARSET = "ISO-8859-1";
  public static final String UTF8 = "UTF-8";
  public static final String CHARSET_KEY = "charset";

  private final String type;
  protected final Map<String, String> params;
  private final String string;

  private static final int CACHE_SIZE = 200;

  private static final Cache<String, MediaType> ISO_CACHE = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();
  private static final Cache<String, MediaType> UTF8_CACHE = CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).build();

  public static MediaType get(final String contentType) {
    return fromCache(ISO_CACHE, contentType, DEFAULT_CHARSET);
  }

  public static MediaType utf8(final String contentType) {
    return fromCache(UTF8_CACHE, contentType, UTF8);
  }

  private static MediaType fromCache(final Cache<String, MediaType> cache, String contentType, final String defaultCharset) {
    if (contentType == null) {
      contentType = "";
    } else {
      contentType = contentType.trim();
    }

    final String finalContentType = contentType;
    try {
      return cache.get(contentType, new Callable<MediaType>() {
        public MediaType call() throws Exception {
          return new DefaultMediaType(finalContentType, defaultCharset);
        }
      });
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private DefaultMediaType(String value, String defaultCharset) {
    ImmutableMap.Builder<String, String> paramsBuilder = ImmutableMap.builder();
    boolean setCharset = false;

    if (value == null) {
      type = null;
    } else {
      value = value.trim();
      if (value.length() == 0) {
        type = null;
      } else {
        String[] parts = value.split(";");
        type = parts[0].toLowerCase();
        if (parts.length > 1) {
          for (int i = 1; i < parts.length; ++i) {
            String part = parts[i].trim();
            String keyPart;
            String valuePart;
            if (part.contains("=")) {
              String[] valueSplit = part.split("=", 2);
              keyPart = valueSplit[0].toLowerCase();
              valuePart = valueSplit[1];

              if (keyPart.equals(CHARSET_KEY)) {
                setCharset = true;
              }
            } else {
              keyPart = part.toLowerCase();
              valuePart = "";
            }
            paramsBuilder.put(keyPart, valuePart);
          }
        }
      }
    }

    if (!setCharset && isText() && !defaultCharset.equals(DEFAULT_CHARSET)) {
      paramsBuilder.put(CHARSET_KEY, defaultCharset);
    }

    params = paramsBuilder.build();
    string = generateString();
  }

  public String getType() {
    return type;
  }

  public Map<String, String> getParams() {
    return Collections.unmodifiableMap(params);
  }

  public String getCharset() {
    return params.containsKey(CHARSET_KEY) ? params.get(CHARSET_KEY) : DEFAULT_CHARSET;
  }

  public boolean isText() {
    return !isEmpty() && getType().startsWith("text/");
  }

  public boolean isJson() {
    return !isEmpty() && getType().equals(APPLICATION_JSON);
  }

  public boolean isForm() {
    return !isEmpty() && getType().equals(APPLICATION_FORM);
  }

  public boolean isEmpty() {
    return getType() == null;
  }

  @Override
  public String toString() {
    return string;
  }

  private String generateString() {
    if (isEmpty()) {
      return "";
    } else {
      StringBuilder s = new StringBuilder(getType());
      for (Map.Entry<String, String> param : getParams().entrySet()) {
        s.append(";").append(param.getKey());
        if (!param.getValue().isEmpty()) {
          s.append("=").append(param.getValue());
        }
      }
      return s.toString();
    }
  }
}
