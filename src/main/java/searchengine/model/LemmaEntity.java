package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "lemma")
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "Id", columnDefinition = "INT", nullable = false)
    private int id;

    @ManyToOne (fetch=FetchType.LAZY)
    @JoinColumn (name = "site_id", columnDefinition = "INT", nullable = false)
    private SiteEntity siteId;

    @Column (name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column (name = "frequency", columnDefinition = "INT", nullable = false)
    private int frequency;

    @OneToMany (mappedBy = "lemmaEntity", cascade = CascadeType.ALL, fetch=FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<IndexEntity> indexEntityList;

}
