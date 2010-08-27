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
package org.apache.aries.blueprint.pojos;

import java.io.File;

import org.osgi.service.blueprint.container.ReifiedType;
import org.osgi.service.blueprint.container.Converter;

public class ConverterA implements Converter {

    public boolean canConvert(Object fromValue, ReifiedType toType) {
        return fromValue instanceof String && toType.getRawClass() == File.class;
    }

    public Object convert(Object source, ReifiedType toType) throws Exception {
        if (source instanceof String) {
            return new File((String) source);
        }
        throw new Exception("Unable to convert from " + (source != null ? source.getClass().getName() : "<null>") + " to " + File.class.getName());
    }

}
