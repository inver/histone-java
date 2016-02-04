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

package ru.histone.v2.evaluator.node;

import ru.histone.v2.rtti.HistoneType;

import static ru.histone.v2.rtti.HistoneType.T_NUMBER;
import static ru.histone.v2.rtti.HistoneType.T_UNDEFINED;

/**
 * Created by inv3r on 19/01/16.
 */
public class FloatEvalNode extends EvalNode<Float> {
    public FloatEvalNode(Float val) {
        super(val);
        if (val == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public HistoneType getType() {
        if (value == null || Float.isNaN(value) || !Float.isFinite(value)) {
            return T_UNDEFINED;
        }
        return T_NUMBER;
    }
}
