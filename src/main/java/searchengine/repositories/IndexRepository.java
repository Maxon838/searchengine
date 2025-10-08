package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    List<IndexEntity> findAllByPageEntity(PageEntity pageEntity);
    List<IndexEntity> findAllByLemmaEntity(LemmaEntity lemmaEntity);
    @Query(value = "SELECT index_table.page_id FROM index_table JOIN lemma ON index_table.lemma_id = lemma.id WHERE lemma = :lemma AND page_id IN :pagesList", nativeQuery = true )
    List<Integer> findPagesWithPagesAndLemmas(@Param("lemma") String lemma, @Param("pagesList") List<Integer> pagesList);
}
