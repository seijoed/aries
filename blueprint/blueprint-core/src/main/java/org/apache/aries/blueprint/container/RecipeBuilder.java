/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.aries.blueprint.ComponentDefinitionRegistry;
import org.apache.aries.blueprint.ExtendedBeanMetadata;
import org.apache.aries.blueprint.ExtendedBlueprintContainer;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.di.ArrayRecipe;
import org.apache.aries.blueprint.di.CollectionRecipe;
import org.apache.aries.blueprint.di.ComponentFactoryRecipe;
import org.apache.aries.blueprint.di.DependentComponentFactoryRecipe;
import org.apache.aries.blueprint.di.IdRefRecipe;
import org.apache.aries.blueprint.di.MapRecipe;
import org.apache.aries.blueprint.di.PassThroughRecipe;
import org.apache.aries.blueprint.di.Recipe;
import org.apache.aries.blueprint.di.RefRecipe;
import org.apache.aries.blueprint.di.ValueRecipe;
import org.apache.aries.blueprint.ext.ComponentFactoryMetadata;
import org.apache.aries.blueprint.ext.DependentComponentFactoryMetadata;
import org.apache.aries.blueprint.mutable.MutableMapMetadata;
import org.apache.aries.blueprint.reflect.MetadataUtil;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * TODO: javadoc
 *
 * @version $Rev$, $Date$
 */
public class RecipeBuilder {

    private final Set<String> names = new HashSet<String>();
    private final ExtendedBlueprintContainer blueprintContainer;
    private final ComponentDefinitionRegistry registry;
    private final IdSpace recipeIdSpace;

    public RecipeBuilder(ExtendedBlueprintContainer blueprintContainer, IdSpace recipeIdSpace) {
        this.recipeIdSpace = recipeIdSpace;
        this.blueprintContainer = blueprintContainer;
        this.registry = blueprintContainer.getComponentDefinitionRegistry();
    }
    
    public BlueprintRepository createRepository() {
        BlueprintRepository repository = new BlueprintRepository(blueprintContainer);
        // Create component recipes
        for (String name : registry.getComponentDefinitionNames()) {
            ComponentMetadata component = registry.getComponentDefinition(name);
            Recipe recipe = createRecipe(component);
            repository.putRecipe(recipe.getName(), recipe);
        }
        repository.validate();
        return repository;
    }

    public Recipe createRecipe(ComponentMetadata component) {

        // Custom components should be handled before built-in ones
        // in case we have a custom component that also implements a built-in metadata
        
        if (component instanceof DependentComponentFactoryMetadata) { 
            return createDependentComponentFactoryMetadata((DependentComponentFactoryMetadata) component);
        } else if (component instanceof ComponentFactoryMetadata) {
            return createComponentFactoryMetadata((ComponentFactoryMetadata) component);
        } else if (component instanceof BeanMetadata) {
            return createBeanRecipe((BeanMetadata) component);
        } else if (component instanceof ServiceMetadata) {
            return createServiceRecipe((ServiceMetadata) component);
        } else if (component instanceof ReferenceMetadata) {
            return createReferenceRecipe((ReferenceMetadata) component);
        } else if (component instanceof ReferenceListMetadata) {
            return createReferenceListRecipe((ReferenceListMetadata) component);
        } else if (component instanceof PassThroughMetadata) {
            return createPassThroughRecipe((PassThroughMetadata) component);
        } else {
            throw new IllegalStateException("Unsupported component type " + component.getClass());
        }
    }

    private Recipe createComponentFactoryMetadata(ComponentFactoryMetadata metadata) {
        return new ComponentFactoryRecipe<ComponentFactoryMetadata>(
                metadata.getId(), metadata, blueprintContainer, getDependencies(metadata));
    }
    
    private Recipe createDependentComponentFactoryMetadata(DependentComponentFactoryMetadata metadata) {
        return new DependentComponentFactoryRecipe(
                metadata.getId(), metadata, blueprintContainer, getDependencies(metadata));
    }
    
    private List<Recipe> getDependencies(ComponentMetadata metadata) {
        List<Recipe> deps = new ArrayList<Recipe>();
        for (String name : metadata.getDependsOn()) {
            deps.add(new RefRecipe(getName(null), name));
        }
        return deps;
    }

    private Recipe createPassThroughRecipe(PassThroughMetadata passThroughMetadata) {
        return new PassThroughRecipe(getName(passThroughMetadata.getId()), 
            passThroughMetadata.getObject());
    }

    private Recipe createReferenceListRecipe(ReferenceListMetadata metadata) {
        CollectionRecipe listenersRecipe = null;
        if (metadata.getReferenceListeners() != null) {
            listenersRecipe = new CollectionRecipe(getName(null), ArrayList.class);
            for (ReferenceListener listener : metadata.getReferenceListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        ReferenceListRecipe recipe = new ReferenceListRecipe(getName(metadata.getId()),
                                                 blueprintContainer,
                                                 metadata,
                                                 listenersRecipe,
                                                 getDependencies(metadata));
        return recipe;
    }

    private ReferenceRecipe createReferenceRecipe(ReferenceMetadata metadata) {
        CollectionRecipe listenersRecipe = null;
        if (metadata.getReferenceListeners() != null) {
            listenersRecipe = new CollectionRecipe(getName(null), ArrayList.class);
            for (ReferenceListener listener : metadata.getReferenceListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        ReferenceRecipe recipe = new ReferenceRecipe(getName(metadata.getId()),
                                                     blueprintContainer,
                                                     metadata,
                                                     listenersRecipe,
                                                     getDependencies(metadata));
        return recipe;
    }

    private Recipe createServiceRecipe(ServiceMetadata serviceExport) {
        CollectionRecipe listenersRecipe = new CollectionRecipe(getName(null), ArrayList.class);
        if (serviceExport.getRegistrationListeners() != null) {
            for (RegistrationListener listener : serviceExport.getRegistrationListeners()) {
                listenersRecipe.add(createRecipe(listener));
            }
        }
        ServiceRecipe recipe = new ServiceRecipe(getName(serviceExport.getId()),
                                                 blueprintContainer,
                                                 serviceExport,
                                                 getValue(serviceExport.getServiceComponent(), null),
                                                 listenersRecipe,
                                                 getServicePropertiesRecipe(serviceExport),
                                                 getDependencies(serviceExport));
        return recipe;
    }

    protected MapRecipe getServicePropertiesRecipe(ServiceMetadata metadata) {
        List<MapEntry> properties = metadata.getServiceProperties();
        if (properties != null) {
            MutableMapMetadata map = MetadataUtil.createMetadata(MutableMapMetadata.class);
            for (MapEntry e : properties) {
                map.addEntry(e);
            }
            return createMapRecipe(map);
        } else {
            return null;
        }
    }
    
    private Object getBeanClass(BeanMetadata beanMetadata) {
        if (beanMetadata instanceof ExtendedBeanMetadata) {
            ExtendedBeanMetadata extBeanMetadata = (ExtendedBeanMetadata) beanMetadata;
            if (extBeanMetadata.getRuntimeClass() != null) {
                return extBeanMetadata.getRuntimeClass();
            }
        }
        return beanMetadata.getClassName();        
    }
    
    private boolean allowsFieldInjection(BeanMetadata beanMetadata) {
        if (beanMetadata instanceof ExtendedBeanMetadata) {
            return ((ExtendedBeanMetadata) beanMetadata).getFieldInjection();
        }
        return false;
    }
    
    private BeanRecipe createBeanRecipe(BeanMetadata beanMetadata) {
        BeanRecipe recipe = new BeanRecipe(
                getName(beanMetadata.getId()),
                blueprintContainer,
                getBeanClass(beanMetadata),
                allowsFieldInjection(beanMetadata));
        // Create refs for explicit dependencies
        recipe.setExplicitDependencies(getDependencies(beanMetadata));
        recipe.setPrototype(MetadataUtil.isPrototypeScope(beanMetadata));
        recipe.setInitMethod(beanMetadata.getInitMethod());
        recipe.setDestroyMethod(beanMetadata.getDestroyMethod());
        recipe.setInterceptorLookupKey(beanMetadata);
        List<BeanArgument> beanArguments = beanMetadata.getArguments();
        if (beanArguments != null && !beanArguments.isEmpty()) {
            boolean hasIndex = (beanArguments.get(0).getIndex() >= 0);
            if (hasIndex) {
                List<BeanArgument> beanArgumentsCopy = new ArrayList<BeanArgument>(beanArguments);
                Collections.sort(beanArgumentsCopy, MetadataUtil.BEAN_COMPARATOR);
                beanArguments = beanArgumentsCopy;
            }
            List<Object> arguments = new ArrayList<Object>();
            List<String> argTypes = new ArrayList<String>();
            for (BeanArgument argument : beanArguments) {
                Recipe value = getValue(argument.getValue(), null);
                arguments.add(value);
                argTypes.add(argument.getValueType());
            }
            recipe.setArguments(arguments);
            recipe.setArgTypes(argTypes);
            recipe.setReorderArguments(!hasIndex);
        }
        recipe.setFactoryMethod(beanMetadata.getFactoryMethod());
        if (beanMetadata.getFactoryComponent() != null) {
            recipe.setFactoryComponent(getValue(beanMetadata.getFactoryComponent(), null));
        }
        for (BeanProperty property : beanMetadata.getProperties()) {
            Recipe value = getValue(property.getValue(), null);
            recipe.setProperty(property.getName(), value);
        }
        return recipe;
    }

    private Recipe createRecipe(RegistrationListener listener) {
        BeanRecipe recipe = new BeanRecipe(getName(null), blueprintContainer, ServiceListener.class, false);
        recipe.setProperty("listener", getValue(listener.getListenerComponent(), null));
        if (listener.getRegistrationMethod() != null) {
            recipe.setProperty("registerMethod", listener.getRegistrationMethod());
        }
        if (listener.getUnregistrationMethod() != null) {
            recipe.setProperty("unregisterMethod", listener.getUnregistrationMethod());
        }
        recipe.setProperty("blueprintContainer", blueprintContainer);
        return recipe;
    }

    private Recipe createRecipe(ReferenceListener listener) {
        BeanRecipe recipe = new BeanRecipe(getName(null), blueprintContainer, AbstractServiceReferenceRecipe.Listener.class, false);
        recipe.setProperty("listener", getValue(listener.getListenerComponent(), null));
        recipe.setProperty("metadata", listener);
        recipe.setProperty("blueprintContainer", blueprintContainer);
        return recipe;
    }

    private Recipe getValue(Metadata v, Object groupingType) {
        if (v instanceof NullMetadata) {
            return null;
        } else if (v instanceof ComponentMetadata) {
            return createRecipe((ComponentMetadata) v);
        } else if (v instanceof ValueMetadata) {
            ValueMetadata stringValue = (ValueMetadata) v;
            Object type = stringValue.getType();
            type = (type == null) ? groupingType : type;
            ValueRecipe vr = new ValueRecipe(getName(null), stringValue, type);
            return vr;
        } else if (v instanceof RefMetadata) {
            // TODO: make it work with property-placeholders?
            String componentName = ((RefMetadata) v).getComponentId();
            RefRecipe rr = new RefRecipe(getName(null), componentName);
            return rr;
        } else if (v instanceof CollectionMetadata) {
            CollectionMetadata collectionMetadata = (CollectionMetadata) v;
            Class cl = collectionMetadata.getCollectionClass();
            Object type = collectionMetadata.getValueType();
            if (cl == Object[].class) {
                ArrayRecipe ar = new ArrayRecipe(getName(null), type);
                for (Metadata lv : collectionMetadata.getValues()) {
                    ar.add(getValue(lv, type));
                }
                return ar;
            } else {
                CollectionRecipe cr = new CollectionRecipe(getName(null), cl != null ? cl : ArrayList.class);
                for (Metadata lv : collectionMetadata.getValues()) {
                    cr.add(getValue(lv, type));
                }
                return cr;
            }
        } else if (v instanceof MapMetadata) {
            return createMapRecipe((MapMetadata) v);
        } else if (v instanceof PropsMetadata) {
            PropsMetadata mapValue = (PropsMetadata) v;
            MapRecipe mr = new MapRecipe(getName(null), Properties.class);
            for (MapEntry entry : mapValue.getEntries()) {
                Recipe key = getValue(entry.getKey(), String.class);
                Recipe val = getValue(entry.getValue(), String.class);
                mr.put(key, val);
            }
            return mr;
        } else if (v instanceof IdRefMetadata) {
            // TODO: make it work with property-placeholders?
            String componentName = ((IdRefMetadata) v).getComponentId();
            IdRefRecipe rnr = new IdRefRecipe(getName(null), componentName);
            return rnr;
        } else {
            throw new IllegalStateException("Unsupported value: " + v.getClass().getName());
        }
    }

    private MapRecipe createMapRecipe(MapMetadata mapValue) {
        String keyType = mapValue.getKeyType();
        String valueType = mapValue.getValueType();
        MapRecipe mr = new MapRecipe(getName(null), HashMap.class);
        for (MapEntry entry : mapValue.getEntries()) {
            Recipe key = getValue(entry.getKey(), keyType);
            Recipe val = getValue(entry.getValue(), valueType);
            mr.put(key, val);
        }
        return mr;
    }

    private String getName(String name) {
        if (name == null) {
            do {
                name = "#recipe-" + recipeIdSpace.nextId();
            } while (names.contains(name) || registry.containsComponentDefinition(name));
        }
        names.add(name);
        return name;
    }

}
