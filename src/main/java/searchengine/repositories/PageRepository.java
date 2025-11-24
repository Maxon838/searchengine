package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    Optional<PageEntity> findByPagePath (String pagePath);
    List<PageEntity> findAllBySiteId (int siteId);
    int countBySiteId(SiteEntity siteId);
}
