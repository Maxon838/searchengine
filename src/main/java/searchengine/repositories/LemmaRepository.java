package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    List<LemmaEntity> findAllByLemma (String lemma);
    LemmaEntity findBySiteId (int siteId);
    List<LemmaEntity> findAllBySiteId (int siteId);
    int countBySiteId(SiteEntity siteId);
}
