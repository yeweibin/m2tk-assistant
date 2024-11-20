/*
 *  Copyright (c) M2TK Project. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package m2tk.assistant.app.kernel;

import lombok.Getter;

@Getter
public class KernelException extends RuntimeException
{
    private final int code;

    public KernelException(int code)
    {
        super("code = " + code);
        this.code = code;
    }

    public KernelException(int code, String message)
    {
        super(message);
        this.code = code;
    }
}
