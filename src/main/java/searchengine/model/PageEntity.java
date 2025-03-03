package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table (name = "page")//, indexes = @IndexEntity(columnList = "path"))
public class PageEntity {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "Id", columnDefinition = "INT", nullable = false)
    private int id;

    @ManyToOne
    @JoinColumn (name = "site_id", columnDefinition = "INT", nullable = false)
    private SiteEntity siteId;

    @Column (name = "path", columnDefinition = "TEXT", nullable = false)
    private String pagePath;

    @Column (name = "code", columnDefinition = "INT", nullable = false)
    private int pageResponseHttpCode;

    @Column (name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String pageContentHttpCode;

//    @OneToMany (mappedBy = "pageEntity", fetch=FetchType.LAZY)
//    private List<IndexEntity> indexEntityList;

}
