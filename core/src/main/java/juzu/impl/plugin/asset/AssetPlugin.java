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

package juzu.impl.plugin.asset;

import juzu.PropertyType;
import juzu.asset.AssetLocation;
import juzu.impl.common.Name;
import juzu.impl.common.Tools;
import juzu.impl.plugin.PluginDescriptor;
import juzu.impl.asset.AssetManager;
import juzu.impl.asset.AssetMetaData;
import juzu.impl.plugin.PluginContext;
import juzu.impl.plugin.application.ApplicationPlugin;
import juzu.impl.request.Request;
import juzu.impl.request.RequestFilter;
import juzu.impl.common.JSON;
import juzu.plugin.asset.WithAssets;
import juzu.request.Result;
import juzu.io.Chunk;
import juzu.io.Stream;
import juzu.io.StreamableDecorator;
import juzu.request.Phase;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class AssetPlugin extends ApplicationPlugin implements RequestFilter {

  /** . */
  private LinkedHashMap<String, Chunk.Property<String>> assets;

  /** . */
  private AssetDescriptor descriptor;

  /** . */
  private PluginContext context;

  /** The path to the assets dir. */
  private String assetsPath;

  /** . */
  @Inject
  AssetManager assetManager;

  public AssetPlugin() {
    super("asset");
  }

  public AssetManager getAssetManager() {
    return assetManager;
  }

  /**
   * Returns the plugin assets path.
   *
   * @return the assets path
   */
  public String getAssetsPath() {
    return assetsPath;
  }

  @Override
  public PluginDescriptor init(PluginContext context) throws Exception {
    JSON config = context.getConfig();
    String assetsPath;
    List<AssetMetaData> assets;
    if (config != null) {
      String packageName = config.getString("package");
      AssetLocation location = AssetLocation.safeValueOf(config.getString("location"));
      if (location == null) {
        location = AssetLocation.APPLICATION;
      }
      assets = load(packageName, location, config.getList("assets", JSON.class));
      assetsPath = "/" + Name.parse(application.getPackageName()).append(packageName).toString().replace('.', '/') + "/";
    } else {
      assets = Collections.emptyList();
      assetsPath = null;
    }
    this.descriptor = new AssetDescriptor(assets);
    this.context = context;
    this.assetsPath = assetsPath;
    return descriptor;
  }

  private List<AssetMetaData> load(
      String packageName,
      AssetLocation defaultLocation,
      List<? extends JSON> scripts) throws Exception {
    List<AssetMetaData> abc = Collections.emptyList();
    if (scripts != null && scripts.size() > 0) {
      abc = new ArrayList<AssetMetaData>();
      for (JSON script : scripts) {
        String id = script.getString("id");
        AssetLocation location = AssetLocation.safeValueOf(script.getString("location"));

        // We handle here location / perhaps we could handle it at compile time instead?
        if (location == null) {
          location = defaultLocation;
        }

        //
        String value = script.getString("value");
        if (!value.startsWith("/") && location == AssetLocation.APPLICATION) {
          value = "/" + application.getPackageName().replace('.', '/') + "/" + packageName.replace('.', '/') + "/" + value;
        }

        //
        AssetMetaData descriptor = new AssetMetaData(
          id,
          location,
          value,
          script.getArray("depends", String.class)
        );
        abc.add(descriptor);
      }
    }
    return abc;
  }

  @PostConstruct
  public void start() throws Exception {
    this.assets = process(descriptor.getAssets());
  }

  public URL resolve(AssetLocation location, String path) {
    switch (location) {
      case APPLICATION:
        return context.getApplicationResolver().resolve(path);
      case SERVER:
        return context.getServerResolver().resolve(path);
      default:
        return null;
    }
  }

  private LinkedHashMap<String, Chunk.Property<String>> process(List<AssetMetaData> data) throws Exception {
    LinkedHashMap<String, Chunk.Property<String>> assets = new LinkedHashMap<String, Chunk.Property<String>>();
    for (AssetMetaData script : data) {

      // Validate assets
      AssetLocation location = script.getLocation();
      URL url;
      if (location == AssetLocation.APPLICATION) {
        url = resolve(AssetLocation.APPLICATION, script.getValue());
        if (url == null) {
          throw new Exception("Could not resolve application  " + script.getValue());
        }
      } else if (location == AssetLocation.SERVER) {
        if (!script.getValue().startsWith("/")) {
          url = resolve(AssetLocation.SERVER, "/" + script.getValue());
          if (url == null) {
            throw new Exception("Could not resolve server asset " + script.getValue());
          }
        } else {
          url = null;
        }
      } else {
        url = null;
      }

      //
      String id = assetManager.addAsset(script, url);
      assets.put(id, new Chunk.Property<String>(id, PropertyType.ASSET));
    }

    //
    return assets;
  }

  private Collection<Chunk.Property<String>> foo(AnnotatedElement elt, List<Chunk.Property<String>> bar) {
    WithAssets decl = elt.getAnnotation(WithAssets.class);
    if (decl != null) {
      String[] value = decl.value();
      for (String s : value) {
        if (s.equals("*")) {
          return assets.values();
        } else {
          Chunk.Property<String> p = assets.get(s);
          if (p == null) {
            throw new UnsupportedOperationException("handle me gracefully");
          } else {
            if (bar.size() == 0) {
              bar = new ArrayList<Chunk.Property<String>>();
            }
            bar.add(p);
          }
        }
      }
    }
    if (elt instanceof Method) {
      Method methodElt = (Method)elt;
      return foo(methodElt.getDeclaringClass(), bar);
    } else if (elt instanceof Class<?>) {
      Class<?> classElt = (Class<Object>)elt;
      String pkgName;
      if (classElt.getSimpleName().equals("package-info")) {
        pkgName = Tools.parentPackageOf(Tools.parentPackageOf(classElt.getName()));
      } else {
        pkgName = Tools.parentPackageOf(classElt.getName());
      }
      while (pkgName != null) {
        Class<?> currentPackage = Tools.getPackageClass(Thread.currentThread().getContextClassLoader(), pkgName);
        if (currentPackage != null) {
          return foo(currentPackage, bar);
        } else {
          pkgName = Tools.parentPackageOf(pkgName);
        }
      }
      return bar;
    } else {
      return bar;
    }
  }

  public void invoke(Request request) {
    request.invoke();
    if (request.getPhase() == Phase.VIEW) {
      Result result = request.getResult();
      if (result instanceof Result.Status) {
        final Collection<Chunk.Property<String>> bar = foo(request.getMethod().getMethod(), Collections.<Chunk.Property<String>>emptyList());
        Result.Status status = (Result.Status)result;
        if (status.decorated && (bar.size() > 0)) {
          status = new Result.Status(status.code, true, new StreamableDecorator(status.streamable) {
            @Override
            protected void sendHeader(Stream consumer) {
              for (Chunk.Property<String> asset : bar) {
                consumer.provide(asset);
              }
            }
          });
          request.setResult(status);
        }
      }
    }
  }
}
