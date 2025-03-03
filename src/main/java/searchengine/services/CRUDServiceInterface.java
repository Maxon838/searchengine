package searchengine.services;

import java.util.Collection;

public interface CRUDServiceInterface<T>{

    T getById(Long id);
    Collection<T> getAll();
    void create(T item);
    int update(T item);
    int deleteById(Long id);

}
