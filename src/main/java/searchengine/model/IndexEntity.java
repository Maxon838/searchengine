package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "index_table")
public class IndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "Id", columnDefinition = "INT", nullable = false)
    private int id;

    @ManyToOne (fetch=FetchType.LAZY)
    @JoinColumn(name = "page_id", columnDefinition = "INT", nullable = false)
    private PageEntity pageEntity;

    @ManyToOne (fetch=FetchType.LAZY)
    @JoinColumn(name = "lemma_id", columnDefinition = "INT", nullable = false)
    private LemmaEntity lemmaEntity;

    @Column(name = "rank_field", columnDefinition = "FLOAT", nullable = false)
    private int rank;

}
