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

package org.ratpackframework.jackson.internal;

import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import org.ratpackframework.handling.Context;
import org.ratpackframework.jackson.Json;
import org.ratpackframework.jackson.JsonRenderer;
import org.ratpackframework.render.internal.ByTypeRenderer;
import org.ratpackframework.util.internal.ByteBufWriteThroughOutputStream;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;

public class DefaultJsonRenderer extends ByTypeRenderer<Json<?>> implements JsonRenderer {

  private final ObjectWriter defaultObjectWriter;

  @Inject
  public DefaultJsonRenderer(ObjectWriter defaultObjectWriter) {
    this.defaultObjectWriter = defaultObjectWriter;
  }

  @Override
  public void render(final Context context, final Json<?> object) {
    context.respond(context.getByContent().json(new Runnable() {
      @Override
      public void run() {
        final ByteBuf body = context.getResponse().getBody();
        OutputStream outputStream = new ByteBufWriteThroughOutputStream(body);

        ObjectWriter writer = object.getObjectWriter();
        if (writer == null) {
          writer = defaultObjectWriter;
        }

        try {
          writer.writeValue(outputStream, object.getObject());
        } catch (IOException e) {
          context.error(e);
        }

        context.getResponse().send();
      }
    }));
  }

}
