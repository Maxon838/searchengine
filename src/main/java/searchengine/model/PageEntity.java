package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table (name = "page")
public class PageEntity {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "Id", columnDefinition = "INT", nullable = false)
    private int id;

    @ManyToOne (fetch=FetchType.LAZY)
    @JoinColumn (name = "site_id", columnDefinition = "INT", nullable = false)
    private SiteEntity siteId;

    @Column (name = "path", columnDefinition = "TEXT", nullable = false)
    private String pagePath;

    @Column (name = "code", columnDefinition = "INT", nullable = false)
    private int pageResponseHttpCode;

    @Column (name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String pageContentHttpCode;

    @OneToMany (mappedBy = "pageEntity", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<IndexEntity> indexEntityList;

}
