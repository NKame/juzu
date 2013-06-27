/*
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package juzu.impl.plugin.amd;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 * @version $Id$
 * 
 */
public class AMDManager {

  /** . */
  protected final LinkedHashMap<String, Module> assets = new LinkedHashMap<String, Module>();

  /** . */
  protected final HashMap<String, URL> resources = new HashMap<String, URL>();

  public Module addAMD(AMDMetaData data, URL url) throws NullPointerException, IllegalArgumentException, IOException {
    String name = data.name;

    // Use value hashcode if no id is provided
    if (name == null) {
      name = "" + data.getPath().hashCode();
    }

    //
    Module module = assets.get(name);
    if (module == null) {
      assets.put(name, module = new Module(name, data.location, data.path));

      //
      switch (data.location) {
        case APPLICATION :
          if (data.isRequire) {
            resources.put(data.path, url);
          } else {
            String content = wrap(data, url);
            resources.put(data.path, new URL("amd", null, 0, "/", new AMDURLStreamHandler(content)));
          }
          break;
        case SERVER :
          resources.put(data.path, url);
          break;
        default :
          // Nothing to do
          break;
      }
    }

    //
    return module;
  }

  public URL resolveAsset(String path) {
    return resources.get(path);
  }

  private void joinDepenencies(StringBuilder sb, AMDMetaData data) {
    for (Iterator<AMDDependency> i = data.dependencies.values().iterator(); i.hasNext();) {
      AMDDependency dependency = i.next();
      sb.append("'").append(dependency.name).append("'");
      if (i.hasNext())
        sb.append(", ");
    }
  }

  private void joinParams(StringBuilder sb, AMDMetaData data) {
    for (Iterator<AMDDependency> i = data.dependencies.values().iterator(); i.hasNext();) {
      AMDDependency dependency = i.next();
      if (dependency.alias != null && !dependency.alias.isEmpty()) {
        sb.append(dependency.alias);
      } else {
        sb.append(dependency.name);
      }
      if (i.hasNext())
        sb.append(", ");
    }
  }

  private String wrap(AMDMetaData data, URL url) throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("\ndefine('").append(data.name).append("', [");
    joinDepenencies(sb, data);

    sb.append("], function(");
    joinParams(sb, data);

    sb.append(") {\nvar require = Wrapper.require, requirejs = Wrapper.require,define = Wrapper.define;");
    sb.append("\nWrapper.define.names=[");
    joinDepenencies(sb, data);
    sb.append("];");
    sb.append("\nWrapper.define.deps=[");
    joinParams(sb, data);
    sb.append("];");
    sb.append("\nreturn ");

    int idx = -1;
    String adapter = data.getAdapter();
    if (adapter != null && !adapter.isEmpty()) {
      idx = adapter.indexOf("@{include}");
    }

    // start of adapter
    if (idx != -1) {
      sb.append(adapter.substring(0, idx)).append("\n");
    }

    NormalizeJSReader reader = new NormalizeJSReader(new InputStreamReader(url.openStream()));
    char[] buffer = new char[512];
    while (true) {
      int i = reader.read(buffer);
      if (i == 0) {
        continue;
      }
      if (i == -1) {
        break;
      }
      sb.append(buffer, 0, i);
    }

    // end of adapter
    if (idx != -1) {
      sb.append(adapter.substring(idx + "@{include}".length(), adapter.length()));
    }

    sb.append("\n});");
    return sb.toString();
  }

  private class AMDURLStreamHandler extends URLStreamHandler {

    /** . */
    private final String data;

    public AMDURLStreamHandler(String data) {
      this.data = data;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
      return new AMDURLConnection(u, data);
    }
  }

  private class AMDURLConnection extends URLConnection {

    /** . */
    private final String data;

    protected AMDURLConnection(URL url, String data) {
      super(url);
      this.data = data;
    }

    @Override
    public void connect() throws IOException {

    }

    @Override
    public InputStream getInputStream() throws IOException {
      ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes("UTF-8"));
      return bis;
    }
  }

}
