/*
 * Copyright (c) M2TK Project. All rights reserved.
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

package m2tk.assistant.app.ui.task;

import org.jdesktop.application.Application;
import org.jdesktop.application.Task;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsyncQueryTask<V> extends Task<V, Void>
{
    private final Supplier<V> action;
    private final Consumer<V> consumer;
    private final Consumer<Throwable> exceptionConsumer;

    public AsyncQueryTask(Application application, Supplier<V> action, Consumer<V> consumer)
    {
        super(application);
        this.action = Objects.requireNonNull(action);
        this.consumer = Objects.requireNonNull(consumer);
        this.exceptionConsumer = t -> {};
    }

    public AsyncQueryTask(Application application, Supplier<V> action, Consumer<V> consumer, Consumer<Throwable> exceptionConsumer)
    {
        super(application);
        this.action = Objects.requireNonNull(action);
        this.consumer = Objects.requireNonNull(consumer);
        this.exceptionConsumer = Objects.requireNonNull(exceptionConsumer);
    }

    @Override
    protected V doInBackground()
    {
        return action.get();
    }

    @Override
    protected void succeeded(V result)
    {
        consumer.accept(result);
    }

    @Override
    protected void failed(Throwable cause)
    {
        exceptionConsumer.accept(cause);
    }
}
