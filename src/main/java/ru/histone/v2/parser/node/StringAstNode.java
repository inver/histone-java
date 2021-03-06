/*
 * Copyright (c) 2016 MegaFon
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

package ru.histone.v2.parser.node;

import java.io.Serializable;

/**
 *
 * Created by gali.alykoff on 20/01/16.
 */
public class StringAstNode extends ValueNode<String> implements Serializable {
    public StringAstNode(String value) {
        super(value);
    }

    public StringAstNode escaped() {
        // TODO
        return this;
    }

    @Override
    public String toString() {
        return "{\"StringAstNode\": {\"value\": \"" + value + "\"}}";
    }
}
