package br.com.fatec.tcc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes_carona",
       uniqueConstraints = @UniqueConstraint(columnNames = {"carona_id", "passageiro_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AvaliacaoCarona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Carona que está sendo avaliada */
    @ManyToOne
    @JoinColumn(name = "carona_id", nullable = false)
    private Carona carona;

    /** Passageiro que faz a avaliação */
    @ManyToOne
    @JoinColumn(name = "passageiro_id", nullable = false)
    private Usuario passageiro;

    /** Motorista que recebe a avaliação */
    @ManyToOne
    @JoinColumn(name = "motorista_id", nullable = false)
    private Usuario motorista;

    /** Nota de 1 a 5 estrelas */
    @Column(nullable = false)
    private Integer estrelas;

    /** Comentário opcional */
    @Column(length = 500)
    private String comentario;

    @CreatedDate
    private LocalDateTime dataAvaliacao;
}
