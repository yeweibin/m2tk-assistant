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

public final class ErrorCode
{
    public static final int OK = 200;
    public static final int QUARANTINE_CHECK_ERROR = 10010;
    public static final int USER_IDENTITY_MISMATCH = 10012;
    public static final int TOO_MANY_UPLOADS = 10014;
    public static final int BAD_REQUEST = 10400;
    public static final int UNAUTHORIZED = 10401;
    public static final int NOT_FOUND = 10404;
    public static final int METHOD_NOT_ALLOWED = 10405;
    public static final int REJECTED = 10409;
    public static final int TOO_MANY_REQUEST = 10429;
    public static final int SYSTEM_BUSY = 10430;
    public static final int INTERNAL_ERROR = 10500;
    public static final int SERVICE_UNAVAILABLE = 10503;
    public static final int DATABASE_ERROR = 11000;
    public static final int INTERNAL_DATA_ERROR = 11001;
}
