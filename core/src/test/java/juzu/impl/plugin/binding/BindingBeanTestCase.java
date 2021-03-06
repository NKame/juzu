/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.impl.plugin.binding;

import juzu.impl.compiler.CompilationError;
import juzu.impl.inject.spi.InjectorProvider;
import juzu.test.AbstractInjectTestCase;
import juzu.test.CompilerAssert;
import juzu.test.protocol.mock.MockApplication;
import juzu.test.protocol.mock.MockClient;
import juzu.test.protocol.mock.MockRenderBridge;
import org.junit.Test;

import java.io.File;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class BindingBeanTestCase extends AbstractInjectTestCase {

  public BindingBeanTestCase(InjectorProvider di) {
    super(di);
  }

  @Test
  public void testCreate() throws Exception {
    MockApplication<?> app = application("plugin.binding.create").init();

    //
    MockClient client = app.client();
    MockRenderBridge render = client.render();
    assertEquals("pass", render.assertStringResult());
  }

  @Test
  public void testScope() throws Exception {
    MockApplication<?> app = application("plugin.binding.scope").init();

    //
    MockClient client = app.client();
    MockRenderBridge render = client.render();
    String url = render.assertStringResult();
    assertNotSame("", url);

    //
    render = (MockRenderBridge)client.invoke(url);
    String result = render.assertStringResult();
    assertEquals("pass", result);
  }
}
