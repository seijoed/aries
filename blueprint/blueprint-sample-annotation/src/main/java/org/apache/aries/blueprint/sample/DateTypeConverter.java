/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.blueprint.sample;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.aries.blueprint.annotation.Bean;
import org.apache.aries.blueprint.annotation.Inject;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

@Bean(id="converter1")
public class DateTypeConverter implements Converter {

    @Inject(name="format", value="yyyy.MM.dd")
    DateFormat dateFormat;
    
    public void setFormat(String format) {
        this.dateFormat = new SimpleDateFormat(format);
    }
    
    public Object convert(Object source, ReifiedType toType) throws Exception {
        return this.dateFormat.parse(source.toString());
    }

    public boolean canConvert(Object fromValue, ReifiedType toType) {
        return Date.class.isAssignableFrom(toType.getRawClass());
    }

}
