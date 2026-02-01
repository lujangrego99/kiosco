package ar.com.kiosco.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cadenas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Cadena {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Usuario owner;

    @OneToMany(mappedBy = "cadena", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Kiosco> kioscos = new ArrayList<>();

    @OneToMany(mappedBy = "cadena", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CadenaMember> members = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void addKiosco(Kiosco kiosco) {
        kioscos.add(kiosco);
        kiosco.setCadena(this);
    }

    public void removeKiosco(Kiosco kiosco) {
        kioscos.remove(kiosco);
        kiosco.setCadena(null);
    }

    public void addMember(CadenaMember member) {
        members.add(member);
        member.setCadena(this);
    }

    public void removeMember(CadenaMember member) {
        members.remove(member);
    }
}
