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

package juzu.impl.plugin.template.metamodel;

import juzu.impl.compiler.ElementHandle;
import juzu.impl.compiler.MessageCode;
import juzu.impl.metamodel.Key;
import juzu.impl.metamodel.MetaModelEvent;
import juzu.impl.metamodel.MetaModelObject;
import juzu.impl.common.JSON;
import juzu.impl.common.Path;
import juzu.impl.template.spi.Template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A template.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class TemplateMetaModel extends TemplateRefMetaModel {

  /** . */
  public static final MessageCode CANNOT_WRITE_TEMPLATE_STUB = new MessageCode("CANNOT_WRITE_TEMPLATE_STUB", "The template stub %1$s cannot be written");

  /** . */
  public static final MessageCode CANNOT_WRITE_TEMPLATE_CLASS = new MessageCode("CANNOT_WRITE_TEMPLATE_CLASS", "The template class %1$s cannot be written");

  /** . */
  public static final MessageCode CANNOT_WRITE_APPLICATION = new MessageCode("CANNOT_WRITE_APPLICATION", "The application %1$s cannot be written");

  /** . */
  public static final MessageCode TEMPLATE_NOT_RESOLVED = new MessageCode("TEMPLATE_NOT_RESOLVED", "The template %1$s cannot be resolved");

  /** . */
  public static final MessageCode TEMPLATE_SYNTAX_ERROR = new MessageCode("TEMPLATE_SYNTAX_ERROR", "Template syntax error");

  /** . */
  public static final MessageCode TEMPLATE_VALIDATION_ERROR = new MessageCode("TEMPLATE_VALIDATION_ERROR", "Template validation error");

  /** . */
  public static final MessageCode TEMPLATE_ILLEGAL_PATH = new MessageCode("TEMPLATE_ILLEGAL_PATH", "The reference to the template %1$s is malformed");

  /** . */
  public static final MessageCode CANNOT_WRITE_TEMPLATE_SCRIPT = new MessageCode("CANNOT_WRITE_TEMPLATE_SCRIPT", "The template script %1$s cannot be written");

  /** . */
  public static final MessageCode TEMPLATE_CYCLE = new MessageCode("TEMPLATE_CYCLE", "A template cycle was detected");

  /** . */
  public static final MessageCode CONTROLLER_NOT_RESOLVED = new MessageCode("CONTROLLER_NOT_RESOLVED",
      "Controller %1$s not found in template %2$s at (%3$s,%4$s)");

  /** . */
  public final static Key<TemplateMetaModel> KEY = Key.of(TemplateMetaModel.class);

  /** The related application. */
  TemplatesMetaModel templates;

  /** . */
  final Path.Relative path;

  /** . */
  int refCount;

  /** The related template. */
  Template<?> template;

  public TemplateMetaModel(Path.Relative path) {
    this.path = path;
    this.template = null;
    this.refCount = 0;
  }

  public TemplatesMetaModel getTemplates() {
    return templates;
  }

  /**
   * Compute the element references to this template.
   *
   * @return the ancestors
   */
  public Collection<ElementTemplateRefMetaModel> getElementReferences() {
    Collection<TemplateRefMetaModel> refs = getReferences();
    for (Iterator<TemplateRefMetaModel> i = refs.iterator();i.hasNext();) {
      if (i.next() instanceof TemplateMetaModel) {
        i.remove();
      }
    }
    // Safe cast as the collection now only contains instances of ElementTemplateRefMetaModel
    return (Collection)refs;
  }

  /**
   * Compute the references to this template.
   *
   * @return the ancestors
   */
  public Collection<TemplateRefMetaModel> getReferences() {
    HashSet<TemplateRefMetaModel> refs = new HashSet<TemplateRefMetaModel>();
    buildReferences(refs);
    return refs;
  }

  private void buildReferences(HashSet<TemplateRefMetaModel> refs) {
    for (MetaModelObject parent : getParents()) {
      if (parent instanceof TemplateRefMetaModel) {
        TemplateRefMetaModel parentRef = (TemplateRefMetaModel)parent;
        if (!refs.contains(parentRef)) {
          refs.add(parentRef);
          if (parentRef instanceof TemplateMetaModel) {
            TemplateMetaModel parentTemplate = (TemplateMetaModel)parentRef;
            parentTemplate.buildReferences(refs);
          }
        }
      }
    }
  }

  public Path.Relative getPath() {
    return path;
  }

  public JSON toJSON() {
    JSON json = new JSON();
    json.set("path", path.getCanonical());
    return json;
  }

  @Override
  protected void postAttach(MetaModelObject parent) {
    if (parent instanceof TemplatesMetaModel) {
      queue(MetaModelEvent.createAdded(this));
      this.templates = (TemplatesMetaModel)parent;
    }
    else if (parent instanceof TemplateRefMetaModel) {
      refCount++;
    }
  }

  @Override
  protected void preDetach(MetaModelObject parent) {
    if (parent instanceof TemplatesMetaModel) {
      ElementHandle.Package handle = templates.application.getHandle();
      this.templates = null;
      this.template = null;
      queue(MetaModelEvent.createRemoved(this, handle));
    }
    else if (parent instanceof TemplateRefMetaModel) {
      refCount--;
    }
  }
}
