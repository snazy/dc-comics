package com.dremio.cloud.comic.api.spi;

import com.dremio.cloud.comic.api.Instance;
import com.dremio.cloud.comic.api.RuntimeContainer;
import com.dremio.cloud.comic.api.container.ComicBean;

/**
 * Defines a context, e.g. a way to define a scope for beans.
 */
public interface ComicContext {

    /**
     * @return the annotation which enables the scope when put on a bean. It must be marked with {@link com.dremio.cloud.comic.api.container.DetectableContext}.
     */
    Class<?> marker();

    /**
     * Lookups an instance of the bean in the context.
     *
     * @param container the related framework.
     * @param bean the bean to lookup.
     * @return the instance.
     * @param <T> the instance type.
     */
    <T> Instance<T> getOrCreate(RuntimeContainer container, ComicBean<T> bean);

}
