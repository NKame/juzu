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

package examples.tutorial.weather5;

import examples.tutorial.WeatherService;
import juzu.Action;
import juzu.Path;
import juzu.Response;
import juzu.Route;
import juzu.template.Template;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class Weather {

  static Set<String> locations = new HashSet<String>();

  static {
    locations.add("marseille");
    locations.add("paris");
  }

  @Inject
  WeatherService weatherService;

  @Inject
  @Path("index.gtmpl")
  Template index;

  @juzu.View
  public Response.Content index() {
    return index("marseille");
  }

  @juzu.View
  @Route("/show/{location}")
  public Response.Content index(String location) {
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("location", location);
    parameters.put("temperature", weatherService.getTemperature(location));
    parameters.put("locations", locations);
    return index.with(parameters).ok();
  }

  @Action
  @Route("/add")
  public Response.View add(String location) {
    locations.add(location);
    return Weather_.index(location);
  }
}
