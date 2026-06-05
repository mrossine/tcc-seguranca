package br.com.fatec.tcc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Reação de um usuário a um alerta (LIKE ou DISLIKE).
 * Um usuário pode ter no máximo uma reação por alerta — a chave única
 * garante isso no banco. Reaplicar o mesmo tipo remove a reação (toggle).
 */
@Entity
@Table(name = "alerta_reacoes",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_alerta_usuario",
               columnNames = {"alerta_id", "usuario_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AlertaReacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "alerta_id", nullable = false)
    private Alerta alerta;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(10)")
    private TipoReacao tipo;

    @CreatedDate
    private LocalDateTime dataCriacao;

    public enum TipoReacao {
        LIKE, DISLIKE
    }
}
