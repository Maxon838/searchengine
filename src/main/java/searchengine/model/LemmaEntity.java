//package searchengine.model;
//
//import lombok.Getter;
//import lombok.Setter;
//
//import javax.persistence.*;
//import java.util.List;
//
//@Getter
//@Setter
//@Entity
//@Table(name = "lemma")
//public class LemmaEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column (name = "Id", columnDefinition = "INT", nullable = false)
//    private int id;
//
//    @ManyToOne (cascade = CascadeType.ALL)
//    @JoinColumn (name = "site_id", columnDefinition = "INT", nullable = false)
//    private SiteEntity siteEntity;
//
//    @Column (name = "lemma", columnDefinition = "VARCHAR(255)", nullable = false)
//    private String lemma;
//
//    @Column (name = "frequency", columnDefinition = "INT", nullable = false)
//    private int frequency;
//
//    @OneToMany (mappedBy = "lemmaEntity", fetch=FetchType.LAZY)
//    private List<IndexEntity> indexEntityList;
//
//}
