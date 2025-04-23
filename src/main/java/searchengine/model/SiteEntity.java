package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "site")
public class SiteEntity {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "Id", columnDefinition = "INT", nullable = false)
    private int id;

    @Enumerated (EnumType.STRING)
    @Column (name = "status", columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private Status status;

    @Column (name = "status_time", columnDefinition = "DATETIME", nullable = false)
    @UpdateTimestamp
    private Instant statusTime;

    @Column (name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column (name = "url", columnDefinition = "VARCHAR(255)", nullable = false)
    private String mainPageURL;

    @Column (name = "name", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany (mappedBy = "siteId", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<PageEntity> pageEntityList;

    @OneToMany (mappedBy = "siteId", cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<LemmaEntity> lemmaEntityList;

}
